package com.hyp.service;

import com.hyp.entity.AddonGroup;
import com.hyp.repository.AddonGroupRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

@Service
public class AddonGroupService extends BaseServiceImpl<AddonGroup, String> {
    @Autowired
    AddonGroupRepository addonGroupRepository;

    @Autowired
    MongoTemplate mongoTemplate;

    public List<AddonGroup> getAddonGroupsAndItemsById(String addonGroupId) {
        Criteria criteria = Criteria.where("_id").is(addonGroupId);

        Aggregation aggregation =
                Aggregation.newAggregation(getAddonGroupsItemsLookupOperation(), Aggregation.match(criteria));

        AggregationResults<AddonGroup> results = mongoTemplate.aggregate(aggregation, "addon_groups", AddonGroup.class);
        return results.getMappedResults();
    }

    public List<AddonGroup> getAllAddonGroupsAndItems() {
        Aggregation aggregation = Aggregation.newAggregation(getAddonGroupsItemsLookupOperation());

        AggregationResults<AddonGroup> results = mongoTemplate.aggregate(aggregation, "addon_groups", AddonGroup.class);
        return results.getMappedResults();
    }

    private LookupOperation getAddonGroupsItemsLookupOperation() {
        return LookupOperation.newLookup()
                .from("addon_items")
                .localField("addon_group_items")
                .foreignField("_id")
                .as("addon_items");
    }
}
