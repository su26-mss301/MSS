CATEGORY_ATTRIBUTES = {
    "short_sleeve_top": {
        "style": ["casual", "daily"],
        "occasion": ["daily", "school", "casual outing"],
        "gender": "unisex"
    },
    "long_sleeve_top": {
        "style": ["casual", "smart casual"],
        "occasion": ["office", "school", "daily"],
        "gender": "unisex"
    },
    "short_sleeve_outwear": {
        "style": ["casual", "streetwear"],
        "occasion": ["daily", "outdoor"],
        "gender": "unisex"
    },
    "long_sleeve_outwear": {
        "style": ["formal", "smart casual"],
        "occasion": ["office", "meeting", "event"],
        "gender": "unisex"
    },
    "vest": {
        "style": ["formal", "business"],
        "occasion": ["office", "meeting", "event"],
        "gender": "unisex"
    },
    "sling": {
        "style": ["casual", "summer"],
        "occasion": ["daily", "beach", "summer"],
        "gender": "female"
    },
    "shorts": {
        "style": ["casual", "sport"],
        "occasion": ["daily", "sport", "outdoor"],
        "gender": "unisex"
    },
    "trousers": {
        "style": ["formal", "smart casual"],
        "occasion": ["office", "meeting", "daily"],
        "gender": "unisex"
    },
    "skirt": {
        "style": ["feminine", "smart casual"],
        "occasion": ["office", "date", "daily"],
        "gender": "female"
    },
    "short_sleeve_dress": {
        "style": ["elegant", "feminine"],
        "occasion": ["party", "date", "event"],
        "gender": "female"
    },
    "long_sleeve_dress": {
        "style": ["elegant", "formal"],
        "occasion": ["party", "event", "formal meeting"],
        "gender": "female"
    },
    "vest_dress": {
        "style": ["formal", "elegant"],
        "occasion": ["office", "event", "meeting"],
        "gender": "female"
    },
    "sling_dress": {
        "style": ["elegant", "summer"],
        "occasion": ["party", "date", "beach"],
        "gender": "female"
    }
}


def infer_attributes(class_name):
    default_attributes = {
        "style": ["unknown"],
        "occasion": ["unknown"],
        "gender": "unknown"
    }

    return CATEGORY_ATTRIBUTES.get(class_name, default_attributes)