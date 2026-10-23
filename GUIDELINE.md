# Harmonia — Design System & Guidelines
> Local Music Player App · Material Design 3 spirit · Muted Earth Tones

---

## 1. Design Philosophy

**Stance:** Minimalist-Calm
The app is designed to feel like a quiet room — nothing competes for attention, nothing startles. Every visual decision is made to reduce cognitive load and create a sense of ease. Inspired by the restraint of Aesop product pages and the warmth of Kinfolk editorial design, applied to a mobile music player context.

**Principle:** Committed earth-tone minimalism. One primary action per screen. Generous whitespace. Soft gradients replace photography. Type does the heavy lifting.

---

## 2. Color Palette

### Light Mode
| Token | Hex | Usage |
|---|---|---|
| `--background` | `#f5f4f0` | Page background — warm off-white, not pure white |
| `--foreground` | `#2c2c2c` | Primary text — near-black with warmth |
| `--card` | `#ffffff` | Cards, bottom nav, search bar surface |
| `--card-foreground` | `#2c2c2c` | Text on cards |
| `--primary` | `#5a7a6a` | Sage green — main interactive color, active nav, pills, toggles |
| `--primary-foreground` | `#ffffff` | Text/icons on primary-colored surfaces |
| `--secondary` | `#c9c3d8` | Soft lavender — secondary surfaces |
| `--secondary-foreground` | `#2c2c2c` | Text on secondary |
| `--muted` | `#e8e6e0` | Muted backgrounds, toggle track off, sort pill inactive |
| `--muted-foreground` | `#7a7870` | Captions, labels, inactive icons, timestamps |
| `--accent` | `#3d6b7a` | Deep ocean blue — secondary accent, used sparingly |
| `--accent-foreground` | `#ffffff` | Text on accent |
| `--destructive` | `#c0392b` | Delete, error states |
| `--border` | `rgba(90,122,106,0.15)` | Hairline dividers — tinted with primary green |
| `--ring` | `#5a7a6a` | Focus rings |
| `--radius` | `0.625rem` | Base radius (10px) |

### Dark Mode
| Token | Hex | Usage |
|---|---|---|
| `--background` | `#1e2022` | Soft charcoal — OLED-friendly, not pure black |
| `--foreground` | `#e8e6e0` | Warm off-white text |
| `--card` | `#272b2d` | Card / nav surface — slightly lighter than background |
| `--primary` | `#7aab8a` | Lighter sage green — passes AA contrast on dark |
| `--primary-foreground` | `#1a2620` | Very dark green for text on primary |
| `--secondary` | `#4a4462` | Muted deep lavender |
| `--secondary-foreground` | `#e8e6e0` | Text on secondary |
| `--muted` | `#2e3235` | Muted surface |
| `--muted-foreground` | `#9a9890` | Warm gray for captions |
| `--accent` | `#5a8fa0` | Lighter ocean blue |
| `--destructive` | `#e57373` | Soft red for dark mode |
| `--border` | `rgba(122,171,138,0.15)` | Hairline — tinted green |
| `--ring` | `#7aab8a` | Focus rings |

### Album Art Gradients (12 presets, auto-assigned by song index % 12)
All gradients are dark-to-darker monochromatic earth tones. They serve as album art placeholders and full-player backgrounds.

```
0.  #4e7c65 → #2e5a48  (forest green)
1.  #3a6878 → #1e4a5c  (deep teal)
2.  #6a5c7e → #4a3e5e  (muted plum)
3.  #5e7050 → #3e5030  (olive)
4.  #7a5e52 → #5a3e32  (terracotta)
5.  #4a607a → #2a4060  (slate blue)
6.  #6e4a62 → #4e2a42  (dusty rose-plum)
7.  #587a68 → #385a48  (sage)
8.  #7a6a4e → #5a4a2e  (warm khaki)
9.  #486878 → #285060  (ocean)
10. #5a4e7a → #3a2e5a  (soft violet)
11. #688060 → #486040  (moss)
```

