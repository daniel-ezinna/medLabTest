package com.medlabapp.model;

import java.sql.Timestamp;

public class TestRequest {
    private int id;
    private int customerId;
    private int testTypeId;
    private String paymentStatus; // PAID, UNPAID
    private Timestamp orderDate;
    private Timestamp deadlineDate;

    public TestRequest(int id, int customerId, int testTypeId, String paymentStatus, Timestamp orderDate, Timestamp deadlineDate) {
        this.id = id;
        this.customerId = customerId;
        this.testTypeId = testTypeId;
        this.paymentStatus = paymentStatus;
        this.orderDate = orderDate;
        this.deadlineDate = deadlineDate;
    }

    public int getId() { return id; }
    public int getCustomerId() { return customerId; }
    public int getTestTypeId() { return testTypeId; }
    public String getPaymentStatus() { return paymentStatus; }
    public Timestamp getOrderDate() { return orderDate; }
    public Timestamp getDeadlineDate() { return deadlineDate; }

    public void setId(int id) { this.id = id; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }
    public void setTestTypeId(int testTypeId) { this.testTypeId = testTypeId; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public void setOrderDate(Timestamp orderDate) { this.orderDate = orderDate; }
    public void setDeadlineDate(Timestamp deadlineDate) { this.deadlineDate = deadlineDate; }
}