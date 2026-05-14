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

    public boolean sendSignedDocument(FinancialDigitalDocument document) {
        if (!emailEnabled) {
            return false;
        }
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            return false;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(document.getFuncionarioEmail());
        message.setSubject(document.getAssuntoEmail());
        message.setText(document.getMensagemEmail() + "\n\nAssinatura digital: " + document.getAssinaturaDigitalHash() + "\n\n" + document.getConteudo());
        mailSender.send(message);
        return true;
    }
}
