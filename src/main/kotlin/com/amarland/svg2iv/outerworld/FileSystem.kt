package com.amarland.svg2iv.outerworld

import net.harawata.appdirs.AppDirsFactory

val APP_DATA_DIRECTORY_PATH_STRING: String =
    AppDirsFactory.getInstance().getUserDataDir("svg2iv-desktop", null, "com.amarland", true)
