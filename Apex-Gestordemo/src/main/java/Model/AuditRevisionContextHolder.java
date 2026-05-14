package Model;

public final class AuditRevisionContextHolder {

    private static final ThreadLocal<AuditRevisionContext> CONTEXT = new ThreadLocal<>();

    private AuditRevisionContextHolder() {
    }

    public static void set(AuditRevisionContext context) {
        CONTEXT.set(context);
    }

    public static AuditRevisionContext get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
