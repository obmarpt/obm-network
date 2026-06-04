# MythicMobs — OMD Network

## Problema

`ExampleRandomSpawns.yml` fazia spawn de **Skeleton King** no mundo `world` (SMP) com `Chance: 0.001` e `Action: REPLACE`.

## Correção

- `Enabled: false` nos random spawns de exemplo
- `Worlds: dungeon` (mundo inexistente por defeito = sem spawn natural no SMP)
- `Chance: 0.0`

## Deploy

```
server-config/mythicmobs/randomspawns/ExampleRandomSpawns.yml
  → plugins/MythicMobs/randomspawns/ExampleRandomSpawns.yml
```

`/mm reload` ou reinício.

Mobs custom em dungeons: usar `plugins/MythicMobs/spawners/` com `Worlds: NomeDoMundoDungeon` apenas.
