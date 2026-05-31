from PIL import Image, ImageDraw
import math
import os

def gen_icon(size, output_path):
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    s = size / 1024
    cx, cy = size // 2, size // 2

    bg_r = int(480 * s)
    for y_img in range(size):
        for x_img in range(size):
            d = math.sqrt((x_img - cx) ** 2 + (y_img - cy) ** 2)
            if d <= bg_r:
                t = d / bg_r
                r = int(255)
                g = int(245 - t * 30)
                b = int(248 - t * 22)
                img.putpixel((x_img, y_img), (r, g, b, 255))

    cup_body = [
        (int(300*s), int(300*s)),
        (int(720*s), int(300*s)),
        (int(690*s), int(880*s)),
        (int(330*s), int(880*s)),
    ]
    draw.polygon(cup_body, fill=(255, 175, 210, 255))

    water = [
        (int(320*s), int(500*s)),
        (int(700*s), int(500*s)),
        (int(688*s), int(875*s)),
        (int(332*s), int(875*s)),
    ]
    draw.polygon(water, fill=(180, 225, 255, 230))

    wave_points = []
    for i in range(40):
        x = int((320 + (700 - 320) * i / 39) * s)
        y = int((500 + 6 * math.sin(i * 0.8)) * s)
        wave_points.append((x, y))
    wave_points_rev = list(reversed([(x, int(y + 12*s)) for x, y in wave_points]))
    draw.polygon(wave_points + wave_points_rev, fill=(150, 210, 255, 180))

    rim = [
        (int(280*s), int(285*s)),
        (int(740*s), int(285*s)),
        (int(720*s), int(315*s)),
        (int(300*s), int(315*s)),
    ]
    draw.polygon(rim, fill=(255, 155, 195, 255))

    draw.ellipse([int(285*s), int(280*s), int(735*s), int(310*s)],
                 fill=(255, 190, 218, 255))
    draw.ellipse([int(300*s), int(285*s), int(720*s), int(305*s)],
                 fill=(200, 232, 255, 255))

    straw_w = int(22 * s)

    for xx_off in range(-straw_w//2, straw_w//2 + 1):
        for yy in range(int(100*s), int(290*s)):
            stripe = (yy - int(100*s)) // int(28 * s)
            if stripe % 2 == 0:
                color = (255, 120, 165, 255)
            else:
                color = (255, 245, 250, 255)
            px = int(520*s) + xx_off
            py = yy
            if 0 <= px < size and 0 <= py < size:
                img.putpixel((px, py), color)

    bend_cx = int(560 * s)
    bend_cy = int(115 * s)
    bend_r = int(55 * s)
    for angle in range(0, 95, 1):
        rad = math.radians(angle)
        bx = int(bend_cx + bend_r * math.sin(rad))
        by = int(bend_cy + bend_r - bend_r * math.cos(rad))
        for w in range(-straw_w//2, straw_w//2 + 1):
            stripe = angle // 18
            if stripe % 2 == 0:
                color = (255, 120, 165, 255)
            else:
                color = (255, 245, 250, 255)
            px = bx + w
            py = by
            if 0 <= px < size and 0 <= py < size:
                img.putpixel((px, py), color)

    tip_sx = bend_cx + bend_r
    tip_sy = bend_cy - int(10*s)
    tip_ex = int(tip_sx + 100*s * math.cos(math.radians(20)))
    tip_ey = int(tip_sy - 100*s * math.sin(math.radians(20)))
    for i in range(120):
        t = i / 119
        xx = int(tip_sx + (tip_ex - tip_sx) * t)
        yy = int(tip_sy + (tip_ey - tip_sy) * t)
        for w in range(-straw_w//2, straw_w//2 + 1):
            stripe = i // 10
            if stripe % 2 == 0:
                color = (255, 120, 165, 255)
            else:
                color = (255, 245, 250, 255)
            dx = tip_ex - tip_sx
            dy = tip_ey - tip_sy
            dl = math.sqrt(dx*dx + dy*dy)
            if dl > 0:
                px = int(xx + w * (-dy) / dl)
                py = int(yy + w * dx / dl)
                if 0 <= px < size and 0 <= py < size:
                    img.putpixel((px, py), color)

    def draw_heart(hx, hy, hs, color):
        points = []
        for i in range(80):
            t = 2 * math.pi * i / 80
            x = hs * 16 * math.sin(t) ** 3
            y = -hs * (13 * math.cos(t) - 5 * math.cos(2*t) - 2 * math.cos(3*t) - math.cos(4*t))
            points.append((int(hx + x / 16), int(hy + y / 16)))
        draw.polygon(points, fill=color)

    draw_heart(int(180*s), int(400*s), int(28*s), (255, 130, 170, 180))
    draw_heart(int(830*s), int(340*s), int(22*s), (255, 130, 170, 160))
    draw_heart(int(850*s), int(680*s), int(18*s), (255, 150, 185, 140))
    draw_heart(int(165*s), int(730*s), int(20*s), (255, 140, 178, 150))

    star_positions = [(int(170*s), int(580*s)), (int(850*s), int(530*s)),
                      (int(190*s), int(280*s)), (int(830*s), int(250*s))]
    for sx, sy in star_positions:
        sr = int(14 * s)
        for a in range(4):
            a1 = math.radians(a * 90 - 90)
            x1 = int(sx + sr * math.cos(a1))
            y1 = int(sy + sr * math.sin(a1))
            draw.line([(sx, sy), (x1, y1)], fill=(255, 225, 120, 210), width=max(1, int(3*s)))
        draw.ellipse([sx - int(3*s), sy - int(3*s), sx + int(3*s), sy + int(3*s)],
                     fill=(255, 240, 160, 200))

    img.save(output_path)
    print(f"Generated: {output_path} ({size}x{size})")

sizes = {
    'mipmap-mdpi': 48,
    'mipmap-hdpi': 72,
    'mipmap-xhdpi': 96,
    'mipmap-xxhdpi': 144,
    'mipmap-xxxhdpi': 192,
}

base = '/Users/hejinlin/Documents/RN/WaterReminder/android/app/src/main/res'
for folder, size in sizes.items():
    path = os.path.join(base, folder, 'ic_launcher.png')
    gen_icon(size, path)
    round_path = os.path.join(base, folder, 'ic_launcher_round.png')
    gen_icon(size, round_path)

print("Done!")
