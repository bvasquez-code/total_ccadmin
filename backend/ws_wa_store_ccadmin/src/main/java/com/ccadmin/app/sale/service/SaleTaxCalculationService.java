package com.ccadmin.app.sale.service;

import com.ccadmin.app.product.model.entity.ProductTaxConfigEntity;
import com.ccadmin.app.product.repository.ProductTaxConfigRepository;
import com.ccadmin.app.sale.exception.SaleBuildException;
import com.ccadmin.app.sale.model.constants.SaleTaxConstants;
import com.ccadmin.app.sale.model.dto.CreditNoteTaxCalculationResultDto;
import com.ccadmin.app.sale.model.dto.SaleDetailSplitLineDto;
import com.ccadmin.app.sale.model.dto.SaleTaxCalculationResultDto;
import com.ccadmin.app.sale.model.entity.CreditNoteDetEntity;
import com.ccadmin.app.sale.model.entity.CreditNoteDetTaxEntity;
import com.ccadmin.app.sale.model.entity.PresaleDetEntity;
import com.ccadmin.app.sale.model.entity.SaleDetEntity;
import com.ccadmin.app.sale.model.entity.SaleDetTaxEntity;
import com.ccadmin.app.sale.model.entity.TaxAffectationEntity;
import com.ccadmin.app.sale.model.entity.TaxEntity;
import com.ccadmin.app.sale.model.factory.CreditNoteDetTaxEntityFactory;
import com.ccadmin.app.sale.model.factory.CreditNoteTaxCalculationResultDtoFactory;
import com.ccadmin.app.sale.model.factory.SaleDetEntityFactory;
import com.ccadmin.app.sale.model.factory.SaleDetTaxEntityFactory;
import com.ccadmin.app.sale.model.factory.SaleTaxCalculationResultDtoFactory;
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

    @Autowired
    private ProductTaxConfigRepository productTaxConfigRepository;
    @Autowired
    private TaxRepository taxRepository;
    @Autowired
    private TaxAffectationRepository taxAffectationRepository;

    public SaleTaxCalculationResultDto buildSaleDetails(
            List<PresaleDetEntity> presaleDetailList,
            String saleCod,
            String storeCod,
            String userCod
    ) {
        List<SaleDetEntity> saleDetailList = new ArrayList<>();
        List<SaleDetTaxEntity> saleDetailTaxList = new ArrayList<>();

        for (PresaleDetEntity presaleDet : presaleDetailList) {
            List<ProductTaxConfigEntity> configList = this.findProductTaxConfigList(presaleDet, storeCod);
            ProductTaxConfigEntity mainConfig = this.findMainTaxConfig(configList);
            TaxAffectationEntity mainAffectation = this.findMainTaxAffectation(mainConfig, presaleDet.ProductCod);
            List<ProductTaxConfigEntity> sortedConfigList = this.sortTaxConfigList(configList);
            List<SaleDetTaxEntity> taxDetailList = this.createSaleDetTaxEntities(
                    presaleDet,
                    saleCod,
                    sortedConfigList,
                    mainConfig,
                    mainAffectation,
                    userCod
            );
            SaleDetEntity saleDet = this.createSaleDetEntity(presaleDet, saleCod, taxDetailList, userCod);

            saleDetailList.add(saleDet);
            saleDetailTaxList.addAll(taxDetailList);
        }

        return SaleTaxCalculationResultDtoFactory.fromLines(
                saleDetailList,
                saleDetailTaxList
        );
    }

    public SaleTaxCalculationResultDto splitExistingSaleDetail(
            SaleDetEntity originDetail,
            List<SaleDetTaxEntity> originTaxList,
            List<SaleDetailSplitLineDto> splitLineList,
            String userCod
    ) {
        this.validateSplit(originDetail, splitLineList);

        int originQuantity = originDetail.NumUnit;
        List<BigDecimal> totalPriceList = this.distributeExact(
                originDetail.NumTotalPrice, splitLineList, originQuantity, 2
        );
        List<BigDecimal> subtotalList = this.distributeExact(
                originDetail.NumPriceSubTotal, splitLineList, originQuantity, 2
        );
        List<BigDecimal> totalTaxList = this.distributeExact(
                originDetail.NumTotalTax, splitLineList, originQuantity, 2
        );

        List<List<SaleDetTaxEntity>> taxListBySplit = new ArrayList<>();
        for (int index = 0; index < splitLineList.size(); index++) {
            taxListBySplit.add(new ArrayList<>());
        }

        List<SaleDetTaxEntity> safeOriginTaxList = originTaxList == null ? List.of() : originTaxList;
        for (SaleDetTaxEntity originTax : safeOriginTaxList) {
            List<BigDecimal> baseAmountList = this.distributeExact(
                    originTax.TaxBaseAmount, splitLineList, originQuantity, 2
            );
            List<BigDecimal> taxQuantityList = this.distributeExact(
                    originTax.TaxQuantity, splitLineList, originQuantity, 4
            );
            List<BigDecimal> taxAmountList = this.distributeExact(
                    originTax.TaxAmount, splitLineList, originQuantity, 2
            );
            for (int index = 0; index < splitLineList.size(); index++) {
                SaleDetailSplitLineDto splitLine = splitLineList.get(index);
                SaleDetTaxEntity splitTax = SaleDetTaxEntityFactory.fromSplit(
                        originTax,
                        splitLine.ItemNumber,
                        baseAmountList.get(index),
                        taxQuantityList.get(index),
                        taxAmountList.get(index)
                );
                splitTax.session(userCod).validate();
                taxListBySplit.get(index).add(splitTax);
            }
        }

        List<SaleDetEntity> splitDetailList = new ArrayList<>();
        List<SaleDetTaxEntity> splitTaxList = new ArrayList<>();
        for (int index = 0; index < splitLineList.size(); index++) {
            SaleDetailSplitLineDto splitLine = splitLineList.get(index);
            SaleDetEntity splitDetail = SaleDetEntityFactory.fromSplit(
                    originDetail,
                    splitLine,
                    totalPriceList.get(index),
                    subtotalList.get(index),
                    totalTaxList.get(index)
            );
            splitDetail.session(userCod).validate();
            splitDetailList.add(splitDetail);
            splitTaxList.addAll(taxListBySplit.get(index));
        }
        return SaleTaxCalculationResultDtoFactory.fromLines(
                splitDetailList,
                splitTaxList
        );
    }

    public SaleTaxCalculationResultDto renumberExistingSaleDetail(
            SaleDetEntity originDetail,
            List<SaleDetTaxEntity> originTaxList,
            int itemNumber,
            String userCod
    ) {
        if (originDetail == null || itemNumber <= 0) {
            throw new SaleBuildException("El detalle de venta que se conservara no es valido");
        }

        SaleDetEntity detail = SaleDetEntityFactory.copyForItem(originDetail, itemNumber);
        detail.session(userCod).validate();

        List<SaleDetTaxEntity> taxList = (originTaxList == null ? List.<SaleDetTaxEntity>of() : originTaxList)
                .stream()
                .map(originTax -> {
                    SaleDetTaxEntity tax = SaleDetTaxEntityFactory.copyForItem(
                            originTax,
                            itemNumber
                    );
                    return tax.session(userCod).validate();
                })
                .toList();

        return SaleTaxCalculationResultDtoFactory.fromLines(
                List.of(detail),
                taxList
        );
    }

    public CreditNoteTaxCalculationResultDto buildCreditNoteTaxResult(
            String creditNoteCod,
            CreditNoteDetEntity creditNoteDetail,
            SaleDetEntity originDetail,
            List<SaleDetTaxEntity> originTaxList,
            String userCod
    ) {
        List<CreditNoteDetTaxEntity> taxDetailList = this.buildCreditNoteTaxLines(
                creditNoteCod, creditNoteDetail, originDetail, originTaxList, userCod
        );
        BigDecimal totalTax = taxDetailList.isEmpty()
                ? this.prorateAmount(originDetail.NumTotalTax, creditNoteDetail.NumUnit, originDetail.NumUnit)
                : taxDetailList.stream()
                        .map(tax -> amount(tax.TaxAmount))
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .setScale(2, RoundingMode.HALF_UP);
        BigDecimal priceSubtotal = amount(creditNoteDetail.NumTotalPrice)
                .subtract(totalTax)
                .setScale(2, RoundingMode.HALF_UP);
        String isAppliedTax = totalTax.compareTo(BigDecimal.ZERO) > 0
                ? SaleTaxConstants.YES
                : SaleTaxConstants.NO;
        return CreditNoteTaxCalculationResultDtoFactory.fromCalculation(
                taxDetailList,
                totalTax,
                priceSubtotal,
                isAppliedTax
        );
    }

    private List<CreditNoteDetTaxEntity> buildCreditNoteTaxLines(
            String creditNoteCod,
            CreditNoteDetEntity creditNoteDetail,
            SaleDetEntity originDetail,
            List<SaleDetTaxEntity> originTaxList,
            String userCod
    ) {
        if (originTaxList == null || originTaxList.isEmpty()) {
            return List.of();
        }
        List<CreditNoteDetTaxEntity> taxList = originTaxList.stream()
                .map(originTax -> this.buildCreditNoteTaxLine(
                        creditNoteCod, creditNoteDetail, originDetail, originTax
                ))
                .sorted(Comparator
                        .comparingInt((CreditNoteDetTaxEntity tax) -> tax.CalculationOrder)
                        .thenComparing(tax -> tax.TaxCod))
                .toList();
        for (int index = 0; index < taxList.size(); index++) {
            taxList.get(index).TaxLineNumber = index + 1;
            taxList.get(index).session(userCod).validate();
        }
        return taxList;
    }

    private BigDecimal prorateAmount(BigDecimal value, Integer units, Integer originUnits) {
        return amount(value)
                .multiply(this.ratio(units, originUnits))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal prorateQuantity(BigDecimal value, Integer units, Integer originUnits) {
        return valueOrZero(value)
                .multiply(this.ratio(units, originUnits))
                .setScale(4, RoundingMode.HALF_UP);
    }

    private void validateSplit(
            SaleDetEntity originDetail,
            List<SaleDetailSplitLineDto> splitLineList
    ) {
        if (originDetail == null || originDetail.NumUnit <= 0) {
            throw new SaleBuildException("El detalle de venta de origen no es valido");
        }
        if (splitLineList == null || splitLineList.isEmpty()) {
            throw new SaleBuildException("Debe existir al menos una linea para dividir el detalle de venta");
        }
        int splitQuantity = 0;
        Set<Integer> itemNumberSet = new HashSet<>();
        for (SaleDetailSplitLineDto splitLine : splitLineList) {
            if (splitLine == null || splitLine.ItemNumber <= 0 || splitLine.NumUnit <= 0) {
                throw new SaleBuildException("Las lineas divididas deben tener item y cantidad validos");
            }
            if (!itemNumberSet.add(splitLine.ItemNumber)) {
                throw new SaleBuildException("No se puede repetir el item de una linea dividida");
            }
            try {
                splitQuantity = Math.addExact(splitQuantity, splitLine.NumUnit);
            } catch (ArithmeticException ex) {
                throw new SaleBuildException("La cantidad dividida excede el limite permitido");
            }
        }
        if (splitQuantity != originDetail.NumUnit) {
            throw new SaleBuildException("La cantidad dividida no coincide con el detalle de venta original");
        }
    }

    private List<BigDecimal> distributeExact(
            BigDecimal originalValue,
            List<SaleDetailSplitLineDto> splitLineList,
            int originQuantity,
            int scale
    ) {
        BigDecimal totalValue = valueOrZero(originalValue).setScale(scale, RoundingMode.HALF_UP);
        BigDecimal assignedValue = BigDecimal.ZERO.setScale(scale, RoundingMode.HALF_UP);
        List<BigDecimal> result = new ArrayList<>();
        for (int index = 0; index < splitLineList.size(); index++) {
            BigDecimal splitValue;
            if (index == splitLineList.size() - 1) {
                splitValue = totalValue.subtract(assignedValue).setScale(scale, RoundingMode.HALF_UP);
            } else {
                splitValue = totalValue
                        .multiply(BigDecimal.valueOf(splitLineList.get(index).NumUnit))
                        .divide(BigDecimal.valueOf(originQuantity), scale, RoundingMode.HALF_UP);
                assignedValue = assignedValue.add(splitValue).setScale(scale, RoundingMode.HALF_UP);
            }
            result.add(splitValue);
        }
        return result;
    }

    private BigDecimal ratio(Integer units, Integer originUnits) {
        if (units == null || originUnits == null || originUnits <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(units)
                .divide(BigDecimal.valueOf(originUnits), 8, RoundingMode.HALF_UP);
    }

    private CreditNoteDetTaxEntity buildCreditNoteTaxLine(
            String creditNoteCod,
            CreditNoteDetEntity creditNoteDetail,
            SaleDetEntity originDetail,
            SaleDetTaxEntity originTax
    ) {
        return CreditNoteDetTaxEntityFactory.fromSaleTax(
                creditNoteCod,
                creditNoteDetail,
                originTax,
                this.prorateAmount(
                        originTax.TaxBaseAmount,
                        creditNoteDetail.NumUnit,
                        originDetail.NumUnit
                ),
                this.prorateQuantity(
                        originTax.TaxQuantity,
                        creditNoteDetail.NumUnit,
                        originDetail.NumUnit
                ),
                this.prorateAmount(
                        originTax.TaxAmount,
                        creditNoteDetail.NumUnit,
                        originDetail.NumUnit
                )
        );
    }

    private List<ProductTaxConfigEntity> findProductTaxConfigList(PresaleDetEntity presaleDet, String storeCod) {
        List<ProductTaxConfigEntity> configList = this.productTaxConfigRepository
                .findActiveByProductAndStore(presaleDet.ProductCod, storeCod);
        validateConfigList(configList, presaleDet.ProductCod, storeCod);
        return configList;
    }

    private ProductTaxConfigEntity findMainTaxConfig(List<ProductTaxConfigEntity> configList) {
        return configList.stream()
                .filter(config -> SaleTaxConstants.YES.equals(config.IsMainTax))
                .findFirst()
                .orElseThrow(() -> new SaleBuildException("Producto/local no tiene configuracion tributaria principal"));
    }

    private TaxAffectationEntity findMainTaxAffectation(ProductTaxConfigEntity mainConfig, String productCod) {
        TaxAffectationEntity mainAffectation = this.taxAffectationRepository
                .findActiveByCodeAndTax(mainConfig.TaxAffectationCod, mainConfig.TaxCod);
        if (mainAffectation == null) {
            throw new SaleBuildException("Combinacion tributaria principal no permitida para producto " + productCod);
        }
        return mainAffectation;
    }

    private List<ProductTaxConfigEntity> sortTaxConfigList(List<ProductTaxConfigEntity> configList) {
        return configList.stream()
                .sorted(Comparator
                        .comparingInt((ProductTaxConfigEntity config) -> config.CalculationOrder)
                        .thenComparing(config -> config.TaxCod))
                .toList();
    }

    private List<SaleDetTaxEntity> createSaleDetTaxEntities(
            PresaleDetEntity presaleDet,
            String saleCod,
            List<ProductTaxConfigEntity> sortedConfigList,
            ProductTaxConfigEntity mainConfig,
            TaxAffectationEntity mainAffectation,
            String userCod
    ) {
        BigDecimal total = amount(presaleDet.NumTotalPrice);
        BigDecimal fixedTaxTotal = calculateFixedTaxTotal(sortedConfigList, presaleDet.NumUnit, presaleDet.ProductCod, total);
        BigDecimal percentInclusiveTotal = total.subtract(fixedTaxTotal).setScale(2, RoundingMode.HALF_UP);
        BigDecimal baseAmount = calculateBaseAmount(sortedConfigList, mainConfig, mainAffectation, percentInclusiveTotal);

        List<SaleDetTaxEntity> taxLineList = new ArrayList<>();
        BigDecimal additionalPercentTaxAmount = addAdditionalPercentTaxLines(
                taxLineList,
                presaleDet,
                saleCod,
                sortedConfigList,
                baseAmount,
                userCod
        );
        addMainTaxLine(
                taxLineList,
                presaleDet,
                saleCod,
                mainConfig,
                mainAffectation,
                percentInclusiveTotal,
                baseAmount,
                additionalPercentTaxAmount,
                userCod
        );
        addFixedTaxLines(taxLineList, presaleDet, saleCod, sortedConfigList, userCod);
        assignTaxLineNumbers(taxLineList);

        return taxLineList;
    }

    private BigDecimal calculateFixedTaxTotal(
            List<ProductTaxConfigEntity> sortedConfigList,
            int numUnit,
            String productCod,
            BigDecimal total
    ) {
        BigDecimal fixedTaxTotal = sortedConfigList.stream()
                .filter(this::isRealFixedTax)
                .map(config -> fixedTaxAmount(config, numUnit))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        if (fixedTaxTotal.compareTo(total) > 0) {
            throw new SaleBuildException("Impuesto fijo supera total del producto " + productCod);
        }
        return fixedTaxTotal;
    }

    private BigDecimal calculateBaseAmount(
            List<ProductTaxConfigEntity> sortedConfigList,
            ProductTaxConfigEntity mainConfig,
            TaxAffectationEntity mainAffectation,
            BigDecimal percentInclusiveTotal
    ) {
        BigDecimal additionalPercentRate = sortedConfigList.stream()
                .filter(config -> !SaleTaxConstants.YES.equals(config.IsMainTax))
                .filter(this::isRealPercentTax)
                .map(config -> rate(effectiveTaxRateValue(config)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal mainRate = isTaxedMainPercent(mainConfig, mainAffectation) ? rate(effectiveTaxRateValue(mainConfig)) : BigDecimal.ZERO;
        BigDecimal divisor = BigDecimal.ONE.add(additionalPercentRate);

        if (mainRate.compareTo(BigDecimal.ZERO) > 0) {
            divisor = divisor.multiply(BigDecimal.ONE.add(mainRate));
        }

        return divisor.compareTo(BigDecimal.ZERO) == 0
                ? percentInclusiveTotal
                : percentInclusiveTotal.divide(divisor, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal addAdditionalPercentTaxLines(
            List<SaleDetTaxEntity> taxLineList,
            PresaleDetEntity presaleDet,
            String saleCod,
            List<ProductTaxConfigEntity> sortedConfigList,
            BigDecimal baseAmount,
            String userCod
    ) {
        BigDecimal additionalPercentTaxAmount = BigDecimal.ZERO;

        for (ProductTaxConfigEntity config : sortedConfigList) {
            if (SaleTaxConstants.YES.equals(config.IsMainTax) || !isRealPercentTax(config)) {
                continue;
            }
            BigDecimal taxAmount = baseAmount.multiply(rate(effectiveTaxRateValue(config))).setScale(2, RoundingMode.HALF_UP);
            additionalPercentTaxAmount = additionalPercentTaxAmount.add(taxAmount).setScale(2, RoundingMode.HALF_UP);
            taxLineList.add(buildTaxLine(
                    presaleDet,
                    saleCod,
                    config,
                    findTax(config.TaxCod),
                    null,
                    baseAmount,
                    BigDecimal.valueOf(presaleDet.NumUnit),
                    taxAmount,
                    userCod
            ));
        }

        return additionalPercentTaxAmount;
    }

    private void addMainTaxLine(
            List<SaleDetTaxEntity> taxLineList,
            PresaleDetEntity presaleDet,
            String saleCod,
            ProductTaxConfigEntity mainConfig,
            TaxAffectationEntity mainAffectation,
            BigDecimal percentInclusiveTotal,
            BigDecimal baseAmount,
            BigDecimal additionalPercentTaxAmount,
            String userCod
    ) {
        BigDecimal mainTaxAmount = calculateMainTaxAmount(
                mainConfig,
                mainAffectation,
                percentInclusiveTotal,
                baseAmount,
                additionalPercentTaxAmount
        );
        BigDecimal mainBaseAmount = mainTaxAmount.compareTo(BigDecimal.ZERO) > 0
                ? baseAmount.add(additionalPercentTaxAmount).setScale(2, RoundingMode.HALF_UP)
                : baseAmount;

        taxLineList.add(buildTaxLine(
                presaleDet,
                saleCod,
                mainConfig,
                findTax(mainConfig.TaxCod),
                mainAffectation,
                mainBaseAmount,
                BigDecimal.valueOf(presaleDet.NumUnit),
                mainTaxAmount,
                userCod
        ));
    }

    private BigDecimal calculateMainTaxAmount(
            ProductTaxConfigEntity mainConfig,
            TaxAffectationEntity mainAffectation,
            BigDecimal percentInclusiveTotal,
            BigDecimal baseAmount,
            BigDecimal additionalPercentTaxAmount
    ) {
        if (!isTaxedMainPercent(mainConfig, mainAffectation)) {
            return BigDecimal.ZERO;
        }
        return percentInclusiveTotal
                .subtract(baseAmount)
                .subtract(additionalPercentTaxAmount)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private void addFixedTaxLines(
            List<SaleDetTaxEntity> taxLineList,
            PresaleDetEntity presaleDet,
            String saleCod,
            List<ProductTaxConfigEntity> sortedConfigList,
            String userCod
    ) {
        for (ProductTaxConfigEntity config : sortedConfigList) {
            if (!isRealFixedTax(config)) {
                continue;
            }
            taxLineList.add(buildTaxLine(
                    presaleDet,
                    saleCod,
                    config,
                    findTax(config.TaxCod),
                    null,
                    BigDecimal.ZERO,
                    BigDecimal.valueOf(presaleDet.NumUnit),
                    fixedTaxAmount(config, presaleDet.NumUnit),
                    userCod
            ));
        }
    }

    private void assignTaxLineNumbers(List<SaleDetTaxEntity> taxLineList) {
        taxLineList.sort(Comparator
                .comparingInt((SaleDetTaxEntity line) -> line.CalculationOrder)
                .thenComparing(line -> line.TaxCod));

        for (int i = 0; i < taxLineList.size(); i++) {
            taxLineList.get(i).TaxLineNumber = i + 1;
            taxLineList.get(i).validate();
        }
    }

    private SaleDetEntity createSaleDetEntity(
            PresaleDetEntity presaleDet,
            String saleCod,
            List<SaleDetTaxEntity> taxLineList,
            String userCod
    ) {
        BigDecimal total = amount(presaleDet.NumTotalPrice);
        BigDecimal totalTax = taxLineList.stream()
                .map(line -> amount(line.TaxAmount))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal detailSubTotal = total.subtract(totalTax).setScale(2, RoundingMode.HALF_UP);

        SaleDetEntity saleDet = SaleDetEntityFactory.fromPresale(
                presaleDet,
                saleCod,
                detailSubTotal,
                totalTax,
                totalTax.compareTo(BigDecimal.ZERO) > 0
                        ? SaleTaxConstants.YES
                        : SaleTaxConstants.NO
        );
        return saleDet.session(userCod).validate();
    }

    private void validateConfigList(List<ProductTaxConfigEntity> configList, String productCod, String storeCod) {
        if (configList == null || configList.isEmpty()) {
            throw new SaleBuildException("Producto " + productCod + " no tiene configuracion tributaria en tienda " + storeCod);
        }
        long mainCount = configList.stream().filter(config -> SaleTaxConstants.YES.equals(config.IsMainTax)).count();
        if (mainCount != 1) {
            throw new SaleBuildException("Producto " + productCod + " debe tener una sola configuracion tributaria principal");
        }
        Set<String> taxCodSet = new HashSet<>();
        for (ProductTaxConfigEntity config : configList) {
            config.validate();
            if (!taxCodSet.add(config.TaxCod)) {
                throw new SaleBuildException("Producto " + productCod + " tiene tributo duplicado activo");
            }
            if (!SaleTaxConstants.YES.equals(config.IsMainTax) && this.taxAffectationRepository.countActiveByTaxCod(config.TaxCod) > 0) {
                throw new SaleBuildException("Producto " + productCod + " tiene un tributo de afectacion configurado como adicional");
            }
        }

        ProductTaxConfigEntity mainConfig = this.findMainTaxConfig(configList);
        boolean hasAdditionalIgv = configList.stream()
                .anyMatch(config -> SaleTaxConstants.IGV_TAX_COD.equals(config.TaxCod) && !SaleTaxConstants.YES.equals(config.IsMainTax));
        if (!SaleTaxConstants.TAXED_AFFECTATION_COD.equals(mainConfig.TaxAffectationCod) && hasAdditionalIgv) {
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
        SaleDetTaxEntity line = SaleDetTaxEntityFactory.fromTaxConfiguration(
                presaleDet,
                saleCod,
                config,
                tax,
                affectation,
                effectiveTaxRateValue(config).setScale(4, RoundingMode.HALF_UP),
                valueOrZero(config.FixedUnitAmount).setScale(4, RoundingMode.HALF_UP),
                amount(taxBaseAmount),
                valueOrZero(taxQuantity).setScale(4, RoundingMode.HALF_UP),
                amount(taxAmount)
        );
        line.session(userCod);
        return line;
    }

    private TaxEntity findTax(String taxCod) {
        return this.taxRepository.findById(taxCod)
                .orElseThrow(() -> new SaleBuildException("Tributo no existe: " + taxCod));
    }

    private boolean isTaxedMainPercent(ProductTaxConfigEntity config, TaxAffectationEntity affectation) {
        return SaleTaxConstants.YES.equals(affectation.IsTaxed) && isRealPercentTax(config);
    }

    private boolean isRealPercentTax(ProductTaxConfigEntity config) {
        return SaleTaxConstants.TAX_CALCULATION_PERCENT.equals(config.TaxCalculationType)
                && !SaleTaxConstants.YES.equals(config.IsInformative);
    }

    private boolean isRealFixedTax(ProductTaxConfigEntity config) {
        return SaleTaxConstants.TAX_CALCULATION_FIXED.equals(config.TaxCalculationType)
                && !SaleTaxConstants.YES.equals(config.IsInformative);
    }

    private BigDecimal fixedTaxAmount(ProductTaxConfigEntity config, int numUnit) {
        return valueOrZero(config.FixedUnitAmount)
                .multiply(BigDecimal.valueOf(numUnit))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal effectiveTaxRateValue(ProductTaxConfigEntity config) {
        return SaleTaxConstants.IGV_TAX_COD.equals(config.TaxCod)
                ? SaleTaxConstants.STANDARD_IGV_RATE
                : valueOrZero(config.TaxRateValue);
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
}
