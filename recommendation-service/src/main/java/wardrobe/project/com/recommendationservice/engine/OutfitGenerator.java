package wardrobe.project.com.recommendationservice.engine;

import org.springframework.stereotype.Component;
import wardrobe.project.com.recommendationservice.dto.external.ClothingItemExternalDTO;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

@Component
public class OutfitGenerator {

    private String normalizeString(String s) {
        if (s == null) return "";
        String normalized = Normalizer.normalize(s, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "").toLowerCase().trim();
    }

    private String getBroadCategory(String categoryName) {
        String name = normalizeString(categoryName);

        if (name.contains("dam")) return "DRESS";

        if (name.contains("khoac")) return "OUTERWEAR";

        if (name.contains("ao")) return "TOP";

        if (name.contains("quan") || name.contains("vay")) return "BOTTOM";

        return "UNKNOWN";
    }

    public List<ClothingItemExternalDTO> generateBestOutfit(List<ClothingItemExternalDTO> rankedItems) {
        if (rankedItems == null || rankedItems.isEmpty()) return new ArrayList<>();

        List<ClothingItemExternalDTO> tops = new ArrayList<>();
        List<ClothingItemExternalDTO> bottoms = new ArrayList<>();
        List<ClothingItemExternalDTO> dresses = new ArrayList<>();
        List<ClothingItemExternalDTO> outerwears = new ArrayList<>();

        for (ClothingItemExternalDTO item : rankedItems) {
            String catName = (item.getCategory() != null) ? item.getCategory().getCategoryName() : "";

            if (catName == null || catName.trim().isEmpty()) {
                catName = item.getItemName() != null ? item.getItemName() : "";
            }

            String broadCat = getBroadCategory(catName);
            switch (broadCat) {
                case "TOP": tops.add(item); break;
                case "BOTTOM": bottoms.add(item); break;
                case "DRESS": dresses.add(item); break;
                case "OUTERWEAR": outerwears.add(item); break;
            }
        }

        List<ClothingItemExternalDTO> finalOutfit = new ArrayList<>();

        boolean hasDress = !dresses.isEmpty();
        boolean hasTopBottom = !tops.isEmpty() && !bottoms.isEmpty();

        if (hasDress && hasTopBottom) {
            int dressRank = rankedItems.indexOf(dresses.get(0));
            int topRank = rankedItems.indexOf(tops.get(0));

            if (dressRank <= topRank) {
                finalOutfit.add(dresses.get(0));
            } else {
                finalOutfit.add(tops.get(0));
                finalOutfit.add(bottoms.get(0));
            }
        } else if (hasDress) {
            finalOutfit.add(dresses.get(0));
        } else if (hasTopBottom) {
            finalOutfit.add(tops.get(0));
            finalOutfit.add(bottoms.get(0));
        } else {
            if (!tops.isEmpty()) finalOutfit.add(tops.get(0));
            else if (!bottoms.isEmpty()) finalOutfit.add(bottoms.get(0));
        }

        if (!finalOutfit.isEmpty() && !outerwears.isEmpty()) {
            finalOutfit.add(outerwears.get(0));
        }

        return finalOutfit;
    }

    public boolean isValidOutfit(List<ClothingItemExternalDTO> outfit) {
        boolean hasTop = false;
        boolean hasBottom = false;
        boolean hasDress = false;

        for (ClothingItemExternalDTO item : outfit) {
            String catName = (item.getCategory() != null) ? item.getCategory().getCategoryName() : "";
            if (catName == null || catName.trim().isEmpty()) {
                catName = item.getItemName() != null ? item.getItemName() : "";
            }

            String broadCategory = getBroadCategory(catName);
            if ("TOP".equals(broadCategory)) hasTop = true;
            if ("BOTTOM".equals(broadCategory)) hasBottom = true;
            if ("DRESS".equals(broadCategory)) hasDress = true;
        }

        return hasDress || (hasTop && hasBottom);
    }
}