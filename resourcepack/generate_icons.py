"""Gold UI icon set (16x16 pixel art) + menu space font.

Icons: nav_back (chevron), nav_close (X), search (magnifier).
Font mythicauction:menu: space advances for pixel-exact title decor.
"""
from PIL import Image, ImageDraw

GOLD = (255, 215, 0, 255)
GOLD_D = (160, 125, 20, 255)
DARK = (20, 16, 24, 255)


def _base():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    return img, ImageDraw.Draw(img)


def chevron(path):
    img, d = _base()
    for i in range(7):
        d.point((10 - i, 2 + i), fill=GOLD)
        d.point((10 - i, 13 - i), fill=GOLD)
        if i < 5:
            d.point((11 - i, 3 + i), fill=GOLD_D)
            d.point((11 - i, 12 - i), fill=GOLD_D)
    img.save(path)


def cross(path):
    img, d = _base()
    for i in range(10):
        d.point((3 + i, 3 + i), fill=GOLD)
        d.point((12 - i, 3 + i), fill=GOLD)
        d.point((4 + i, 3 + i), fill=GOLD_D)
        d.point((11 - i, 3 + i), fill=GOLD_D)
    img.save(path)


def magnifier(path):
    img, d = _base()
    for x in range(16):
        for y in range(16):
            dx, dy = x - 6, y - 6
            dist = (dx * dx + dy * dy) ** 0.5
            if 3.4 < dist < 5.2:
                img.putpixel((x, y), GOLD)
            elif 2.6 < dist <= 3.4:
                img.putpixel((x, y), DARK)
    for i in range(5):
        img.putpixel((9 + i, 9 + i), GOLD)
        img.putpixel((10 + i, 9 + i), GOLD_D)
    img.save(path)


if __name__ == "__main__":
    import sys
    out = sys.argv[1]
    chevron(f"{out}/nav_back.png")
    cross(f"{out}/nav_close.png")
    magnifier(f"{out}/search.png")
    print("icons done")
