package com.hyp.translation;

import com.fasterxml.jackson.databind.JsonNode;
import com.hyp.request.PosDataRequest;
import java.util.*;
import lombok.extern.slf4j.Slf4j;

/**
 * MenuExtractorId
 *
 * Features:
 *  - ID MODE: useExternalId OR generate POS IDs
 *  - Stores externalId + source="SWIGGY"
 *  - Variation pricing resolved via pricingModels
 *  - Addon groups deduplicated by name
 *  - Automatically applies CGST + SGST taxes to each item
 *
 *  This class is fully production-ready and logs with SLF4J.
 */
@Slf4j
public class SwiggyMenuTranslation {

    private static final String SOURCE = "SWIGGY";
    private static final String SWIGGY_IMG = "https://media-assets.swiggy.com/swiggy/image/upload/";

    private static final Map<String, PosDataRequest.AddonGroupRequest> addonGroupByName = new LinkedHashMap<>();

    private static List<PosDataRequest.VariationRequest> globalVariations;
    private static List<PosDataRequest.AddonGroupRequest> globalAddonGroups;

    // Default tax IDs (generated per extraction)
    private static String cgstId;
    private static String sgstId;

    // ----------------------------------------------------------
    // Utility — Generate new internal POS ID
    // ----------------------------------------------------------
    public static String generateId(String prefix) {
        String core = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return prefix + "_" + core;
    }

    // ======================================================================
    // ENTRY
    // ======================================================================
    public static PosDataRequest runExtraction(JsonNode root, boolean useExternalId) {

        long start = System.currentTimeMillis();
        log.info("Starting Menu extraction ID mode = {}", useExternalId ? "EXTERNAL" : "INTERNAL");

        addonGroupByName.clear();
        globalVariations = new ArrayList<>();
        globalAddonGroups = new ArrayList<>();

        List<PosDataRequest.RestaurantRequest> restaurants = new ArrayList<>();
        List<PosDataRequest.CategoryRequest> categories = new ArrayList<>();
        List<PosDataRequest.ItemRequest> items = new ArrayList<>();

        // ----------------------------------------------------------
        // Generate CGST + SGST once per extraction
        // ----------------------------------------------------------
        cgstId = generateId("tax");
        sgstId = generateId("tax");

        List<PosDataRequest.TaxRequest> taxes = buildBaseTaxes();
        log.info("Generated default taxes → CGST={}, SGST={}", cgstId, sgstId);

        JsonNode dataNode = root.path("data");
        JsonNode cardsNode = dataNode.path("cards");

        // ----------------------------------------------------------
        // Restaurant extraction
        // ----------------------------------------------------------
        if (cardsNode.isArray()) {
            for (JsonNode wrapper : cardsNode) {
                JsonNode card = wrapper.path("card").path("card");
                if (card.path("@type").asText("").contains("swiggy.presentation.food.v2.Restaurant")) {
                    restaurants.add(extractRestaurant(card.path("info"), useExternalId));
                }
            }
        }

        if (restaurants.isEmpty()) {
            log.warn("WARNING: No restaurant node found in Payload.");
        }

        // ----------------------------------------------------------
        // Category + Item extraction
        // ----------------------------------------------------------
        JsonNode grouped = findGroupedCard(cardsNode, dataNode);
        JsonNode regularCards = grouped.path("cardGroupMap").path("REGULAR").path("cards");

        if (!regularCards.isArray()) {
            log.error("Payload JSON does not contain REGULAR → cards. Extraction aborted.");
        } else {
            for (JsonNode section : regularCards) {

                JsonNode card = section.path("card").path("card");
                if (!card.path("@type").asText("").contains("ItemCategory")) continue;

                PosDataRequest.CategoryRequest category = extractCategory(card, useExternalId);
                categories.add(category);

                JsonNode itemCards = card.path("itemCards");
                if (!itemCards.isArray()) continue;

                for (JsonNode itm : itemCards) {
                    JsonNode info = itm.path("card").path("info");
                    if (info.isMissingNode()) continue;

                    PosDataRequest.ItemRequest item = extractItem(info, category.getCategoryid(), useExternalId);

                    // assign CGST,SGST by default
                    item.setItem_tax(cgstId + "," + sgstId);

                    items.add(item);
                    globalVariations.addAll(item.getVariation());
                    globalAddonGroups.addAll(item.getAddon());
                }
            }
        }

        // Deduplicate addon groups
        List<PosDataRequest.AddonGroupRequest> dedupedAddons = new ArrayList<>(addonGroupByName.values());

        // ----------------------------------------------------------
        // Build final response
        // ----------------------------------------------------------
        PosDataRequest out = new PosDataRequest();
        out.setSuccess("1");
        out.setRestaurants(restaurants);
        out.setCategories(categories);
        out.setItems(items);
        out.setVariations(globalVariations);
        out.setAddongroups(dedupedAddons);
        out.setTaxes(taxes);
        out.setServerdatetime(new Date().toString());
        out.setHttp_code(200);

        log.info(
                "Extraction complete. Items={}, Categories={}, Variations={}, AddonGroups={}",
                items.size(),
                categories.size(),
                globalVariations.size(),
                dedupedAddons.size());
        log.info("Total extraction time: {} ms", (System.currentTimeMillis() - start));

        return out;
    }

