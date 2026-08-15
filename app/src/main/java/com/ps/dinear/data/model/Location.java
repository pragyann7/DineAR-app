package com.ps.dinear.data.model;

public class Location {
    private String district;
    private String city;

    public Location(String district, String city) {
        this.district = district;
        this.city = city;
    }

    public String getDistrict() {
        return district;
    }

    public String getCity() {
        return city;
    }

    @Override
    public String toString() {
        return city + ", " + district;
    }
}