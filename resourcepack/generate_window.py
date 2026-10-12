"""Generates the custom generic_54 window background (256x256).

Vanilla geometry (must match exactly or slots drift):
- menu box: (0,0)-(176,222); slots: x=8+col*18, y=18+row*18, 18x18, 9x6
- title text drawn by game at (8,6) -> keep that strip dark and clean
- last row (y=126..144) is our nav row -> subtle red-gold tint band
"""
from PIL import Image, ImageDraw

W, H = 256, 256
MENU_W, MENU_H = 176, 222

GOLD = (255, 215, 0)
GOLD_DARK = (138, 109, 31)
BG = (11, 11, 20)
BG_PANEL = (16, 14, 22)
SLOT = (24, 22, 32)
SLOT_HI = (58, 50, 66)
TITLE_BG = (8, 8, 14)


def build(path):
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # menu base
    d.rectangle([0, 0, MENU_W - 1, MENU_H - 1], fill=BG + (255,))
    # title plate
    d.rectangle([0, 0, MENU_W - 1, 15], fill=TITLE_BG + (255,))
    d.line([0, 15, MENU_W - 1, 15], fill=GOLD_DARK + (255,), width=1)
    # nav band (last row)
    d.rectangle([0, 124, MENU_W - 1, MENU_H - 1], fill=(26, 12, 12, 255))
    d.line([0, 124, MENU_W - 1, 124], fill=GOLD_DARK + (255,), width=1)
    # slots 9x6 with inset look
    for row in range(6):
        for col in range(9):
            x = 8 + col * 18
            y = 18 + row * 18
            d.rectangle([x, y, x + 17, y + 17], fill=SLOT + (255,))
            d.line([x, y, x + 17, y], fill=SLOT_HI + (255,), width=1)
            d.line([x, y, x, y + 17], fill=SLOT_HI + (255,), width=1)
    # gold outer frame
    d.rectangle([0, 0, MENU_W - 1, MENU_H - 1], outline=GOLD + (255,), width=2)
    d.rectangle([3, 3, MENU_W - 4, MENU_H - 4], outline=GOLD_DARK + (255,), width=1)
    # corner gems
    for cx, cy in ((0, 0), (MENU_W - 1, 0), (0, MENU_H - 1), (MENU_W - 1, MENU_H - 1)):
        d.rectangle([cx - 1 if cx else cx, cy - 1 if cy else cy,
                     cx + 1 if not cx else cx, cy + 1 if not cy else cy],
                    fill=GOLD + (255,))
    img.save(path, "PNG")
    return path


if __name__ == "__main__":
    import sys
    out = sys.argv[1] if len(sys.argv) > 1 else "generic_54.png"
    build(out)
    print("wrote", out)
