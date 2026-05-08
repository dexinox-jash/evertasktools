#!/usr/bin/env python3
"""Generate PNG launcher icon fallbacks from vector descriptions."""
import os
from PIL import Image, ImageDraw

# Mint green background color from the app
BACKGROUND_COLOR = "#3EB489"

# Sizes per density (mdpi baseline is 48x48)
SIZES = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}

BASE_DIR = os.path.join(os.path.dirname(__file__), "androidApp", "src", "androidMain", "res")


def draw_icon(size):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # Background: solid mint green circle (or rounded rect for modern look)
    margin = size // 12
    draw.rounded_rectangle(
        [margin, margin, size - margin, size - margin],
        radius=size // 6,
        fill=BACKGROUND_COLOR,
    )

    # Foreground: white checkmark
    # The vector uses roughly M38,52 L48,62 L70,40 inside a 108x108 canvas
    # Scale to our size
    scale = size / 108.0
    padding = size // 6  # account for rounded rect margin
    effective_size = size - 2 * padding
    offset_x = padding
    offset_y = padding

    # Original coords in 108 space, centered in icon area
    # Let's scale them to effective_size and add offset
    def s(v):
        return int(v * scale)

    p1 = (s(38) + offset_x, s(52) + offset_y)
    p2 = (s(48) + offset_x, s(62) + offset_y)
    p3 = (s(70) + offset_x, s(40) + offset_y)

    line_width = max(2, size // 18)
    draw.line([p1, p2, p3], fill="#FFFFFF", width=line_width, joint="curve")

    return img


def main():
    for density, size in SIZES.items():
        dir_path = os.path.join(BASE_DIR, f"mipmap-{density}")
        os.makedirs(dir_path, exist_ok=True)

        icon = draw_icon(size)
        out_path = os.path.join(dir_path, "ic_launcher.png")
        icon.save(out_path, "PNG")
        print(f"Saved {out_path}")

        # Also generate round version
        round_icon = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        draw = ImageDraw.Draw(round_icon)
        draw.ellipse([0, 0, size, size], fill=BACKGROUND_COLOR)

        # Draw checkmark on round icon
        scale = size / 108.0
        offset_x = 0
        offset_y = 0

        def s(v):
            return int(v * scale)

        p1 = (s(38) + offset_x, s(52) + offset_y)
        p2 = (s(48) + offset_x, s(62) + offset_y)
        p3 = (s(70) + offset_x, s(40) + offset_y)
        line_width = max(2, size // 18)
        draw.line([p1, p2, p3], fill="#FFFFFF", width=line_width, joint="curve")

        round_out_path = os.path.join(dir_path, "ic_launcher_round.png")
        round_icon.save(round_out_path, "PNG")
        print(f"Saved {round_out_path}")


if __name__ == "__main__":
    main()
