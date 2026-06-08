package com.medlabapp.model;

public class User {
    private int id;
    private String name;
    private String email;
    private String role; // SUPER_ADMIN, LAB_ATTENDANT, CUSTOMER
    private String passwordHash; // ADDED: Critical for authentication 
    private boolean isVerified;
    private boolean forcePasswordChange;
 
    public User(int id, String name, String email,String passwordHash, String role, boolean isVerified, boolean forcePasswordChange) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.isVerified = isVerified;
        
        this.forcePasswordChange = forcePasswordChange;
    }
 
    public int getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getRole() { return role; }
    public boolean isVerified() { return isVerified; }
    public boolean isForcePasswordChange() { return forcePasswordChange; }

 
    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; } 
    public void setRole(String role) { this.role = role; }
    public void setVerified(boolean verified) { isVerified = verified; }
    public void setForcePasswordChange(boolean forcePasswordChange) { this.forcePasswordChange = forcePasswordChange; }
}