    // ======================================================================
    // TAX GENERATION
    // ======================================================================
    private static List<PosDataRequest.TaxRequest> buildBaseTaxes() {

        PosDataRequest.TaxRequest cgst = PosDataRequest.TaxRequest.builder()
                .taxid(cgstId)
                .taxname("CGST")
                .tax_taxtype("1")
                .tax_coreortotal("2")
                .tax("2.5")
                .tax_ordertype("1,2,3")
                .active("1")
                .build();

        PosDataRequest.TaxRequest sgst = PosDataRequest.TaxRequest.builder()
                .taxid(sgstId)
                .taxname("SGST")
                .tax_taxtype("1")
                .tax_coreortotal("2")
                .tax("2.5")
                .tax_ordertype("1,2,3")
                .active("1")
                .build();

        return List.of(cgst, sgst);
    }

    // ======================================================================
    // EXTRACTORS
    // ======================================================================

    private static String resolveId(String extId, boolean useExt, String prefix) {
        return useExt ? extId : generateId(prefix);
    }

    private static PosDataRequest.RestaurantRequest extractRestaurant(JsonNode info, boolean useExternalId) {

        String extId = info.path("id").asText("");

        PosDataRequest.RestaurantRequest rr = new PosDataRequest.RestaurantRequest();
        PosDataRequest.RestaurantDetails details = new PosDataRequest.RestaurantDetails();

        rr.setRestaurantid(resolveId(extId, useExternalId, "rest"));
        rr.setSourceId(extId);
        rr.setIngestionSource(SOURCE);
        rr.setActive("1");

        details.setRestaurantname(info.path("name").asText(""));
        details.setCity(info.path("city").asText(""));
        details.setAddress(
                (info.path("locality").asText("") + ", " + info.path("areaName").asText("")).trim());

        if (info.has("latLong")) {
            String[] p = info.path("latLong").asText("").split(",");
            if (p.length == 2) {
                details.setLatitude(p[0].trim());
                details.setLongitude(p[1].trim());
            }
        }

        details.setCurrency_html("₹");
        details.setCountry("IN");
        details.setMinimumorderamount("0");

        rr.setDetails(details);
        return rr;
    }

    private static PosDataRequest.CategoryRequest extractCategory(JsonNode card, boolean useExternalId) {

        String extId = card.path("categoryId").asText("-1");

        PosDataRequest.CategoryRequest c = new PosDataRequest.CategoryRequest();
        c.setCategoryid(resolveId(extId, useExternalId, "catg"));
        c.setSourceId(extId);

        c.setActive("1");
        c.setCategoryname(card.path("title").asText(""));

        String img = card.path("image").asText("");
        if (!img.isEmpty()) c.setCategory_image_url(buildImage(img));

        return c;
    }