### Special Gradients
- **Liked Songs card:** `#c0392b → #7a1a2e` (deep red)
- **Full player background:** `linear-gradient(180deg, gradient[0] 0%, gradient[1] 45%, #141618 100%)` — fades to near-black for cinematic depth

---

## 3. Typography

**Font Family:** Nunito (Google Fonts)
**Weights used:** 400, 500, 600, 700, 800
**CSS import:**
```css
@import url('https://fonts.googleapis.com/css2?family=Nunito:wght@400;500;600;700;800&family=Nunito+Sans:wght@300;400;500;600&display=swap');
```
**Root font stack:** `fontFamily: "'Nunito', 'Nunito Sans', sans-serif"` applied inline on the root div.

Nunito is a rounded geometric sans-serif. It reads warm and calm without feeling playful. It pairs well with the earth tone palette.

| Role | Tailwind size | Weight | Extra classes |
|---|---|---|---|
| App name "Harmonia" | `text-xl` | 800 | `tracking-tight` |
| Section header (Playlists, Settings h2) | `text-base` | 700 | — |
| Song title (list row) | `text-sm` | 600 semibold | `truncate` |
| Artist name (list row) | `text-xs` | 400 | `text-muted-foreground truncate` |
| Letter divider | `text-[11px]` | 800 | `uppercase tracking-widest text-primary` |
| Duration / timestamp | `text-xs` | 400 | `tabular-nums text-muted-foreground/70` |
| Bottom nav label | `text-[10px]` | 700 bold | `tracking-wide` |
| Settings section heading | `text-[10px]` | 900 black | `uppercase tracking-[0.14em] text-primary` |
| Full player title | `text-xl` | 800 | `text-white` |
| Full player artist | `text-sm` | 400 | `text-white/60` |
| "NOW PLAYING" label | `text-[10px]` | 700 | `uppercase tracking-widest text-white/50` |
| Pill / badge text | `text-xs` | 600 semibold | — |
| Play count | `text-xs` | 400 | `text-white/40` |

---

## 4. Spacing & Layout

- **App max-width:** 430px, `margin: 0 auto`, `position: relative`
- **Root div:** `size-full flex flex-col bg-background text-foreground overflow-hidden select-none`
- **Top app bar padding:** `px-5 pt-5 pb-2`
- **Standard content padding:** `px-4` horizontal, `px-2` for list container (rows have their own `px-4`)
- **Card border radius:** `rounded-2xl` (16px) — dominant shape language
- **Small radius:** `rounded-xl` (12px) — album art thumbnails, icon containers
- **Full radius:** `rounded-full` — pills, toggle thumbs, circle buttons
- **List row height:** ~68px driven by `py-2.5` + `h-12` art
- **Section gap:** `mb-5` between settings sections, `mb-3` between playlist cards
- **Sticky headers:** `backdrop-blur-md bg-background/96` + `sticky top-0 z-10`

---

## 5. Component Specifications

### 5.1 Top App Bar
- Layout: `flex items-center justify-between` inside `px-5 pt-5 pb-2`
- Left: `<h1>` "Harmonia" + `<p>` subtitle `{n} songs · local library` (`text-[11px] text-muted-foreground mt-0.5`)
- Right: dark mode toggle — `w-9 h-9 rounded-full bg-card border border-border`, Sun/Moon at 16px `strokeWidth={2}`, hover `bg-primary/10`
- No shadow, no bottom border — blends into background

### 5.2 Bottom Navigation Bar
- Container: `bg-card/95 backdrop-blur-md border-t border-border`
- Inner: `<div className="flex">` — 4 equal flex children
- Each tab: `flex-1 flex flex-col items-center pt-2 pb-3 gap-0.5`
- Active indicator: icon inside `motion.div` that animates `scale: 0.92↔1`, wrapped in `px-4 py-1 rounded-full bg-primary/15` (active only)
- Active icon: `color: var(--primary)`, `strokeWidth={2.2}`
- Inactive icon: `color: var(--muted-foreground)`, `strokeWidth={1.6}`
- Label: `text-[10px] font-bold tracking-wide` — `text-primary` (active) / `text-muted-foreground` (inactive)

