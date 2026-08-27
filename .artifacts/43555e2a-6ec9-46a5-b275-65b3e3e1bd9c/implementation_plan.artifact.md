# Implementation Plan: Migrate Android App to New Production Backend

This plan outlines the steps to migrate the DineAR Android application from the legacy `DineAR-backend` to the new `DineAR-dashboard` backend. It also includes adding missing functionality to the new backend to support existing app features.

## User Review Required

> [!IMPORTANT]
> **Authentication Change**: The new backend uses `email` as the unique identifier and login credential instead of `username`. The Android app's Login and Registration screens will need to be updated to collect `email`, `first_name`, and `last_name`.
> **Data Structure Changes**: The new backend uses `slugs` for identifying restaurants and food items in public APIs. The app logic will be updated to prefer these slugs where applicable, or we will add ID-based compatibility aliases in the backend.
> **Favorites Migration**: The new backend enforces a "one target only" constraint for favorites (either a restaurant OR a food item, not both in one entry). This matches the app's current usage but is more strictly validated.

## Proposed Changes

### 1. New Backend Enhancements (`DineAR-dashboard/DineAR-Backend-Dashboard/backend`)

We need to add the `api/favorites/details/` endpoint to the new backend as the Android app relies on it to display the Favorites screen efficiently.

#### [MODIFY] [views.py](file:///Users/pragyanshrestha/Project/DineAR-Android/DineAR-dashboard/DineAR-Backend-Dashboard/backend/mobile_api/views.py)
- Add `FavoriteDetailsView` to return full restaurant and food item details for the user's favorites.

#### [MODIFY] [urls.py](file:///Users/pragyanshrestha/Project/DineAR-Android/DineAR-dashboard/DineAR-Backend-Dashboard/backend/mobile_api/urls.py)
- Register the `favorites/details/` route.

---

### 2. Android App Data Models (`DineAR-app`)

Update the data models to reflect the new backend's JSON structure.

#### [MODIFY] [Restaurant.java](file:///Users/pragyanshrestha/Project/DineAR-Android/DineAR-app/app/src/main/java/com/ps/dinear/data/model/Restaurant.java)
- Update fields to match the new backend (e.g., `logo`, `banner_image`, `slug`).

#### [MODIFY] [MenuItem.java](file:///Users/pragyanshrestha/Project/DineAR-Android/DineAR-app/app/src/main/java/com/ps/dinear/MenuItem.java)
- Rename or update to `FoodItem` if necessary, and adjust fields (e.g., `discount_price`, `currency`).

---

### 3. Android App API Integration (`DineAR-app`)

Update the networking layer to point to the new endpoints and handle the updated authentication flow.

#### [MODIFY] [ApiService.java](file:///Users/pragyanshrestha/Project/DineAR-Android/DineAR-app/app/src/main/java/com/ps/dinear/ApiService.java)
- Update authentication endpoints: `api/auth/login/` and `api/auth/register/`.
- Update restaurant and menu detail endpoints to use slugs.

#### [MODIFY] [LoginRequest.java](file:///Users/pragyanshrestha/Project/DineAR-Android/DineAR-app/app/src/main/java/com/ps/dinear/auth/LoginRequest.java)
- Change `username` to `email`.

#### [MODIFY] [RegistrationRequest.java](file:///Users/pragyanshrestha/Project/DineAR-Android/DineAR-app/app/src/main/java/com/ps/dinear/auth/RegistrationRequest.java)
- Update fields to `email`, `first_name`, `last_name`, and `password`.

---

### 4. Logic Updates (`DineAR-app`)

#### [MODIFY] [LoginActivity.java](file:///Users/pragyanshrestha/Project/DineAR-Android/DineAR-app/app/src/main/java/com/ps/dinear/auth/LoginActivity.java)
- Update UI and logic to use email for login.

#### [MODIFY] [RegistrationActivity.java](file:///Users/pragyanshrestha/Project/DineAR-Android/DineAR-app/app/src/main/java/com/ps/dinear/auth/RegistrationActivity.java)
- Update UI to collect first name and last name instead of username.

## Verification Plan

### Automated Tests
- Run `python manage.py test mobile_api` in the new backend to verify the new favorites detail endpoint.

### Manual Verification
- Deploy the new backend locally.
- Point the Android app to the new backend URL via `RetrofitClient`.
- Verify Login, Registration, Restaurant Browsing, and Favorites functionality on an emulator.
