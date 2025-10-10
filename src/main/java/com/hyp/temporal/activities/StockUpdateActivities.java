package com.hyp.temporal.activities;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import java.util.List;

@ActivityInterface
public interface StockUpdateActivities {
    @ActivityMethod
    void updateStock(List<String> itemIds, String type, boolean inStock);
}
