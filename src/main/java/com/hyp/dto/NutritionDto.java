package com.hyp.dto;

import java.util.List;

import lombok.Data;

@Data
public class NutritionDto {
	
	private AdditiveMapdto additiveMap;
    private List<AllergenDto> allergens;
    private FoodAmountDto foodAmount;
    private FoodAmountDto calories;
    private FoodAmountDto protien;
    private List<MineralDto> minerals;
    private FoodAmountDto sodium;
    private FoodAmountDto carbohydrate;
    private FoodAmountDto totalSugar;
    private FoodAmountDto addedSugar;
    private FoodAmountDto totalFat;
    private FoodAmountDto saturatedFat;
    private FoodAmountDto transFat;
    private FoodAmountDto cholesterol;
    private List<MineralDto> vitamins;
    private AdditionalInfoDto additionalInfo;
    private FoodAmountDto fiber;
    private String servingInfo;

}
