# OSRS Conquest

A RuneLite plugin that tracks clan activity, XP gains, playtime, and member status — all synced to a shared web dashboard.

## Features

### Automatic Clan Tracking
The plugin detects your clan on login and auto-registers — no setup required. Once active, it tracks:

- **Clan Events** — Joins, leaves, kicks, level-ups, drops, pets, quests, collection log entries, personal bests, rank changes, combat achievements, and diary completions
- **XP & Stats** — Periodic snapshots of all 23 skills, submitted every 5 minutes
- **Sessions** — Login/logout tracking with world number and duration
- **Member Sync** — Online status and rank of all clan members, updated every 5 minutes

### Sidebar Panel
A built-in panel inside RuneLite with four tabs:

| Tab | Description |
|-----|-------------|
| **Events** | Live feed of recent clan broadcasts with color-coded event types |
| **Members** | Full member list with online/offline status and ranks |
| **Stats** | Clan summary — total members, active count, online now, events in the last 24 hours |
| **Discord** | Dashboard access and Discord webhook configuration |

### Web Dashboard
Clan admins can open a full web dashboard directly from the plugin's Discord tab. The dashboard provides:

- Clan overview and stats
- Filterable event log
- Sortable member list
- Playtime tracking with per-player breakdowns
- Discord webhook management with per-event-type filtering

### Discord Webhooks
Forward clan events to a Discord channel. Configure directly from the plugin or the web dashboard:

- Choose which event types to forward (joins, drops, pets, level-ups, etc.)
- Paste your Discord webhook URL and save
- Auto-disables on repeated delivery failures to prevent spam

## Configuration

All settings are accessible in RuneLite under the OSRS Conquest plugin config:

| Setting | Default | Description |
|---------|---------|-------------|
| API URL | `https://conquest.projectonyx.net/api` | Backend endpoint |
| API Key | *(auto-generated)* | Auth token, set automatically on first login |
| Data Sharing Consent | Disabled | Must be enabled to activate data collection — confirms your clan is aware of the plugin's use |
| Track Clan Events | Enabled | Send clan broadcasts to the backend |
| Track XP/Stats | Enabled | Send periodic stat snapshots |
| Track Sessions | Enabled | Report login/logout events |
| Auto-Sync Members | Enabled | Sync online status and ranks every 5 minutes |
| Show Sidebar Panel | Enabled | Display the OSRS Conquest panel in RuneLite |

## How It Works

1. **Login** — Plugin detects your clan and registers with the backend (first time only)
2. **Consent** — Enable "Data Sharing Consent" in plugin settings to activate tracking
3. **Play** — Events, stats, and session data are sent automatically in the background
4. **View** — Check the sidebar panel or open the web dashboard to see your clan's activity

All data is sent over HTTPS to `conquest.projectonyx.net` and is scoped to your clan. No personal data beyond your RSN and in-game stats is collected.

## Privacy

- [Privacy Policy](https://conquest.projectonyx.net/privacy.html)
- [Terms of Service](https://conquest.projectonyx.net/terms.html)

## Building

```
./gradlew build
```

Requires Java 11+.

## License

[BSD 2-Clause](LICENSE)
