# Bilingual Reminder System

The MysterriaVoting plugin now features a comprehensive bilingual reminder system that automatically detects player locale and displays reminders in their preferred language (English or Ukrainian).

## 🌟 Key Features

### **Automatic Language Detection**
- Players automatically receive reminders in their client language
- Supports English (`en`) and Ukrainian (`uk`)
- Fallback to English if player's language is unavailable

### **Separate Configuration Files**
- `reminders/en.yml` - English reminder configurations
- `reminders/uk.yml` - Ukrainian reminder configurations
- Clean separation allows easy management and translation

### **Full Feature Support**
- **5 Reminder Types**: Chat, Title, ActionBar, BossBar, Combined
- **Gradient Colors**: Beautiful color transitions using MiniMessage
- **Clickable URLs**: Interactive links with hover tooltips
- **Boss Bars**: Customizable progress bars with colors and styles
- **Advanced Targeting**: World-specific and permission-based reminders
- **Automated Scheduling**: Configurable intervals for each reminder

## 📁 File Structure

```
MysterriaVoting/
├── config.yml                    # Main plugin configuration
├── lang/
│   ├── en.yml                    # English voting translations
│   └── uk.yml                    # Ukrainian voting translations
└── reminders/
    ├── en.yml                    # English reminder configurations
    └── uk.yml                    # Ukrainian reminder configurations
```

## 🔧 Configuration

### English Reminders (`reminders/en.yml`)
```yaml
reminders:
  website:
    name: "Website Reminder"
    type: CHAT
    enabled: true
    interval: 600 # 10 minutes
    messages:
      - "<gradient:green:blue>🌐 Visit our website for the latest updates!</gradient>"
      - "<green><click:open_url:https://mysterria.net>mysterria.net</click>"
    gradient:
      start: "green"
      end: "blue"
```

### Ukrainian Reminders (`reminders/uk.yml`)
```yaml
reminders:
  website:
    name: "Нагадування про сайт"
    type: CHAT
    enabled: true
    interval: 600 # 10 хвилин
    messages:
      - "<gradient:green:blue>🌐 Відвідайте наш сайт для останніх оновлень!</gradient>"
      - "<green><click:open_url:https://mysterria.net>mysterria.net</click>"
    gradient:
      start: "green"
      end: "blue"
```

## 🎯 How It Works

1. **Player Joins**: Plugin detects player's client locale
2. **Language Selection**: Chooses appropriate reminder configuration
3. **Reminder Display**: Shows localized reminder content
4. **Fallback Logic**: Uses English if player's language unavailable

### Language Detection Process
```java
String playerLocale = player.locale().getLanguage(); // "en" or "uk"
Reminder localizedReminder = getLocalizedReminder(player, reminderId);
```

## 📋 Available Reminder Types

### 💬 **Chat Reminders**
Regular chat messages with gradient colors and clickable links.

**English Example:**
```yaml
discord:
  name: "Discord Reminder"
  type: CHAT
  messages:
    - "<gradient:purple:pink>💬 Join our Discord community!</gradient>"
    - "<purple><click:open_url:https://discord.gg/mysterria>discord.gg/mysterria</click>"
```

**Ukrainian Example:**
```yaml
discord:
  name: "Нагадування про Discord"
  type: CHAT
  messages:
    - "<gradient:purple:pink>💬 Приєднуйтесь до нашої спільноти в Discord!</gradient>"
    - "<purple><click:open_url:https://discord.gg/mysterria>discord.gg/mysterria</click>"
```

### 🏆 **Title Reminders**
Large title and subtitle displays.

**English:**
```yaml
rules:
  name: "Rules Reminder"
  type: TITLE
  title:
    text: "<gradient:red:orange>📋 Server Rules</gradient>"
    subtitle: "<yellow>Type /rules to read them!"
```

**Ukrainian:**
```yaml
rules:
  name: "Нагадування про правила"
  type: TITLE
  title:
    text: "<gradient:red:orange>📋 Правила сервера</gradient>"
    subtitle: "<yellow>Введіть /rules щоб прочитати їх!"
```

### 📊 **Boss Bar Reminders**
Prominent boss bar displays with customizable appearance.

**English:**
```yaml
voting:
  name: "Voting Reminder"
  type: BOSSBAR
  bossbar:
    text: "<gradient:gold:yellow>⭐ Vote for the server and get rewards! ⭐</gradient>"
    color: YELLOW
    style: SEGMENTED_20
    duration: 15
```

**Ukrainian:**
```yaml
voting:
  name: "Нагадування про голосування"
  type: BOSSBAR
  bossbar:
    text: "<gradient:gold:yellow>⭐ Голосуйте за сервер і отримуйте нагороди! ⭐</gradient>"
    color: YELLOW
    style: SEGMENTED_20
    duration: 15
```

### 🔄 **Combined Reminders**
Multiple reminder types simultaneously for maximum impact.

## 🎨 Styling Features

### **Gradient Colors**
```yaml
gradient:
  start: "green"
  end: "blue"
```

