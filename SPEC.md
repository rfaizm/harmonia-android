# Spec — finding the right lyrics for files with no artist tag

Commands, project layout and code style are in `CLAUDE.md`; the task list is in `tasks/plan.md`. This spec covers
one problem and the options for solving it.

## Objective

Lyrics lookup fails on this library because most files carry no artist tag, so the app searches for an artist
literally named "Unknown artist", and the sheet shows "Unknown artist" as the song's subtitle. The aim is that a
song gets **its own** lyrics without the user having to tag files first, and that a song we can't identify says so
instead of guessing.

## What the evidence says (measured against the live service)

| Query | Result |
|---|---|
| `track_name=Yellow&artist_name=Unknown artist` | 4 results, none of them the song |
| `track_name=Yellow` (title only) | 20 results, **all with lyrics**, top ones are "Yellow Yellow" by FANTASTICS and Vybz Kartel |
| Same, filtered to ±3 s of the song's 269 s | **0 results** — the right song isn't in the top 20 at all |
| `track_name=Yellow&artist_name=Coldplay` | first result is Coldplay, 268 s ✔ |

**Critical, and true of the build on the phone right now:** with no artist, the app searches by title, takes the
first result that has lyrics, and shows it. For a tagless file that is usually **another artist's song**, displayed
as if it were correct. Silently wrong is worse than "no lyrics found", so whatever else is chosen, the lookup must
stop trusting a title-only match.

## Where an artist can come from

| # | Source | Cost | Covers | Risk |
|---|---|---|---|---|
| **S1** | **The file name**, e.g. `Coldplay - Yellow.mp3` → artist `Coldplay`, title `Yellow`. `Song.fileName` is already stored (T29). | S, pure and testable | Most downloaded files | `Track 01 - Yellow.mp3` gives a nonsense artist; mitigated by S3 |
| **S2** | **Tags the player itself read.** ExoPlayer parses the file's own tags, and MediaStore sometimes misses what it finds (odd encodings, never rescanned). The service already publishes lyrics this way, so the plumbing exists. | S | Files whose tags MediaStore failed on, not files that genuinely have none | None; it is strictly extra information |
| **S3** | **Verify before showing**: accept a result only if its duration is within ±3 s **and** its title resembles ours; otherwise treat it as not found. | S, pure and testable | Every lookup | Slightly fewer hits, no wrong ones |
| **S4** | **Let the user choose** from the candidates (artist, title, album, length); remember the choice for that song. | M, new sheet UI | Everything S1–S3 can't settle | None, but it asks the user to decide |
| **S5** | **Let the user type** the artist and title to search with, for badly named files. | S–M | The long tail | None |
| **S6** | **The folder name**, e.g. `Music/Coldplay/Yellow.mp3`. Needs `RELATIVE_PATH` from MediaStore, which `Song` doesn't keep yet. | M | Libraries filed by artist | Compilation folders give the wrong artist |
| **S7** | **Write the tags back** into the files once identified, so everything else benefits. | L | Future lookups, other apps | Editing the user's files; needs its own consent flow |

## Recommended build order

**S3 first**, because it is the safety fix: it stops the app showing another artist's lyrics, and it makes every
later option safe to try automatically.

1. **S3 — verify the match.** A result is used only when its length is within ±3 s of the song and its title is a
   close match. Anything else is "no lyrics found".
   - Acceptance: a tagless "Yellow" no longer shows FANTASTICS' lyrics; a correct match still shows.
2. **S1 — read the file name.** `Artist - Title.mp3` splits on the first " - "; the parts are cleaned by the
   existing `cleanTag`. Used only when the tag is missing.
   - Acceptance: `Coldplay - Yellow.mp3` with no tags finds Coldplay's lyrics; `Track 01 - Yellow.mp3` doesn't
     show a wrong result (S3 rejects it).
3. **S2 — take the player's tags.** When MediaStore says unknown and the player knows better, use the player's.
   - Acceptance: a file whose artist only the player can read finds its lyrics.
4. **S4 — a chooser** when the automatic path finds nothing: a list of candidates to tap, remembered per song.
   - Acceptance: picking a candidate shows its lyrics and survives reopening the app.
5. **S5 — a "search with…" field** in that same sheet, prefilled with the current guess.

Cosmetic, worth doing with S1: the sheet shows "Unknown artist" as the subtitle. It should show the file name
instead, which is what the user recognises.

## Testing strategy

- Pure functions get unit tests, as everywhere else in this project: splitting a file name, scoring a candidate,
  and the ±3 s rule.
- The DTO mapping is tested with the same Gson the app uses, so tests can't drift from the phone.
- Networking, gestures and MediaStore are checked on the phone; the JVM tests deliberately don't fake them.

## Boundaries

- **Never fetch automatically.** The lookup happens only when the user taps, and only with the setting on.
- **Never show a result the app isn't reasonably sure about** — no lyrics beats another song's lyrics.
- **Send nothing beyond the song's own tags**: title, artist, album, length.
- **Don't modify the user's files** (S7) without a separate, explicit consent flow.
- Keep the offline sources first: tags in the file, then a `.lrc` beside it, then anything already fetched.

## To decide

- Which of S1–S7 to build, and in what order.
- Whether an automatic guess from the file name is welcome at all, or whether the app should ask first.
