"""Generates the PNG textures for the Trash Can mod (no Pillow needed).

Usage: python tools/gen_textures.py <project root>
The UV regions named in the comments must match models/block/trash_can*.json.
"""
import os
import random
import struct
import sys
import zlib


def write_png(path, w, h, px):
    """px: list of rows, each row a list of (r, g, b, a)."""
    raw = bytearray()
    for y in range(h):
        raw.append(0)
        for x in range(w):
            raw.extend(px[y][x])

    def chunk(tag, data):
        c = struct.pack(">I", len(data)) + tag + data
        return c + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(bytes(raw), 9))
    png += chunk(b"IEND", b"")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as f:
        f.write(png)


def canvas(w, h, color=(0, 0, 0, 0)):
    return [[color for _ in range(w)] for _ in range(h)]


def shade(c, d):
    return (max(0, min(255, c[0] + d)), max(0, min(255, c[1] + d)), max(0, min(255, c[2] + d)), c[3])


# galvanized steel, darkest to lightest
STEEL = [(64, 68, 74, 255), (96, 102, 108, 255), (128, 134, 140, 255), (156, 162, 167, 255), (186, 191, 195, 255), (214, 218, 221, 255)]


def side():
    """Body in rows 3-15 (uv [2,3,14,16]) with vertical ribs, lid rim band in rows 0-1 (uv [1,0,15,2])."""
    rnd = random.Random(11)
    px = canvas(16, 16)
    ribs = [STEEL[4], STEEL[3], STEEL[2]]
    for y in range(16):
        for x in range(16):
            c = ribs[x % 3]
            px[y][x] = shade(c, rnd.randint(-5, 5))
    # lid rim: light top edge, darker lower edge
    for x in range(16):
        px[0][x] = shade(STEEL[5], rnd.randint(-4, 2))
        px[1][x] = shade(STEEL[3], rnd.randint(-4, 4))
        px[2][x] = STEEL[1]
    # shadow under the lid, reinforcing band in the middle, darker foot
    for x in range(16):
        px[3][x] = shade(px[3][x], -48)
        px[4][x] = shade(px[4][x], -20)
        px[9][x] = shade(STEEL[4], rnd.randint(-3, 3))
        px[10][x] = shade(STEEL[2], rnd.randint(-3, 3))
        px[14][x] = shade(px[14][x], -24)
        px[15][x] = STEEL[1]
    return px


def lid():
    """Top of the lid in uv [1,1,15,15]; the handle top uses uv [6,7,10,9]."""
    rnd = random.Random(23)
    px = canvas(16, 16)
    for y in range(16):
        for x in range(16):
            px[y][x] = shade(STEEL[3], rnd.randint(-5, 5))
    # beveled edge of the lid: light towards the top left, dark towards the bottom right
    for i in range(1, 15):
        px[1][i] = STEEL[5]
        px[i][1] = STEEL[5]
        px[14][i] = STEEL[1]
        px[i][14] = STEEL[1]
    # pressed ring around the raised middle
    for i in range(3, 13):
        px[3][i] = STEEL[2]
        px[i][3] = STEEL[2]
        px[12][i] = STEEL[4]
        px[i][12] = STEEL[4]
    # handle: dark grip with a highlight on top
    for x in range(6, 10):
        px[7][x] = STEEL[2]
        px[8][x] = STEEL[1]
    for x in range(5, 11):
        px[9][x] = shade(STEEL[2], -10)
    return px


def bottom():
    rnd = random.Random(37)
    px = canvas(16, 16)
    for y in range(16):
        for x in range(16):
            px[y][x] = shade(STEEL[1], rnd.randint(-8, 6))
    return px


def inside():
    """Seen from above when the lid is open: rim and dark inside in uv [2,2,14,14], with some trash in it."""
    rnd = random.Random(41)
    px = canvas(16, 16)
    for y in range(16):
        for x in range(16):
            px[y][x] = shade((34, 36, 40, 255), rnd.randint(-6, 6))
    for i in range(2, 14):
        px[2][i] = STEEL[4]
        px[i][2] = STEEL[4]
        px[13][i] = STEEL[3]
        px[i][13] = STEEL[3]
        px[3][i] = shade(px[3][i], -14)
        px[i][3] = shade(px[i][3], -14)
    # crumpled paper, an apple core and a bit of rotten flesh
    paper = [(5, 5), (6, 5), (5, 6), (6, 6), (7, 6), (6, 7)]
    for (x, y) in paper:
        px[y][x] = (222, 220, 210, 255) if (x + y) % 2 == 0 else (190, 188, 178, 255)
    for (x, y) in ((10, 8), (10, 9), (9, 9)):
        px[y][x] = (196, 170, 110, 255)
    px[8][10] = (120, 80, 40, 255)
    for (x, y) in ((5, 10), (6, 10), (6, 11), (7, 11)):
        px[y][x] = (128, 60, 40, 255)
    px[10][5] = (160, 84, 56, 255)
    return px


def icon():
    """32x32 front view of the can, scaled up to the 128x128 mod icon."""
    px = canvas(32, 32)
    ribs = [STEEL[4], STEEL[3], STEEL[2]]
    for y in range(10, 30):
        for x in range(7, 25):
            px[y][x] = ribs[x % 3]
    for x in range(7, 25):
        px[10][x] = STEEL[1]
        px[19][x] = STEEL[4]
        px[20][x] = STEEL[2]
        px[29][x] = STEEL[1]
    for y in range(10, 30):
        px[y][7] = STEEL[1]
        px[y][24] = STEEL[1]
    # lid and handle
    for x in range(5, 27):
        px[7][x] = STEEL[5]
        px[8][x] = STEEL[3]
        px[9][x] = STEEL[1]
    for x in range(13, 19):
        px[5][x] = STEEL[4]
        px[6][x] = STEEL[2]
    px[5][13] = px[6][13] = px[5][18] = px[6][18] = STEEL[1]
    # outline for the light launcher background
    outline = (40, 42, 46, 255)
    out = [row[:] for row in px]
    for y in range(32):
        for x in range(32):
            if px[y][x][3] == 0 and any(
                0 <= y + dy < 32 and 0 <= x + dx < 32 and px[y + dy][x + dx][3] != 0
                for (dx, dy) in ((1, 0), (-1, 0), (0, 1), (0, -1))
            ):
                out[y][x] = outline
    return [[out[y // 4][x // 4] for x in range(128)] for y in range(128)]


def main(root):
    assets = os.path.join(root, "src", "main", "resources", "assets", "trashcan")
    block = os.path.join(assets, "textures", "block")
    write_png(os.path.join(block, "trash_can_side.png"), 16, 16, side())
    write_png(os.path.join(block, "trash_can_lid.png"), 16, 16, lid())
    write_png(os.path.join(block, "trash_can_bottom.png"), 16, 16, bottom())
    write_png(os.path.join(block, "trash_can_inside.png"), 16, 16, inside())
    write_png(os.path.join(assets, "icon.png"), 128, 128, icon())


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else ".")
