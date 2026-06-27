# Stage 09 Gray Control And Admin Ops

This stage adds runtime controls for real QQ group gray release. It does not add new Dify workflows.

## Admin Commands

Only configured super admins in `qqbot.admins` / `QQBOT_ADMINS` can run these commands for now. OneBot group owner/admin role detection is not required yet.

| Command | Effect |
| --- | --- |
| `#状态` | Show current group config summary. |
| `#开启主动插话` / `#autochaton` | Enable active chat C for the current group. |
| `#关闭主动插话` / `#autochatoff` | Disable active chat C for the current group. |
| `#开启表情包` | Enable meme route A for the current group. |
| `#关闭表情包` | Disable meme route A for the current group. |
| `#机器人安静` / `#botoff` | Disable the whole bot for the current group. |
| `#机器人恢复` / `#boton` | Re-enable the whole bot for the current group. |
| `#chaton` | Enable legacy chat switch. |
| `#chatoff` | Disable legacy chat switch and active chat C. |

Non-admin users receive `permission denied`. If this becomes noisy in real groups, change `AdminCommandService` to return a silent handled result for denied commands.

## Group Config Fields

`group_config` now uses these runtime controls:

| Field | Meaning |
| --- | --- |
| `bot_on` | Whole bot switch. If off, only restore/status commands are handled. |
| `enable_meme` | Meme route A switch. |
| `enable_chat` | Legacy chat switch. Kept for compatibility. |
| `enable_passive_chat` | Passive chat B switch. B is enabled only when both `enable_chat` and `enable_passive_chat` are true. |
| `enable_auto_join` | Active chat C switch. |
| `active_cooldown_seconds` | Per-group active chat cooldown. |
| `active_hour_limit` | Per-group active chat hourly max. |
| `active_day_limit` | Per-group active chat daily max. |
| `safe_word`, `safe_word_reply` | Group safety word and reply. |
| `persona` | Group persona override. |
| `memory_mode` | Context memory mode. |

For an existing MySQL database, run:

```sql
source src/main/resources/db/mysql/stage09_group_config_migration.sql;
```

For a fresh Docker volume, `schema.sql` already contains the new columns.

## Active Chat C Risk Control

Route order stays:

```text
dedup -> group config -> bot switch -> admin/safety words -> passive B -> meme A -> active C -> silent
```

Active chat C is rejected silently with a debug-friendly reason when:

- global `qqbot.active-chat.enabled=false`
- the message mentions the bot or matches bot nickname
- group bot switch is off
- `enable_auto_join=false`
- message is empty, punctuation-only, too short, or too long
- meme A already sent and `allow-after-meme-sent=false`
- last bot message protection rejects it
- Redis cooldown key exists
- hourly count reaches `active_hour_limit`
- daily count reaches `active_day_limit`
- random probability misses
- Dify C returns `shouldReply=false`, empty reply, low confidence, or error

Redis failures use a conservative strategy: cooldown/hour/day checks fail closed and active chat stays silent.

## Redis Keys

```text
qqbot:active:cooldown:{groupId}
qqbot:active:hour:{groupId}:{yyyyMMddHH}
qqbot:active:day:{groupId}:{yyyyMMdd}
```

The cooldown key TTL is the effective cooldown seconds. Hour keys expire after 2 hours. Day keys expire after 2 days.

## Diagnosis

`GET /dev/health/full` is available in `dev` and `local` profiles.

Optional group config lookup:

```http
GET http://localhost:8081/dev/health/full?groupId=736566774
```

The response includes:

- active profiles
- sender type
- OneBot WebSocket enabled, selfId, allowedGroupIds
- Dify enabled
- A/B/C workflow configured flags
- A/B/C API key configured flags
- meme base dir
- MySQL and Redis reachability
- scene/material counts
- requested group config, when `groupId` is provided

API keys and SnowLuma token are never returned.

## Gray Release Advice

Start conservative:

```sql
update group_config
set bot_on = 1,
    enable_meme = 1,
    enable_chat = 1,
    enable_passive_chat = 1,
    enable_auto_join = 0,
    active_cooldown_seconds = 300,
    active_hour_limit = 3,
    active_day_limit = 10
where group_id = 736566774;
```

Verify A and B first, then enable C:

```text
#开启主动插话
```

Emergency stop:

```text
#机器人安静
```

Recover:

```text
#机器人恢复
```
