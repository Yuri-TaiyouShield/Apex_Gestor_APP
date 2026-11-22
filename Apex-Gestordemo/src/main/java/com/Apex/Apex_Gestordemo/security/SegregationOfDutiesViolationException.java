package com.Apex.Apex_Gestordemo.security;

public class SegregationOfDutiesViolationException extends RuntimeException {

    private final String dominioOperacao;

    public SegregationOfDutiesViolationException(String dominioOperacao, String message) {
        super(message);
        this.dominioOperacao = dominioOperacao;
    }

    public String getDominioOperacao() {
        return dominioOperacao;
    }
}
