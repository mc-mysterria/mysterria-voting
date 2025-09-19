# Enhanced Reminder System

The MysterriaVoting plugin has been enhanced with a comprehensive reminder system that supports multiple display types, gradient colors, clickable URLs, boss bars, and much more.

## Features

### Reminder Types
- **CHAT**: Regular chat messages
- **TITLE**: Title and subtitle displays
- **ACTIONBAR**: Action bar messages
- **BOSSBAR**: Boss bar displays with customizable colors and styles
- **COMBINED**: Multiple types simultaneously

### Visual Enhancements
- **Gradient Colors**: Beautiful gradient text using MiniMessage syntax
- **Clickable URLs**: Messages that open URLs when clicked
- **Hover Text**: Custom hover tooltips
- **Emojis**: Support for Unicode emojis

### Targeting Options
- **World-specific**: Target specific worlds
- **Permission-based**: Require permissions to see reminders
- **Scheduled**: Automatic intervals or manual triggering

## Configuration

### Basic Reminder Structure
```yaml
reminders:
  reminder_id:
    name: "Display Name"
    type: CHAT|TITLE|ACTIONBAR|BOSSBAR|COMBINED
    enabled: true
    interval: 300 # seconds
    # Additional type-specific options...
```

### Chat Reminders
```yaml
website:
  name: "Website Reminder"
  type: CHAT
  enabled: true
  interval: 600
  messages:
    - "<gradient:green:blue>Visit our website!</gradient>"
    - "<green><click:open_url:https://example.com>🌐 example.com</click>"
  url: "https://example.com"
  hover-text: "<yellow>Click to visit!"
  gradient:
    start: "green"
    end: "blue"
```

### Title Reminders
```yaml
rules:
  name: "Rules Reminder"
  type: TITLE
  enabled: true
  interval: 1800
  title:
    text: "<gradient:red:orange>📋 Server Rules</gradient>"
    subtitle: "<yellow>Type /rules to read them!"
  gradient:
    start: "red"
    end: "orange"
```

### Boss Bar Reminders
```yaml
voting:
  name: "Voting Reminder"
  type: BOSSBAR
  enabled: true
  interval: 1200
  bossbar:
    text: "<gradient:gold:yellow>⭐ Vote for rewards! ⭐</gradient>"
    color: YELLOW  # BLUE, GREEN, PINK, PURPLE, RED, WHITE, YELLOW
    style: SEGMENTED_20  # SOLID, SEGMENTED_6, SEGMENTED_10, SEGMENTED_12, SEGMENTED_20
    duration: 15
  gradient:
    start: "gold"
    end: "yellow"
```

### Combined Reminders
```yaml
discord:
  name: "Discord Reminder"
  type: COMBINED
  enabled: true
  interval: 900
  messages:
    - "<gradient:purple:pink>Join our Discord!</gradient>"
  title:
    text: "<gradient:purple:pink>Discord</gradient>"
    subtitle: "<yellow>Join our community!"
  actionbar:
    text: "<gradient:purple:pink>💬 Join Discord</gradient>"
  bossbar:
    text: "<gradient:purple:pink>Discord Community</gradient>"
    color: PURPLE
    style: SOLID
    duration: 10
```

### Advanced Options
```yaml
events:
  name: "Events Reminder"
  type: COMBINED
  enabled: true
  interval: 2400
  permission: "voting.events"  # Optional permission requirement
  target-worlds:  # Optional world targeting
    - "world"
    - "world_nether"
  # ... other options
```

## Commands

### `/reminder list`
Lists all configured reminders with their status and type.

### `/reminder send <reminder_id>`
Manually sends a reminder to all eligible players.

### `/reminder sendto <player> <reminder_id>`
Sends a specific reminder to a specific player.

### `/reminder info <reminder_id>`
Shows detailed information about a reminder.

### `/reminder reload`
Reloads all reminders from the configuration.

### `/reminder help`
Shows help information.

## Permissions

- `voting.reminder` - Base reminder permission
- `voting.reminder.list` - List reminders
- `voting.reminder.send` - Send reminders to all
- `voting.reminder.sendto` - Send reminders to specific players
- `voting.reminder.info` - View reminder information
- `voting.reminder.reload` - Reload reminder configuration

## Placeholders

The following placeholders are available in all reminder messages:

- `{player}` - Player's name
- `{displayname}` - Player's display name
- `{world}` - Player's current world
- `{online}` - Number of online players

## MiniMessage Features

The reminder system supports full MiniMessage syntax:

### Colors
- `<red>`, `<green>`, `<blue>`, etc.
- `<#FF0000>` - Hex colors
- `<gradient:red:blue>text</gradient>` - Gradients
- `<rainbow>text</rainbow>` - Rainbow effect

### Formatting
- `<bold>`, `<italic>`, `<underlined>`
- `<strikethrough>`, `<obfuscated>`

### Interactions
- `<click:open_url:https://example.com>text</click>`
- `<hover:show_text:"Hover text">text</hover>`

## Examples

### Website Promotion
```yaml
website:
  name: "Website Reminder"
  type: CHAT
  enabled: true
  interval: 600
  messages:
    - "<gradient:green:blue>🌐 Visit our website for the latest updates!</gradient>"
    - "<green><click:open_url:https://mysterria.net><hover:show_text:\"<yellow>Click to open mysterria.net\">Click here to visit</hover></click>"
  gradient:
    start: "green"
    end: "blue"
```

### Discord Invitation
```yaml
discord:
  name: "Discord Invitation"
  type: TITLE
  enabled: true
  interval: 900
  title:
    text: "<gradient:purple:pink>💬 Discord</gradient>"
    subtitle: "<yellow>Join our community at discord.gg/mysterria"
  gradient:
    start: "purple"
    end: "pink"
```

### Vote Reminder with Boss Bar
```yaml
vote_bossbar:
  name: "Vote Boss Bar"
  type: BOSSBAR
  enabled: true
  interval: 1200
  bossbar:
    text: "<gradient:gold:yellow>⭐ Vote for the server and earn rewards! ⭐</gradient>"
    color: YELLOW
    style: SEGMENTED_20
    duration: 20
  gradient:
    start: "gold"
    end: "yellow"
```

## Migration from Old System

The new reminder system is fully compatible with the existing voting functionality. Your existing voting configuration will continue to work unchanged. The reminder system is an addition that expands the plugin's capabilities.

## Troubleshooting

### Reminders Not Showing
1. Check if the reminder is enabled in config
2. Verify the player has the required permission (if set)
3. Ensure the player is in a target world (if specified)
4. Check console for any error messages

### Boss Bars Not Appearing
1. Verify the boss bar color and style are valid
2. Check that the duration is set properly
3. Ensure the player client supports boss bars

### URLs Not Clickable
1. Verify the URL format in the configuration
2. Check that MiniMessage click syntax is correct
3. Ensure the player's client supports clickable text

## Performance Considerations

- Reminders are scheduled efficiently using Bukkit's scheduler
- Boss bars are automatically cleaned up when players disconnect
- The system handles large numbers of players efficiently
- Configuration reloading stops old tasks before starting new ones