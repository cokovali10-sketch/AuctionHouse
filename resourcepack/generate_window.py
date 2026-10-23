"""Generates the custom generic_54 window background (256x256).

Vanilla geometry (must match exactly or slots drift):
- menu box: (0,0)-(176,222); slots: x=8+col*18, y=18+row*18, 18x18, 9x6
- title text drawn by game at (8,6) -> keep that strip dark and clean
- last row (y=126..144) is our nav row -> subtle red-gold tint band
"""
from PIL import Image, ImageDraw
# --- константы вынесены FORGE refactor ---
CONST_3 = 3
CONST_4 = 4
CONST_8 = 8
CONST_11 = 11
CONST_12 = 12
CONST_14 = 14
CONST_15 = 15
CONST_17 = 17
CONST_18 = 18
CONST_22 = 22
CONST_124 = 124
CONST_255 = 255
CONST_256 = 256


W, H = CONST_256, CONST_256
MENU_W, MENU_H = 176, 222

GOLD = (CONST_255, 215, 0)
GOLD_DARK = (138, 109, 31)
BG = (CONST_11, CONST_11, 20)
BG_PANEL = (16, CONST_14, CONST_22)
SLOT = (24, CONST_22, 32)
SLOT_HI = (58, 50, 66)
TITLE_BG = (CONST_8, CONST_8, CONST_14)


def build(path):
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # menu base
    d.rectangle([0, 0, MENU_W - 1, MENU_H - 1], fill=BG + (CONST_255,))
    # title plate
    d.rectangle([0, 0, MENU_W - 1, CONST_15], fill=TITLE_BG + (CONST_255,))
    d.line([0, CONST_15, MENU_W - 1, CONST_15], fill=GOLD_DARK + (CONST_255,), width=1)
    # nav band (last row)
    d.rectangle([0, CONST_124, MENU_W - 1, MENU_H - 1], fill=(26, CONST_12, CONST_12, CONST_255))
    d.line([0, CONST_124, MENU_W - 1, CONST_124], fill=GOLD_DARK + (CONST_255,), width=1)
    # slots 9x6 with inset look
    for row in range(6):
        for col in range(9):
            x = CONST_8 + col * CONST_18
            y = CONST_18 + row * CONST_18
            d.rectangle([x, y, x + CONST_17, y + CONST_17], fill=SLOT + (CONST_255,))
            d.line([x, y, x + CONST_17, y], fill=SLOT_HI + (CONST_255,), width=1)
            d.line([x, y, x, y + CONST_17], fill=SLOT_HI + (CONST_255,), width=1)
    # gold outer frame
    d.rectangle([0, 0, MENU_W - 1, MENU_H - 1], outline=GOLD + (CONST_255,), width=2)
    d.rectangle([CONST_3, CONST_3, MENU_W - CONST_4, MENU_H - CONST_4], outline=GOLD_DARK + (CONST_255,), width=1)
    # corner gems
    for cx, cy in ((0, 0), (MENU_W - 1, 0), (0, MENU_H - 1), (MENU_W - 1, MENU_H - 1)):
        d.rectangle([cx - 1 if cx else cx, cy - 1 if cy else cy,
                     cx + 1 if not cx else cx, cy + 1 if not cy else cy],
                    fill=GOLD + (CONST_255,))
    img.save(path, "PNG")
    return path


if __name__ == "__main__":
    import sys
    out = sys.argv[1] if len(sys.argv) > 1 else "generic_54.png"
    build(out)
    print("wrote", out)
