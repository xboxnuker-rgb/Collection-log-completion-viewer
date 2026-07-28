# Collection Log Progress

A passive RuneLite interface plugin that makes Collection Log progress easier
to scan. Each page name in the left-hand list can show a whole-number completion
percentage and use fixed state colours: red at 0%, orange from 1–99%, and
RuneScape's completion green at 100%. In-log controls can hide unstarted,
partial or completed pages and compact the remaining list.

## Features

- Adds `0%` to `100%` progress beside Collection Log page and tab names, with
  separate controls for hiding the endpoint labels.
- Uses simple red, orange and green states that remain clear on both small and
  large pages.
- Keeps the five aggregate tab titles on the smooth scale at all times.
- Offers the same smooth scale for page names as an option, disabled by default.
- Provides colour pickers for the unstarted, partial and completed anchors.
- Adds three colour-matched `Hide` checkboxes inside the Collection Log for
  unstarted, partial and completed pages.
- Compacts visible rows and updates the scrollbar when a filter is enabled;
  filter choices persist between sessions.
- Reserves the configured completion colour for fully completed pages.
- Reads page definitions from RuneScape's cache rather than maintaining a static
  item list.
- Leaves the selected-page header, item icons and central item panel unchanged.
- Aggregates page progress into the five top-level tab titles.
- Provides separate toggles for colours and percentages.

To obtain the account's item snapshot, the plugin invokes the Collection Log's
existing Search widget operation and immediately restores the normal page. It
does not add a menu entry or expose the snapshot outside the client.

The plugin does not use external services, write files, alter combat, inject
input, or automate gameplay.

## Development

Java 11 is required.

Build and run the unit tests:

```powershell
.\gradlew.bat clean build
```

Launch the RuneLite development client:

```powershell
.\gradlew.bat run
```

When using a Jagex Account, follow RuneLite's
[development-client login instructions](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).

RuneLite and RuneScape widgets cannot be validated by unit tests alone. A fresh
development-client JVM and manual in-game checks are required after Java changes.

## License

This project is licensed under the BSD 2-Clause License. See `LICENSE`.
