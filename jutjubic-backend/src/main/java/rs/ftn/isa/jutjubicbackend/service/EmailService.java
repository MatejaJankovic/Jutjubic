package rs.ftn.isa.jutjubicbackend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import rs.ftn.isa.jutjubicbackend.model.User;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${email.from}")
    private String emailFrom;

    @Value("${email.from-name}")
    private String emailFromName;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Async
    public void sendActivationEmail(User user, String activationToken) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(emailFrom, emailFromName);
            helper.setTo(user.getEmail());
            helper.setSubject("Aktivacija naloga - Jutjubic");

            String activationLink = frontendUrl + "/activate?token=" + activationToken;
            String htmlContent = buildActivationEmailContent(user.getFirstName(), activationLink);

            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Activation email sent successfully to: {}", user.getEmail());

        } catch (MessagingException e) {
            log.error("Failed to send activation email to: {} - Error: {}", user.getEmail(), e.getMessage());
            throw new RuntimeException("Greška pri slanju aktivacionog emaila", e);
        } catch (Exception e) {
            log.error("Unexpected error while sending email to: {} - Error: {}", user.getEmail(), e.getMessage());
            throw new RuntimeException("Neočekivana greška pri slanju emaila", e);
        }
    }

    private String buildActivationEmailContent(String firstName, String activationLink) {
        return """
                <!DOCTYPE html>
                <html lang="sr">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Aktivacija naloga</title>
                    <link rel="preconnect" href="https://fonts.googleapis.com">
                    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
                    <link href="https://fonts.googleapis.com/css2?family=Rubik:wght@300;400;500;600;700&display=swap" rel="stylesheet">
                </head>
                <body style="margin: 0; padding: 0; font-family: 'Rubik', Arial, sans-serif; background-color: #f4f4f4;">
                    <table role="presentation" style="width: 100%%; border-collapse: collapse;">
                        <tr>
                            <td align="center" style="padding: 40px 0;">
                                <table role="presentation" style="width: 600px; border-collapse: collapse; background-color: #ffffff; box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);">
                                    <!-- Header -->
                                    <tr>
                                        <td style="padding: 40px 30px; text-align: center; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);">
                                            <h1 style="margin: 0; color: #ffffff; font-size: 28px; font-family: 'Rubik', Arial, sans-serif;">Jutjubic</h1>
                                        </td>
                                    </tr>
                                    
                                    <!-- Content -->
                                    <tr>
                                        <td style="padding: 40px 30px;">
                                            <h2 style="margin: 0 0 20px 0; color: #333333; font-size: 24px; font-family: 'Rubik', Arial, sans-serif;">Pozdrav %s,</h2>
                                            
                                            <p style="margin: 0 0 20px 0; color: #666666; font-size: 16px; line-height: 1.6; font-family: 'Rubik', Arial, sans-serif;">
                                                Hvala što si se registrovao/la na <strong>Jutjubic</strong> platformu!
                                            </p>
                                            
                                            <p style="margin: 0 0 30px 0; color: #666666; font-size: 16px; line-height: 1.6; font-family: 'Rubik', Arial, sans-serif;">
                                                Da bi aktivirao/la svoj nalog, klikni na dugme ispod:
                                            </p>
                                            
                                            <!-- Activation Button -->
                                            <table role="presentation" style="width: 100%%; border-collapse: collapse;">
                                                <tr>
                                                    <td align="center" style="padding: 0 0 30px 0;">
                                                        <a href="%s" 
                                                           style="display: inline-block; padding: 16px 40px; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: #ffffff; text-decoration: none; border-radius: 5px; font-size: 16px; font-weight: bold; font-family: 'Rubik', Arial, sans-serif;">
                                                            AKTIVIRAJ NALOG
                                                        </a>
                                                    </td>
                                                </tr>
                                            </table>
                                            
                                            <p style="margin: 30px 0 0 0; color: #999999; font-size: 14px; line-height: 1.6; font-family: 'Rubik', Arial, sans-serif;">
                                                Ako nisi ti kreirao/la ovaj nalog, ignoriši ovaj email.
                                            </p>
                                        </td>
                                    </tr>
                                    
                                    <!-- Footer -->
                                    <tr>
                                        <td style="padding: 30px; text-align: center; background-color: #f8f9fa; border-top: 1px solid #dee2e6;">
                                            <p style="margin: 0 0 10px 0; color: #666666; font-size: 14px; font-family: 'Rubik', Arial, sans-serif;">
                                                Srdačan pozdrav,<br>
                                                <strong>Jutjubic Tim</strong>
                                            </p>
                                            <p style="margin: 10px 0 0 0; color: #999999; font-size: 12px; font-family: 'Rubik', Arial, sans-serif;">
                                                © 2026 Jutjubic. Sva prava zadržana.
                                            </p>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """.formatted(firstName, activationLink);
    }
}

