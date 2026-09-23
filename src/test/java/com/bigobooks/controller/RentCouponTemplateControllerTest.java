package com.bigobooks.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
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

import com.bigobooks.dto.RentCouponTemplateDto;
import com.bigobooks.entities.rents.RentCouponTemplate;
import com.bigobooks.exception.GlobalExceptionHandler;
import com.bigobooks.mappers.RentCouponTemplateMapper;
import com.bigobooks.services.RentCouponTemplateService;

/**
 * Slice web del endpoint de plantillas de cupones. Se usa una configuracion
 * minima propia (sin JPA/repositorios) que solo registra el controlador y sus
 * dependencias simuladas.
 */
@WebMvcTest
@ContextConfiguration(classes = RentCouponTemplateControllerTest.RentWebTestConfig.class)
class RentCouponTemplateControllerTest {

	@SpringBootConfiguration
	@Import(GlobalExceptionHandler.class)
	static class RentWebTestConfig {

		@Bean
		RentCouponTemplateService templateService() {
			return mock(RentCouponTemplateService.class);
		}

		@Bean
		RentCouponTemplateMapper templateMapper() {
			return mock(RentCouponTemplateMapper.class);
		}

		@Bean
		RentCouponTemplateController rentCouponTemplateController(RentCouponTemplateService service,
				RentCouponTemplateMapper mapper) {
			return new RentCouponTemplateController(service, mapper);
		}
	}

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private RentCouponTemplateService templateService;

	@Autowired
	private RentCouponTemplateMapper templateMapper;

	@Test
	void listaLasPlantillas() throws Exception {
		Page<RentCouponTemplate> page = new PageImpl<>(List.of());
		when(templateService.list(isNull(), isNull(), isNull(), anyInt(), anyInt(), isNull())).thenReturn(page);

		mockMvc.perform(get("/rent-coupon-templates").param("page", "0").param("size", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isArray())
				.andExpect(jsonPath("$.totalElements").value(0));
	}

	@Test
	void creaUnaPlantilla() throws Exception {
		RentCouponTemplate template = new RentCouponTemplate();
		RentCouponTemplateDto dto = new RentCouponTemplateDto();
		dto.setId(1L);
		dto.setCode("VERANO10");
		dto.setDiscountPercentage(10);

		when(templateMapper.toEntity(any())).thenReturn(template);
		when(templateService.create(any())).thenReturn(template);
		when(templateMapper.toDto(template)).thenReturn(dto);

		mockMvc.perform(post("/rent-coupon-templates")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"code\":\"VERANO10\",\"discountPercentage\":10}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.code").value("VERANO10"));
	}
}