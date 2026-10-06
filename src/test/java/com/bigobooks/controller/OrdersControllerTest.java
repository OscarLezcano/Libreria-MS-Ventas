package com.bigobooks.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import com.bigobooks.dto.OrderCreateRequest;
import com.bigobooks.dto.OrderDto;
import com.bigobooks.entities.orders.Order;
import com.bigobooks.entities.orders.OrderStatus;
import com.bigobooks.exception.GlobalExceptionHandler;
import com.bigobooks.exception.NotFoundException;
import com.bigobooks.mappers.OrderMapper;
import com.bigobooks.services.OrderService;

/**
 * Slice web del endpoint de ventas. Configuracion minima propia (sin
 * JPA/repositorios) que solo registra el controlador y sus dependencias
 * simuladas.
 */
@WebMvcTest
@ContextConfiguration(classes = OrdersControllerTest.OrdersWebTestConfig.class)
class OrdersControllerTest {

	@SpringBootConfiguration
	@Import(GlobalExceptionHandler.class)
	static class OrdersWebTestConfig {

		@Bean
		OrderService orderService() {
			return org.mockito.Mockito.mock(OrderService.class);
		}

		@Bean
		OrderMapper orderMapper() {
			return org.mockito.Mockito.mock(OrderMapper.class);
		}

		@Bean
		OrdersController ordersController(OrderService service, OrderMapper mapper) {
			return new OrdersController(service, mapper);
		}
	}

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private OrderService orderService;

	@Autowired
	private OrderMapper orderMapper;

	@Test
	void creaUnaVenta() throws Exception {
		Order order = new Order();
		order.setId(1L);
		order.setStatus(OrderStatus.PENDING);
		OrderDto dto = new OrderDto();
		dto.setId(1L);
		dto.setStatus(OrderDto.StatusEnum.PENDING);
		dto.setTotalPrice(2000L);
		when(orderService.createFromRequest(any(OrderCreateRequest.class))).thenReturn(order);
		when(orderMapper.toDto(order)).thenReturn(dto);

		mockMvc.perform(post("/orders")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"items\":[{\"bookId\":10,\"quantity\":2}]}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.status").value("PENDING"))
				.andExpect(jsonPath("$.totalPrice").value(2000));
	}

	@Test
	void rechazaUnaVentaSinItems() throws Exception {
		mockMvc.perform(post("/orders")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void obtieneUnaVentaPorId() throws Exception {
		Order order = new Order();
		order.setId(1L);
		OrderDto dto = new OrderDto();
		dto.setId(1L);
		when(orderService.requireById(1L)).thenReturn(order);
		when(orderMapper.toDto(order)).thenReturn(dto);

		mockMvc.perform(get("/orders/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1));
	}

	@Test
	void obtieneUnaVentaInexistente() throws Exception {
		when(orderService.requireById(99L)).thenThrow(new NotFoundException("No existe la venta con id 99"));

		mockMvc.perform(get("/orders/99"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("No existe la venta con id 99"));
	}

	@Test
	void listaVentasPaginadas() throws Exception {
		Page<Order> page = new PageImpl<>(List.of());
		when(orderService.list(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(page);

		mockMvc.perform(get("/orders").param("page", "0").param("size", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isArray())
				.andExpect(jsonPath("$.totalElements").value(0));
	}
}
