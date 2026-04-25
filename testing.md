# Create UP menu

curl --request POST \
--url http://localhost:8080/api/v2/api/v3/pos/urbanpiper/menu \
--header 'Content-Type: application/json' \
--data '{
"order_bill_components": {
"charges": []
},
"menu": {
"categories": [
{
"subcategories": [],
"description": "",
"title": "Pizzas",
"translations": [],
"items": [
{
"description": "Italian handmade spl chicken bbq pizza with added toppings with new things",
"title": "Chicken BBQ Pizza",
"translations": [],
"discounts": [],
"fulfillment_modes": [],
"tags": [
"packaged-good"
],
"nutritional_info": {
"carbohydrate": {
"unit": "mg/g",
"value": 1.1
},
"fiber": {
"unit": "mg",
"value": 60.0
},
"fat": {
"unit": "mg",
"value": 30.0
},
"protein": {
"unit": "mg",
"value": 10.0
},
"calorie": {
"unit": "kcal",
"value": 1.1
}
},
"recommended": true,
"ref_id": "829122",
"variant_groups": [
{
"variants": [
{
"price": 0,
"title": "Regular Non-veg pizza",
"translations": [],
"in_stock": true,
"food_type": 1,
"nutritional_info": {
"carbohydrate": {
"unit": "mg/g",
"value": 1.1
},
"fiber": {
"unit": "mg",
"value": 60.0
},
"fat": {
"unit": "mg",
"value": 30.0
},
"protein": {
"unit": "mg",
"value": 10.0
},
"calorie": {
"unit": "kcal",
"value": 1.1
}
},
"ref_id": "280663",
"variant_groups": [
{
"variants": [
{
"price": 20,
"title": "Regular",
"translations": [],
"in_stock": true,
"food_type": 1,
"ref_id": "280763"
},
{
"price": 30,
"title": "Large",
"translations": [],
"in_stock": true,
"food_type": 1,
"ref_id": "280764"
}
]
}
]
},
{
"price": 0,
"title": "Large Non veg pizza",
"translations": [],
"in_stock": true,
"food_type": 1,
"ref_id": "280664",
"add_on_groups": [
{
"maximum_allowed": 2,
"addons": [
{
"price": 60,
"title": "Smoked Chicken",
"nutritional_info": {
"carbohydrate": {
"unit": "mg/g",
"value": 1.1
},
"fiber": {
"unit": "mg",
"value": 60.0
},
"fat": {
"unit": "mg",
"value": 30.0
},
"protein": {
"unit": "mg",
"value": 10.0
},
"calorie": {
"unit": "kcal",
"value": 1.1
}
},
"translations": [],
"in_stock": true,
"food_type": 1,
"ref_id": "280668"
},
{
"price": 60,
"title": "Chicken Sausage Tandoori",
"translations": [],
"in_stock": true,
"food_type": 1,
"ref_id": "280669"
},
{
"price": 60,
"title": "Pork Pepperoni",
"translations": [],
"in_stock": true,
"food_type": 1,
"ref_id": "280670"
}
],
"title": "Choose your toppings-medium",
"translations": [],
"minimum_needed": 1,
"ref_id": "77620"
}
]
}
],
"title": "Choose your pizza",
"translations": [
{
"language": "ar",
"title": "اختر البيتزا الخاصة بك",
"description": "..."
},
{
"language": "es",
"title": "Elige tu pizza",
"description": "..."
}
],
"ref_id": "77641"
}
],
"category_ref_id": "61728",
"canonical_id": "603458",
"image_url": "",
"bill_components": {
"charges": [
"3320"
],
"taxes": [
"8218",
"8219"
]
},
"food_type": 2,
"in_stock": true,
"price": 389,
"sub_category_ref_id": ""
},
{
"description": "A veg loaded pizza",
"title": "Veg loaded pizza",
"translations": [],
"discounts": [],
"fulfillment_modes": [],
"tags": [],
"recommended": true,
"ref_id": "829222",
"variant_groups": [
{
"variants": [
{
"price": 300,
"title": "Regular",
"translations": [],
"in_stock": true,
"food_type": 1,
"ref_id": "280963"
},
{
"price": 600,
"title": "Large",
"translations": [],
"in_stock": true,
"food_type": 1,
"ref_id": "280964"
}
],
"title": "Choose your size",
"translations": [],
"ref_id": "77841"
}
],
"category_ref_id": "61728",
"canonical_id": "603458",
"image_url": "",
"add_on_groups": [
{
"maximum_allowed": 1,
"addons": [
{
"price": 0,
"title": "Vanilla ice cream",
"translations": [],
"in_stock": true,
"food_type": 1,
"ref_id": "280652"
}
],
"title": "Desserts",
"translations": [],
"minimum_needed": 0,
"ref_id": "77620"
}
],
"bill_components": {
"charges": [
"3320"
],
"taxes": [
"8218",
"8219"
]
},
"food_type": 2,
"in_stock": true,
"price": "",
"sub_category_ref_id": ""
}
],
"timings": "",
"ref_id": "61728",
"sort_order": 0,
"image_url": ""
},
{
"subcategories": [
{
"description": "",
"title": "Meal For One",
"translations": [],
"items": [
{
"description": "Classic Italian Pizza | Mozzarella Cheese | Homemade Sauce with Garlic bread & Dip",
"title": "Margherita With Garlic Bread & Dip",
"translations": [],
"discounts": [],
"fulfillment_modes": [],
"tags": [],
"recommended": false,
"ref_id": "832567",
"variant_groups": [],
"category_ref_id": "69035",
"canonical_id": "603517",
"image_url": "",
"add_on_groups": [],
"bill_components": {
"charges": [
"3320"
],
"taxes": [
"8218",
"8219"
]
},
"food_type": 1,
"in_stock": false,
"price": 179,
"sub_category_ref_id": "61730"
}
],
"ref_id": "61730",
"sort_order": 8,
"image_url": ""
}
],
"description": "",
"title": "Meal",
"translations": [],
"items": [],
"timings": "",
"ref_id": "69035",
"sort_order": 0,
"image_url": ""
}
]
},
"timings": [
{
"days": [
{
"slots": [
{
"start_time": "08:00",
"end_time": "11:00"
}
],
"day": "Monday"
}
],
"ref_id": "739902"
}
],
"location": {
"min_delivery_time": "00:15",
"min_pickup_time": "00:45",
"ref_id": "31281"
},
"bill_components": {
"charges": [
{
"description": "",
"title": "Packaging Charge",
"fulfillment_modes": [
"delivery",
"dinein",
"pickup"
],
"value": 10,
"ref_id": "3320",
"taxes": [
"8639"
],
"type": "FIXED"
}
],
"taxes": [
{
"title": "CGST",
"description": "",
"value": 2.5,
"ref_id": "8218"
},
{
"title": "SGST",
"description": "",
"value": 2.5,
"ref_id": "8219"
},
{
"title": "GST",
"description": "",
"value": 5,
"ref_id": "8639"
}
]
},
"callback_url": "https://staging.urbanpiper.com/ext/api/v1/generic/menu/callback/f29a02cabd72415a87b4c55154dea9d8/"
}'


# Inventory Update
curl --request POST \
--url http://localhost:8080/api/v2/api/v3/pos/urbanpiper/inventory \
--header 'Content-Type: application/json' \
--data '{
"location_ref_id": "31281",
"items": ["829222"],
"variants": ["280963"],
"add_ons": ["280652"],
"in_stock": false,
"next_available_at": 1593591756
}'