### 5.3 Search Bar
- Container: `flex items-center gap-2.5 bg-card rounded-2xl px-3.5 py-2.5 border border-border shadow-sm`
- Icon: `Search` 16px, `color: var(--muted-foreground)`, `strokeWidth={2}`
- Input: `flex-1 bg-transparent outline-none text-sm text-foreground placeholder:text-muted-foreground`
- Clear button: animated with `AnimatePresence` (`scale: 0.7→1, opacity: 0→1`) — small `X` inside `bg-muted rounded-full p-0.5`

### 5.4 Sort Pills (Songs tab)
- Row: `flex gap-2` below search bar
- Each pill: `px-3 py-1 rounded-full text-xs font-semibold transition-colors`
- Active: `bg-primary text-primary-foreground`
- Inactive: `bg-muted text-muted-foreground hover:bg-muted/80`
- Three options: "A–Z", "Recent", "Most played"

### 5.5 Song List Row
- Wrapper: `relative flex items-center gap-3 px-4 py-2.5 rounded-2xl cursor-pointer transition-colors select-none`
- Active row bg: `bg-primary/10` — title switches to `text-primary`
- Hover: `hover:bg-muted/60 active:bg-muted`
- **Left — Album Art:**
  - `w-12 h-12 shrink-0 rounded-xl`
  - Background: `linear-gradient(140deg, gradient[0], gradient[1])`
  - Icon: `Music2` at `size * 0.36 = ~17px`, `color: rgba(255,255,255,0.55)`, `strokeWidth={1.6}`
- **Middle:**
  - Title: `text-sm font-semibold truncate leading-tight` — `text-primary` if active, else `text-foreground`
  - Artist: `text-xs text-muted-foreground truncate mt-0.5`
  - Both strings pass through `cleanTag()` regex pipeline
- **Right:**
  - Duration: `text-xs text-muted-foreground/70 tabular-nums mr-1.5`
  - Heart: `p-1.5 rounded-full hover:bg-primary/10` — see §5.6
  - More: `p-1.5 rounded-full hover:bg-primary/10` — opens ContextMenu
- **Entry animation:** `motion.div layout`, `initial: {opacity:0, y:8}`, `animate: {opacity:1, y:0}`, `transition: {delay: Math.min(index*0.03, 0.3)}`

### 5.6 Heart Button
- 15px icon (row) / 18px icon (full player)
- **Liked:** `color: #e05c5c, fill: #e05c5c, strokeWidth: 0`
- **Not liked:** `color: var(--muted-foreground), fill: none, strokeWidth: 1.8`

### 5.7 Context Menu (3-dot)
- Position: `absolute right-4 z-50` from the MoreVertical button's relative parent
- Container: `w-52 bg-card border border-border rounded-2xl shadow-xl overflow-hidden`
- Opens at `top: 40px`
- Animation: `scale: 0.92→1, opacity: 0→1, y: -8→0`, duration 0.15s
- Dismiss: invisible `fixed inset-0 z-40` div behind menu
- Header: `px-4 py-3 border-b border-border` — song title + artist `text-xs`
- Playlist section: heading `text-[10px] font-bold text-primary uppercase tracking-widest`, then one button per playlist `text-sm text-foreground px-4 py-2 hover:bg-primary/8`

### 5.8 Alphabetical Letter Divider
```jsx
<div className="flex items-center gap-2 px-4 py-1 mt-2">
  <span className="text-[11px] font-extrabold text-primary tracking-widest uppercase">{letter}</span>
  <div className="flex-1 h-px bg-border" />
</div>
```

### 5.9 Toggle Switch
- Track: `relative w-11 h-6 rounded-full transition-colors`
  - On: `bg-primary` / Off: `bg-muted`
- Thumb: `absolute top-0.5 w-5 h-5 rounded-full bg-white shadow transition-transform`
  - On: `translate-x-[22px]` / Off: `translate-x-0.5`
