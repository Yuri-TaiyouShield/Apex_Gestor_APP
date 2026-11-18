package com.Apex.Apex_Gestordemo.web;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.NoSuchElementException;

import Service.DataProtectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice(basePackages = {"Controller", "com.Apex.Apex_Gestordemo"})
public class ApiExceptionHandler {

    private final DataProtectionService dataProtectionService;

    @Autowired
    public ApiExceptionHandler(DataProtectionService dataProtectionService) {
        this.dataProtectionService = dataProtectionService;
    }

    public ApiExceptionHandler() {
        this(new DataProtectionService());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        ProblemDetail problem = createProblem(HttpStatus.BAD_REQUEST, "Dados invalidos",
                "Revise os campos destacados e tente novamente.", request);
        problem.setProperty("violations", ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + dataProtectionService.maskSensitiveText(error.getDefaultMessage()))
                .toList());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @ExceptionHandler({EntityNotFoundException.class, NoSuchElementException.class})
    public ResponseEntity<ProblemDetail> handleNotFound(Exception ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(createProblem(HttpStatus.NOT_FOUND,
                "Registro nao encontrado", "O item solicitado nao existe ou ja foi removido.", request));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrity(DataIntegrityViolationException ex,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(createProblem(HttpStatus.CONFLICT,
                "Conflito de dados", "A operacao viola uma regra de integridade do banco de dados.", request));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> handleResponseStatus(ResponseStatusException ex,
            HttpServletRequest request) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        return ResponseEntity.status(status).body(createProblem(status, "Operacao nao concluida",
                safeBusinessMessage(ex.getReason()), request));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> handleOptimisticLock(ObjectOptimisticLockingFailureException ex,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(createProblem(HttpStatus.CONFLICT,
                "Conflito de concorrencia", "O registro foi alterado por outra operacao. Recarregue e tente novamente.", request));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ProblemDetail> handleDataAccess(DataAccessException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(createProblem(HttpStatus.SERVICE_UNAVAILABLE,
                "Dependencia indisponivel", "O armazenamento nao esta disponivel no momento. Tente novamente.", request));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(IllegalArgumentException ex,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(createProblem(HttpStatus.BAD_REQUEST,
                "Requisicao invalida", safeBusinessMessage(ex.getMessage()), request));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(createProblem(HttpStatus.FORBIDDEN,
                "Acesso negado", safeBusinessMessage(ex.getMessage()), request));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthentication(AuthenticationException ex,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(createProblem(HttpStatus.UNAUTHORIZED,
                "Nao autenticado", "Credenciais invalidas ou sessao expirada.", request));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ProblemDetail> handleBusiness(IllegalStateException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(createProblem(HttpStatus.BAD_REQUEST,
                "Regra de negocio nao atendida", safeBusinessMessage(ex.getMessage()), request));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createProblem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro interno",
                "Nao foi possivel concluir a operacao agora. Tente novamente ou acione o suporte com o codigo de rastreio.",
                request));
    }

    private ProblemDetail createProblem(HttpStatus status, String title, String detail, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("https://apexgestor.local/problems/" + status.value()));
        problem.setProperty("path", request.getRequestURI());
        problem.setProperty("timestamp", OffsetDateTime.now().toString());
        problem.setProperty("traceId", request.getAttribute(RequestTraceFilter.TRACE_ID_ATTRIBUTE));
        return problem;
    }

    private String safeBusinessMessage(String message) {
        if (message == null || message.isBlank()) {
            return "A operacao nao pode ser concluida com os dados informados.";
        }
        String masked = dataProtectionService.maskSensitiveText(message);
        return masked.length() > 240 ? masked.substring(0, 240) : masked;
    }
}
