# Hygiene Mod

Système de propreté progressive pour serveurs RP Minecraft. Les joueurs accumulent de la saleté avec le temps et doivent se laver régulièrement, sous peine d'effets de nausée pour eux-mêmes et leur entourage.

![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-62B47A)
![Forge](https://img.shields.io/badge/Forge-47.4.x-1E2D4F)
![Version](https://img.shields.io/badge/version-1.0.7-blue)

## Mod ID
`hygiene`

## Compatibilité
- Minecraft 1.20.1
- Forge 47.4.x
- Conquest Reforged (bloc `wooden_washing_tub`)

## Fonctionnalités

### Niveaux de saleté (0 à 4)
La saleté progresse avec le temps. Aux niveaux élevés, le joueur subit de la nausée et des messages RP, et incommode les joueurs proches.

| Niveau | Effet |
|---|---|
| 0 — Propre | Aucun |
| 1 — Léger | Message privé |
| 2 — Moyen | Les joueurs proches (< 3 blocs) sont notifiés |
| 3 — Fort | Nausée sur soi + voisins après 5 s de proximité |
| 4 — Critique | Nausée immédiate sur soi + tous les joueurs < 5 blocs |

### Méthodes de nettoyage
- **Baquet** (bloc Conquest rempli) : 1 minute de trempage → très efficace
- **Eau naturelle** (lac, rivière, mer, 2×2×2) : 2 minutes → moins efficace

### Savon Viking
Item utilisable pendant un bain : +30 min de bonus en baquet, +15 min en eau naturelle.

### Immunité créatif
Les joueurs en mode créatif ne se salissent jamais.

## Changelog

### 1.0.7
- Build de maintenance, alignement de version avec les autres mods du serveur.

## Licence
CC BY-NC-ND 4.0 — Akirabane.
