package com.bigobooks.services;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.bigobooks.entities.mail.MailOutbox;
import com.bigobooks.repositories.MailOutboxRepository;

@Service
public class MailOutboxService {

	private static final Logger log = LoggerFactory.getLogger(MailOutboxService.class);

	private final MailOutboxRepository repository;
	private final JavaMailSender mailSender;
	private final String from;

	public MailOutboxService(MailOutboxRepository repository, JavaMailSender mailSender,
			@Value("${EMAIL_FROM}") String from) {
		this.repository = repository;
		this.mailSender = mailSender;
		this.from = from;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void enqueue(String recipient, String subject, String body) {
		MailOutbox outbox = new MailOutbox();
		outbox.setRecipient(recipient);
		outbox.setSubject(subject);
		outbox.setBody(body);
		repository.save(outbox);
	}

	@Scheduled(cron = "0 */3 * * * *")
	@Transactional(propagation = Propagation.REQUIRED)
	public void sendPending() {
		for (MailOutbox outbox : repository.findTop50ByPendingTrueOrderByCreatedAtAsc()) {
			SimpleMailMessage message = new SimpleMailMessage();
			message.setFrom(from);
			message.setTo(outbox.getRecipient());
			message.setSubject(outbox.getSubject());
			message.setText(outbox.getBody());
			try {
				mailSender.send(message);
				outbox.setPending(false);
				outbox.setSentAt(LocalDateTime.now());
			} catch (MailException exception) {
				log.warn("No se pudo enviar el correo {} a {}: {}", outbox.getId(), outbox.getRecipient(),
						exception.getMessage());
			}
		}
	}
}
