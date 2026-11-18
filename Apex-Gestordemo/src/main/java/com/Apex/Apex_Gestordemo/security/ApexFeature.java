package com.Apex.Apex_Gestordemo.security;

public enum ApexFeature {
    BASIC_PRICING,
    FINANCIAL_CORE,
    ADVANCED_FINANCE,
    COMMISSION_OMNICHANNEL,
    B2C_WHITE_LABEL,
    MULTI_STORE_ROUTING,
    SOFT_STOCK_ALLOCATION,
    HIERARCHICAL_REPORTS;

    public static ApexFeature fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Codigo de funcionalidade nao pode ser vazio.");
        }
        return ApexFeature.valueOf(code.trim().toUpperCase(java.util.Locale.ROOT));
    }
}
