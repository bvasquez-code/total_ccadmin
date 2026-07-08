package com.ccadmin.app.sale.service;

import com.ccadmin.app.product.model.entity.ProductTaxConfigEntity;
import com.ccadmin.app.product.repository.ProductTaxConfigRepository;
import com.ccadmin.app.sale.exception.SaleBuildException;
import com.ccadmin.app.sale.model.entity.PresaleDetEntity;
import com.ccadmin.app.sale.model.entity.SaleDetEntity;
import com.ccadmin.app.sale.model.entity.SaleDetTaxEntity;
import com.ccadmin.app.sale.model.entity.TaxAffectationEntity;
import com.ccadmin.app.sale.model.entity.TaxEntity;
import com.ccadmin.app.sale.repository.TaxAffectationRepository;
import com.ccadmin.app.sale.repository.TaxRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class SaleTaxCalculationService {

    private static final BigDecimal STANDARD_IGV_RATE = new BigDecimal("18.0000");

    @Autowired
    private ProductTaxConfigRepository productTaxConfigRepository;
    @Autowired
    private TaxRepository taxRepository;
    @Autowired
    private TaxAffectationRepository taxAffectationRepository;

    public SaleTaxCalculationResult buildSaleDetails(
            List<PresaleDetEntity> presaleDetailList,
            String saleCod,
            String storeCod,
            String userCod
    ) {
        SaleTaxCalculationResult result = new SaleTaxCalculationResult();

        for (PresaleDetEntity presaleDet : presaleDetailList) {
            SaleLineTaxCalculation lineCalculation = calculateLine(presaleDet, saleCod, storeCod, userCod);
            result.DetailList.add(lineCalculation.SaleDet);
            result.TaxDetailList.addAll(lineCalculation.TaxDetailList);
        }

        result.recalculateTotals();
        return result;
    }

    private SaleLineTaxCalculation calculateLine(
            PresaleDetEntity presaleDet,
            String saleCod,
            String storeCod,
            String userCod
    ) {
        List<ProductTaxConfigEntity> configList = this.productTaxConfigRepository
                .findActiveByProductAndStore(presaleDet.ProductCod, storeCod);
        validateConfigList(configList, presaleDet.ProductCod, storeCod);

        ProductTaxConfigEntity mainConfig = configList.stream()
                .filter(config -> "S".equals(config.IsMainTax))
                .findFirst()
                .orElseThrow(() -> new SaleBuildException("Producto/local no tiene configuracion tributaria principal"));
        TaxAffectationEntity mainAffectation = this.taxAffectationRepository
                .findActiveByCodeAndTax(mainConfig.TaxAffectationCod, mainConfig.TaxCod);
        if (mainAffectation == null) {
            throw new SaleBuildException("Combinacion tributaria principal no permitida para producto " + presaleDet.ProductCod);
        }

        List<ProductTaxConfigEntity> sortedConfigList = configList.stream()
                .sorted(Comparator
                        .comparingInt((ProductTaxConfigEntity config) -> config.CalculationOrder)
                        .thenComparing(config -> config.TaxCod))
                .toList();

        BigDecimal total = amount(presaleDet.NumTotalPrice);
        BigDecimal fixedTaxTotal = sortedConfigList.stream()
                .filter(this::isRealFixedTax)
                .map(config -> fixedTaxAmount(config, presaleDet.NumUnit))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        if (fixedTaxTotal.compareTo(total) > 0) {
            throw new SaleBuildException("Impuesto fijo supera total del producto " + presaleDet.ProductCod);
        }

        BigDecimal percentInclusiveTotal = total.subtract(fixedTaxTotal).setScale(2, RoundingMode.HALF_UP);
        BigDecimal additionalPercentRate = sortedConfigList.stream()
                .filter(config -> !"S".equals(config.IsMainTax))
                .filter(this::isRealPercentTax)
                .map(config -> rate(effectiveTaxRateValue(config)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal mainRate = isTaxedMainPercent(mainConfig, mainAffectation) ? rate(effectiveTaxRateValue(mainConfig)) : BigDecimal.ZERO;
        BigDecimal divisor = BigDecimal.ONE.add(additionalPercentRate);
        if (mainRate.compareTo(BigDecimal.ZERO) > 0) {
            divisor = divisor.multiply(BigDecimal.ONE.add(mainRate));
        }

        BigDecimal baseAmount = divisor.compareTo(BigDecimal.ZERO) == 0
                ? percentInclusiveTotal
                : percentInclusiveTotal.divide(divisor, 2, RoundingMode.HALF_UP);

        List<SaleDetTaxEntity> taxLineList = new ArrayList<>();
        BigDecimal additionalPercentTaxAmount = BigDecimal.ZERO;

        for (ProductTaxConfigEntity config : sortedConfigList) {
            if ("S".equals(config.IsMainTax) || !isRealPercentTax(config)) {
                continue;
            }
            TaxEntity tax = findTax(config.TaxCod);
            BigDecimal taxAmount = baseAmount.multiply(rate(effectiveTaxRateValue(config))).setScale(2, RoundingMode.HALF_UP);
            additionalPercentTaxAmount = additionalPercentTaxAmount.add(taxAmount).setScale(2, RoundingMode.HALF_UP);
            taxLineList.add(buildTaxLine(presaleDet, saleCod, config, tax, null, baseAmount, BigDecimal.valueOf(presaleDet.NumUnit), taxAmount, userCod));
        }

        BigDecimal mainTaxAmount = BigDecimal.ZERO;
        if (mainRate.compareTo(BigDecimal.ZERO) > 0) {
            mainTaxAmount = percentInclusiveTotal
                    .subtract(baseAmount)
                    .subtract(additionalPercentTaxAmount)
                    .setScale(2, RoundingMode.HALF_UP);
        }

        TaxEntity mainTax = findTax(mainConfig.TaxCod);
        BigDecimal mainBaseAmount = mainTaxAmount.compareTo(BigDecimal.ZERO) > 0
                ? baseAmount.add(additionalPercentTaxAmount).setScale(2, RoundingMode.HALF_UP)
                : baseAmount;
        taxLineList.add(buildTaxLine(
                presaleDet,
                saleCod,
                mainConfig,
                mainTax,
                mainAffectation,
                mainBaseAmount,
                BigDecimal.valueOf(presaleDet.NumUnit),
                mainTaxAmount,
                userCod
        ));

        for (ProductTaxConfigEntity config : sortedConfigList) {
            if (!isRealFixedTax(config)) {
                continue;
            }
            TaxEntity tax = findTax(config.TaxCod);
            BigDecimal taxAmount = fixedTaxAmount(config, presaleDet.NumUnit);
            taxLineList.add(buildTaxLine(
                    presaleDet,
                    saleCod,
                    config,
                    tax,
                    null,
                    BigDecimal.ZERO,
                    BigDecimal.valueOf(presaleDet.NumUnit),
                    taxAmount,
                    userCod
            ));
        }

        taxLineList.sort(Comparator
                .comparingInt((SaleDetTaxEntity line) -> line.CalculationOrder)
                .thenComparing(line -> line.TaxCod));
        for (int i = 0; i < taxLineList.size(); i++) {
            taxLineList.get(i).TaxLineNumber = i + 1;
            taxLineList.get(i).validate();
        }

        BigDecimal totalTax = taxLineList.stream()
                .map(line -> amount(line.TaxAmount))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal detailSubTotal = total.subtract(totalTax).setScale(2, RoundingMode.HALF_UP);

        SaleDetEntity saleDet = new SaleDetEntity()
                .build(presaleDet, saleCod)
                .tax(detailSubTotal, totalTax);
        saleDet.IsAppliedTax = totalTax.compareTo(BigDecimal.ZERO) > 0 ? "S" : "N";
        saleDet.session(userCod).validate();

        SaleLineTaxCalculation lineCalculation = new SaleLineTaxCalculation();
        lineCalculation.SaleDet = saleDet;
        lineCalculation.TaxDetailList = taxLineList;
        return lineCalculation;
    }

    private void validateConfigList(List<ProductTaxConfigEntity> configList, String productCod, String storeCod) {
        if (configList == null || configList.isEmpty()) {
            throw new SaleBuildException("Producto " + productCod + " no tiene configuracion tributaria en tienda " + storeCod);
        }
        long mainCount = configList.stream().filter(config -> "S".equals(config.IsMainTax)).count();
        if (mainCount != 1) {
            throw new SaleBuildException("Producto " + productCod + " debe tener una sola configuracion tributaria principal");
        }
        Set<String> taxCodSet = new HashSet<>();
        for (ProductTaxConfigEntity config : configList) {
            config.validate();
            if (!taxCodSet.add(config.TaxCod)) {
                throw new SaleBuildException("Producto " + productCod + " tiene tributo duplicado activo");
            }
            if (!"S".equals(config.IsMainTax) && this.taxAffectationRepository.countActiveByTaxCod(config.TaxCod) > 0) {
                throw new SaleBuildException("Producto " + productCod + " tiene un tributo de afectacion configurado como adicional");
            }
        }
        ProductTaxConfigEntity mainConfig = configList.stream()
                .filter(config -> "S".equals(config.IsMainTax))
                .findFirst()
                .orElseThrow();
        if (!"10".equals(mainConfig.TaxAffectationCod)
                && configList.stream().anyMatch(config -> "1000".equals(config.TaxCod) && !"S".equals(config.IsMainTax))) {
            throw new SaleBuildException("Producto " + productCod + " no puede calcular IGV real con afectacion no gravada");
        }
    }

    private SaleDetTaxEntity buildTaxLine(
            PresaleDetEntity presaleDet,
            String saleCod,
            ProductTaxConfigEntity config,
            TaxEntity tax,
            TaxAffectationEntity affectation,
            BigDecimal taxBaseAmount,
            BigDecimal taxQuantity,
            BigDecimal taxAmount,
            String userCod
    ) {
        SaleDetTaxEntity line = new SaleDetTaxEntity();
        line.SaleCod = saleCod;
        line.ItemNumber = presaleDet.ItemNumber;
        line.TaxCod = config.TaxCod;
        line.SunatTaxCod = tax.SunatTaxCod;
        line.TaxName = tax.Name;
        line.TaxAffectationCod = affectation == null ? null : affectation.TaxAffectationCod;
        line.TaxAffectationName = affectation == null ? null : affectation.Name;
        line.TaxCalculationType = config.TaxCalculationType;
        line.IsInformative = config.IsInformative;
        line.TaxRateValue = effectiveTaxRateValue(config).setScale(4, RoundingMode.HALF_UP);
        line.FixedUnitAmount = valueOrZero(config.FixedUnitAmount).setScale(4, RoundingMode.HALF_UP);
        line.TaxBaseAmount = amount(taxBaseAmount);
        line.TaxQuantity = valueOrZero(taxQuantity).setScale(4, RoundingMode.HALF_UP);
        line.TaxAmount = amount(taxAmount);
        line.CalculationOrder = config.CalculationOrder;
        line.session(userCod);
        return line;
    }

    private TaxEntity findTax(String taxCod) {
        return this.taxRepository.findById(taxCod)
                .orElseThrow(() -> new SaleBuildException("Tributo no existe: " + taxCod));
    }

    private boolean isTaxedMainPercent(ProductTaxConfigEntity config, TaxAffectationEntity affectation) {
        return "S".equals(affectation.IsTaxed) && isRealPercentTax(config);
    }

    private boolean isRealPercentTax(ProductTaxConfigEntity config) {
        return "P".equals(config.TaxCalculationType) && !"S".equals(config.IsInformative);
    }

    private boolean isRealFixedTax(ProductTaxConfigEntity config) {
        return "F".equals(config.TaxCalculationType) && !"S".equals(config.IsInformative);
    }

    private BigDecimal fixedTaxAmount(ProductTaxConfigEntity config, int numUnit) {
        return valueOrZero(config.FixedUnitAmount)
                .multiply(BigDecimal.valueOf(numUnit))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal effectiveTaxRateValue(ProductTaxConfigEntity config) {
        return "1000".equals(config.TaxCod) ? STANDARD_IGV_RATE : valueOrZero(config.TaxRateValue);
    }

    private BigDecimal rate(BigDecimal value) {
        return valueOrZero(value).divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);
    }

    private BigDecimal amount(BigDecimal value) {
        return valueOrZero(value).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal valueOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static class SaleLineTaxCalculation {
        SaleDetEntity SaleDet;
        List<SaleDetTaxEntity> TaxDetailList = new ArrayList<>();
    }

    public static class SaleTaxCalculationResult {
        public List<SaleDetEntity> DetailList = new ArrayList<>();
        public List<SaleDetTaxEntity> TaxDetailList = new ArrayList<>();
        public BigDecimal NumTotalPriceNoTax = BigDecimal.ZERO;
        public BigDecimal NumTotalTax = BigDecimal.ZERO;
        public BigDecimal NumTotalPrice = BigDecimal.ZERO;

        void recalculateTotals() {
            NumTotalPriceNoTax = DetailList.stream()
                    .map(detail -> detail.NumPriceSubTotal == null ? BigDecimal.ZERO : detail.NumPriceSubTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);
            NumTotalTax = DetailList.stream()
                    .map(detail -> detail.NumTotalTax == null ? BigDecimal.ZERO : detail.NumTotalTax)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);
            NumTotalPrice = DetailList.stream()
                    .map(detail -> detail.NumTotalPrice == null ? BigDecimal.ZERO : detail.NumTotalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);
        }
    }
}
