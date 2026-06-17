import cv2
import numpy as np
import webcolors
from sklearn.cluster import KMeans


BASIC_COLOR_MAP = {
    "black": ["black"],
    "white": ["white", "snow", "ivory", "floralwhite", "ghostwhite", "whitesmoke"],

    "blue": [
        "blue", "navy", "darkblue", "royalblue", "skyblue",
        "dodgerblue", "steelblue", "lightblue", "midnightblue",
        "slategray", "slategrey", "lightslategray", "lightslategrey",
        "lightsteelblue", "cornflowerblue", "cadetblue"
    ],

    "gray": [
        "gray", "grey", "darkgray", "darkgrey", "lightgray",
        "lightgrey", "silver", "dimgray", "dimgrey"
    ],

    "red": ["red", "darkred", "crimson", "firebrick", "indianred"],
    "green": ["green", "darkgreen", "seagreen", "forestgreen", "olive", "lime", "mediumseagreen"],
    "yellow": ["yellow", "gold", "khaki", "lightyellow"],
    "orange": ["orange", "coral", "tomato", "orangered", "darkorange"],
    "pink": ["pink", "hotpink", "deeppink", "lightpink"],
    "purple": ["purple", "violet", "orchid", "plum", "indigo", "mediumpurple"],
    "brown": ["brown", "sienna", "chocolate", "saddlebrown", "peru", "tan"],
    "beige": ["beige", "wheat", "linen", "bisque", "antiquewhite"]
}


def rgb_to_hex(rgb):
    return "#{:02x}{:02x}{:02x}".format(
        int(rgb[0]),
        int(rgb[1]),
        int(rgb[2])
    )


def closest_css_color_name(rgb):
    min_distance = float("inf")
    closest_name = "unknown"

    for name in webcolors.names("css3"):
        css_rgb = webcolors.name_to_rgb(name)

        distance = (
                (rgb[0] - css_rgb.red) ** 2 +
                (rgb[1] - css_rgb.green) ** 2 +
                (rgb[2] - css_rgb.blue) ** 2
        )

        if distance < min_distance:
            min_distance = distance
            closest_name = name

    return closest_name


def map_to_basic_color(color_name):
    color_name = color_name.lower()

    for base_color, color_names in BASIC_COLOR_MAP.items():
        if color_name in color_names:
            return base_color

    return color_name


def fix_denim_blue(rgb, base_color):
    r, g, b = int(rgb[0]), int(rgb[1]), int(rgb[2])

    # Jean thường có B cao hơn R, nhìn hơi xám xanh
    if b > r and b >= g and (b - r) >= 18:
        return "blue"

    # Xanh jean nhạt: R/G/B gần nhau nhưng B vẫn nhỉnh hơn
    if 80 <= r <= 190 and 90 <= g <= 200 and 100 <= b <= 220:
        if b > r and (b - r) >= 10:
            return "blue"

    return base_color


def crop_bbox(image, bbox):
    h, w = image.shape[:2]

    x1 = max(0, int(bbox["x1"]))
    y1 = max(0, int(bbox["y1"]))
    x2 = min(w, int(bbox["x2"]))
    y2 = min(h, int(bbox["y2"]))

    return image[y1:y2, x1:x2]


def remove_crop_border(crop, margin_ratio=0.12):
    h, w = crop.shape[:2]

    margin_x = int(w * margin_ratio)
    margin_y = int(h * margin_ratio)

    inner_crop = crop[
                 margin_y:h - margin_y,
                 margin_x:w - margin_x
                 ]

    if inner_crop.size == 0:
        return crop

    return inner_crop


def filter_background_pixels(pixels):
    filtered = []

    for p in pixels:
        r, g, b = int(p[0]), int(p[1]), int(p[2])

        # bỏ pixel gần trắng
        if r > 245 and g > 245 and b > 245:
            continue

        # bỏ pixel gần đen
        if r < 10 and g < 10 and b < 10:
            continue

        filtered.append(p)

    if len(filtered) == 0:
        return pixels

    return np.array(filtered)


def detect_dominant_color(image_path, bbox):
    image = cv2.imread(image_path)

    if image is None:
        return {
            "name": "unknown",
            "base_color": "unknown",
            "hex": "#000000"
        }

    crop = crop_bbox(image, bbox)

    if crop is None or crop.size == 0:
        return {
            "name": "unknown",
            "base_color": "unknown",
            "hex": "#000000"
        }

    crop = remove_crop_border(crop)

    rgb_crop = cv2.cvtColor(crop, cv2.COLOR_BGR2RGB)
    pixels = rgb_crop.reshape(-1, 3)

    pixels = filter_background_pixels(pixels)

    if len(pixels) < 3:
        dominant_rgb = np.mean(pixels, axis=0).astype(int)
    else:
        k = min(3, len(pixels))

        kmeans = KMeans(
            n_clusters=k,
            random_state=42,
            n_init=10
        )

        kmeans.fit(pixels)

        counts = np.bincount(kmeans.labels_)
        dominant_rgb = kmeans.cluster_centers_[np.argmax(counts)].astype(int)

    color_name = closest_css_color_name(dominant_rgb)
    base_color = map_to_basic_color(color_name)
    base_color = fix_denim_blue(dominant_rgb, base_color)
    hex_color = rgb_to_hex(dominant_rgb)

    return {
        "name": color_name,
        "base_color": base_color,
        "hex": hex_color
    }