- Also: `role="switch" aria-checked={value}`, `focus-visible:ring-2 focus-visible:ring-ring`

### 5.10 Settings Row
- Layout: `flex items-center gap-3 px-4 py-3.5`
- Icon container: `w-8 h-8 rounded-xl bg-primary/10 flex items-center justify-center shrink-0`
- Icon: 15px, `color: var(--primary)`, `strokeWidth={1.8}`
- Label: `text-sm font-medium text-foreground`
- Sub-label: `text-xs text-muted-foreground mt-0.5 truncate`
- Section wrapper: `bg-card rounded-2xl border border-border overflow-hidden divide-y divide-border`
- Section heading above: `text-[10px] font-black text-primary uppercase tracking-[0.14em] px-1 mb-2`

---

## 6. Playlist System

### Liked Songs Card
- Width: full
- Background: `linear-gradient(135deg, #c0392b, #7a1a2e)`
- Container: `rounded-2xl overflow-hidden cursor-pointer hover:opacity-95`
- Layout: `p-4 flex items-center gap-4`
- Icon area: `w-14 h-14 rounded-xl bg-white/15` with `Heart` 26px `fill="white" strokeWidth={0}`
- Text: name `font-bold text-white`, count `text-sm text-white/65`
- Chevron: `ChevronRight` 18px `rgba(255,255,255,0.5)` `ml-auto`

### Playlist Card (list)
- Container: `bg-card border border-border rounded-2xl overflow-hidden cursor-pointer hover:shadow-md transition-shadow`
- **Art strip:** `h-20 relative flex items-end px-4 pb-3`
  - Background: `linear-gradient(140deg, gradient[0], gradient[1])`
  - Mini previews: up to 4 × `w-8 h-8 rounded-lg border border-white/30` with their own gradient
  - If empty: single `w-8 h-8 rounded-lg bg-white/15` with Music2
- **Meta:** `px-4 py-3 flex items-center justify-between`
  - Name: `text-sm font-semibold text-foreground`
  - Count + date: `text-xs text-muted-foreground`
  - Play button: `p-1.5 rounded-full bg-primary/10 hover:bg-primary/20` with `PlayCircle` 18px
  - Delete: `p-1.5 rounded-full hover:bg-destructive/10` with `Trash2` 15px

### Playlist Detail View
- Replaces list via conditional state (not a route)
- **Header section:** `px-4 pt-10 pb-5`, gradient background `gradient[0] → gradient[1]`
  - Back button: `<ChevronDown className="rotate-90" />` + "Back" text, `text-white/80 text-sm mb-4`
  - Art block: `w-20 h-20 rounded-2xl bg-white/20` with `ListMusic` 36px `strokeWidth={1.4}` white
  - Name: `text-lg font-bold text-white`
  - Meta: `text-sm text-white/65`, `text-xs text-white/45`
  - Play button: white pill `px-4 py-2 rounded-full`, icon+text, icon color = `gradient[0]`
- **Song list:** SongRow with `showIndex=true` (shows track number or soundbar animation instead of album art)

### Create Playlist Form
- Animated with `AnimatePresence`: `height: 0→auto, opacity: 0→1` (overflow-hidden wrapper)
- Input: `flex-1 bg-transparent outline-none text-sm`, placeholder "Playlist name…"
- Confirm: `bg-primary text-primary-foreground p-1.5 rounded-full` with `Check` 13px `strokeWidth={2.5}`
- Cancel: `bg-muted p-1.5 rounded-full` with `X` 13px
- Keyboard: Enter → save & close, Esc → cancel

---

## 7. Mini Player

Position: between main content and bottom nav, `mx-3 mb-2`.

- **Container:** `rounded-2xl overflow-hidden cursor-pointer active:scale-[0.98] transition-transform`
- **Background:** `linear-gradient(110deg, gradient[0]f0, gradient[1]f0)` + `backdropFilter: blur(12px)`
- **Shadow:** `0 8px 32px gradient[1]60`
- **Content row:** `flex items-center gap-3 px-3 py-2.5`
  - Album art: 42px, `className="shadow-sm"`
  - Title: `text-sm font-bold text-white truncate`
  - Artist: `text-xs text-white/65 truncate`
  - Play/Pause: `w-9 h-9 rounded-full bg-white/15 hover:bg-white/25` — icon 17px white fill
  - Skip forward: `w-9 h-9 rounded-full bg-white/10 hover:bg-white/20`
