# MysterriaVoting audit events

MysterriaVoting emits best-effort, staff-restricted events through its shaded
neutral audit client (`dev.ua.ikeepcalm.mysterria:audit-client`, relocated to
`net.mysterria.voting.libs.audit`). The producer id is `mysterria-voting` and
events are spooled to `plugins/mysterria-audit-spool`. If the client cannot
start, the plugin logs one warning and every audit call becomes a no-op. Rows
are built and emitted inside a guard, so an audit failure never changes a
menu click, a reward, or a command. All rows are built on the main thread
from plain values; the client writes the spool on its own worker.

Every row has privacy `STAFF_RESTRICTED`.

## Vote reward claims

The plugin does not verify votes. A menu item's `run-command.console` list is
treated as its reward. A player can claim each menu item (the *service*, which
is the `menu-items` key, for example `vote-site`) once. Both click types and
both languages share the claim. The claim is written to
`plugins/MysterriaVoting/claims.yml` (`claims.<player-uuid>.<service>: <epoch
millis>`, via a temporary file and an atomic move) **before** any console
command is dispatched. A crash after the write can therefore lose a reward but
never duplicate one. If `claims.yml` exists but cannot be parsed, or the write
fails, the reward is withheld and a `SEVERE` line is logged. The file is not
reset by `/voting reload`. To let a player claim again, remove their entry
from the file while the server is stopped.

`run-command.player` commands, messages, titles and the menu close still run
on every click. They are not gated.

## Event catalog

| Event type | Outcome | When | Actor / subject | Business id | Risk |
| --- | --- | --- | --- | --- | --- |
| `voting.menu.click_action` | `OBSERVED` | A click in a view titled like the vote menu hit a configured item slot and its click-actions branch exists | Actor: clicking player | service | `LOW` |
| `voting.reward.claimed` | `COMMITTED` | The claim was persisted to `claims.yml`. Emitted before the console commands run | Actor: player | `<player-uuid>:<service>` | `NORMAL` |
| `voting.reward.claimed` | `FAILED` | The claim write failed (`reason=persist_failed`). No command was dispatched | Actor: player | `<player-uuid>:<service>` | `HIGH` |
| `voting.reward.claim_denied` | `DENIED` | The item has console commands but the player already claimed it (`already_claimed`), or `claims.yml` is unreadable (`claims_unavailable`) | Actor: player | `<player-uuid>:<service>` | `NORMAL` |
| `voting.reward.command_dispatched` | `COMMITTED` / `FAILED` | One row per console command, after `Bukkit.dispatchCommand` returns (`COMMITTED` when it returned true). A thrown exception produces a `FAILED` row with `error` and is then rethrown, as before | Actor: player | `<player-uuid>:<service>` | `HIGH` if the template contains `give`, `item`, `i`, `eco`, `economy`, `money`, `pay`, `xp`, `experience`, `lp` or `luckperms` as a word (namespace and leading `/` ignored), else `NORMAL` |
| `voting.reward.player_command` | `COMMITTED` / `FAILED` | One row per `run-command.player` entry after `performCommand` returns | Actor: player | service | `NORMAL` |
| `voting.admin.reload` | `COMMITTED` | After `/voting reload` (scope `voting`, which also reloads reminders) or `/reminder reload` (scope `reminders`) has been applied | Actor: staff player UUID, or none for console | scope | `HIGH` for a `voting` reload whose menu actions changed or could not be hashed, else `NORMAL` |
| `voting.admin.broadcast` | `COMMITTED` | After `/voting send` has sent the clickable vote message | Actor: sender | `vote_broadcast` | `LOW` |
| `voting.admin.reminder_sent` | `COMMITTED` | After `/reminder send <id>` or `/reminder sendto <player> <id>` | Actor: sender; subject: target player (`sendto` only) | reminder id | `LOW` |

Rows from one menu click share a correlation id: `click_action`, `claimed` or
`claim_denied`, `player_command`, and `command_dispatched`. Each admin row has
its own correlation id.

### Click volume limit

A click that claims a reward, or whose claim write fails, is always audited.
Other clicks on the same service by the same player (already claimed, no
reward configured, claims unavailable) produce rows only once per five
minutes. The limit is kept in memory for at most 1024 player/service pairs
(least recently used are evicted first) and resets on restart. Inside the
window, `click_action`, `claim_denied` and `player_command` rows for that
click are all skipped. `command_dispatched` rows are never skipped, because
they occur only on a successful claim.

## Metadata

Menu rows (`voting.menu.*`, `voting.reward.*`) always carry:

- `menu_key` (`voting`), `service`, `menu_lang` (language file used), `slot`
  (`InventoryClickEvent#getSlot`), `click_type` (`left` / `right`),
  `actor_type` (`player`)
- `world`, `x`, `y`, `z`: the player's block position at click time

Per event:

- `voting.menu.click_action`: `raw_slot`, `clicked_inventory` (`top`, `bottom`
  or `outside`), `inventory_holder_type` (simple class name of the top
  inventory holder, or `none`; the real vote menu has no holder),
  `view_title` (plain text), `reward_state` (`claimed`, `already_claimed`,
  `claims_unavailable`, `persist_failed` or `no_reward`), `item_material`,
  and `item_uuid` / `parent_item_uuid` when the clicked item carries the
  `circleofimagination:item_uuid` / `circleofimagination:item_parent` string
  tags. The menu is recognised by its inventory, not its title, and only
  clicks in the menu's own slots run actions, so `clicked_inventory` is
  always `top` and `inventory_holder_type` is `none`. The fields stay as a
  check on that rule. Another inventory with the same title, or a slot in the
  player's own inventory, produces no row. A menu left open across
  `/voting reload` stays locked but runs no actions until it is reopened.
- `voting.reward.claimed` / `claim_denied`: `command_count`, `reason` (not on
  `COMMITTED`), `claimed_at` (epoch millis of the stored claim, when one
  exists).
- `voting.reward.command_dispatched`: `command_index`, `command_count`,
  `command_template` (as configured), `command_resolved` (after `{target}`
  substitution, so it contains the player name), `dispatch_result`, and
  `error` (exception class) on a thrown dispatch.
- `voting.reward.player_command`: `command_template`, `command_resolved`,
  `result`.

Admin rows carry `actor_type` (`player` or `console`), `actor_name` for
non-player senders only, and `world` / `x` / `y` / `z` for player senders.

- `voting.admin.reload`: `scope`, `reminders_hash_before`,
  `reminders_hash_after`, `reminders_changed`, `reminder_count_before`,
  `reminder_count_after`. For scope `voting` it also has
  `menu_actions_hash_before`, `menu_actions_hash_after`,
  `menu_actions_changed`, `console_command_templates` (distinct console
  templates in all language files after the reload, sorted, joined with
  ` | `) and `console_command_template_count`. The menu hash covers
  `menu-name`, each item's slot and each click type's console and player
  commands. The reminder hash covers each reminder's id, type, enabled flag,
  interval, permission, URL and messages. Hashes are the first 8 bytes of
  SHA-256 in hex. They read `unavailable` (and the counts `-1`) if hashing
  failed.
- `voting.admin.broadcast`: `recipient_count` (online players messaged).
- `voting.admin.reminder_sent`: `reminder_id`, `mode` (`all` / `player`),
  `online_count` for `all`.

Text values are capped at 256 characters, except command text and the
template list, which are capped at 1024. At most 48 metadata keys are sent.

## Not audited

Timed reminders, `/vote` menu opens, `/reminder list|info|help`, rejected
commands (no permission, unknown reminder or player) and clicks outside
configured slots produce no rows.
