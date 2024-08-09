package com.hyp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.hyp.request.MailNotificationRequest;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MailService  {

	@Autowired
	JavaMailSender javaMailSender;
	
	@Autowired
	PartnerService partnerService;
	
	@Value("${notification.email.address}")
	private String groupEmailAddress;

	public void sendNotificationEmail(MailNotificationRequest notificationRequest) {
	    if (groupEmailAddress == null || groupEmailAddress.isEmpty()) {
	        log.warn("No valid email address found in application properties.");
	        return;
	    }

	    SimpleMailMessage mailMessage = new SimpleMailMessage();
	    mailMessage.setTo(groupEmailAddress);
	    mailMessage.setSubject(notificationRequest.getSubject());
	    mailMessage.setText(notificationRequest.getMessage());

	    try {
	        javaMailSender.send(mailMessage);
	        log.info("Notification email sent successfully to {}", groupEmailAddress);
	    } catch (Exception e) {
	        log.error("Failed to send notification email: {}", e.getMessage());
	    }
	}
}
