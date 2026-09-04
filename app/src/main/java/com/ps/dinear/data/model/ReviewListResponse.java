package com.ps.dinear.data.model;

import java.util.List;

public class ReviewListResponse {
    private List<Review> results;
    private ReviewSummary summary;
    private String next;
    private String previous;
    private int count;

    public List<Review> getResults() { return results; }
    public ReviewSummary getSummary() { return summary; }
    public String getNext() { return next; }
    public String getPrevious() { return previous; }
    public int getCount() { return count; }
}