    private static PosDataRequest.ItemRequest extractItem(JsonNode info, String categoryId, boolean useExternalId) {

        String extId = info.path("id").asText("");

        PosDataRequest.ItemRequest item = new PosDataRequest.ItemRequest();
        item.setItemid(resolveId(extId, useExternalId, "itm"));
        item.setSourceId(extId);

        item.setItem_categoryid(categoryId);
        item.setItemname(info.path("name").asText(""));
        item.setItemdescription(info.path("description").asText(""));
        item.setPrice(formatPrice(info.path("price").asInt(0)));
        item.setActive("1");
        item.setIn_stock(info.path("inStock").asInt(1) == 1 ? "1" : "0");
        item.setItem_ordertype("1,2,3");
        item.setItem_image_url(buildImage(info.path("imageId").asText("")));

        // addons
        List<PosDataRequest.AddonGroupRequest> addons = extractAddonGroups(info, useExternalId);
        item.setAddon(addons);
        item.setItemallowaddon(addons.isEmpty() ? "0" : "1");

        // variations
        List<PosDataRequest.VariationRequest> vars = extractVariations(info, useExternalId);
        item.setVariation(vars);
        item.setItemallowvariation(vars.isEmpty() ? "0" : "1");

        return item;
    }

    // ======================================================================
    // VARIATIONS — pricingModels support
    // ======================================================================
    private static List<PosDataRequest.VariationRequest> extractVariations(JsonNode info, boolean useExternalId) {

        List<PosDataRequest.VariationRequest> list = new ArrayList<>();

        JsonNode groups = info.path("variantsV2").path("variantGroups");
        JsonNode pricingModels = info.path("variantsV2").path("pricingModels");

        // Build varId -> price map
        Map<String, Integer> priceMap = new HashMap<>();

        // Build variation → addonGroupId mapping
        Map<String, Set<String>> variationToAddonGroups = new HashMap<>();

        if (pricingModels.isArray()) {
            for (JsonNode pm : pricingModels) {

                int pricePaise = pm.path("price").asInt(0);
                JsonNode pmVars = pm.path("variations");
                JsonNode addonCombinations = pm.path("addonCombinations");

                // Extract prices
                if (pmVars.isArray()) {
                    for (JsonNode pmVar : pmVars) {
                        String varId = pmVar.path("variationId").asText("");
                        if (varId.isEmpty()) varId = pmVar.path("id").asText("");
                        if (!varId.isEmpty()) {
                            priceMap.put(varId, pricePaise);
                        }
                    }
                }

                // Extract variation → addon groups mapping
                if (pmVars.isArray() && addonCombinations.isArray()) {
                    for (JsonNode pmVar : pmVars) {

                        String varId = pmVar.path("variationId").asText("");
                        if (varId.isEmpty()) varId = pmVar.path("id").asText("");
                        if (varId.isEmpty()) continue;

                        Set<String> groupIds = variationToAddonGroups.computeIfAbsent(varId, x -> new HashSet<>());

                        for (JsonNode combo : addonCombinations) {
                            String gId = combo.path("groupId").asText("");
                            if (!gId.isEmpty()) {
                                groupIds.add(gId);
                            }
                        }
                    }
                }
            }
        }

        // No variant groups? return empty
        if (!groups.isArray()) return list;

        for (JsonNode group : groups) {
            String groupName = group.path("name").asText("");

            for (JsonNode v : group.path("variations")) {

                String extVarId = v.path("id").asText("");

                PosDataRequest.VariationRequest vr = new PosDataRequest.VariationRequest();
                vr.setId(resolveId(extVarId, useExternalId, "var"));
                vr.setVariationid(resolveId(extVarId, useExternalId, "var"));

                vr.setSourceId(extVarId);
                vr.setGroupname(groupName);
                vr.setName(v.path("name").asText(""));

                int price = priceMap.getOrDefault(extVarId, v.path("price").asInt(0));
                vr.setPrice(formatPrice(price));

                vr.setActive(v.path("isEnabled").asInt(1) == 1 ? "1" : "0");

                // -----------------------------------------------------------
                //  MAP VARIATION ADDONS TO DEDUPED ADDON GROUPS
                // -----------------------------------------------------------
                List<PosDataRequest.AddonGroupRequest> mappedAddonGroups = new ArrayList<>();

                Set<String> addonGroupIds = variationToAddonGroups.getOrDefault(extVarId, Collections.emptySet());

                for (String rawGroupId : addonGroupIds) {

                    // we must locate deduped AddonGroupRequest by EXTERNAL ID
                    PosDataRequest.AddonGroupRequest deduped = addonGroupByName.values().stream()
                            .filter(a -> rawGroupId.equals(a.getSourceId()))
                            .findFirst()
                            .orElse(null);

                    if (deduped != null) {
                        PosDataRequest.AddonGroupRequest clone = new PosDataRequest.AddonGroupRequest();
                        clone.setAddon_group_id(deduped.getAddongroupid()); // POS final ID
                        clone.setAddon_item_selection_min(deduped.getAddon_item_selection_min());
                        clone.setAddon_item_selection_max(deduped.getAddon_item_selection_max());
                        mappedAddonGroups.add(clone);
                    }
                }

                vr.setAddon(mappedAddonGroups);
                vr.setVariationallowaddon(mappedAddonGroups.isEmpty() ? 0 : 1);

                list.add(vr);
            }
        }

        return list;
    }

