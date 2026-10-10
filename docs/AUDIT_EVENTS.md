# MysterriaVoting audit events

Producer `mysterria-voting`, shaded audit client relocated to `net.mysterria.voting.libs.audit`. Rows are
spooled to `plugins/mysterria-audit-spool`, all with privacy `STAFF_RESTRICTED`. If the client cannot start,
auditing is a no-op. Rows from one menu click share a correlation id; admin rows each get their own.

| Event type | Outcome | Key facts |
| --- | --- | --- |
| `voting.menu.click_action` | `OBSERVED` | Click on a configured vote-menu slot. Business id: service (`menu-items` key). `reward_state` (`no_reward`, `claimed`, `claim_refused`, or `not_attempted` when a player command throws before the claim), `item_material`. Risk `LOW`. |
| `voting.reward.claimed` | `COMMITTED` | Claim persisted to `claims.yml` before console commands run. Business id `<player-uuid>:<service>`. `command_count`, `claimed_at` (the time passed to the claim). |
| `voting.reward.claim_denied` | `DENIED` | Item has console rewards but the claim store refused the claim: `reason=claim_refused` covers already claimed, `claims.yml` unreadable and a failed write (the store logs read and write failures itself). Business id `<player-uuid>:<service>`. |
| `voting.reward.command_dispatched` | `COMMITTED` / `FAILED` | One per console command after dispatch. `command_index`, `command_count`, `command_template`, `command_resolved`, `dispatch_result`, `error` on exception. `HIGH` if the template contains `give`, `item`, `i`, `eco`, `economy`, `money`, `pay`, `xp`, `experience`, `lp` or `luckperms`. |
| `voting.reward.player_command` | `COMMITTED` / `FAILED` | One per `run-command.player` entry. `command_template`, `command_resolved`, `result`. |
| `voting.admin.reload` | `COMMITTED` / `FAILED` | `/voting reload` (scope `voting`, `HIGH` because it can change reward commands) or `/reminder reload` (scope `reminders`, `NORMAL`). `FAILED` (`HIGH`) when the reload throws: `reason` and `error` hold the exception, and the exception still propagates. |
| `voting.admin.broadcast` | `COMMITTED` | `/voting send`. `recipient_count`. Risk `LOW`. |
| `voting.admin.reminder_sent` | `COMMITTED` | `/reminder send` / `sendto`. `reminder_id`, `mode`, subject = target player for `sendto`. Risk `LOW`. |

Menu rows also carry `menu_key`, `service`, `menu_lang`, `slot`, `click_type` and the player's block position;
admin rows carry `actor_type`, `actor_name` (the player name, or `CONSOLE` / the sender name for console and RCON), the actor uuid for players, and the position for players. `click_action`, `claim_denied` and
`player_command` rows are audited at most once per player per service per five minutes; a click that claims
its reward (the `click_action` and `claimed` rows) and `command_dispatched` rows are always recorded.

Main thread: rows only use values already in hand (the clicked slot and its material, the sender, the command
result). Dropped so logging adds no main-thread work: `item_uuid` / `parent_item_uuid` on `click_action` (item
meta read), the reload config hashes, `*_changed`, reminder counts and console command templates (config walk
and SHA-256), and `online_count` on `reminder_sent`.
