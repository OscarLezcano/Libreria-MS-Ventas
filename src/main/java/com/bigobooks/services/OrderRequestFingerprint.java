package com.bigobooks.services;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.bigobooks.dto.OrderCreateRequest;
import com.bigobooks.dto.OrderItemCreate;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Huella canonica del payload de creacion de pedido.
 * Se usa con Idempotency-Key para distinguir reintentos exactos de reutilizaciones
 * de la misma clave con datos distintos (que deben responder 409).
 */
@Component
public class OrderRequestFingerprint {

	private final ObjectMapper objectMapper = new ObjectMapper();

	public String of(OrderCreateRequest request) {
		if (request == null || request.getItems() == null) {
			throw new IllegalArgumentException("Debe incluir al menos un item en la venta");
		}

		Map<String, Object> canonical = new LinkedHashMap<>();
		canonical.put("warehouseId", request.getWarehouseId());
		canonical.put("userId", request.getUserId());
		canonical.put("couponCode", normalized(request.getCouponCode()));
		canonical.put("invoiceRuc", normalized(request.getInvoiceRuc()));
		canonical.put("invoiceName", normalized(request.getInvoiceName()));
		canonical.put("invoiceLastName", normalized(request.getInvoiceLastName()));
		canonical.put("invoicePhone", normalized(request.getInvoicePhone()));
		canonical.put("shippingName", normalized(request.getShippingName()));
		canonical.put("shippingLastName", normalized(request.getShippingLastName()));
		canonical.put("shippingDepartment", normalized(request.getShippingDepartment()));
		canonical.put("shippingCity", normalized(request.getShippingCity()));
		canonical.put("shippingAddress", normalized(request.getShippingAddress()));
		canonical.put("shippingReference", normalized(request.getShippingReference()));
		canonical.put("shippingPhone", normalized(request.getShippingPhone()));

		List<Map<String, Object>> items = new ArrayList<>();
		for (OrderItemCreate item : request.getItems()) {
			Map<String, Object> canonicalItem = new LinkedHashMap<>();
			canonicalItem.put("bookId", item == null ? null : item.getBookId());
			canonicalItem.put("quantity", item == null ? null : item.getQuantity());
			items.add(canonicalItem);
		}
		items.sort(Comparator
				.comparing((Map<String, Object> item) -> String.valueOf(item.get("bookId")))
				.thenComparing(item -> String.valueOf(item.get("quantity"))));
		canonical.put("items", items);

		try {
			byte[] digest = MessageDigest.getInstance("SHA-256")
					.digest(objectMapper.writeValueAsBytes(canonical));
			StringBuilder hex = new StringBuilder(digest.length * 2);
			for (byte value : digest) {
				hex.append(String.format("%02x", value));
			}
			return hex.toString();
		} catch (NoSuchAlgorithmException | JsonProcessingException exception) {
			throw new IllegalStateException("No se pudo calcular la huella del pedido", exception);
		}
	}

	private String normalized(String value) {
		if (value == null) {
			return null;
		}
		String normalized = value.trim().replaceAll("\\s+", " ");
		return normalized.isEmpty() ? null : normalized;
	}
}