- **Progress bar:** `h-[2px] bg-white/15` full width (no margin), fill `bg-white/60 rounded-full`
- **Entry animation:** spring stiffness 340, damping 30, `y: 56→0, opacity: 0→1`
- Tap anywhere (except control buttons) → opens Full Player

---

## 8. Full Player Sheet

Covers full screen via `fixed inset-0 z-50`.

**Entry animation:** spring stiffness 300, damping 34, `y: "100%"→0`

**Background:** `linear-gradient(180deg, gradient[0] 0%, gradient[1] 45%, #141618 100%)`

### Layout (top to bottom, all `shrink-0`)
1. **Top bar** (`px-5 pt-5 pb-2 flex items-center justify-between`):
   - Left: `w-9 h-9 rounded-full bg-white/10 hover:bg-white/20` → `ChevronDown` 20px white `strokeWidth={2}` → closes sheet
   - Center: "NOW PLAYING" (`text-[10px] font-bold text-white/50 uppercase tracking-widest`)
   - Right: same circle button → `Heart` 18px → toggles like

2. **Album art** (`flex justify-center mt-4 mb-7 px-10`):
   - `motion.div key={song.id}` — re-mounts on song change
   - Initial: `scale: 0.82, opacity: 0` — animate to `scale: isPlaying ? 1 : 0.88, opacity: 1`
   - Spring: stiffness 240, damping 22
   - Shape: `w-full aspect-square rounded-3xl`
   - Background: gradient, box-shadow: `0 24px 64px gradient[1]80`
   - Icon: `Music2` 80px, `rgba(255,255,255,0.45)`, `strokeWidth={1.2}`

3. **Song info** (`px-7 mb-5 flex items-start justify-between`):
   - Left: title `text-xl font-extrabold text-white leading-tight truncate` + artist/album `text-sm text-white/60 mt-0.5`
   - Right: `Star` 12px + count number, `text-white/40` (play count badge)

4. **Progress scrubber** (`px-7 mb-5`):
   - Track: `h-1 bg-white/20 rounded-full cursor-pointer group` — `ref` attached, onClick calculates pct
   - Fill: `bg-white rounded-full h-full`
   - Hover thumb: `absolute right-0 top-1/2 -translate-y-1/2 w-3 h-3 bg-white rounded-full shadow opacity-0 group-hover:opacity-100`
   - Time labels: `flex justify-between mt-1.5`, `text-[11px] text-white/40 tabular-nums`

5. **Controls row** (`px-7 flex items-center justify-between mb-7`):
   - Shuffle: `p-2`, opacity `100` (active) / `35` (inactive). Active indicator: `w-1 h-1 rounded-full bg-white mx-auto mt-0.5`
   - SkipBack: `p-2`, `SkipBack` 28px `fill="white" strokeWidth={0}`, `opacity-80 hover:opacity-100`
   - Play/Pause: `w-16 h-16 rounded-full bg-white shadow-xl hover:scale-105 active:scale-95 transition-transform` — icon 26px `color: gradient[0] fill: gradient[0] strokeWidth={0}`, Play has `className="ml-0.5"`
   - SkipForward: same as SkipBack
   - Repeat: same pattern as Shuffle (Repeat / Repeat1 based on `repeatMode === 2`)

6. **Volume** (`px-7 flex items-center gap-3`):
   - Toggle mute: `Volume2` / `VolumeX` 18px `strokeWidth={1.8}` white, `opacity-60 hover:opacity-100`
   - Slider: `<input type="range">` `flex-1 h-1 cursor-pointer`, `accentColor: "white"`

---

## 9. Artists / Albums Tab

