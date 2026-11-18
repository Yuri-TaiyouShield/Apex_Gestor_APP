package com.Apex.Apex_Gestordemo.security;

public class FeatureNotLicensedException extends RuntimeException {

    private final ApexFeature feature;
    private final String licenseStatus;

    public FeatureNotLicensedException(ApexFeature feature, String licenseStatus, String message) {
        super(message);
        this.feature = feature;
        this.licenseStatus = licenseStatus;
    }

    public ApexFeature getFeature() {
        return feature;
    }

    public String getLicenseStatus() {
        return licenseStatus;
    }
}
