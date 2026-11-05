# This fork: the weather and a three-day forecast under the clock, from the Weather add-on

A fork of [CleanYourTV](https://github.com/Gathin23/cleanyourtv) (on its v3.3.0 launcher) that adds one thing: **two
lines under Home's clock, the weather now and the next three days, read from the Weather add-on**. The launcher keeps no
`INTERNET` permission and goes online for nothing; the add-on does the fetching.

![Home with the two weather lines under the clock](docs/switches/weather_feed.jpg)

```
Lisbon  ·  13°  ·  86%  ·  feels 13°
Fri 16°/8°  ·  Sat 16°/7° rain 60%  ·  Sun 15°/7°
```

The first line is the place, the temperature, the humidity and the feels-like temperature. The second is the next three
days with their high and low; a day shows its chance of rain only from 50%, so a dry week stays short. The two lines are
one focus target: press Up from the dock and OK to open the Weather add-on's full page.

## See exactly what changed

This fork is built on the original's **v3.3.0** release and is two commits ahead of it: 15 files, about 650 added lines.
GitHub shows every changed line here:
[Gathin23/cleanyourtv v3.3.0 ... lukumaki:weather-feed](https://github.com/Gathin23/cleanyourtv/compare/v3.3.0...lukumaki:weather-feed).
The README keeps the original text under a short note at the top, so a later merge from the original stays easy.

## What is different

| Part | What it does |
|---|---|
| `weather_feed` (launcher, `config.xml`) | The package of an add-on that publishes the weather. Empty by default: nothing is read and Home looks as it did. A launcher that cannot find the add-on, or finds a provider another app holds, shows nothing. |
| `addons/weather` (the Weather add-on) | Publishes a read-only content provider, `io.github.sumyabazar1.weather.feed`, with `/now` (place, temperature, feels-like, humidity, wind, air quality) and `/days` (weekday, sky, high, low, chance of rain). It holds only a place name and the weather there. |

The columns are in [docs/modules.md](docs/modules.md), "The weather on Home". The add-on asks for one more permission than
before, `ACCESS_NETWORK_STATE`, only to read whether Android has validated the network; the launcher's manifest is unchanged. They are not the `weather_now` columns the
other add-on-fed forks use; they carry the three days and the chance of rain too.

How often it reads, so the TV is not kept online for it:

- The launcher asks when Home comes to the front and every 3 hours while Home is on screen, on its own thread.
- The add-on reuses its last reading for 3 hours and goes online only when asked and the reading is older. It asks
  Android whether the network is validated first, so it does not try in the seconds after a boot when the Wi-Fi is up
  and the internet is not; waiting for that costs nothing, and on a network Android never validates it tries anyway
  after 2 minutes. After a fetch that really failed it waits 5 minutes before the next try. The launcher asks again
  after 1 minute when it got nothing.
- A reading older than 6 hours is dropped and the lines disappear, rather than show an old temperature as current.

## Tested on

A Tesla Smart TV Q55E655GUS (Expressluck, sepolia, MediaTek mt5896), Google TV 14, with this launcher as Home, in use since
4 November 2026. The launcher builds to 589,365 bytes, under upstream's 589,824 byte gate, and its unit tests and lint
pass. Other TVs have not been tried.

Known limits: the weekday names follow the TV's language, but the sky words come from the add-on, which has English and
French only, and "feels" and "rain" are English in the launcher.

## Install

1. Build the launcher from this fork (`launcher/`, JDK 17 and the Android SDK) and install it as the Home app, as the main
   repo's README describes.
2. Build and install the add-on: `cd addons/weather && ./gradlew assembleRelease`, then `adb install -r` the APK. Open
   it once and pick your city.
3. Name the add-on in `launcher/app/src/main/res/values/config.xml` before building the launcher:

   ```xml
   <string name="weather_feed" translatable="false">io.github.sumyabazar1.weather</string>
   ```

Nothing else in the launcher changes, so an existing theme or list keeps working.

## Credits

The launcher, its tools and the Weather add-on are the work of [Gathin23](https://github.com/Gathin23) and
[Sumyabazar1](https://github.com/Sumyabazar1). Other forks that read the weather from an add-on were a model for
keeping the launcher offline: see [SHOWCASE.md](SHOWCASE.md) upstream. MIT, as the repo it forks.
