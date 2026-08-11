package com.ccadmin.app.sale.service;

import com.ccadmin.app.sale.model.dto.SalesContextDto;
import com.ccadmin.app.shared.model.constants.AuditUserConstants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SalesContextServiceTest {

    @Test
    void buildsInternalContextFromTheAuthenticatedApplicationSession() {
        SalesContextService service = new InternalSalesContextService();

        SalesContextDto context = service.getInternalContext();

        assertEquals("T001", context.StoreCod);
        assertEquals("USER01", context.UserCod);
        assertEquals(25L, context.CashSessionID);
    }

    @Test
    void buildsWebContextWithoutUsingAnAdministrativeSession() {
        SalesContextService service = new WebSalesContextService();

        SalesContextDto context = service.getWebContext("T002");

        assertEquals("T002", context.StoreCod);
        assertEquals(AuditUserConstants.USER_WEB, context.UserCod);
        assertNull(context.CashSessionID);
    }

    @Test
    void rejectsWebContextWithoutStore() {
        SalesContextService service = new SalesContextService();

        assertThrows(IllegalArgumentException.class, () -> service.getWebContext(" "));
    }

    private static class InternalSalesContextService extends SalesContextService {

        @Override
        public String getStoreCod() {
            return "T001";
        }

        @Override
        public String getUserCod() {
            return "USER01";
        }

        @Override
        public Long getCashSessionID() {
            return 25L;
        }
    }

    private static class WebSalesContextService extends SalesContextService {

        @Override
        public String getStoreCod() {
            throw new AssertionError("El contexto web no debe consultar la tienda de una sesion administrativa");
        }

        @Override
        public String getUserCod() {
            throw new AssertionError("El contexto web no debe consultar un usuario administrativo");
        }

        @Override
        public Long getCashSessionID() {
            throw new AssertionError("El contexto web no debe consultar una caja administrativa");
        }
    }
}
