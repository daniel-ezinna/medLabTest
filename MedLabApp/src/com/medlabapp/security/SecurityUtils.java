
package com.medlabapp.security;

import org.mindrot.jbcrypt.BCrypt;

public class SecurityUtils {

    /**
     * Hashes a plain text password using BCrypt with a workload factor of 12.
     * Use this when creating new users or changing passwords.
     */
    public static String hashPassword(String plainTextPassword) {
        return BCrypt.hashpw(plainTextPassword, BCrypt.gensalt(12));
    }

    /**
     * Compares a plain text password against a stored BCrypt hash.
     * Use this during the Login process.
     */
    public static boolean checkPassword(String plainTextPassword, String hashedPassword) {
        if (hashedPassword == null || !hashedPassword.startsWith("$2a$")) {
            return false;  
        }
        return BCrypt.checkpw(plainTextPassword, hashedPassword);
    }
}