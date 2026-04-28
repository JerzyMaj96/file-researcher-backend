package com.jerzymaj.file_researcher_backend.unit_tests;

import com.jerzymaj.file_researcher_backend.services.ZipEmailSender;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ZipEmailSenderUnitTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MimeMessage message;

    @InjectMocks
    private ZipEmailSender zipEmailSender;

    @Test
    public void shouldSendZipArchiveByEmail(@TempDir Path tempDir) throws IOException, MessagingException {

        Path zipFilePath = Files.createFile(tempDir.resolve("test.zip"));

        message = new MimeMessage((Session) null);

        when(mailSender.createMimeMessage()).thenReturn(message);

        zipEmailSender.sendZipArchiveByEmail("test@gmail.com", zipFilePath, "Subject", "Content");

        verify(mailSender).send(any(MimeMessage.class));
    }
}
