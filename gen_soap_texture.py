"""
Génération du pixel art 16x16 pour le Savon Viking.

Palette viking — cire d'os + suif + cendres de bois :
  • Teinte chaude crème/ivoire pour le corps (suif rendu)
  • Stries gris-brun cendrées, en diagonale (cendres mêlées)
  • Face supérieure plus claire (spéculaire top-gauche)
  • Bande séparatrice + bordures sombres pour la profondeur
"""

from PIL import Image

# ── Palette ──────────────────────────────────────────────
K  = ( 50,  25,  10, 255)   # bordure / contour sombre
W  = (255, 250, 228, 255)   # spéculaire blanc-crème
T1 = (242, 224, 168, 255)   # face du dessus — lumière
T2 = (222, 204, 148, 255)   # face du dessus — mi-ton
T3 = (198, 180, 130, 255)   # face du dessus — ombre (bord droit)
F1 = (228, 210, 160, 255)   # face avant — haut lumière
F2 = (210, 190, 140, 255)   # face avant — ton principal
F3 = (188, 168, 120, 255)   # face avant — ombre progressive
F4 = (164, 144, 100, 255)   # face avant — ombre bord droit
A1 = (158, 145, 124, 255)   # strie cendres — claire
A2 = (108,  98,  80, 255)   # strie cendres — moyenne
A3 = ( 70,  62,  50, 255)   # strie cendres — foncée
B1 = (142, 122,  82, 255)   # bande basse — ombre douce
B2 = (112,  94,  58, 255)   # bande basse — ombre forte
Z  = (  0,   0,   0,   0)   # transparent

# ── Carte pixel par pixel (16 lignes × 16 colonnes) ──────
# Savon : colonnes 2-13 (12px large), lignes 2-13 (12px haut)
# Intérieur : colonnes 3-12 (10px), lignes 3-12
# Structure :
#   L2     : arête supérieure (K plein)
#   L3-L4  : face du dessus (crème clair → ombre droite)
#   L5     : ligne de séparation (K plein) — donne le volume
#   L6     : rangée haute de la face avant (lumière directe)
#   L7-L11 : face avant avec stries de cendres en diagonale
#   L12    : bande d'ombre basse (hétérogène)
#   L13    : arête inférieure (K plein)

# fmt: off
data = [
# col: 0   1    2    3    4    5    6    7    8    9   10   11   12   13   14   15
  [Z,  Z,  Z,   Z,   Z,   Z,   Z,   Z,   Z,   Z,   Z,   Z,   Z,   Z,  Z,  Z],  # L0
  [Z,  Z,  Z,   Z,   Z,   Z,   Z,   Z,   Z,   Z,   Z,   Z,   Z,   Z,  Z,  Z],  # L1
  [Z,  Z,  K,   K,   K,   K,   K,   K,   K,   K,   K,   K,   K,   K,  Z,  Z],  # L2  ┌ arête sup
  [Z,  Z,  K,   W,  T1,  T1,  T1,  T1,  T1,  T1,  T1,  T2,  T3,  K,  Z,  Z],  # L3  │ face top (spéculaire coin haut-gauche)
  [Z,  Z,  K,  T1,  T2,  T2,  T2,  T2,  T2,  T2,  T3,  T3,  T3,  K,  Z,  Z],  # L4  │ face top (ombre se renforce vers la droite)
  [Z,  Z,  K,   K,   K,   K,   K,   K,   K,   K,   K,   K,   K,   K,  Z,  Z],  # L5  └ séparateur (profondeur)
  [Z,  Z,  K,  F1,  F1,  F1,  F1,  F1,  F1,  F1,  F1,  F1,  F1,  K,  Z,  Z],  # L6    rangée lumière directe
  [Z,  Z,  K,  F1,  A1,  F2,  F2,  A2,  F2,  F2,  F2,  A1,  F2,  K,  Z,  Z],  # L7  ┐ stries cendres — diagonale 1
  [Z,  Z,  K,  F2,  F2,  F2,  A1,  F2,  F2,  A3,  F2,  F2,  F2,  K,  Z,  Z],  # L8  │         — décalage +2
  [Z,  Z,  K,  F2,  F2,  A2,  F2,  F2,  F2,  F2,  F2,  A2,  F3,  K,  Z,  Z],  # L9  │         — l'ombre s'installe à droite
  [Z,  Z,  K,  F2,  A1,  F2,  F2,  F2,  A3,  F2,  F2,  F3,  F4,  K,  Z,  Z],  # L10 │ ombre progressive bord droit
  [Z,  Z,  K,  F3,  F3,  F3,  A2,  F3,  F3,  A1,  F3,  F3,  F4,  K,  Z,  Z],  # L11 └ zone d'ombre
  [Z,  Z,  K,  B1,  B2,  B1,  B1,  B2,  B1,  B1,  B2,  B1,  B2,  K,  Z,  Z],  # L12   bande d'ombre basse (irrégulière)
  [Z,  Z,  K,   K,   K,   K,   K,   K,   K,   K,   K,   K,   K,   K,  Z,  Z],  # L13 arête inférieure
  [Z,  Z,  Z,   Z,   Z,   Z,   Z,   Z,   Z,   Z,   Z,   Z,   Z,   Z,  Z,  Z],  # L14
  [Z,  Z,  Z,   Z,   Z,   Z,   Z,   Z,   Z,   Z,   Z,   Z,   Z,   Z,  Z,  Z],  # L15
]
# fmt: on

# Validation
assert len(data) == 16, "Doit avoir 16 lignes"
for i, row in enumerate(data):
    assert len(row) == 16, f"Ligne {i} : {len(row)} colonnes (attendu 16)"

# Génération PNG
img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
px = img.load()
for y, row in enumerate(data):
    for x, color in enumerate(row):
        px[x, y] = color

out = r"d:\disque\dev\Mod minecraft\Hygiene Mod\src\main\resources\assets\hygiene\textures\item\viking_soap.png"
img.save(out)
print(f"Texture sauvegardée : {out}")

# Aperçu ASCII (utile pour vérifier)
chars = {
    Z: ' ', K: '█', W: '·', T1: 't', T2: 'T', T3: 'Ṯ',
    F1: 'f', F2: 'F', F3: 'Ḟ', F4: 'Ƒ',
    A1: 'a', A2: 'A', A3: '▒',
    B1: 'b', B2: 'B',
}
print("\nAperçu (16×16) :")
for row in data:
    print(''.join(chars.get(c, '?') for c in row))
