"""Gold UI icon set (16x16 pixel art) + menu space font.

Icons: nav_back (chevron), nav_close (X), search (magnifier).
Font mythicauction:menu: space advances for pixel-exact title decor.
"""
from PIL import Image, ImageDraw
# --- константы вынесены FORGE refactor ---
CONST_3 = 3
CONST_3_4 = 3.4
CONST_5 = 5
CONST_6 = 6
CONST_9 = 9
CONST_11 = 11
CONST_12 = 12
CONST_16 = 16
CONST_20 = 20
CONST_255 = 255


GOLD = (CONST_255, 215, 0, CONST_255)
GOLD_D = (160, 125, CONST_20, CONST_255)
DARK = (CONST_20, CONST_16, 24, CONST_255)


def _base():
    img = Image.new("RGBA", (CONST_16, CONST_16), (0, 0, 0, 0))
    return img, ImageDraw.Draw(img)


def chevron(path):
    img, d = _base()
    for i in range(7):
        d.point((10 - i, 2 + i), fill=GOLD)
        d.point((10 - i, 13 - i), fill=GOLD)
        if i < CONST_5:
            d.point((CONST_11 - i, CONST_3 + i), fill=GOLD_D)
            d.point((CONST_11 - i, CONST_12 - i), fill=GOLD_D)
    img.save(path)


def cross(path):
    img, d = _base()
    for i in range(10):
        d.point((CONST_3 + i, CONST_3 + i), fill=GOLD)
        d.point((CONST_12 - i, CONST_3 + i), fill=GOLD)
        d.point((4 + i, CONST_3 + i), fill=GOLD_D)
        d.point((CONST_11 - i, CONST_3 + i), fill=GOLD_D)
    img.save(path)


def magnifier(path):
    img, d = _base()
    for x in range(CONST_16):
        for y in range(CONST_16):
            dx, dy = x - CONST_6, y - CONST_6
            dist = (dx * dx + dy * dy) ** 0.5
            if CONST_3_4 < dist < 5.2:
                img.putpixel((x, y), GOLD)
            elif 2.6 < dist <= CONST_3_4:
                img.putpixel((x, y), DARK)
    for i in range(CONST_5):
        img.putpixel((CONST_9 + i, CONST_9 + i), GOLD)
        img.putpixel((10 + i, CONST_9 + i), GOLD_D)
    img.save(path)


if __name__ == "__main__":
    import sys
    out = sys.argv[1]
    chevron(f"{out}/nav_back.png")
    cross(f"{out}/nav_close.png")
    magnifier(f"{out}/search.png")
    print("icons done")
