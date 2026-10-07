package com.ccadmin.app.sunat.model.dto;

public class SunatInitializationFormDto {
    public String Mode = "NONE";
    public String IssuerRuc;
    public String SolUser;
    public boolean HasSolPassword;
    public boolean HasCertificate;
    public boolean HasCertificatePassword;
    public boolean GuideEnabled;
    public String GuideClientId;
    public boolean HasGuideClientSecret;
    public String BetaEndpoint;
    public String ProductionEndpoint;
    public String GuideEndpoint;
    public String GuideTokenEndpoint;
}
