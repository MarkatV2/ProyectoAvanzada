package co.edu.uniquindio.proyecto.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final String verificationTemplate;
    private final String resetPasswordTemplate;
    private final String commentTemplate;
    private final String reportTemplate;

    public EmailService(JavaMailSender mailSender,
                        @Value("#{@verificationEmailTemplate}") String verificationTemplate,
                        @Value("#{@resetPasswordEmailTemplate}") String resetPasswordTemplate,
                        @Value("#{@commentNotificationEmailTemplate}") String commentTemplate,
                        @Value("#{@reportNotificationEmailTemplate}") String reportTemplate) {
        this.mailSender = mailSender;
        this.verificationTemplate = verificationTemplate;
        this.resetPasswordTemplate = resetPasswordTemplate;
        this.commentTemplate = commentTemplate;
        this.reportTemplate = reportTemplate;
    }

    public void sendVerificationEmail(String toEmail, String code) {
        String content = verificationTemplate
                .replace("{{email}}", toEmail)
                .replace("{{code}}", code);
        sendHtmlEmail(toEmail, "Verificación de cuenta", content);
    }

    public void sendPasswordResetEmail(String toEmail, String code) {
        String content = resetPasswordTemplate
                .replace("{{email}}", toEmail)
                .replace("{{code}}", code);
        sendHtmlEmail(toEmail, "Restablecimiento de contraseña", content);
    }

    public void sendCommentEmail(String toEmail, String username, String reportTitle, String commentContent) {
        String content = commentTemplate
                .replace("${username}", username)
                .replace("${reportTitle}", reportTitle)
                .replace("${commentContent}", commentContent);
        sendHtmlEmail(toEmail, "Nuevo comentario en tu reporte", content);
    }

    public void sendNearbyReportEmail(String toEmail, String username, String reportTitle,String reportDescription) {
        String content = reportTemplate
                .replace("${username}", username)
                .replace("${reportTitle}", reportTitle)
                .replace("${reportDescription}", reportDescription);

        sendHtmlEmail(toEmail, "Nuevo reporte cerca de ti", content);
    }

    private void sendHtmlEmail(String toEmail, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(html, true);

            mailSender.send(message);
            log.info("Correo enviado a {}", toEmail);
        } catch (MessagingException e) {
            log.error("Error al enviar correo a {}", toEmail, e);
            throw new RuntimeException("Error al enviar correo", e);
        }
    }
}


