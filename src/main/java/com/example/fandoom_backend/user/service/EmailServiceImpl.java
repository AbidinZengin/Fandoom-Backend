package com.example.fandoom_backend.user.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender javaMailSender;

    @Override
    @Async
    public void sendVerificationEmail(String to, String username, String verificationLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Fandoom — e-posta adresinizi doğrulayın");
        message.setText("""
                Merhaba %s,

                Fandoom hesabınızı aktifleştirmek için aşağıdaki linke tıklayın:
                %s

                Bu linki siz talep etmediyseniz bu e-postayı yok sayabilirsiniz.
                """.formatted(username, verificationLink));
        try {
            javaMailSender.send(message);
        } catch (MailException e) {
            // Async çalıştığı için burada yakalanmazsa hata sessizce kaybolur;
            // register akışını bozmadan sadece logluyoruz.
            log.error("Doğrulama e-postası gönderilemedi: to={}", to, e);
        }
    }
}
