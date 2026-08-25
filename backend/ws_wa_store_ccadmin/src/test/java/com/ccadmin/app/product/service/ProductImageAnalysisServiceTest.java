package com.ccadmin.app.product.service;

import com.ccadmin.app.product.model.dto.ProductImageAnalysisDto;
import com.ccadmin.app.shared.model.entity.BusinessConfigEntity;
import com.ccadmin.app.shared.service.BusinessConfigSearchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductImageAnalysisServiceTest {

    @Mock private BusinessConfigSearchService businessConfigSearchService;
    @Mock private HttpClient httpClient;
    @Mock private HttpResponse<String> httpResponse;

    private ProductImageAnalysisService productImageAnalysisService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        productImageAnalysisService = new ProductImageAnalysisService(
                businessConfigSearchService,
                objectMapper,
                httpClient
        );
    }

    @Test
    void analyzesImageUsingDatabaseConfigurationAndNormalizesResponse() throws Exception {
        BusinessConfigEntity configuration = activeConfiguration();
        when(businessConfigSearchService.findByConfigCod(
                "UrlExtensionesWeb", "UrlGeminiView"
        )).thenReturn(configuration);
        when(httpResponse.statusCode()).thenReturn(200);
        String analyzedProduct = objectMapper.writeValueAsString(Map.of(
                "BrandCod", " ACME ",
                "NumPrice", 12.50,
                "BrandInput", "Acme",
                "CategoryCod", "FILTROS",
                "ProductDesc", "Filtro de aceite",
                "ProductNameList", List.of(
                        "Filtro Acme", "Filtro Acme", "Filtro de aceite Acme"
                ),
                "CategoryInput", "Filtros",
                "Barcode", " 775-1234 567890 "
        ));
        when(httpResponse.body()).thenReturn(objectMapper.writeValueAsString(Map.of(
                "candidates", List.of(Map.of(
                        "content", Map.of(
                                "parts", List.of(Map.of("text", analyzedProduct))
                        )
                ))
        )));
        when(httpClient.send(
                any(HttpRequest.class),
                org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()
        )).thenReturn(httpResponse);

        ProductImageAnalysisDto result = productImageAnalysisService.analyze(
                validImage("frontImage"),
                validImage("sideImage"),
                validImage("barcodeImage")
        );

        assertEquals("ACME", result.BrandCod);
        assertEquals("Filtro de aceite", result.ProductDesc);
        assertEquals(2, result.ProductNameList.size());
        assertEquals("7751234567890", result.Barcode);
        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(
                requestCaptor.capture(),
                org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()
        );
        assertEquals("test-key", requestCaptor.getValue().uri().getQuery().split("key=")[1]);
        assertTrue(requestCaptor.getValue().headers()
                .firstValue("Content-Type").orElse("")
                .contains("application/json"));
    }

    @Test
    void rejectsNonImageBeforeReadingGeminiConfiguration() throws Exception {
        MockMultipartFile invalidImage = new MockMultipartFile(
                "image", "product.txt", "text/plain", "not-an-image".getBytes()
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> productImageAnalysisService.analyze(
                        invalidImage,
                        validImage("sideImage"),
                        validImage("barcodeImage")
                )
        );

        assertEquals("Solo se aceptan imagenes JPEG o PNG", exception.getMessage());
        verify(businessConfigSearchService, never()).findByConfigCod(any(), any());
        verify(httpClient, never()).send(
                any(HttpRequest.class),
                org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()
        );
    }

    @Test
    void reportsGeminiQuotaWithoutExposingProviderResponse() throws Exception {
        when(businessConfigSearchService.findByConfigCod(
                "UrlExtensionesWeb", "UrlGeminiView"
        )).thenReturn(activeConfiguration());
        when(httpResponse.statusCode()).thenReturn(429);
        when(httpClient.send(
                any(HttpRequest.class),
                org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()
        )).thenReturn(httpResponse);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> productImageAnalysisService.analyze(
                        validImage("frontImage"),
                        validImage("sideImage"),
                        validImage("barcodeImage")
                )
        );

        assertEquals(
                "Gemini alcanzo su cuota. Puede continuar manualmente.",
                exception.getMessage()
        );
    }

    private BusinessConfigEntity activeConfiguration() {
        BusinessConfigEntity configuration = new BusinessConfigEntity();
        configuration.Status = "A";
        configuration.ConfigVal = "https://example.test/generateContent";
        configuration.Str4Config = "test-key";
        return configuration;
    }

    private MockMultipartFile validImage(String fieldName) throws Exception {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return new MockMultipartFile(
                fieldName, "product.png", "image/png", output.toByteArray()
        );
    }
}
