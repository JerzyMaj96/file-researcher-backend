package com.jerzymaj.file_researcher_backend.configuration;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
public class TestMailConfig {

    @Bean
    public JavaMailSender javaMailSender() {
        return new JavaMailSender() {
            @Override
            public MimeMessage createMimeMessage() {
                return new MimeMessage((jakarta.mail.Session) null);
            }

            @Override
            public MimeMessage createMimeMessage(java.io.InputStream contentStream) throws MailException {
                try {
                    return new MimeMessage(null, contentStream);
                } catch (MessagingException e) {
                    throw new MailException("Failed to create MimeMessage") {};
                }
            }

            @Override
            public void send(MimeMessage mimeMessage) throws MailException {
            }

            @Override
            public void send(MimeMessage... mimeMessages) throws MailException {
            }

            @Override
            public void send(SimpleMailMessage simpleMessage) throws MailException {
            }

            @Override
            public void send(SimpleMailMessage... simpleMessages) throws MailException {
            }
        };
    }
}
