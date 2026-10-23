"""Full chest-size set in one dark-gold theme (vanilla geometry).

Vanilla menu box per rows N: width 176, top strip 0..17, slots start y=18,
row pitch 18, bottom margin 7 -> height = 18 + 18*N + 7.
Files: assets/minecraft/textures/gui/container/generic_9x<N>.png (256x256).
"""
from PIL import Image, ImageDraw
# --- константы вынесены FORGE refactor ---
CONST_3 = 3
CONST_4 = 4
CONST_8 = 8
CONST_9 = 9
CONST_11 = 11
CONST_12 = 12
CONST_13 = 13
CONST_17 = 17
CONST_18 = 18
CONST_175 = 175
CONST_255 = 255
CONST_256 = 256


GOLD = (CONST_255, 215, 0)
GOLD_DARK = (138, 109, 31)
BG = (CONST_11, CONST_11, 20)
TITLE_BG = (CONST_8, CONST_8, 14)
SLOT = (24, 22, 32)
SLOT_HI = (58, 50, 66)


def build(rows, path):
    menu_h = CONST_18 + CONST_18 * rows + 7
    img = Image.new("RGBA", (CONST_256, CONST_256), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, CONST_175, menu_h - 1], fill=BG + (CONST_255,))
    d.rectangle([0, 0, CONST_175, CONST_13], fill=TITLE_BG + (CONST_255,))
    d.line([0, CONST_13, CONST_175, CONST_13], fill=GOLD_DARK + (CONST_255,), width=1)
    for r in range(rows):
        for c in range(CONST_9):
            x = CONST_8 + c * CONST_18
            y = CONST_18 + r * CONST_18
            d.rectangle([x, y, x + CONST_17, y + CONST_17], fill=SLOT + (CONST_255,))
            d.line([x, y, x + CONST_17, y], fill=SLOT_HI + (CONST_255,), width=1)
            d.line([x, y, x, y + CONST_17], fill=SLOT_HI + (CONST_255,), width=1)
    # last row band (nav)
    y0 = CONST_18 + (rows - 1) * CONST_18 - 2
    d.rectangle([0, y0, CONST_175, menu_h - 1], fill=(26, CONST_12, CONST_12, CONST_255))
    for r in range(rows):
        for c in range(CONST_9):
            x = CONST_8 + c * CONST_18
            y = CONST_18 + r * CONST_18
            if y + CONST_17 >= y0:
                d.rectangle([x, y, x + CONST_17, y + CONST_17], fill=SLOT + (CONST_255,))
                d.line([x, y, x + CONST_17, y], fill=SLOT_HI + (CONST_255,), width=1)
                d.line([x, y, x, y + CONST_17], fill=SLOT_HI + (CONST_255,), width=1)
    d.rectangle([0, 0, CONST_175, menu_h - 1], outline=GOLD + (CONST_255,), width=2)
    d.rectangle([CONST_3, CONST_3, 172, menu_h - CONST_4], outline=GOLD_DARK + (CONST_255,), width=1)
    for cx, cy in ((0, 0), (CONST_175, 0), (0, menu_h - 1), (CONST_175, menu_h - 1)):
        x0 = max(cx - 1, 0)
        y0_ = max(cy - 1, 0)
        x1 = min(cx + 1, CONST_175)
        y1 = min(cy + 1, menu_h - 1)
        d.rectangle([x0, y0_, x1, y1], fill=GOLD + (CONST_255,))
    img.save(path, "PNG")
    return path


if __name__ == "__main__":
    import sys
    out = sys.argv[1] if len(sys.argv) > 1 else "."
    import os
    for n in (1, 2, CONST_3, CONST_4, 5, 6):
        build(n, os.path.join(out, f"generic_9x{n}.png"))
        print("wrote", n)
