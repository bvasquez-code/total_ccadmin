package com.ccadmin.app.product.service;

import com.ccadmin.app.product.model.dto.ProductImageAnalysisDto;
import com.ccadmin.app.shared.model.entity.BusinessConfigEntity;
import com.ccadmin.app.shared.model.myconst.StatusConst;
import com.ccadmin.app.shared.service.BusinessConfigSearchService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ProductImageAnalysisService {
    private static final String CONFIG_GROUP = "UrlExtensionesWeb";
    private static final String CONFIG_CODE = "UrlGeminiView";
    private static final int MAX_IMAGE_BYTES = 5 * 1024 * 1024;
    private static final Pattern API_KEY_PARAMETER = Pattern.compile(
            "(?i)([?&]key=)[^&]*"
    );
    private static final String PROMPT = """
            Se adjuntan 3 fotos del mismo producto (Frontal, Lateral y Código de
            barras/Etiqueta). Analízalas para extraer la información comercial y
            técnica. De la foto del código de barras, lee con precisión la secuencia
            numérica de dígitos del código (EAN, UPC o similar) y colócala en el
            campo 'Barcode'. Si no hay precio explícito en las etiquetas, usa 0 en
            'NumPrice'. Si el código no es legible o no existe, coloca un string
            vacío. No inventes datos y devuelve exclusivamente el JSON solicitado.
            """;

    private final BusinessConfigSearchService businessConfigSearchService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public ProductImageAnalysisService(
            BusinessConfigSearchService businessConfigSearchService,
            ObjectMapper objectMapper
    ) {
        this(
                businessConfigSearchService,
                objectMapper,
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .build()
        );
    }

    ProductImageAnalysisService(
            BusinessConfigSearchService businessConfigSearchService,
            ObjectMapper objectMapper,
            HttpClient httpClient
    ) {
        this.businessConfigSearchService = businessConfigSearchService;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    public ProductImageAnalysisDto analyze(
            MultipartFile frontImage,
            MultipartFile sideImage,
            MultipartFile barcodeImage
    ) {
        ValidatedImage validatedFrontImage = validateImage(
                frontImage, "fotografia frontal"
        );
        ValidatedImage validatedSideImage = validateImage(
                sideImage, "fotografia lateral"
        );
        ValidatedImage validatedBarcodeImage = validateImage(
                barcodeImage, "fotografia del codigo de barras"
        );
        BusinessConfigEntity configuration = findConfiguration();
        URI endpoint = buildEndpoint(configuration);

        try {
            String requestBody = objectMapper.writeValueAsString(
                    buildGeminiRequest(
                            validatedFrontImage,
                            validatedSideImage,
                            validatedBarcodeImage
                    )
            );
            HttpRequest request = HttpRequest.newBuilder(endpoint)
                    .timeout(Duration.ofSeconds(45))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString()
            );
            validateHttpResponse(response);
            return parseResponse(response.body());
        } catch (HttpTimeoutException exception) {
            throw new IllegalStateException(
                    "Gemini excedio el tiempo de espera. Puede continuar manualmente."
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "El analisis de imagen fue interrumpido. Puede continuar manualmente."
            );
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Gemini no esta disponible. Puede continuar manualmente."
            );
        }
    }

    private ValidatedImage validateImage(MultipartFile image, String imageName) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Debe enviar la " + imageName);
        }
        if (image.getSize() > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException(
                    "La " + imageName + " no puede superar 5 MB"
            );
        }

        try {
            byte[] bytes = image.getBytes();
            String mimeType = detectMimeType(bytes);
            try (ByteArrayInputStream input = new ByteArrayInputStream(bytes)) {
                BufferedImage decodedImage = ImageIO.read(input);
                if (decodedImage == null
                        || decodedImage.getWidth() < 1
                        || decodedImage.getHeight() < 1) {
                    throw new IllegalArgumentException(
                            "El archivo enviado no es una imagen valida"
                    );
                }
            }
            return new ValidatedImage(bytes, mimeType);
        } catch (IOException exception) {
            throw new IllegalArgumentException("No fue posible leer la imagen");
        }
    }

    private String detectMimeType(byte[] bytes) {
        if (bytes.length >= 3
                && (bytes[0] & 0xff) == 0xff
                && (bytes[1] & 0xff) == 0xd8
                && (bytes[2] & 0xff) == 0xff) {
            return "image/jpeg";
        }
        if (bytes.length >= 8
                && (bytes[0] & 0xff) == 0x89
                && bytes[1] == 0x50
                && bytes[2] == 0x4e
                && bytes[3] == 0x47) {
            return "image/png";
        }
        throw new IllegalArgumentException(
                "Solo se aceptan imagenes JPEG o PNG"
        );
    }

    private BusinessConfigEntity findConfiguration() {
        BusinessConfigEntity configuration = businessConfigSearchService
                .findByConfigCod(CONFIG_GROUP, CONFIG_CODE);
        if (configuration == null
                || !StatusConst.ACTIVE.equals(configuration.Status)) {
            throw new IllegalStateException(
                    "El analisis con Gemini no esta configurado o esta inactivo"
            );
        }
        return configuration;
    }

    private URI buildEndpoint(BusinessConfigEntity configuration) {
        String url = clean(configuration.ConfigVal);
        String apiKey = clean(configuration.Str4Config);
        if (url.isEmpty() || apiKey.isEmpty()) {
            throw new IllegalStateException(
                    "La URL o API Key de Gemini no esta configurada"
            );
        }

        String encodedApiKey = URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
        String endpoint;
        if (url.contains("{apiKey}")) {
            endpoint = url.replace("{apiKey}", encodedApiKey);
        } else {
            Matcher matcher = API_KEY_PARAMETER.matcher(url);
            if (matcher.find()) {
                endpoint = matcher.replaceFirst(
                        Matcher.quoteReplacement(matcher.group(1) + encodedApiKey)
                );
            } else {
                endpoint = url + (url.contains("?") ? "&" : "?")
                        + "key=" + encodedApiKey;
            }
        }

        try {
            return URI.create(endpoint);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("La URL de Gemini no es valida");
        }
    }

    private Map<String, Object> buildGeminiRequest(
            ValidatedImage frontImage,
            ValidatedImage sideImage,
            ValidatedImage barcodeImage
    ) {
        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(Map.of("text", PROMPT));
        parts.add(imagePart(frontImage));
        parts.add(imagePart(sideImage));
        parts.add(imagePart(barcodeImage));

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("BrandCod", stringProperty(
                "Código o nombre estandarizado de la marca "
                        + "(ej: HONDA, PHILIPS, GENERICO)."
        ));
        properties.put("NumPrice", Map.of(
                "type", "NUMBER",
                "description", "Precio visible; 0 si no se observa"
        ));
        properties.put("BrandInput", stringProperty(
                "Nombre de la marca tal como se lee en el empaque o 'GENERICO'."
        ));
        properties.put("CategoryCod", stringProperty(
                "Categoría general (ej: MOTOS, ACCESORIOS, REPUESTOS, FERRETERIA)."
        ));
        properties.put("ProductDesc", stringProperty(
                "Descripción técnica detallada incluyendo especificaciones "
                        + "leídas en la foto lateral/frontal."
        ));
        properties.put("ProductNameList", Map.of(
                "type", "ARRAY",
                "items", Map.of("type", "STRING"),
                "description", "3 variantes de nombres comerciales probables"
        ));
        properties.put("CategoryInput", stringProperty(
                "Nombre legible de la categoría sugerida."
        ));
        properties.put("Barcode", stringProperty(
                "Número exacto del código de barras leído en la foto "
                        + "(dígitos EAN-13, EAN-8, UPC, etc.). Si no es legible "
                        + "o no existe, colocar un string vacío ''."
        ));

        Map<String, Object> responseSchema = new LinkedHashMap<>();
        responseSchema.put("type", "OBJECT");
        responseSchema.put("properties", properties);
        responseSchema.put("required", List.of(
                "BrandCod", "NumPrice", "BrandInput", "CategoryCod",
                "ProductDesc", "ProductNameList", "CategoryInput", "Barcode"
        ));

        return Map.of(
                "contents", List.of(Map.of("parts", parts)),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "responseSchema", responseSchema
                )
        );
    }

    private Map<String, Object> imagePart(ValidatedImage image) {
        Map<String, Object> inlineData = new LinkedHashMap<>();
        inlineData.put("mimeType", image.mimeType());
        inlineData.put("data", Base64.getEncoder().encodeToString(image.bytes()));
        return Map.of("inlineData", inlineData);
    }

    private Map<String, Object> stringProperty(String description) {
        return Map.of("type", "STRING", "description", description);
    }

    private void validateHttpResponse(HttpResponse<String> response) {
        if (response.statusCode() == 429) {
            throw new IllegalStateException(
                    "Gemini alcanzo su cuota. Puede continuar manualmente."
            );
        }
        if (response.statusCode() == 408 || response.statusCode() == 504) {
            throw new IllegalStateException(
                    "Gemini excedio el tiempo de espera. Puede continuar manualmente."
            );
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException(
                    "Gemini no pudo analizar la imagen. Puede continuar manualmente."
            );
        }
    }

    private ProductImageAnalysisDto parseResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode textNode = root.path("candidates")
                    .path(0)
                    .path("content")
                    .path("parts")
                    .path(0)
                    .path("text");
            if (!textNode.isTextual() || clean(textNode.asText()).isEmpty()) {
                throw new IllegalStateException(
                        "Gemini devolvio una respuesta sin datos utilizables"
                );
            }
            ProductImageAnalysisDto result = objectMapper.readValue(
                    textNode.asText(), ProductImageAnalysisDto.class
            );
            normalize(result);
            return result;
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Gemini devolvio una respuesta invalida. Puede continuar manualmente."
            );
        }
    }

    private void normalize(ProductImageAnalysisDto result) {
        if (result == null) {
            throw new IllegalStateException(
                    "Gemini devolvio una respuesta sin datos utilizables"
            );
        }
        result.BrandCod = limit(result.BrandCod, 128);
        result.BrandInput = limit(result.BrandInput, 128);
        result.CategoryCod = limit(result.CategoryCod, 128);
        result.CategoryInput = limit(result.CategoryInput, 128);
        result.ProductDesc = limit(result.ProductDesc, 256);
        result.Barcode = clean(result.Barcode)
                .replaceAll("[^0-9]", "");
        result.Barcode = limit(result.Barcode, 20);
        if (result.NumPrice == null || result.NumPrice.signum() < 0) {
            result.NumPrice = java.math.BigDecimal.ZERO;
        }
        List<String> names = result.ProductNameList == null
                ? List.of() : result.ProductNameList;
        result.ProductNameList = names.stream()
                .map(name -> limit(name, 128))
                .filter(name -> !name.isEmpty())
                .distinct()
                .limit(3)
                .toList();
    }

    private String limit(String value, int maxLength) {
        String cleanValue = clean(value);
        return cleanValue.length() <= maxLength
                ? cleanValue : cleanValue.substring(0, maxLength);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private record ValidatedImage(byte[] bytes, String mimeType) {
    }
}