### **Clickable URLs**
```yaml
url: "https://mysterria.net"
hover-text: "<yellow>Click to visit our website!"
```

### **Boss Bar Styles**
- `SOLID` - Solid progress bar
- `SEGMENTED_6` - 6 segments
- `SEGMENTED_10` - 10 segments
- `SEGMENTED_12` - 12 segments
- `SEGMENTED_20` - 20 segments

### **Boss Bar Colors**
- `BLUE`, `GREEN`, `PINK`, `PURPLE`, `RED`, `WHITE`, `YELLOW`

## 🎮 Commands

### `/reminder list`
Shows all available reminders with their status and language availability.

### `/reminder send <reminder_id>`
Sends a reminder to all eligible players in their language.

### `/reminder sendto <player> <reminder_id>`
Sends a specific reminder to a player in their language.

### `/reminder info <reminder_id>`
Shows detailed reminder information including all language versions.

### `/reminder reload`
Reloads all reminder configurations from both language files.

## 🔐 Permissions

- `voting.reminder` - Base reminder permission
- `voting.reminder.list` - List reminders
- `voting.reminder.send` - Send reminders to all players
- `voting.reminder.sendto` - Send reminders to specific players
- `voting.reminder.info` - View reminder information
- `voting.reminder.reload` - Reload reminder configurations

## 🌍 Language Support

### **Supported Languages**
- **English** (`en`) - Default language
- **Ukrainian** (`uk`) - Full translation support

### **Adding New Languages**
1. Create new file: `reminders/{language_code}.yml`
2. Copy structure from `en.yml`
3. Translate all text content
4. Update language array in `ReminderManager.java`

### **Fallback Logic**
- Player's language not available → Falls back to English
- Reminder not found in player's language → Uses English version
- No English version → Reminder skipped

## 🔧 Advanced Configuration

### **World Targeting**
Limit reminders to specific worlds:
```yaml
target-worlds:
  - "world"
  - "world_nether"
  - "world_the_end"
```

### **Permission Requirements**
Require specific permissions to see reminders:
```yaml
permission: "voting.events"
```

### **Timing Configuration**
```yaml
interval: 600  # Seconds between automatic reminders
```

### **Multiple Messages**
```yaml
messages:
  - "First line of the reminder"
  - "Second line with different styling"
  - "<click:open_url:https://example.com>Clickable link</click>"
```

## 🛠️ Placeholders

Available in all reminder messages:

- `{player}` - Player's name
- `{displayname}` - Player's display name
- `{world}` - Player's current world
- `{online}` - Number of online players

## 🚀 Installation & Setup

1. **Install Plugin**: Place jar in `plugins/` folder
2. **First Start**: Plugin creates default configuration files
3. **Configure Reminders**: Edit `reminders/en.yml` and `reminders/uk.yml`
4. **Reload**: Use `/reminder reload` to apply changes

## 📈 Performance

- **Efficient Scheduling**: Single scheduler per reminder type
- **Memory Optimized**: Language configs loaded once at startup
- **Boss Bar Cleanup**: Automatic cleanup on player disconnect
- **Locale Caching**: Player locales cached for performance

## 🔄 Migration from Single Config

The plugin automatically handles migration:
- Old config.yml reminders section is ignored
- New separate language files are created with defaults
- All existing voting functionality remains unchanged

## 📝 Example Complete Configuration

### English (`reminders/en.yml`)
```yaml
reminders:
  website:
    name: "Website Reminder"
    type: CHAT
    enabled: true
    interval: 600
    messages:
      - "<gradient:green:blue>🌐 Visit our website!</gradient>"
    url: "https://mysterria.net"
    hover-text: "<yellow>Click to visit!"
    gradient:
      start: "green"
      end: "blue"

  voting:
    name: "Voting Reminder"
    type: BOSSBAR
    enabled: true
    interval: 1200
    bossbar:
      text: "<gradient:gold:yellow>⭐ Vote for rewards! ⭐</gradient>"
      color: YELLOW
      style: SEGMENTED_20
      duration: 15
    gradient:
      start: "gold"
      end: "yellow"
```

### Ukrainian (`reminders/uk.yml`)
```yaml
reminders:
  website:
    name: "Нагадування про сайт"
    type: CHAT
    enabled: true
    interval: 600
    messages:
      - "<gradient:green:blue>🌐 Відвідайте наш сайт!</gradient>"
    url: "https://mysterria.net"
    hover-text: "<yellow>Натисніть, щоб відвідати!"
    gradient:
      start: "green"
      end: "blue"

  voting:
    name: "Нагадування про голосування"
    type: BOSSBAR
    enabled: true
    interval: 1200
    bossbar:
      text: "<gradient:gold:yellow>⭐ Голосуйте за нагороди! ⭐</gradient>"
      color: YELLOW
      style: SEGMENTED_20
      duration: 15
    gradient:
      start: "gold"
      end: "yellow"
```

This bilingual reminder system provides a seamless, localized experience for all players while maintaining the existing voting functionality and expanding the plugin's capabilities significantly.