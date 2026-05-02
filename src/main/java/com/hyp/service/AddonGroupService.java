package com.hyp.service;

import com.hyp.entity.AddonGroup;
import com.hyp.entity.AddonItem;
import com.hyp.repository.AddonGroupRepository;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

@Service
public class AddonGroupService extends BaseServiceImpl<AddonGroup, String> {

    private static final String ADDON_CACHE = "addonGroupItems";

    @Autowired
    AddonGroupRepository addonGroupRepository;

    @Autowired
    MongoTemplate mongoTemplate;

    public List<AddonGroup> getAddonGroupsAndItemsById(String addonGroupId) {
        List<AddonGroup> groups = mongoTemplate.find(
                Query.query(Criteria.where("_id").is(addonGroupId)), AddonGroup.class, "addon_groups");
        populateAddonItems(groups);
        return groups;
    }

    @SuppressWarnings("unchecked")
    public List<AddonGroup> getAllAddonGroupsAndItems() {
        return (List<AddonGroup>)
                cacheService().getOrLoad(ADDON_CACHE, "all", List.class, this::loadAllAddonGroupsAndItems);
    }

    private List<AddonGroup> loadAllAddonGroupsAndItems() {
        List<AddonGroup> groups = mongoTemplate.find(
                Query.query(Criteria.where("is_deleted").ne(true)), AddonGroup.class, "addon_groups");
        populateAddonItems(groups);
        return groups;
    }

    void populateAddonItems(List<AddonGroup> groups) {
        // Collect all addon item IDs from all groups
        Set<String> allItemIds = new HashSet<>();
        for (AddonGroup group : groups) {
            if (group.getAddonGroupItems() != null) {
                allItemIds.addAll(group.getAddonGroupItems());
            }
        }
        if (allItemIds.isEmpty()) return;

        // Single query for all addon items
        Map<String, AddonItem> itemMap =
                mongoTemplate
                        .find(Query.query(Criteria.where("_id").in(allItemIds)), AddonItem.class, "addon_items")
                        .stream()
                        .collect(Collectors.toMap(AddonItem::getId, Function.identity()));

        // Distribute to groups
        for (AddonGroup group : groups) {
            if (group.getAddonGroupItems() != null) {
                group.setAddonItems(group.getAddonGroupItems().stream()
                        .map(itemMap::get)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()));
            }
        }
    }
}
