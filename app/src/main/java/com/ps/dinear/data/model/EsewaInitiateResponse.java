package com.ps.dinear.data.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class EsewaInitiateResponse implements Serializable {
    private String amount;
    
    @SerializedName("tax_amount")
    private String taxAmount;
    
    @SerializedName("total_amount")
    private String totalAmount;
    
    @SerializedName("transaction_uuid")
    private String transactionUuid;
    
    @SerializedName("product_code")
    private String productCode;
    
    @SerializedName("product_service_charge")
    private String productServiceCharge;
    
    @SerializedName("product_delivery_charge")
    private String productDeliveryCharge;
    
    @SerializedName("success_url")
    private String successUrl;
    
    @SerializedName("failure_url")
    private String failureUrl;
    
    @SerializedName("signed_field_names")
    private String signedFieldNames;
    
    private String signature;
    
    @SerializedName("esewa_url")
    private String esewaUrl;

    public String getAmount() { return amount; }
    public String getTaxAmount() { return taxAmount; }
    public String getTotalAmount() { return totalAmount; }
    public String getTransactionUuid() { return transactionUuid; }
    public String getProductCode() { return productCode; }
    public String getProductServiceCharge() { return productServiceCharge; }
    public String getProductDeliveryCharge() { return productDeliveryCharge; }
    public String getSuccessUrl() { return successUrl; }
    public String getFailureUrl() { return failureUrl; }
    public String getSignedFieldNames() { return signedFieldNames; }
    public String getSignature() { return signature; }
    public String getEsewaUrl() { return esewaUrl; }
}
