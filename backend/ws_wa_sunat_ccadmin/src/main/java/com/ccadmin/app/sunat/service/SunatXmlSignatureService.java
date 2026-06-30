package com.ccadmin.app.sunat.service;

import com.ccadmin.app.sunat.model.entity.SunatConfigEntity;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

@Service
public class SunatXmlSignatureService {

    private static final String EXT_NS = "urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2";

    public String sign(SunatConfigEntity config, String unsignedXml) {
        if (unsignedXml == null || unsignedXml.isBlank()) {
            throw new IllegalArgumentException("XML sin firmar requerido");
        }
        try {
            CertificateData certificate = loadCertificate(config);
            Document document = parseXml(unsignedXml);
            DOMSignContext signContext = new DOMSignContext(certificate.privateKey, findExtensionContent(document));
            signContext.setDefaultNamespacePrefix("ds");

            XMLSignatureFactory factory = XMLSignatureFactory.getInstance("DOM");
            Reference reference = factory.newReference(
                    "",
                    factory.newDigestMethod(DigestMethod.SHA256, null),
                    Collections.singletonList(factory.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null)),
                    null,
                    null
            );
            SignedInfo signedInfo = factory.newSignedInfo(
                    factory.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE, (C14NMethodParameterSpec) null),
                    factory.newSignatureMethod(SignatureMethod.RSA_SHA256, null),
                    Collections.singletonList(reference)
            );
            KeyInfo keyInfo = buildKeyInfo(factory, certificate.certificate);
            XMLSignature signature = factory.newXMLSignature(signedInfo, keyInfo);
            signature.sign(signContext);
            return toXml(document);
        } catch (Exception ex) {
            throw new IllegalArgumentException("No se pudo firmar XML SUNAT: " + ex.getMessage(), ex);
        }
    }

    private CertificateData loadCertificate(SunatConfigEntity config) throws Exception {
        if (config.CertificatePath == null || config.CertificatePath.isBlank()) {
            throw new IllegalArgumentException("Ruta de certificado requerida");
        }
        if (config.CertificatePassword == null || config.CertificatePassword.isBlank()) {
            throw new IllegalArgumentException("Clave de certificado requerida");
        }
        String keyStoreType = "JKS".equalsIgnoreCase(config.CertificateType) ? "JKS" : "PKCS12";
        KeyStore keyStore = KeyStore.getInstance(keyStoreType);
        char[] password = config.CertificatePassword.toCharArray();
        try (var input = Files.newInputStream(Path.of(config.CertificatePath))) {
            keyStore.load(input, password);
        }
        Enumeration<String> aliases = keyStore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            if (!keyStore.isKeyEntry(alias)) {
                continue;
            }
            Key key = keyStore.getKey(alias, password);
            if (key instanceof PrivateKey privateKey && keyStore.getCertificate(alias) instanceof X509Certificate certificate) {
                return new CertificateData(privateKey, certificate);
            }
        }
        throw new IllegalArgumentException("No se encontro llave privada en el certificado configurado");
    }

    private Document parseXml(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private org.w3c.dom.Node findExtensionContent(Document document) {
        NodeList nodes = document.getElementsByTagNameNS(EXT_NS, "ExtensionContent");
        if (nodes.getLength() == 0) {
            throw new IllegalArgumentException("XML no contiene ext:ExtensionContent para insertar firma");
        }
        return nodes.item(0);
    }

    private KeyInfo buildKeyInfo(XMLSignatureFactory factory, X509Certificate certificate) {
        KeyInfoFactory keyInfoFactory = factory.getKeyInfoFactory();
        X509Data x509Data = keyInfoFactory.newX509Data(List.of(certificate));
        return keyInfoFactory.newKeyInfo(List.of(x509Data));
    }

    private String toXml(Document document) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.STANDALONE, "no");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(writer));
        return writer.toString();
    }

    private record CertificateData(PrivateKey privateKey, X509Certificate certificate) {
    }
}
