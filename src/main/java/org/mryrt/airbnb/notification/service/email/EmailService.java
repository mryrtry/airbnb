package org.mryrt.airbnb.notification.service.email;

public interface EmailService {

    void send(String to, String subject, String body);
}
