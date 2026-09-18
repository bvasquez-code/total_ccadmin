package com.ccadmin.app.report.repository;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.data.jpa.repository.Query;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.*;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas optativas en MySQL local con CTE y una tabla temporal; sin cambios persistentes. */
@EnabledIfSystemProperty(named = "report.mysql.tests", matches = "true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReportRepositoryMySqlTest {
    private static final List<Class<?>> REPOSITORIES = List.of(PurchaseSalesReportRepository.class, SalesReportRepository.class, SoldProductsReportRepository.class, StockReportRepository.class, PaymentMethodsReportRepository.class, DocumentsReportRepository.class, ClientsReportRepository.class, OrdersReportRepository.class, CreditNotesReportRepository.class, ProfitReportRepository.class);
    private static final List<String> TABLES = List.of("kardex", "user_store", "store", "person", "client", "product", "sale_head", "sale_det", "product_info", "product_config", "payment_method", "cash_session", "sale_payments", "trx_payments", "sale_document", "credit_note_head", "credit_note_det", "credit_note_document", "presale_head", "presale_channel", "sale_delivery", "pucharse_head", "product_traceability", "currency", "stock_entry_head", "stock_exit_head", "transfer_head");
    private Connection connection;
    private NamedParameterJdbcTemplate jdbc;
    private String fixtures;

    @BeforeAll
    void connectToLocalDatabase() throws Exception {
        Properties properties = new Properties();
        try (InputStream input = getClass().getResourceAsStream("/application.properties")) {
            properties.load(input);
        }
        for (String profile : properties.getProperty("spring.profiles.active", "").split(",")) {
            try (InputStream input = getClass().getResourceAsStream("/application-" + profile.trim() + ".properties")) {
                if (input != null) properties.load(input);
            }
        }
        String url = properties.getProperty("spring.datasource.url");
        assertTrue(url != null && (url.startsWith("jdbc:mysql://localhost:") || url.startsWith("jdbc:mysql://127.0.0.1:")),
                "Estas pruebas solo se ejecutan contra MySQL local");
        connection = DriverManager.getConnection(url, properties.getProperty("spring.datasource.username"),
                properties.getProperty("spring.datasource.password"));
        connection.setReadOnly(true);
        jdbc = new NamedParameterJdbcTemplate(new SingleConnectionDataSource(connection, true));
        try (InputStream input = getClass().getResourceAsStream("/report/report-fixtures.sql")) {
            fixtures = new String(Objects.requireNonNull(input).readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @AfterAll
    void closeConnection() throws Exception {
        if (connection != null) connection.close();
    }

    @Test
    void everyRepositoryQueryAndCountRunAgainstTheInstalledSchema() {
        for (Class<?> repository : REPOSITORIES) {
            for (var method : repository.getDeclaredMethods()) {
                Query query = method.getAnnotation(Query.class);
                Map<String, Object> parameters = parameters("");
                parameters.put("DateFrom", java.sql.Date.valueOf("9997-01-01"));
                parameters.put("DateTo", java.sql.Date.valueOf("9997-02-01"));
                parameters.put("StoreCod", "NONE");
                assertDoesNotThrow(() -> jdbc.queryForList(bindSearchParameters(query.value()), parameters), repository.getSimpleName() + "." + method.getName());
            }
        }
    }

    @Test
    void jpaRepositoriesBindStructuredFiltersThroughTheSharedPaginator() {
        org.hibernate.cfg.Configuration configuration = new org.hibernate.cfg.Configuration();
        configuration.addAnnotatedClass(com.ccadmin.app.sale.model.entity.SaleHeadEntity.class);
        configuration.addAnnotatedClass(com.ccadmin.app.product.model.entity.ProductInfoEntity.class);
        configuration.addAnnotatedClass(com.ccadmin.app.sale.model.entity.PresaleHeadEntity.class);
        configuration.addAnnotatedClass(com.ccadmin.app.sale.model.entity.CreditNoteHeadEntity.class);
        configuration.getProperties().put("hibernate.connection.datasource",
                new SingleConnectionDataSource(connection, true));
        configuration.setProperty("hibernate.hbm2ddl.auto", "none");
        try (var sessionFactory = configuration.buildSessionFactory();
             var entityManager = sessionFactory.createEntityManager()) {
            var repositoryFactory = new org.springframework.data.jpa.repository.support.JpaRepositoryFactory(entityManager);
            var filters = new com.ccadmin.app.report.model.dto.ReportFilterDto(
                    "9997-01-01", "9997-01-31", "NONE", "", "", "", 2);
            for (Class<?> repositoryType : REPOSITORIES) {
                var repository = (com.ccadmin.app.shared.interfaceccadmin.CcAdminRepository<?, ?>)
                        repositoryFactory.getRepository(repositoryType);
                var search = com.ccadmin.app.report.model.dto.ReportSearchDto.forPeriod(filters, "REPORT_TEST");
                var response = new com.ccadmin.app.shared.service.SearchTService<>(repository).findAll(search, 10);
                assertEquals(0, response.TotalResult, repositoryType.getSimpleName());
                assertTrue(response.resultSearch.isEmpty(), repositoryType.getSimpleName());
                assertEquals(2, response.Page);
            }
        }
    }

    @Test
    void allReportsKeepTheScopeOfActiveUserStores() {
        for (Class<?> repository : REPOSITORIES) {
            var unassignedStore = parameters("");
            unassignedStore.put("StoreCod", "T002");
            assertTrue(execute(repository, false, unassignedStore).isEmpty(), repository.getSimpleName());
            assertEquals(0, ((Number) execute(repository, true, unassignedStore)
                    .getFirst().values().iterator().next()).intValue(), repository.getSimpleName());

            var allUserStores = parameters("");
            allUserStores.put("StoreCod", "");
            assertEquals(rows(repository, "").size(), execute(repository, false, allUserStores).size(),
                    repository.getSimpleName());

            allUserStores.put("UserCod", "UNASSIGNED");
            assertTrue(execute(repository, false, allUserStores).isEmpty(), repository.getSimpleName());
        }
    }

    @Test
    void salesRespectStatusStoreAndInclusiveLastDay() {
        var rows = rows(SalesReportRepository.class, "C");
        assertEquals(3, rows.size());
        assertEquals(Set.of("S1", "S2", "S6"), rows.stream().map(row -> row.get("SaleCod")).collect(java.util.stream.Collectors.toSet()));
        assertEquals(3, count(SalesReportRepository.class, "C"));
        var page = parameters("C");
        page.put("Init", 1);
        page.put("Limit", 1);
        assertEquals(1, execute(SalesReportRepository.class, false, page).size());
    }

    @Test
    void productsAggregateOnlyConfirmedSalesAndKeepCurrenciesSeparate() {
        var rows = rows(SoldProductsReportRepository.class, "");
        assertEquals(3, rows.size());
        var penProduct = rows.stream().filter(row -> "P1".equals(row.get("ProductCod")) && "PEN".equals(row.get("CurrencyCod"))).findFirst().orElseThrow();
        assertMoney("3", penProduct.get("Quantity"));
        assertMoney("177", penProduct.get("Amount"));
        assertEquals(3, count(SoldProductsReportRepository.class, ""));
    }

    @Test
    void stockReportsCurrentZonesAndLowStock() {
        var rows = rows(StockReportRepository.class, "LOW");
        assertEquals(2, rows.size());
        var product = rows.stream().filter(row -> "P1".equals(row.get("ProductCod"))).findFirst().orElseThrow();
        assertMoney("2", product.get("PhysicalStock"));
        assertMoney("1", product.get("ReservedStock"));
        assertMoney("4", product.get("TotalStock"));
        assertEquals(1, rows(StockReportRepository.class, "EMPTY").size());
    }

    @Test
    void paymentsCountEachTransactionOnceAndIncludeWebReversalsWithoutCashSession() {
        var rows = rows(PaymentMethodsReportRepository.class, "OK");
        assertEquals(2, rows.size());
        var pen = rows.stream().filter(row -> "PEN".equals(row.get("CurrencyCod"))).findFirst().orElseThrow();
        assertMoney("2", pen.get("PaymentCount"));
        assertMoney("118", pen.get("Income"));
        assertMoney("59", pen.get("Reversals"));
        assertMoney("59", pen.get("Amount"));
        assertEquals(2, count(PaymentMethodsReportRepository.class, "OK"));
    }

    @Test
    void documentsKeepProformaFiscalAndCreditNoteRowsDistinct() {
        assertEquals(3, rows(DocumentsReportRepository.class, "").size());
        var rows = rows(DocumentsReportRepository.class, "07");
        assertEquals(1, rows.size());
        assertMoney("-59", rows.getFirst().get("Amount"));
        assertEquals(3, count(DocumentsReportRepository.class, ""));
    }

    @Test
    void clientsShowConsumptionPerCurrencyWithoutMultiplyingSalesByDetails() {
        var rows = rows(ClientsReportRepository.class, "");
        assertEquals(2, rows.size());
        var pen = rows.stream().filter(row -> "PEN".equals(row.get("CurrencyCod"))).findFirst().orElseThrow();
        assertMoney("2", pen.get("SaleCount"));
        assertMoney("295", pen.get("Amount"));
        assertMoney("147.50", pen.get("AverageTicket"));
    }

    @Test
    void ordersHaveOneRowAndUseLatestAssociatedSale() {
        var rows = rows(OrdersReportRepository.class, "");
        assertEquals(1, rows.size());
        assertEquals("S7", rows.getFirst().get("SaleCod"));
        assertEquals("TRACK1", rows.getFirst().get("TrackingNumber"));
        assertEquals(1, count(OrdersReportRepository.class, ""));
    }

    @Test
    void creditNotesExcludePendingNotesAndShowReturnedStock() {
        var rows = rows(CreditNotesReportRepository.class, "C");
        assertEquals(1, rows.size());
        assertMoney("1", rows.getFirst().get("ReturnedQuantity"));
        assertMoney("59", rows.getFirst().get("Amount"));
    }

    @Test
    void profitUsesAllCostAllocationsReversesReturnedCostAndLeavesUnknownCostsNull() {
        var rows = rows(ProfitReportRepository.class, "");
        assertEquals(5, rows.size());
        var sale = rows.stream().filter(row -> "S1".equals(row.get("OperationCod")) && "P1".equals(row.get("ProductCod"))).findFirst().orElseThrow();
        assertMoney("40", sale.get("Cost"));
        assertMoney("78", sale.get("Profit"));
        var note = rows.stream().filter(row -> "N1".equals(row.get("OperationCod"))).findFirst().orElseThrow();
        assertMoney("-20", note.get("Cost"));
        assertMoney("-39", note.get("Profit"));
        var differentCurrency = rows.stream().filter(row -> "S2".equals(row.get("OperationCod"))).findFirst().orElseThrow();
        assertNull(differentCurrency.get("Cost"));
        assertNull(differentCurrency.get("Profit"));
        assertEquals("MISSING", differentCurrency.get("CostStatus"));
        assertEquals(2, rows(ProfitReportRepository.class, "COMPLETE").size());
        assertEquals(3, count(ProfitReportRepository.class, "MISSING"));
    }


    @Test
    void menuScriptIsRerunnableAndRegistersExactlyTenChildren() throws Exception {
        connection.setReadOnly(false);
        try {
            jdbc.getJdbcTemplate().execute("CREATE TEMPORARY TABLE report_menu_fixture LIKE app_menu");
            String script = java.nio.file.Files.readString(java.nio.file.Path.of(
                    "..", "..", "database", "db_store_01_mysql", "insert_report_menu_20260907.sql"
            )).replace("INSERT INTO app_menu", "INSERT INTO report_menu_fixture");
            script = script.replaceAll("(?m)^--.*$", "");
            for (int execution = 0; execution < 2; execution++) {
                for (String statement : script.split(";")) {
                    if (!statement.isBlank()) jdbc.getJdbcTemplate().execute(statement);
                }
            }
            String financialScript = java.nio.file.Files.readString(java.nio.file.Path.of(
                    "..", "..", "database", "db_store_01_mysql", "insert_financial_report_menu_20260916.sql"
            )).replace("INSERT INTO app_menu", "INSERT INTO report_menu_fixture").replaceAll("(?m)^--.*$", "");
            for (int execution = 0; execution < 2; execution++) {
                for (String statement : financialScript.split(";")) {
                    if (!statement.isBlank()) jdbc.getJdbcTemplate().execute(statement);
                }
            }
            assertEquals(11, jdbc.getJdbcTemplate().queryForObject("SELECT COUNT(*) FROM report_menu_fixture", Integer.class));
            assertEquals(10, jdbc.getJdbcTemplate().queryForObject(
                    "SELECT COUNT(*) FROM report_menu_fixture WHERE MenuDadCod = 'RP000000' AND IsMenuDad = 'N'", Integer.class));
        } finally {
            connection.setReadOnly(true);
        }
    }

    @Test
    void financialOverviewMapsJpaProjectionsAndNeverMergesCurrencies() {
        org.hibernate.cfg.Configuration configuration = new org.hibernate.cfg.Configuration();
        configuration.addAnnotatedClass(com.ccadmin.app.sale.model.entity.SaleHeadEntity.class);
        configuration.getProperties().put("hibernate.connection.datasource",
                new SingleConnectionDataSource(connection, true));
        configuration.getProperties().put("hibernate.session_factory.statement_inspector",
                (org.hibernate.resource.jdbc.spi.StatementInspector) this::fixtureSql);
        configuration.setProperty("hibernate.hbm2ddl.auto", "none");
        try (var sessionFactory = configuration.buildSessionFactory();
             var entityManager = sessionFactory.createEntityManager()) {
            var factory = new org.springframework.data.jpa.repository.support.JpaRepositoryFactory(entityManager);
            var search = com.ccadmin.app.report.model.dto.ReportSearchDto.forPeriod(
                    new com.ccadmin.app.report.model.dto.ReportFilterDto(
                            "2026-09-01", "2026-09-30", "T001", "", "", "", 1), "U001");
            var profit = new com.ccadmin.app.report.model.dto.FinancialReportOverviewDto(
                    factory.getRepository(ProfitReportRepository.class).findDaily(search));
            assertEquals(2, profit.Summary.size());
            var pen = profit.Summary.stream().filter(row -> "PEN".equals(row.CurrencyCod)).findFirst().orElseThrow();
            assertMoney("39", pen.KnownResult);
            assertMoney("236", pen.Sales);
            assertEquals(2, pen.MissingCostRows);
            assertNull(pen.Result);
            assertNull(pen.Margin);
            assertEquals("2026-09-01", profit.Daily.getFirst().ReportDay);
            var comparison = new com.ccadmin.app.report.model.dto.FinancialReportOverviewDto(
                    factory.getRepository(PurchaseSalesReportRepository.class).findDaily(search));
            pen = comparison.Summary.stream().filter(row -> "PEN".equals(row.CurrencyCod)).findFirst().orElseThrow();
            assertMoney("-461", pen.Result);
            assertMoney("1039", pen.Purchases);
            assertEquals(0, pen.MissingCostRows);
        }
    }

    @Test
    void storeBalanceUsesTracedAmountsAndGroupsAllocationsWithoutCountingDocumentTotals() {
        var rows = rows(PurchaseSalesReportRepository.class, "");
        assertEquals(9, rows.size());
        assertEquals(2, count(PurchaseSalesReportRepository.class, "PURCHASE"));
        assertTrue(rows.stream().noneMatch(row -> Set.of("B1", "B3", "B4", "S6", "X2", "E2").contains(row.get("OperationCod"))));
        var note = rows.stream().filter(row -> "N1".equals(row.get("OperationCod"))).findFirst().orElseThrow();
        assertMoney("0", note.get("Sales"));
        assertMoney("59", note.get("Purchases"));
        assertMoney("-59", note.get("Balance"));
        var daily = daily(PurchaseSalesReportRepository.class, parameters(""));
        assertMoney("-461", sum(daily, "PEN", "KnownResult"));
        assertMoney("18", sum(daily, "USD", "KnownResult"));
        assertMoney("1039", sum(daily, "PEN", "Purchases"));
        assertMoney("578", sum(daily, "PEN", "Sales"));
        var purchase = rows.stream().filter(row -> "B2".equals(row.get("OperationCod"))).findFirst().orElseThrow();
        assertMoney("10", purchase.get("Quantity"));
        assertMoney("500", purchase.get("Purchases"));
    }

    @Test
    void dailyTotalsCoverAllRowsRegardlessOfPaginationAndFlagMissingCosts() {
        var params = parameters("");
        params.put("Limit", 1);
        assertEquals(1, execute(ProfitReportRepository.class, false, params).size());
        var daily = daily(ProfitReportRepository.class, params);
        assertMoney("236", sum(daily, "PEN", "Sales"));
        assertMoney("39", sum(daily, "PEN", "KnownResult"));
        assertMoney("2", sum(daily, "PEN", "MissingCostRows"));
        assertMoney("1", sum(daily, "USD", "MissingCostRows"));
        params.put("Query", "%P2%");
        assertMoney("118", sum(daily(ProfitReportRepository.class, params), "PEN", "Sales"));
    }

    @Test
    void rejectedCreditNoteUnitsDoNotRecoverTheirCost() {
        String original = fixtures;
        try {
            fixtures = fixtures.replace("1.00 AS `NumUnitStockReturned`", "0.00 AS `NumUnitStockReturned`");
            var note = rows(ProfitReportRepository.class, "").stream()
                    .filter(row -> "N1".equals(row.get("OperationCod"))).findFirst().orElseThrow();
            assertMoney("0", note.get("Cost"));
            assertMoney("-59", note.get("Profit"));
        } finally { fixtures = original; }
    }

    @Test
    void partialReturnsSubtractRejectedTraceAllocationsAndPendingReturnsStayUnverified() {
        String original = fixtures;
        try {
            fixtures = fixtures.replace("'S' AS `TypeOperation`, 'P1' AS `ProductCod`, '0000' AS `Variant`, 'T001' AS `StoreCod`, 1.00 AS `NumUnit`, 20.00 AS `NumTotalPriceCost`",
                    "'S' AS `TypeOperation`, 'P1' AS `ProductCod`, '0000' AS `Variant`, 'T001' AS `StoreCod`, 2.00 AS `NumUnit`, 40.00 AS `NumTotalPriceCost`");
            int end = fixtures.lastIndexOf(')');
            fixtures = fixtures.substring(0, end)
                    + " UNION ALL SELECT 999, 'LT1', 4, 'N1', 1, 'credit_note_head', 'R', 'P1', '0000', 'T001', 1, 20, 'A', 0, '2026-09-18 12:00:00' "
                    + fixtures.substring(end);
            var note = rows(ProfitReportRepository.class, "").stream()
                    .filter(row -> "N1".equals(row.get("OperationCod"))).findFirst().orElseThrow();
            assertMoney("-20", note.get("Cost"));
            assertMoney("-39", note.get("Profit"));
            fixtures = original.replace("'S' AS `IsStockReturned`", "'N' AS `IsStockReturned`");
            note = rows(ProfitReportRepository.class, "").stream()
                    .filter(row -> "N1".equals(row.get("OperationCod"))).findFirst().orElseThrow();
            assertNull(note.get("Cost"));
            assertNull(note.get("Profit"));
        } finally { fixtures = original; }
    }

    @Test
    void effectiveMovementDateDeterminesThePeriodInsteadOfDraftCreationDate() {
        String original = fixtures;
        try {
            fixtures = fixtures.replace("'2026-09-01 00:00:00' AS CreationDate", "'2026-10-01 00:00:00' AS CreationDate");
            assertTrue(rows(ProfitReportRepository.class, "").stream().noneMatch(row -> "S1".equals(row.get("OperationCod"))));
            assertTrue(rows(PurchaseSalesReportRepository.class, "").stream().anyMatch(row -> "S1".equals(row.get("OperationCod"))));
        } finally { fixtures = original; }
    }


    @Test
    void transfersAffectOnlyTheirOwnStoreAndUseTheMovementDate() {
        var movements = rows(PurchaseSalesReportRepository.class, "TRANSFER");
        assertEquals(2, movements.size());
        var inbound = movements.stream().filter(row -> "TR1".equals(row.get("OperationCod"))).findFirst().orElseThrow();
        assertEquals("S", inbound.get("TypeOperation"));
        assertEquals("T002 -> T001", inbound.get("Reference"));
        assertMoney("-40", inbound.get("Balance"));
        var outbound = movements.stream().filter(row -> "TR2".equals(row.get("OperationCod"))).findFirst().orElseThrow();
        assertEquals("R", outbound.get("TypeOperation"));
        assertMoney("20", outbound.get("Balance"));
        var params = parameters("TRANSFER");
        params.put("StoreCod", "T002");
        assertTrue(execute(PurchaseSalesReportRepository.class, false, params).isEmpty());
        params.put("UserCod", "U002");
        movements = execute(PurchaseSalesReportRepository.class, false, params);
        assertEquals(1, movements.size());
        assertEquals("TR1", movements.getFirst().get("OperationCod"));
        assertMoney("40", movements.getFirst().get("Balance"));
        params.put("StoreCod", "");
        assertEquals(1, execute(PurchaseSalesReportRepository.class, false, params).size());
    }

    @Test
    void allStoresKeepTransferSidesDistinctAndSummariesIncludeEverySelectedStore() {
        String original = fixtures;
        try {
            fixtures = fixtures.replace("'U001', 'T002', 'I'", "'U001', 'T002', 'A'");
            var params = parameters("TRANSFER");
            params.put("StoreCod", "");
            var movements = execute(PurchaseSalesReportRepository.class, false, params);
            assertEquals(3, movements.size());
            assertEquals(2, movements.stream().filter(row -> "TR1".equals(row.get("OperationCod"))).count());
            assertMoney("20", sum(daily(PurchaseSalesReportRepository.class, params), "PEN", "KnownResult"));
            params.put("StoreCod", "T001");
            assertEquals(2, execute(PurchaseSalesReportRepository.class, false, params).size());
            assertMoney("-20", sum(daily(PurchaseSalesReportRepository.class, params), "PEN", "KnownResult"));
        } finally { fixtures = original; }
    }

    @Test
    void manualStockUsesTheConfiguredSystemCurrencyAndTransfersKeepTheLotCurrency() {
        String original = fixtures;
        try {
            // Cambiar la moneda del sistema prueba que el reporte no fija PEN por código.
            fixtures = fixtures.replace("'PEN' AS CurrencyCod, 'S' AS IsMonedaSystem",
                    "'EUR' AS CurrencyCod, 'S' AS IsMonedaSystem");
            var entry = rows(PurchaseSalesReportRepository.class, "STOCK_ENTRY").getFirst();
            var exit = rows(PurchaseSalesReportRepository.class, "STOCK_EXIT").getFirst();
            assertEquals("EUR", entry.get("CurrencyCod"));
            assertEquals("EUR", exit.get("CurrencyCod"));
            assertMoney("-440", entry.get("Balance"));
            assertMoney("440", exit.get("Balance"));
            assertEquals("PEN", rows(PurchaseSalesReportRepository.class, "TRANSFER").getFirst().get("CurrencyCod"));
            fixtures = fixtures.replace("10, 'LT1', 9, 'TR1'", "10, 'LT4', 8, 'TR1'");
            var transfer = rows(PurchaseSalesReportRepository.class, "TRANSFER").getFirst();
            assertEquals("USD", transfer.get("CurrencyCod"));
            assertMoney("-40", transfer.get("Balance"));
        } finally { fixtures = original; }
    }

    @Test
    void stockBalanceUsesTraceOperationDateAndDoesNotFallBackToHeadTotals() {
        String original = fixtures;
        try {
            fixtures = fixtures.replace("'2026-09-01 00:00:00' AS `OperationDate`",
                    "'2026-10-01 00:00:00' AS `OperationDate`");
            var movements = rows(PurchaseSalesReportRepository.class, "");
            assertTrue(movements.stream().noneMatch(row -> Set.of("S1", "S2", "S6").contains(row.get("OperationCod"))));
        } finally { fixtures = original; }
    }

    @Test
    void rejectedReturnsAreSeparateOutboundCostMovements() {
        String original = fixtures;
        try {
            int end = fixtures.lastIndexOf(')');
            fixtures = fixtures.substring(0, end)
                    + " UNION ALL SELECT 999, 'LT1', 4, 'N1', 1, 'credit_note_head', 'R', 'P1', '0000', 'T001', 1, 20, 'A', 0, '2026-09-18 12:00:00' "
                    + fixtures.substring(end);
            var notes = rows(PurchaseSalesReportRepository.class, "CREDIT_NOTE");
            assertEquals(2, notes.size());
            assertMoney("-59", notes.getFirst().get("Balance"));
            assertMoney("20", notes.getLast().get("Balance"));
            assertEquals("R", notes.getLast().get("TypeOperation"));
        } finally { fixtures = original; }
    }

    @Test
    void storeBalanceSummaryAndExportApplyTheSameCurrencyTypeAndQueryFilters() {
        var params = parameters("");
        params.put("Limit", 1);
        assertEquals(1, execute(PurchaseSalesReportRepository.class, false, params).size());
        assertMoney("-461", sum(daily(PurchaseSalesReportRepository.class, params), "PEN", "KnownResult"));
        params.put("CurrencyCod", "USD");
        assertMoney("18", sum(daily(PurchaseSalesReportRepository.class, params), "USD", "KnownResult"));
        params.put("CurrencyCod", "PEN");
        params.put("State", "TRANSFER");
        params.put("Query", "%TR1%");
        var detail = execute(PurchaseSalesReportRepository.class, false, params);
        assertEquals(1, detail.size());
        assertMoney("-40", detail.getFirst().get("Balance"));
        assertMoney("-40", sum(daily(PurchaseSalesReportRepository.class, params), "PEN", "KnownResult"));
    }

    private BigDecimal sum(List<Map<String, Object>> rows, String currency, String field) {
        return rows.stream().filter(row -> currency.equals(row.get("CurrencyCod")))
                .map(row -> new BigDecimal(row.get(field).toString())).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<Map<String, Object>> daily(Class<?> repository, Map<String, Object> parameters) {
        try {
            return executeSql(repository.getMethod("findDaily", com.ccadmin.app.shared.model.dto.SearchDto.class)
                    .getAnnotation(Query.class).value(), parameters);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private List<Map<String, Object>> rows(Class<?> repository, String state) {
        return execute(repository, false, parameters(state));
    }

    private int count(Class<?> repository, String state) {
        return ((Number) execute(repository, true, parameters(state)).getFirst().values().iterator().next()).intValue();
    }

    private List<Map<String, Object>> execute(Class<?> repository, boolean count, Map<String, Object> parameters) {
        String sql = Arrays.stream(repository.getDeclaredMethods())
                .filter(method -> method.getName().equals(count ? "countByQueryText" : "findByQueryText")).findFirst().orElseThrow()
                .getAnnotation(Query.class).value();
        return executeSql(sql, parameters);
    }

    private List<Map<String, Object>> executeSql(String sql, Map<String, Object> parameters) {
        sql = fixtureSql(sql);
        return jdbc.queryForList(bindSearchParameters(sql), parameters);
    }

    private String fixtureSql(String sql) {
        for (String table : TABLES) {
            sql = sql.replaceAll("(?i)\\b(FROM|JOIN)\\s+" + Pattern.quote(table) + "\\b", "$1 fixture_" + table);
        }
        return sql.stripLeading().startsWith("WITH ")
                ? "WITH " + fixtures + ", " + sql.stripLeading().substring(5)
                : "WITH " + fixtures + " " + sql;
    }

    private Map<String, Object> parameters(String state) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("DateFrom", java.sql.Date.valueOf("2026-09-01"));
        parameters.put("DateTo", java.sql.Date.valueOf("2026-10-01"));
        parameters.put("StoreCod", "T001");
        parameters.put("UserCod", "U001");
        parameters.put("CurrencyCod", "");
        parameters.put("Query", "%");
        parameters.put("State", state);
        parameters.put("Init", 0);
        parameters.put("Limit", 10);
        return parameters;
    }

    private String bindSearchParameters(String sql) {
        return sql.replaceAll(":#\\{#search\\.(\\w+)\\}", ":$1");
    }

    private void assertMoney(String expected, Object actual) {
        assertNotNull(actual);
        assertEquals(0, new BigDecimal(expected).compareTo(new BigDecimal(actual.toString())));
    }
}
