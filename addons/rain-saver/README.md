# Rain

A screensaver for Android TV, and a card on cleanyourtv's Home. You look out of a window at a neon city in the rain:
the towers are out of focus, traffic and the odd police chase pass far away, drops run down the glass, and the time
hangs in the city like a glowing sign, drifting to a new spot every 50 seconds so nothing burns in. Any button wakes
the TV, and the TV still goes to sleep on its own timer.

The scene is the rainy city from [Home Deck](https://github.com/glidiandev/home-deck) by Glidian (MIT, see
[Credits](#credits)), put in a small app of its own that runs it as a screensaver. It is drawn on the TV by the TV's own
WebView from files inside this app.

- Package: `io.github.lukumaki.rainsaver`
- Author: [@lukumaki](https://github.com/lukumaki) (the app around the scene; the scene is Glidian's)
- Tested on: Tesla Smart TV Q55E655GUS (MediaTek mt5896, quad-core, about 1.8 GB), Google TV 14, WebView 156

## Permissions

None. No network access (no `INTERNET` permission): the page is read from the app's own files and the WebView is set
to block network loads, so nothing leaves the TV. No background service: it runs only while the screensaver or its own
screen is showing.

## Install

Sideload the APK built from this folder (`./gradlew --no-daemon assembleRelease`, JDK 17 and the Android SDK) with
`adb install -r app/build/outputs/apk/release/app-release.apk`, then choose it:

- Settings > Device Preferences > Screen saver > **Rain**, or, if your TV hides that menu:

  ```bash
  adb shell settings put secure screensaver_components io.github.lukumaki.rainsaver/.RainDream
  ```

- The **Rain** tile (and, once you answer Show card, a card on cleanyourtv's Home) opens the scene full screen so you
  can see it without waiting; any button leaves.

To go back, pick another screen saver in the same menu. How soon it starts is the TV's own setting (Screen saver >
Start time).

## Options

`app/src/main/res/values/options.xml`, set before building:

| Option | Default | What it does |
|---|---|---|
| `clock_24h` | `true` | 24-hour clock; `false` gives 12-hour with am/pm. |
| `rain_fps` | `30` | The most the scene redraws a second; `0` has no cap. Lower is cooler and lighter, and hard to see: the city is out of focus. |
| `rain_view` | `window` | `window`: looking across at the towers in light rain. `downpour`: right against the glass in a storm, heavier rain and lightning. |

The date uses the TV's language.

## Needs, and what it costs

The TV's **Android System WebView** (every Android TV 8 and newer has it). The scene is drawn live by the page's own
scripts, so it is not free. Measured on the Tesla above, whole-TV CPU in use while it runs, against about 7% to 35% on
Home: about 70% with no cap, about 52% capped at 30 frames a second (the default) and about 47% at 20. It also takes
about 160 MB of RAM while it shows, which goes back when it ends. Drawing it smaller does not help (tried at 100%, 67% and
50%: the same CPU), so the cap is the setting that matters. If your TV has no fan and the screensaver will run for hours,
lower `rain_fps`, or keep the TV's own sleep timer. A very old box may stutter.

## Credits

The rainy city (`app/src/main/assets/rain/core.js`, `towers.js`, `rain.css` and the idea of this screen) is
[Home Deck](https://github.com/glidiandev/home-deck)'s, Copyright (c) 2026 Glidian, MIT: the licence is in
[`LICENSE.home-deck`](LICENSE.home-deck), and the files carry a notice at the top. The clock font is
[Rajdhani](https://fonts.google.com/specimen/Rajdhani) under the SIL Open Font License 1.1 ([`OFL-Rajdhani.txt`](OFL-Rajdhani.txt)).
The rest, the Android app around them, is MIT like cleanyourtv.
