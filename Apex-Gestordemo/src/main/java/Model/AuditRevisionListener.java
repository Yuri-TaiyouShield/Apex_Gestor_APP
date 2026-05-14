package Model;

import org.hibernate.envers.RevisionListener;

public class AuditRevisionListener implements RevisionListener {

    @Override
    public void newRevision(Object revisionEntity) {
        AuditRevision revision = (AuditRevision) revisionEntity;
        AuditRevisionContext context = AuditRevisionContextHolder.get();
        if (context == null) {
            revision.setActor("system");
            return;
        }
        revision.setActor(blankToDefault(context.actor(), "system"));
        revision.setSourceIp(context.sourceIp());
        revision.setUserAgent(truncate(context.userAgent(), 255));
        revision.setCorrelationId(context.correlationId());
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
