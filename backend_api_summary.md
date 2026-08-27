# DineAR Backend API Documentation Summary

This document provides a detailed summary of the API endpoints, including required fields, data types, and example URLs.

## Base URL Reference
- **Local Development:** `http://127.0.0.1:8000/`
- **Android Emulator:** `http://10.0.2.2:8000/` (points to the host machine)

---

## 1. Authentication (`users` app)

### **Login**
- **Method:** `POST`
- **Endpoint:** `api/token/`
- **Example URL:** `http://127.0.0.1:8000/api/token/`
- **Request Body (JSON):**
    - `username` (String) - **Required**
    - `password` (String) - **Required**
- **Response:** `access` (JWT Access Token) and `refresh` (JWT Refresh Token).

### **Registration**
- **Method:** `POST`
- **Endpoint:** `api/register/`
- **Example URL:** `http://127.0.0.1:8000/api/register/`
- **Request Body (JSON):**
    - `username` (String) - **Required**
    - `email` (String) - **Required**
    - `phone_number` (String) - **Required**
    - `password` (String) - **Required**
    - `is_restaurant_owner` (Boolean) - Optional

---

## 2. Restaurants & Discovery (`restaurants` app)

### **List Restaurants**
- **Method:** `GET`
- **Endpoint:** `api/restaurants/`
- **Example URL:** `http://127.0.0.1:8000/api/restaurants/?city=Kathmandu&category=Cuisine`
- **Query Params:** `city` (String), `category` (String)
- **Response Fields (List of Objects):**
    - `id` (Integer)
    - `name` (String)
    - `description` (String)
    - `image_url` (String/URL)
    - `rating` (Decimal)
    - `cuisine` (String)
    - `category` (String)
    - `price_range` (String)
    - `delivery_time` (String)
    - `city` (String, via Location)

### **Restaurant Details**
- **Method:** `GET`
- **Endpoint:** `api/restaurants/{id}/`
- **Example URL:** `http://127.0.0.1:8000/api/restaurants/1/`
- **Response Fields:** Same as List, plus potentially a nested `menu_items` list.

---

## 3. Menu Items (`restaurants` app)

### **List Menu Items**
- **Method:** `GET`
- **Endpoint:** `api/menu-items/`
- **Example URL:** `http://127.0.0.1:8000/api/menu-items/?restaurant=1`
- **Query Params:** `restaurant` (Integer ID) - **Required**
- **Response Fields (List of Objects):**
    - `id` (Integer)
    - `restaurant_id` (Integer)
    - `name` (String)
    - `price` (Decimal)
    - `description` (String)
    - `image_url` (String/URL)
    - `model_url` (String/URL): 3D file (.glb/.gltf)
    - `model_name` (String)
    - `model_version` (String)
    - `category` (String)
    - `tag1` (String)
    - `tag2` (String)
    - `is_available` (Boolean)
    - `status` (String): e.g., "APPROVED"
    - `has_3d` (Boolean): Calculated field (True if `model_url` exists & status is "APPROVED")

---

## 4. Search (`restaurants` app)

### **Global Search**
- **Method:** `GET`
- **Endpoint:** `api/search/`
- **Example URL:** `http://127.0.0.1:8000/api/search/?q=momo&city=Kathmandu`
- **Query Params:** `q` (String) - **Required**, `city` (String) - Optional
- **Response (Object):**
    - `restaurants` (List of Restaurant objects)
    - `food_items` (List of MenuItem objects)

---

## 5. Favorites (`restaurants` app)
*Requires `Authorization: Bearer <token>` header*

### **Get Favorite IDs**
- **Method:** `GET`
- **Endpoint:** `api/favorites/ids/`
- **Example URL:** `http://127.0.0.1:8000/api/favorites/ids/`
- **Response Fields:**
    - `restaurants` (List of Integers): IDs of favorited restaurants.
    - `foods` (List of Integers): IDs of favorited menu items.

### **Add Favorite**
- **Method:** `POST`
- **Endpoint:** `api/favorites/`
- **Example URL:** `http://127.0.0.1:8000/api/favorites/`
- **Request Body (JSON):**
    - `type` (String): `"restaurant"` or `"food"` - **Required**
    - `item_id` (Integer) - **Required**

### **Remove Favorite**
- **Method:** `DELETE`
- **Endpoint:** `api/favorites/{type}/{id}/`
- **Example URL:** `http://127.0.0.1:8000/api/favorites/restaurant/1/`
- **Path Parameters:**
    - `type` (String): `"restaurant"` or `"food"`
    - `id` (Integer): The ID of the item to remove.

---

## 6. Web Dashboards & Static Assets
- **Owner Registration Page:** `http://127.0.0.1:8000/dashboard/register/`
- **Images/Models Path:** Static files are served via `/media/` paths (e.g., `http://127.0.0.1:8000/media/models/dish.glb`).
