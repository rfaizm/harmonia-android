package com.rfaizm.harmoniamusic

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Where Hilt builds the app's object graph (M6). Named apart from the `HarmoniaApp` composable, which is the UI root.
 */
@HiltAndroidApp
class HarmoniaApplication : Application()