    // ======================================================================
    // ADDON GROUPS — dedupe by groupName
    // ======================================================================
    private static List<PosDataRequest.AddonGroupRequest> extractAddonGroups(JsonNode info, boolean useExternalId) {

        List<PosDataRequest.AddonGroupRequest> list = new ArrayList<>();
        JsonNode addons = info.path("addons");

        if (!addons.isArray()) return list;

        for (JsonNode g : addons) {

            String groupName = g.path("groupName").asText("");

            PosDataRequest.AddonGroupRequest ag = addonGroupByName.computeIfAbsent(groupName, k -> {
                String extId = g.path("groupId").asText("");
                String id = resolveId(extId, useExternalId, "adg");
                PosDataRequest.AddonGroupRequest x = new PosDataRequest.AddonGroupRequest();
                x.setAddongroupid(id);
                x.setAddon_group_id(id);
                x.setSourceId(extId);
                x.setActive("1");
                x.setAddongroupitems(new ArrayList<>());
                return x;
            });

            ag.setAddongroup_name(groupName);
            ag.setAddon_item_selection_max(String.valueOf(g.path("maxAddons").asInt(0)));

            int min = g.path("maxFreeAddons").asInt(0);
            if (min == -1) min = 1; // SPECIAL RULE
            ag.setAddon_item_selection_min(String.valueOf(min));

            // Addon items
            List<PosDataRequest.AddonItemRequest> items = new ArrayList<>();
            for (JsonNode c : g.path("choices")) {

                String extId = c.path("id").asText("");

                PosDataRequest.AddonItemRequest ai = new PosDataRequest.AddonItemRequest();
                ai.setAddonitemid(resolveId(extId, useExternalId, "aditm"));
                ai.setSourceId(extId);

                ai.setAddonitem_name(c.path("name").asText(""));
                ai.setAddonitem_price(formatPrice(c.path("price").asInt(0)));
                ai.setActive(c.path("isEnabled").asInt(1) == 1 ? "1" : "0");

                items.add(ai);
            }

            ag.setAddongroupitems(items);
            list.add(ag);
        }

        return list;
    }

    // ======================================================================
    // HELPERS
    // ======================================================================
    private static JsonNode findGroupedCard(JsonNode cardsNode, JsonNode dataNode) {
        if (cardsNode.isArray()) {
            for (JsonNode n : cardsNode) {
                if (n.has("groupedCard")) return n.path("groupedCard");
            }
        }
        return dataNode.has("groupedCard") ? dataNode.path("groupedCard") : dataNode;
    }

    private static String formatPrice(int paise) {
        return String.format("%.2f", paise / 100.0);
    }

    private static String buildImage(String id) {
        if (id == null || id.isBlank()) return "";
        return id.startsWith("http") ? id : SWIGGY_IMG + id;
    }
}
