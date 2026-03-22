package com.deliveryland.backend.common.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import jakarta.mail.internet.MimeMessage;

@Slf4j
@Service
public class EmailService {

    @Value("${app.base-url}")
    private String baseUrl;
    private final JavaMailSender mailSender;

    private String getLogo() {
        return baseUrl + "/logo.png";
    }

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async("emailTaskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void sendEmail(String toEmail, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send email to {} with subject '{}'", toEmail, subject, e);
        }
    }

    public void sendWelcomeEmail(String email, String fullName) {
        String subject = "Welcome to DeliveryLand!";

        String htmlContent = "<!DOCTYPE html>"
                + "<html lang='en'>"
                + "  <head>"
                + "    <meta charset='UTF-8' />"
                + "    <meta name='viewport' content='width=device-width, initial-scale=1.0' />"
                + "    <title>Welcome Email</title>"
                + "  </head>"
                + "  <body style='font-family: Arial, sans-serif; color: #333; background: #f9f9f9; margin: 0; padding: 0;'>"
                + "    <div style='max-width: 500px; width: 100%; margin: auto; background: #fff; border-radius: 10px; overflow: hidden;'>"
                + "      <div style='text-align: center; padding: 20px; background: #2e86c1; color: white;'>"
                + "        <h2 style='margin: 0;'>DeliveryLand</h2>"
                + "      </div>"
                + "      <div style='text-align:center; padding: 20px;'>"
                + "        <img src='" + getLogo() + "' alt='DeliveryLand Logo' "
                + "             style='width: 120px; height: auto; margin-bottom: 20px;'/>"
                + "      </div>"
                + "      <div style='padding: 0 20px 40px 20px;'>"
                + "        <h3 style='color: #2e86c1;'>Welcome to DeliveryLand, " + fullName + "!</h3>"
                + "        <p style='line-height: 1.6;'>We're thrilled to have you on board at <strong>DeliveryLand</strong>! "
                + "        Get ready to track, manage, and enjoy seamless deliveries from start to finish.</p>"
                + "        <div style='text-align: center; margin: 30px 0;'>"
                + "          <a href='" + baseUrl + "/dashboard' "
                + "             style='background: #2e86c1; color: #fff; padding: 12px 25px; text-decoration: none; "
                + "                    border-radius: 5px; font-size: 16px; display: inline-block;'>"
                + "             Go to Your Dashboard"
                + "          </a>"
                + "        </div>"
                + "        <p style='line-height: 1.6;'>If you have any questions, feel free to reach out – our support team is always ready to help.</p>"
                + "        <hr style='margin: 30px 0; border: none; border-top: 1px solid #ccc' />"
                + "        <p style='font-size: 13px; color: #666; text-align: center;'>You’re receiving this email because you created an account with DeliveryLand.</p>"
                + "        <p style='font-size: 12px; color: #999; text-align: center; margin-top: 15px;'>© "
                + java.time.Year.now().getValue() + " DeliveryLand – All rights reserved.</p>"
                + "      </div>"
                + "    </div>"
                + "  </body>"
                + "</html>";

        sendEmail(email, subject, htmlContent);
    }

    public void sendPasswordResetEmail(String email, String token) {
        String subject = "DeliveryLand - Password Reset Code";

        String htmlContent = "<!DOCTYPE html>"
                + "<html lang='en'>"
                + "  <head>"
                + "    <meta charset='UTF-8' />"
                + "    <meta name='viewport' content='width=device-width, initial-scale=1.0' />"
                + "    <title>Password Reset</title>"
                + "  </head>"
                + "  <body style='font-family: Arial, sans-serif; color: #333; background: #f4f6f8; margin: 0; padding: 0;'>"
                + "    <div style='max-width: 500px; width: 100%; margin: auto; background: #fff; border-radius: 10px; overflow: hidden;'>"

                // Header
                + "      <div style='text-align: center; padding: 20px; background: #1e3a8a; color: white;'>"
                + "        <h2 style='margin: 0;'>DeliveryLand</h2>"
                + "      </div>"

                // Logo
                + "      <div style='text-align:center; padding: 20px;'>"
                + "        <img src='" + getLogo() + "' alt='DeliveryLand Logo' "
                + "             style='width: 120px; height: auto; margin-bottom: 20px;'/>"
                + "      </div>"

                // Content
                + "      <div style='padding: 0 20px 40px 20px;'>"
                + "        <h3 style='color: #1e3a8a;'>Reset Your Password</h3>"

                + "        <p style='line-height: 1.6;'>We received a request to reset your password for your <strong>DeliveryLand</strong> account.</p>"

                + "        <p style='line-height: 1.6;'>Use the verification code below to continue:</p>"

                // Token
                + "        <div style='text-align: center; margin: 30px 0;'>"
                + "          <p style='font-size: 28px; font-weight: bold; color: #1e3a8a; letter-spacing: 4px;'>"
                +                token
                + "          </p>"
                + "        </div>"

                + "        <p style='line-height: 1.6;'>Enter this code in the app or website to reset your password. "
                + "        This code will expire in <strong>30 minutes</strong>.</p>"

                + "        <p style='line-height: 1.6;'>For your security, do not share this code with anyone.</p>"

                // Footer
                + "        <hr style='margin: 30px 0; border: none; border-top: 1px solid #ccc' />"

                + "        <p style='font-size: 13px; color: #666; text-align: center;'>"
                + "        If you did not request this, you can safely ignore this email."
                + "        </p>"

                + "        <p style='font-size: 12px; color: #999; text-align: center; margin-top: 15px;'>© "
                + java.time.Year.now().getValue() + " DeliveryLand – All rights reserved.</p>"

                + "      </div>"
                + "    </div>"
                + "  </body>"
                + "</html>";

        sendEmail(email, subject, htmlContent);
    }

    public void sendAccountVerificationEmail(String email, String token) {
        String subject = "DeliveryLand - Verify Your Account";

        String htmlContent = "<!DOCTYPE html>"
                + "<html lang='en'>"
                + "  <head>"
                + "    <meta charset='UTF-8' />"
                + "    <meta name='viewport' content='width=device-width, initial-scale=1.0' />"
                + "    <title>Email Verification</title>"
                + "  </head>"
                + "  <body style='font-family: Arial, sans-serif; color: #333; background: #f4f6f8; margin: 0; padding: 0;'>"
                + "    <div style='max-width: 500px; width: 100%; margin: auto; background: #fff; border-radius: 10px; overflow: hidden;'>"

                // Header (same blue)
                + "      <div style='text-align: center; padding: 20px; background: #1e3a8a; color: white;'>"
                + "        <h2 style='margin: 0;'>DeliveryLand</h2>"
                + "      </div>"

                // Logo
                + "      <div style='text-align:center; padding: 20px;'>"
                + "        <img src='" + getLogo() + "' alt='DeliveryLand Logo' "
                + "             style='width: 120px; height: auto; margin-bottom: 20px;'/>"
                + "      </div>"

                // Content
                + "      <div style='padding: 0 20px 40px 20px;'>"
                + "        <h3 style='color: #1e3a8a;'>Verify Your Email Address</h3>"

                + "        <p style='line-height: 1.6;'>Welcome to <strong>DeliveryLand</strong>! "
                + "        To complete your registration, please verify your email using the code below:</p>"

                + "        <p style='line-height: 1.6;'>Use the verification code below to activate your account:</p>"

                // Token (same styling)
                + "        <div style='text-align: center; margin: 30px 0;'>"
                + "          <p style='font-size: 28px; font-weight: bold; color: #1e3a8a; letter-spacing: 4px;'>"
                +                token
                + "          </p>"
                + "        </div>"

                + "        <p style='line-height: 1.6;'>Enter this code in the app or website to verify your account. "
                + "        This code will expire shortly for security reasons.</p>"

                + "        <p style='line-height: 1.6;'>Once verified, you’ll be able to start placing orders, "
                + "        managing deliveries, or running your store depending on your role.</p>"

                // Footer
                + "        <hr style='margin: 30px 0; border: none; border-top: 1px solid #ccc' />"

                + "        <p style='font-size: 13px; color: #666; text-align: center;'>"
                + "        If you didn’t create this account, you can safely ignore this email."
                + "        </p>"

                + "        <p style='font-size: 12px; color: #999; text-align: center; margin-top: 15px;'>© "
                + java.time.Year.now().getValue() + " DeliveryLand – All rights reserved.</p>"

                + "      </div>"
                + "    </div>"
                + "  </body>"
                + "</html>";

        sendEmail(email, subject, htmlContent);
    }

    public void sendEmailChangeVerificationEmail(String newEmail, String token) {
        String subject = "AI Recipe Generator - Confirm Your New Email";

        String htmlContent = "<!DOCTYPE html>"
                + "<html lang='en'>"
                + "  <head>"
                + "    <meta charset='UTF-8' />"
                + "    <meta name='viewport' content='width=device-width, initial-scale=1.0' />"
                + "    <title>Email Change Verification</title>"
                + "  </head>"
                + "  <body style='font-family: Arial, sans-serif; color: #333; background: #f9f9f9; margin: 0; padding: 0;'>"
                + "    <div style='max-width: 500px; width: 100%; margin: auto; background: #fff; border-radius: 10px; overflow: hidden;'>"
                + "      <div style='text-align: center; padding: 20px; background: #2e86c1; color: white;'>"
                + "        <h2 style='margin: 0;'>AI Recipe Generator</h2>"
                + "      </div>"
                + "      <div style='text-align:center; padding: 20px;'>"
                + "        <img src='" + getLogo() + "' alt='AI Recipe Generator Logo' "
                + "             style='width: 120px; height: auto; margin-bottom: 20px;'/>"
                + "      </div>"
                + "      <div style='padding: 0 20px 40px 20px;'>"
                + "        <h3 style='color: #2e86c1;'>Verify Your New Email Address</h3>"
                + "        <p style='line-height: 1.6;'>You recently requested to update your email address for your "
                + "        <strong>AI Recipe Generator</strong> account. To confirm this change, please enter the code below:</p>"
                + "        <div style='text-align: center; margin: 30px 0;'>"
                + "          <p style='font-size: 26px; font-weight: bold; color: #2e86c1; letter-spacing: 3px;'>"
                + "            " + token
                + "          </p>"
                + "        </div>"
                + "        <p style='line-height: 1.6;'>Enter this verification code in the app or website to complete your email update.</p>"
                + "        <hr style='margin: 30px 0; border: none; border-top: 1px solid #ccc' />"
                + "        <p style='font-size: 13px; color: #666; text-align: center;'>Didn’t request an email change? Please ignore this email.</p>"
                + "        <p style='font-size: 12px; color: #999; text-align: center; margin-top: 15px;'>© "
                + "            " + java.time.Year.now().getValue() + " AI Recipe Generator – All rights reserved.</p>"
                + "      </div>"
                + "    </div>"
                + "  </body>"
                + "</html>";

        // Send confirmation token to the new email
        sendEmail(newEmail, subject, htmlContent);
    }

    public void sendAccountDeletionEmail(String email, String fullName) {
        String subject = "Delivery Land - Account Deletion Confirmation";

        String htmlContent = "<!DOCTYPE html>"
                + "<html lang='en'>"
                + "  <head>"
                + "    <meta charset='UTF-8' />"
                + "    <meta name='viewport' content='width=device-width, initial-scale=1.0' />"
                + "    <title>Account Deletion Confirmation</title>"
                + "  </head>"
                + "  <body style='font-family: Arial, sans-serif; color: #333; background: #f9f9f9; margin: 0; padding: 0;'>"
                + "    <div style='max-width: 500px; width: 100%; margin: auto; background: #fff; border-radius: 10px; overflow: hidden;'>"

                // Header
                + "      <div style='text-align: center; padding: 20px; background: #e74c3c; color: white;'>"
                + "        <h2 style='margin: 0;'>Delivery Land</h2>"
                + "      </div>"

                // Logo
                + "      <div style='text-align:center; padding: 20px;'>"
                + "        <img src='" + getLogo() + "' alt='Delivery Land Logo' "
                + "             style='width: 120px; height: auto; margin-bottom: 20px;'/>"
                + "      </div>"

                // Body
                + "      <div style='padding: 0 20px 40px 20px;'>"
                + "        <h3 style='color: #e74c3c;'>Account Deleted</h3>"
                + "        <p style='line-height: 1.6;'>Hi " + fullName + ",</p>"
                + "        <p style='line-height: 1.6;'>Your <strong>Delivery Land</strong> account has been successfully deleted as requested.</p>"

                + "        <p style='line-height: 1.6;'>All associated data, including orders, delivery history, and account details, "
                + "        has been permanently removed and cannot be recovered.</p>"

                + "        <p style='line-height: 1.6;'>We're sorry to see you go. If you change your mind in the future, "
                + "        you're always welcome to create a new account and continue using our services.</p>"

                // Footer
                + "        <hr style='margin: 30px 0; border: none; border-top: 1px solid #ccc' />"
                + "        <p style='font-size: 13px; color: #666; text-align: center;'>If you did not request this action, please contact support immediately.</p>"
                + "        <p style='font-size: 12px; color: #999; text-align: center; margin-top: 15px;'>© "
                +              java.time.Year.now().getValue() + " Delivery Land – All rights reserved.</p>"
                + "      </div>"

                + "    </div>"
                + "  </body>"
                + "</html>";

        sendEmail(email, subject, htmlContent);
    }

}
