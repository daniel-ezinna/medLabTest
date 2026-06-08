package com.medlabapp.util;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

 
public class EmailService {

    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
     
    private static final String SENDER_EMAIL = "jojobiggaboy183@gmail.com";
    private static final String SENDER_PASSWORD = "bcegiigxgokykvye";
 

    private static Session buildSession() {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
            }
        });
    }

 

    /**
     * Sends a "result ready" email to the customer.
     * Called by Dev 2 (LabAttendantController) after marking a sample as VALIDATED.
     *
     * @param recipientEmail  The customer's email address
     * @param recipientName   The customer's full name
     * @param testName        The name of the validated test
     */
    public static void sendResultReadyEmail(String recipientEmail, String recipientName, String testName) {
        try {
            Message message = new MimeMessage(buildSession());
            message.setFrom(new InternetAddress(SENDER_EMAIL, "Sante Diagnostics"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject("Your Test Result is Ready - Sante Diagnostics");
            message.setText(
                "Dear " + recipientName + ",\n\n" +
                "Great news! Your result for the following test has been reviewed, " +
                "validated, and is now available in your patient dashboard:\n\n" +
                "  Test: " + testName + "\n\n" +
                "Please log in to your Sante Diagnostics account to view and " +
                "download your report.\n\n" +
                "If you have any questions about your results, please contact " +
                "our laboratory directly.\n\n" +
                "Regards,\n" +
                "Sante Diagnostics Laboratory\n" +
                "admin@sante.com"
            );

            Transport.send(message);
            System.out.println("Result notification email sent to: " + recipientEmail);

        } catch (Exception e) {
            System.err.println("Failed to send result email to " + recipientEmail + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    
    
    
    /**
     * Dispatches an automated onboarding email containing temporary 
     * access credentials to a manually provisioned customer.
     */
    public static void sendAccountCreatedEmail(String toEmail, String name, String plainTextPassword) {
        jakarta.mail.Session session = buildSession(); 

        try {
            jakarta.mail.Message message = new jakarta.mail.internet.MimeMessage(session);
            message.setFrom(new jakarta.mail.internet.InternetAddress("no-reply@santediagnostics.com", "Sante Diagnostics"));
            message.setRecipients(jakarta.mail.Message.RecipientType.TO, jakarta.mail.internet.InternetAddress.parse(toEmail));
            
            message.setSubject("Welcome to Sante Diagnostics - Your Account Details");

            String htmlContent = "<html><body style='font-family: Arial, sans-serif; color: #333333; line-height: 1.6;'>"
                    + "<div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #DEE2E6; border-radius: 8px;'>"
                    + "<h2 style='color: #2E7D32;'>Account Provisioned Successfully</h2>"
                    + "<p>Dear <strong>" + name + "</strong>,</p>"
                    + "<p>A patient profile has been created for you at Sante Diagnostics. You can now log in to track your test requests and download secure clinical results.</p>"
                    + "<div style='background-color: #F8F9FA; padding: 15px; border-left: 4px solid #2E7D32; margin: 20px 0; font-family: monospace;'>"
                    + "<strong>Portal URL:</strong> Sante Diagnostics LIMS Desktop Application<br/>"
                    + "<strong>Username/Email:</strong> " + toEmail + "<br/>"
                    + "<strong>Temporary Password:</strong> " + plainTextPassword
                    + "</div>"
                    + "<p style='color: #DC3545; font-size: 13px;'><strong>Security Notice:</strong> You will be prompted to change this temporary password immediately upon your first login to secure your personal health records.</p>"
                    + "<hr style='border: none; border-top: 1px solid #DEE2E6; margin-top: 30px;'/>"
                    + "<p style='font-size: 11px; color: #6C757D;'>This is an automated system alert. Please do not reply directly to this inbox.</p>"
                    + "</div></body></html>";

            message.setContent(htmlContent, "text/html; charset=utf-8");
            jakarta.mail.Transport.send(message);
            System.out.println("Onboarding email dispatched cleanly to " + toEmail);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
    
    
    
 

    /**
     * Sends a password reset email containing a 6-digit code.
     * Called by ForgotPasswordController after generating and saving the code.
     *
     * @param recipientEmail  
     * @param recipientName  
     * @param resetCode       
     */
    public static void sendPasswordResetEmail(String recipientEmail, String recipientName, String resetCode) {
        try {
            Message message = new MimeMessage(buildSession());
            message.setFrom(new InternetAddress(SENDER_EMAIL, "Sante Diagnostics"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject("Your Password Reset Code - Sante Diagnostics");
            message.setText(
                "Dear " + recipientName + ",\n\n" +
                "We received a request to reset your password for your " +
                "Sante Diagnostics account.\n\n" +
                "Your 6-digit reset code is:\n\n" +
                "  " + resetCode + "\n\n" +
                "This code expires in 15 minutes. Enter it on the reset screen " +
                "along with your new password.\n\n" +
                "If you did not request a password reset, you can safely ignore " +
                "this email. Your account remains secure.\n\n" +
                "Regards,\n" +
                "Sante Diagnostics Laboratory\n" +
                "admin@sante.com"
            );

            Transport.send(message);
            System.out.println("Password reset email sent to: " + recipientEmail);

        } catch (Exception e) {
            System.err.println("Failed to send reset email to " + recipientEmail + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
 

    /**
     * Sends an account verification email containing a 6-digit code.
     * Called by RegisterController immediately after a new customer registers.
     *
     * @param recipientEmail  The customer's registered email address
     * @param recipientName   The customer's full name
     * @param verifyCode      The generated 6-digit verification code
     */
    public static void sendVerificationEmail(String recipientEmail, String recipientName, String verifyCode) {
        try {
            Message message = new MimeMessage(buildSession());
            message.setFrom(new InternetAddress(SENDER_EMAIL, "Sante Diagnostics"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject("Verify Your Sante Diagnostics Account");
            message.setText(
                "Dear " + recipientName + ",\n\n" +
                "Welcome to Sante Diagnostics! Your account has been created successfully.\n\n" +
                "To activate your account, please enter the 6-digit verification code below:\n\n" +
                "  " + verifyCode + "\n\n" +
                "This code expires in 15 minutes.\n\n" +
                "If you did not create this account, please ignore this email.\n\n" +
                "Regards,\n" +
                "Sante Diagnostics Laboratory\n" +
                "admin@sante.com"
            );

            Transport.send(message);
            System.out.println("Verification email sent to: " + recipientEmail);

        } catch (Exception e) {
            System.err.println("Failed to send verification email to " + recipientEmail + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    
}