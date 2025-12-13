package com.hyp.entity;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "addon_groups")
public class AddonGroup extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Field("addon_group_name")
    private String addonGroupName;

    private String active;

    @Field("addon_group_rank")
    private String addonGroupRank;

    @Field("addon_group_items")
    private List<String> addonGroupItems;

    @Field("addon_item_selection_max")
    private String addonItemSelectionMax;

    @Field("addon_item_selection_min")
    private String addonItemSelectionMin;

    @Field("addon_items")
    private List<AddonItem> addonItems;

    @Field("source_id")
    private String sourceId;
}
