package Service;

import Model.FinancialDigitalDocument;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class FinancialDocumentMessagingService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${apex.messaging.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${apex.messaging.email.from:noreply@apexgestor.local}")
    private String from;

    public FinancialDocumentMessagingService(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSenderProvider = mailSenderProvider;
    }

    public DeliveryResult sendSignedDocument(FinancialDigitalDocument document) {
        if (!emailEnabled) {
            return DeliveryResult.SKIPPED;
        }
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            return DeliveryResult.SKIPPED;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(stripHeaderInjection(from));
        message.setTo(stripHeaderInjection(document.getFuncionarioEmail()));
        message.setSubject(stripHeaderInjection(document.getAssuntoEmail()));
        message.setText(document.getMensagemEmail()
                + "\n\nAssinatura digital: " + document.getAssinaturaDigitalHash()
                + "\n\n" + document.getConteudo());
        mailSender.send(message);
        return DeliveryResult.SENT;
    }

    private String stripHeaderInjection(String value) {
        return value == null ? null : value.replaceAll("[\\r\\n]+", " ").trim();
    }

    public enum DeliveryResult {
        SENT,
        SKIPPED
    }
}
