# MysterriaVoting audit events

Producer `mysterria-voting`, shaded audit client relocated to `net.mysterria.voting.libs.audit`. Rows are
spooled to `plugins/mysterria-audit-spool`, all with privacy `STAFF_RESTRICTED`. If the client cannot start,
auditing is a no-op. Rows from one menu click share a correlation id; admin rows each get their own.

| Event type | Outcome | Key facts |
| --- | --- | --- |
| `voting.menu.click_action` | `OBSERVED` | Click on a configured vote-menu slot. Business id: service (`menu-items` key). `reward_state`, `raw_slot`, `clicked_inventory`, `inventory_holder_type`, `view_title`, `item_material`, `item_uuid` / `parent_item_uuid`. Risk `LOW`. |
| `voting.reward.claimed` | `COMMITTED` / `FAILED` | Claim persisted to `claims.yml` before console commands run (`COMMITTED`, `NORMAL`), or the write failed and nothing was dispatched (`FAILED`, `reason=persist_failed`, `HIGH`). Business id `<player-uuid>:<service>`. `command_count`, `claimed_at`. |
| `voting.reward.claim_denied` | `DENIED` | Item has console rewards but `reason` is `already_claimed` or `claims_unavailable`. Business id `<player-uuid>:<service>`. |
| `voting.reward.command_dispatched` | `COMMITTED` / `FAILED` | One per console command after dispatch. `command_index`, `command_count`, `command_template`, `command_resolved`, `dispatch_result`, `error` on exception. `HIGH` if the template contains `give`, `item`, `i`, `eco`, `economy`, `money`, `pay`, `xp`, `experience`, `lp` or `luckperms`. |
| `voting.reward.player_command` | `COMMITTED` / `FAILED` | One per `run-command.player` entry. `command_template`, `command_resolved`, `result`. |
| `voting.admin.reload` | `COMMITTED` | `/voting reload` (scope `voting`) or `/reminder reload` (scope `reminders`). Before/after reminder hashes and counts; for `voting` also menu action hashes and the live console command templates. `HIGH` when menu actions changed or could not be hashed. |
| `voting.admin.broadcast` | `COMMITTED` | `/voting send`. `recipient_count`. Risk `LOW`. |
| `voting.admin.reminder_sent` | `COMMITTED` | `/reminder send` / `sendto`. `reminder_id`, `mode`, subject = target player for `sendto`. Risk `LOW`. |

Menu rows also carry `menu_key`, `service`, `menu_lang`, `slot`, `click_type` and the player's block position;
admin rows carry `actor_type`, `actor_name` (console) or position (player). Non-reward clicks are audited at most
once per player per service per five minutes; `command_dispatched` rows are never skipped.
