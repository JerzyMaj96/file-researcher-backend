package com.jerzymaj.file_researcher_backend.services;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Service
public class ZipEmailSender {

    JavaMailSender mailSender;

    /**
     * Sends a ZIP archive as an email attachment to the specified recipient.
     *
     * @param recipientEmail the recipient's email address
     * @param zipFilePath    the path to the ZIP file to be sent
     * @param subject        the subject of the email
     * @param text           the body text of the email
     * @throws MessagingException if an error occurs while sending the email
     */

    public void sendZipArchiveByEmail(String recipientEmail,
                                      Path zipFilePath,
                                      String subject,
                                      String text) throws MessagingException {

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(recipientEmail);
        helper.setSubject(subject);
        helper.setText(text);
        helper.addAttachment(zipFilePath.getFileName().toString(),
                new FileSystemResource(zipFilePath.toFile()));

        mailSender.send(message);
    }
}
