package io.codekvast.backoffice.service.impl;

import io.codekvast.backoffice.service.MailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author olle.hallin@crisp.se
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MailSenderImpl implements MailSender {

    @Override
    public void sendMail(String template, Long customerId) {
        // TODO: implement mail sending.
        logger.debug("Would have sent mail using template '{}' to customer {}", template, customerId);
    }
}