### Segmented Control
```jsx
<div className="flex p-1 bg-muted rounded-2xl">
  // for each option:
  <button className={`flex-1 py-1.5 text-sm font-semibold rounded-xl capitalize transition-all ${
    active ? "bg-card text-foreground shadow-sm" : "text-muted-foreground"
  }`}>
```

### Artists List
- Tab switch animation: `x: -12→0` (enter) / `x: 0→12` (exit), `opacity: 0→1`, duration 0.18s
- Container: `px-4 divide-y divide-border`
- Each row: `flex items-center gap-3.5 py-3`, hover `bg-primary/5 rounded-2xl px-2 -mx-2`
- Avatar: `w-12 h-12 rounded-full` gradient background, `Mic2` 20px `rgba(255,255,255,0.7)` `strokeWidth={1.5}`
- Name: `text-sm font-semibold text-foreground`
- Count: `text-xs text-muted-foreground`
- Right: `ChevronRight` 16px muted `strokeWidth={1.5}`
- Row entry: `opacity: 0→1`, stagger `delay: i*0.03`

### Albums Grid
- Tab switch animation: `x: 12→0` (enter) / `x: 0→-12` (exit)
- Layout: `grid grid-cols-2 gap-3 px-4 pt-2`
- Card entry: `opacity: 0→1, y: 10→0`, stagger `delay: i*0.04`
- Card: `bg-card border border-border rounded-2xl overflow-hidden cursor-pointer hover:shadow-md transition-shadow`
- Art: `h-28` gradient bg, `Disc3` 44px `rgba(255,255,255,0.55)` `strokeWidth={1.2}`
- Meta: `p-2.5 pb-3`
  - Album: `text-xs font-bold text-foreground truncate`
  - Artist: `text-[11px] text-muted-foreground truncate`
  - Year/tracks: `text-[10px] text-muted-foreground/55 mt-0.5`

---

## 10. Motion System

All animations use `motion/react` (Framer Motion v11+). Import from `motion/react`, NOT `framer-motion`.

| Trigger | Config |
|---|---|
| Tab switch | `opacity: 0→1`, duration `0.16s`, `AnimatePresence mode="wait"` |
| Song row entry | `opacity:0→1, y:8→0`, delay `Math.min(index*0.03, 0.3)` |
| Context menu open | `scale:0.92→1, opacity:0→1, y:-8→0`, duration `0.15s` |
| Context menu close | reverse of open |
| Mini player enter | Spring stiffness 340, damping 30, `y:56→0, opacity:0→1` |
| Mini player exit | `y:56, opacity:0` |
| Full player enter | Spring stiffness 300, damping 34, `y:"100%"→0` |
| Full player exit | `y:"100%"` |
| Album art (playing ↔ paused) | Spring stiffness 240, damping 22, `scale: 0.88↔1` |
| Album art (song change) | `key={song.id}` re-mounts, initial `scale:0.82, opacity:0` |
| Create playlist form | `height:0→auto, opacity:0→1`, `overflow:hidden` |
| Artists/Albums switch | `x:±12, opacity:0→1`, duration `0.18s` |
| Nav pill scale | `scale: 0.92` (inactive) ↔ `1` (active) |
| Search clear button | `scale:0.7→1, opacity:0→1` |
| Playlist create row | `opacity:0→1, height:0→auto` |

### Soundbar Animation (active track in playlist detail)
```css
@keyframes soundbar {
  from { transform: scaleY(0.3); }
  to   { transform: scaleY(1); }
}
```
Three bars: `w-0.5 bg-primary rounded-full h-full`, delays `0s / 0.15s / 0.30s`, `alternate infinite`, duration `0.8s ease-in-out`.

---

## 11. Text Cleaning (Regex)

Applied to all song titles and artist names before rendering:

```ts
function cleanTag(str: string) {
  return str
    .replace(/[\[\(].*?[\]\)]/g, "")                       // strip [brackets] and (parens)
    .replace(/\b(feat|ft|prod|vs)\b\.?\s+\S+/gi, "")       // strip feat./ft. credits
    .replace(/[-_]+/g, " ")                                  // dashes and underscores → space
    .replace(/\s{2,}/g, " ")                                // collapse whitespace
    .trim();
}
```

