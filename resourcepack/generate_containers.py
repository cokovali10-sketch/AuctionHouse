"""Full chest-size set in one dark-gold theme (vanilla geometry).

Vanilla menu box per rows N: width 176, top strip 0..17, slots start y=18,
row pitch 18, bottom margin 7 -> height = 18 + 18*N + 7.
Files: assets/minecraft/textures/gui/container/generic_9x<N>.png (256x256).
"""
from PIL import Image, ImageDraw

GOLD = (255, 215, 0)
GOLD_DARK = (138, 109, 31)
BG = (11, 11, 20)
TITLE_BG = (8, 8, 14)
SLOT = (24, 22, 32)
SLOT_HI = (58, 50, 66)


def build(rows, path):
    menu_h = 18 + 18 * rows + 7
    img = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 175, menu_h - 1], fill=BG + (255,))
    d.rectangle([0, 0, 175, 13], fill=TITLE_BG + (255,))
    d.line([0, 13, 175, 13], fill=GOLD_DARK + (255,), width=1)
    for r in range(rows):
        for c in range(9):
            x = 8 + c * 18
            y = 18 + r * 18
            d.rectangle([x, y, x + 17, y + 17], fill=SLOT + (255,))
            d.line([x, y, x + 17, y], fill=SLOT_HI + (255,), width=1)
            d.line([x, y, x, y + 17], fill=SLOT_HI + (255,), width=1)
    # last row band (nav)
    y0 = 18 + (rows - 1) * 18 - 2
    d.rectangle([0, y0, 175, menu_h - 1], fill=(26, 12, 12, 255))
    for r in range(rows):
        for c in range(9):
            x = 8 + c * 18
            y = 18 + r * 18
            if y + 17 >= y0:
                d.rectangle([x, y, x + 17, y + 17], fill=SLOT + (255,))
                d.line([x, y, x + 17, y], fill=SLOT_HI + (255,), width=1)
                d.line([x, y, x, y + 17], fill=SLOT_HI + (255,), width=1)
    d.rectangle([0, 0, 175, menu_h - 1], outline=GOLD + (255,), width=2)
    d.rectangle([3, 3, 172, menu_h - 4], outline=GOLD_DARK + (255,), width=1)
    for cx, cy in ((0, 0), (175, 0), (0, menu_h - 1), (175, menu_h - 1)):
        x0 = max(cx - 1, 0)
        y0_ = max(cy - 1, 0)
        x1 = min(cx + 1, 175)
        y1 = min(cy + 1, menu_h - 1)
        d.rectangle([x0, y0_, x1, y1], fill=GOLD + (255,))
    img.save(path, "PNG")
    return path


if __name__ == "__main__":
    import sys
    out = sys.argv[1] if len(sys.argv) > 1 else "."
    import os
    for n in (1, 2, 3, 4, 5, 6):
        build(n, os.path.join(out, f"generic_9x{n}.png"))
        print("wrote", n)
