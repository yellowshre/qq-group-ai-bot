# Memes Directory

This directory is the default local base directory for meme image files.

## Rules

- Do not commit real meme image files to Git.
- Keep real image files in local working copies only.
- Store `meme_material.file_path` as a relative path from this directory.
- Use lowercase English letters, numbers, and underscores only.
- Do not use Chinese characters, spaces, brackets, or special symbols in filenames.

## Naming

Use this pattern:

```text
scene_001.png
scene_002.gif
```

Examples:

```text
laugh/laugh_001.png
laugh/laugh_002.gif
awkward/awkward_001.png
confused/confused_001.png
comfort/comfort_001.jpg
```

Recommended formats:

- `.png`
- `.jpg`
- `.jpeg`
- `.gif`
- `.webp` only after SnowLuma / OneBot tests confirm it works in your environment

## Database Path

`meme_material.file_path` should contain a relative path:

```text
laugh/laugh_001.png
```

Do not store hard-coded absolute paths such as:

```text
C:\qqbot\memes\laugh_001.png
```

At runtime, the backend resolves the relative path using:

```yaml
qqbot:
  meme:
    base-dir: ${QQBOT_MEME_BASE_DIR:./memes}
```

Then it converts the resolved path to a OneBot-compatible file URI, for example:

```text
file:///C:/Users/yang/Desktop/XM/QQbot/bot/memes/laugh/laugh_001.png
```
