# Implementation Plan: Migrate Android App to "temp" Backend

This plan outlines the steps to migrate the DineAR Android application to the new "temp" backend located in the parent directory (`/temp/DineAR-Backend-Dashboard/backend/`).

## User Review Required

> [!IMPORTANT]
> **Authentication Identification**: The "temp" backend primarily uses `email` for authentication. The current Android app registration flow collects `username`, `email`, `phone_number`, `first_name`, `last_name`, and `password`. The "temp" backend's registration API currently expects `email`, `first_name`, `last_name`, and `password`. We need to decide whether to:
> 1. Update the "temp" backend to accept `username` and `phone_number` during registration (recommended for data consistency).
> 2. Update the Android app to only send the fields the new backend expects.
>
> **Endpoint Paths**: The "temp" backend defaults to `/api/auth/` for authentication. The Android app currently uses `/api/register/` and `/api/token/`. We will add compatibility aliases to the "temp" backend to avoid changing the app's networking code.

## Proposed Changes

### 1. Backend Enhancements (`temp` backend)

We need to align the "temp" backend with the app's requirements.

#### [MODIFY] [views.py](file:///Users/pragyanshrestha/Project/DineAR-Android/temp/DineAR-Backend-Dashboard/backend/mobile_api/views.py)
- Add `FavoriteDetailsView` to return full restaurant and food item details for the user's favorites. The app currently calls `api/favorites/details/`.

#### [MODIFY] [urls.py](file:///Users/pragyanshrestha/Project/DineAR-Android/temp/DineAR-Backend-Dashboard/backend/mobile_api/urls.py)
- Register the `favorites/details/` route.

#### [MODIFY] [urls.py](file:///Users/pragyanshrestha/Project/DineAR-Android/temp/DineAR-Backend-Dashboard/backend/main/urls.py)
- Add legacy aliases for authentication:
    - `path("api/register/", include("authentication.urls"))` -> mapped to `RegisterView`.
    - `path("api/token/", include("authentication.urls"))` -> mapped to `LoginView`.

#### [MODIFY] [models.py](file:///Users/pragyanshrestha/Project/DineAR-Android/temp/DineAR-Backend-Dashboard/backend/authentication/models.py)
- Ensure the `User` model has a `username` field (it currently has `email` as `USERNAME_FIELD` but might need a separate `username` field for display/search if the app expects it).

---

### 2. Android App Data Models (`DineAR-app`)

Verify and update data models to reflect the "temp" backend's JSON structure.

#### [MODIFY] [Restaurant.java](file:///Users/pragyanshrestha/Project/DineAR-Android/DineAR-app/app/src/main/java/com/ps/dinear/data/model/Restaurant.java)
- Ensure fields match the `temp` backend (e.g., `logo` vs `image_url`, `banner_image`).

#### [MODIFY] [MenuItem.java](file:///Users/pragyanshrestha/Project/DineAR-Android/DineAR-app/app/src/main/java/com/ps/dinear/MenuItem.java)
- Ensure fields match the `temp` backend (e.g., `price`, `discount_price`, `currency`).

---

### 3. Verification Plan

#### Automated Tests
- Run `python manage.py test mobile_api` in the `temp` backend to verify the new favorites detail endpoint.

#### Manual Verification
- Deploy the `temp` backend locally.
- Point the Android app to the `temp` backend URL via `RetrofitClient`.
- Verify Login, Registration, Restaurant Browsing, and Favorites functionality.
