package com.ps.dinear.data.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class PaymentRequest implements Serializable {
    @SerializedName("order_id")
    private int orderId;
    
    private String method;

    public PaymentRequest(int orderId, String method) {
        this.orderId = orderId;
        this.method = method;
    }
}
