# Implementation Plan - Food Fetching and Display Alignment

This plan aligns the Android application's food fetching and displaying logic with the naming conventions and structure defined in the backend (located in `temp/DineAR-Backend-Dashboard`).

## User Review Required

> [!IMPORTANT]
> The `MobileSearchView` on the backend requires a non-empty `q` (query) parameter. The current `setupFeaturedFoods` in `MainActivity` passes an empty string, which will cause a 400 Bad Request. I will update it to use a generic query as a workaround until a dedicated featured foods endpoint is available.

## Proposed Changes

### Data Models & API

#### [MODIFY] [MenuItem.java](file:///Users/pragyanshrestha/Project/DineAR-Android/DineAR-app/app/src/main/java/com/ps/dinear/MenuItem.java)
- Ensure all fields match the `FoodItemListSerializer` from the backend.
- Add `slug`, `currency`, and `discount_price` with correct `@SerializedName` annotations.
- Verify `ARMapping` and `ARAsset` structure matches `FoodARMappingDisplaySerializer`.

#### [MODIFY] [ApiService.java](file:///Users/pragyanshrestha/Project/DineAR-Android/DineAR-app/app/src/main/java/com/ps/dinear/ApiService.java)
- Verify all endpoints match `mobile_api/urls.py` and other backend routers.
- Ensure `getMenu` uses the `/api/menu/restaurant/{slug}/` path.

### Activities & Adapters

#### [MODIFY] [MainActivity.java](file:///Users/pragyanshrestha/Project/DineAR-Android/DineAR-app/app/src/main/java/com/ps/dinear/MainActivity.java)
- Fix `setupFeaturedFoods` to avoid empty query validation errors.
- Update `updatePromoBanner` to handle new `MenuItem` structure (discount prices, etc.).

#### [MODIFY] [MenuActivity.java](file:///Users/pragyanshrestha/Project/DineAR-Android/DineAR-app/app/src/main/java/com/ps/dinear/menu/MenuActivity.java)
- Ensure the menu category group flattening logic correctly assigns category names to items for filtering.

#### [MODIFY] [FoodDetailsActivity.java](file:///Users/pragyanshrestha/Project/DineAR-Android/DineAR-app/app/src/main/java/com/ps/dinear/menu/FoodDetailsActivity.java)
- Update UI to show `discount_price` if available.
- Ensure restaurant name is correctly displayed if passed through the Intent.

#### [MODIFY] [MenuFoodAdapter.java](file:///Users/pragyanshrestha/Project/DineAR-Android/DineAR-app/app/src/main/java/com/ps/dinear/menu/MenuFoodAdapter.java)
- Update `onBindViewHolder` to use the new `MenuItem` fields (currency, discount).
- Ensure image loading uses the `primary_image` logic or the first image in the list.

## Verification Plan

### Automated Tests
- N/A (Unit tests for data parsing can be added if requested).

### Manual Verification
- Deploy to Android Emulator.
- Verify Home screen featured foods are loading.
- Verify Search functionality for both restaurants and foods.
- Verify Restaurant menu loads correctly with categories.
- Verify Food details show correct pricing and images.
- Verify AR Scan button is enabled for items with AR assets.
