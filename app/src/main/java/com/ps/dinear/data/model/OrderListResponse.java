package com.ps.dinear.data.model;

import java.util.List;

public class OrderListResponse {
    private List<Order> results;
    private String next;
    private String previous;
    private int count;

    public List<Order> getResults() { return results; }
    public String getNext() { return next; }
    public String getPrevious() { return previous; }
    public int getCount() { return count; }
}