---

## 12. Data Models

```ts
interface Song {
  id: string;
  title: string;         // raw tag — cleaned on render
  artist: string;        // raw tag — cleaned on render
  album: string;
  year: number;
  duration: number;      // seconds — displayed as M:SS
  gradient: readonly [string, string];  // index into GRADIENTS[]
  liked: boolean;
  playCount: number;     // increments on play, shown as ★ N in full player
}

interface Playlist {
  id: string;
  name: string;
  songIds: string[];     // ordered array of song IDs
  gradient: readonly [string, string];
  createdAt: string;     // ISO date string YYYY-MM-DD
}
```

---

## 13. Icon Reference

All from `lucide-react`. Import individually.

| Purpose | Icon name | Size | strokeWidth |
|---|---|---|---|
| Bottom nav: Songs | `Music2` | 22 | 1.6 / 2.2 active |
| Bottom nav: Playlists | `ListMusic` | 22 | same |
| Bottom nav: Artists | `Users2` | 22 | same |
| Bottom nav: Settings | `Settings2` | 22 | same |
| Album art placeholder | `Music2` | `size*0.36` | 1.6 |
| Search | `Search` | 16 | 2 |
| Clear search | `X` | 13 | default |
| Like (row) | `Heart` | 15 | 1.8 / 0 filled |
| Like (player) | `Heart` | 18 | 1.8 / 0 filled |
| 3-dot menu | `MoreVertical` | 15 | 1.8 |
| Artist avatar | `Mic2` | 20 | 1.5 |
| Album grid art | `Disc3` | 44 | 1.2 |
| Playlist header art | `ListMusic` | 36 | 1.4 |
| Liked songs | `Heart` | 26 | 0 (filled white) |
| Play (general) | `Play` | 14–28 | 0 (filled) |
| Pause | `Pause` | 17–26 | 0 (filled) |
| Skip back | `SkipBack` | 28 | 0 (filled) |
| Skip forward | `SkipForward` | 28 | 0 (filled) |
| Shuffle | `Shuffle` | 20 | 1.8 |
| Repeat all | `Repeat` | 20 | 1.8 |
| Repeat one | `Repeat1` | 20 | 1.8 |
| Volume on | `Volume2` | 18 | 1.8 |
| Volume muted | `VolumeX` | 18 | 1.8 |
| Close full player | `ChevronDown` | 20 | 2 |
| Back in playlist | `ChevronDown` | 18 | default, `rotate-90` |
| Dark / light mode | `Moon` / `Sun` | 16 | 2 |
| New playlist | `Plus` | 13 | 2.5 |
| Confirm | `Check` | 13 | 2.5 |
| Dismiss | `X` | 13 | default |
| Play count | `Star` | 12 | 1.5 |
| Nav forward | `ChevronRight` | 16–18 | 1.5 |
| Play playlist | `PlayCircle` | 18 | default |
| Delete playlist | `Trash2` | 15 | default |
| Settings: filters | `SlidersHorizontal` | 15 | 1.8 |
| Settings: scan | `Clock` | 15 | 1.8 |
| Settings: rate | `Star` | 15 | 1.8 |
| Settings: info | `Info` | 15 | 1.8 |
| Settings: wifi | `Wifi` | 15 | 1.8 |
| Settings: download | `Download` | 15 | 1.8 |
| Settings: bell | `Bell` | 15 | 1.8 |

---

## 14. Scrollbar Policy

```css
/* in global <style> tag injected in root component */
::-webkit-scrollbar { display: none; }
```

Plus `style={{ scrollbarWidth: "none" }}` on every scrollable container (Firefox).

---

## 15. Accessibility

