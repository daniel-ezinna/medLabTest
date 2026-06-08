
package com.medlabapp.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String URL = "jdbc:postgresql://localhost:5432/medlabapp";
    private static final String USER = "postgres"; 
    private static final String PASSWORD = "daniel"; // Change 

   
    public static Connection getConnection() throws SQLException {
        try {
             
            Class.forName("org.postgresql.Driver");
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (ClassNotFoundException e) {
            throw new SQLException("PostgreSQL JDBC Driver not found in the lib/ directory.", e);
        }
    }
}