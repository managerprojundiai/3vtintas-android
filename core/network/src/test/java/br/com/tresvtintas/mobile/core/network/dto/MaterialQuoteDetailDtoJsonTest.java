package br.com.tresvtintas.mobile.core.network.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import br.com.tresvtintas.mobile.core.network.MobileApiFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

public final class MaterialQuoteDetailDtoJsonTest {
    @Test
    public void readsCurrentProductCompatibilityPriceWithHiddenStock()
            throws Exception {
        ObjectMapper mapper = MobileApiFactory.objectMapper();

        MaterialQuoteDetailDto detail = mapper.readValue(
                """
                {
                  "id": 5,
                  "customer": {"id": 7, "name": "Cliente teste"},
                  "organizationId": 1,
                  "title": "TESTE_ANDROID_CONTRATO",
                  "status": "draft",
                  "subtotal": "1009.42",
                  "discount": "0.00",
                  "total": "1009.42",
                  "revision": 1,
                  "itemCount": 1,
                  "validUntil": null,
                  "createdAt": "2026-08-30T17:00:00.000Z",
                  "updatedAt": "2026-08-30T17:00:00.000Z",
                  "notes": null,
                  "pricing": {
                    "state": "RESOLVED",
                    "priceListCode": "AUTCOM_2",
                    "priceListName": "Tabela AUTCOM 2",
                    "priceListVersionPublicId": "123e4567-e89b-42d3-a456-426614174000",
                    "priceListVersionNumber": 3,
                    "policyRevision": 1,
                    "selectionMode": "PRIMARY",
                    "resolvedAt": "2026-08-30T17:00:00.000Z"
                  },
                  "items": [{
                    "id": 9,
                    "productId": 7288,
                    "description": "EMBORRACHADA COGUMELO JAPONES",
                    "quantity": "2.00",
                    "unit": "UN",
                    "unitPrice": "504.71",
                    "total": "1009.42",
                    "tint": null,
                    "currentProduct": {
                      "active": true,
                      "currentPrice": "504.71",
                      "stock": null
                    }
                  }]
                }
                """,
                MaterialQuoteDetailDto.class);

        MaterialQuoteProductStateDto current =
                detail.items().get(0).currentProduct();
        assertEquals("compatibility price", "504.71", current.currentPrice());
        assertNull("inventory remains hidden", current.stock());
    }
}
