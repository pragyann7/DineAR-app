package com.ps.dinear.data.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class UserAddress implements Serializable {
    private int id;
    private String title;
    
    @SerializedName("address_line")
    private String addressLine;
    
    private String city;
    private String district;
    
    @SerializedName("is_default")
    private boolean isDefault;

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getAddressLine() { return addressLine; }
    public String getCity() { return city; }
    public String getDistrict() { return district; }
    public boolean isDefault() { return isDefault; }
    
    public void setTitle(String title) { this.title = title; }
    public void setAddressLine(String addressLine) { this.addressLine = addressLine; }
    public void setCity(String city) { this.city = city; }
    public void setDistrict(String district) { this.district = district; }
    public void setDefault(boolean aDefault) { isDefault = aDefault; }

    @Override
    public String toString() {
        return addressLine + ", " + city;
    }
}