- Icon-only buttons: `aria-label` on all
- Toggle switches: `role="switch" aria-checked={value}`
- Contrast ratios (WCAG AA minimum 4.5:1 for normal text):
  - Light: `#2c2c2c` on `#f5f4f0` ≈ **10.2:1** ✓
  - Dark: `#e8e6e0` on `#1e2022` ≈ **9.6:1** ✓
  - Primary on white: `#5a7a6a` on `#fff` ≈ **4.8:1** ✓
  - Dark primary on bg: `#7aab8a` on `#1e2022` ≈ **5.1:1** ✓
- Focus rings: `outline-ring/50` via Tailwind `@layer base` on `*`

---

## 16. State Architecture

All state lives in the root `App` component and is passed down as props. No external state library.

| State var | Type | Default | Purpose |
|---|---|---|---|
| `songs` | `Song[]` | SEED_SONGS (26 items) | Full library |
| `playlists` | `Playlist[]` | SEED_PLAYLISTS (3 items) | User playlists |
| `tab` | `Tab` | `"songs"` | Active bottom nav tab |
| `activeSong` | `Song \| null` | `null` | Currently loaded song |
| `isPlaying` | `boolean` | `false` | Playback state |
| `fullPlayer` | `boolean` | `false` | Full player sheet visibility |
| `shuffle` | `boolean` | `false` | Shuffle mode |
| `repeatMode` | `0\|1\|2` | `0` | 0=off, 1=repeat all, 2=repeat one |
| `darkMode` | `boolean` | `true` | Theme — synced to `document.documentElement.classList` |

Local state (within components):
- `SongsTab`: `query`, `sortBy`
- `PlaylistsTab`: `creating`, `newName`, `openPlaylist`
- `ExploreTab`: `view` (`"artists" | "albums"`)
- `FullPlayer`: `progress`, `volume`, `muted`
- `SettingsTab`: `notifications`, `wifiOnly`, `autoDownload`, `crossfade`, `gapless`

Key behaviors:
- `handlePlay` increments `playCount` + sets activeSong + sets isPlaying true
- `handleLike` updates both `songs[]` array and `activeSong` object simultaneously
- `handleNext/Prev` wraps around array; shuffle picks random index
- Dark mode synced via `useEffect(() => { document.documentElement.classList.toggle("dark", darkMode) }, [darkMode])`

<!--

System Guidelines

Use this file to provide the AI with rules and guidelines you want it to follow.
This template outlines a few examples of things you can add. You can add your own sections and format it to suit your needs

TIP: More context isn't always better. It can confuse the LLM. Try and add the most important rules you need

# General guidelines

Any general rules you want the AI to follow.
For example:

* Only use absolute positioning when necessary. Opt for responsive and well structured layouts that use flexbox and grid by default
* Refactor code as you go to keep code clean
* Keep file sizes small and put helper functions and components in their own files.

--------------

# Design system guidelines
Rules for how the AI should make generations look like your company's design system

Additionally, if you select a design system to use in the prompt box, you can reference
your design system's components, tokens, variables and components.
For example:

* Use a base font-size of 14px
* Date formats should always be in the format “Jun 10”
* The bottom toolbar should only ever have a maximum of 4 items
* Never use the floating action button with the bottom toolbar
* Chips should always come in sets of 3 or more
* Don't use a dropdown if there are 2 or fewer options

You can also create sub sections and add more specific details
For example:


## Button
The Button component is a fundamental interactive element in our design system, designed to trigger actions or navigate
users through the application. It provides visual feedback and clear affordances to enhance user experience.

### Usage
Buttons should be used for important actions that users need to take, such as form submissions, confirming choices,
or initiating processes. They communicate interactivity and should have clear, action-oriented labels.

### Variants
* Primary Button
  * Purpose : Used for the main action in a section or page
  * Visual Style : Bold, filled with the primary brand color
  * Usage : One primary button per section to guide users toward the most important action
* Secondary Button
  * Purpose : Used for alternative or supporting actions
  * Visual Style : Outlined with the primary color, transparent background
  * Usage : Can appear alongside a primary button for less important actions
* Tertiary Button
  * Purpose : Used for the least important actions
  * Visual Style : Text-only with no border, using primary color
  * Usage : For actions that should be available but not emphasized
-->
