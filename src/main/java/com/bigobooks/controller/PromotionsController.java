package com.bigobooks.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.bigobooks.dto.PageResponseDto;
import com.bigobooks.dto.PromotionBookAdd;
import com.bigobooks.dto.PromotionCreateRequest;
import com.bigobooks.dto.PromotionDto;
import com.bigobooks.dto.PromotionUpdateRequest;
import com.bigobooks.mappers.PromotionMapper;
import com.bigobooks.model.Promotion;
import com.bigobooks.services.PromotionService;

/**
 * Controlador de promociones: implementa la interfaz generada desde
 * openapi.yaml.
 */
@RestController
public class PromotionsController implements PromotionsApi {

	private static final Logger log = LoggerFactory.getLogger(PromotionsController.class);

	private final PromotionService promotionService;
	private final PromotionMapper promotionMapper;

	public PromotionsController(PromotionService promotionService, PromotionMapper promotionMapper) {
		this.promotionService = promotionService;
		this.promotionMapper = promotionMapper;
	}

	@Override
	public ResponseEntity<PromotionDto> createPromotion(PromotionCreateRequest promotionCreateRequest) {
		log.info("POST /promotions: creacion de promocion recibida");
		Promotion created = promotionService.createFromRequest(promotionCreateRequest);
		return new ResponseEntity<>(promotionMapper.toDto(created), HttpStatus.CREATED);
	}

	@Override
	public ResponseEntity<PageResponseDto> listPromotions(String name, Boolean active, Integer minDiscountPercent,
			Long bookId, Integer page, Integer size, String sort) {
		log.info("GET /promotions: listado de promociones (page={}, size={}, sort={})", page, size, sort);
		Page<Promotion> promotions = promotionService.list(name, active, minDiscountPercent, bookId, page, size, sort);
		return ResponseEntity.ok(PageResponseDto.from(promotions.map(promotionMapper::toDto)));
	}

	@Override
	public ResponseEntity<PromotionDto> getPromotionById(Long id) {
		log.info("GET /promotions/{}: detalle de promocion", id);
		return ResponseEntity.ok(promotionMapper.toDto(promotionService.requireById(id)));
	}

	@Override
	public ResponseEntity<PromotionDto> updatePromotion(Long id,
			PromotionUpdateRequest promotionUpdateRequest) {
		log.info("PUT /promotions/{}: actualizacion de promocion recibida", id);
		return ResponseEntity.ok(promotionMapper.toDto(promotionService.update(id, promotionUpdateRequest)));
	}

	@Override
	public ResponseEntity<PromotionDto> addBooksToPromotion(Long id, PromotionBookAdd promotionBookAdd) {
		log.info("POST /promotions/{}/books: alta de libros en la promocion recibida", id);
		return new ResponseEntity<>(promotionMapper.toDto(promotionService.addBooks(id, promotionBookAdd)),
				HttpStatus.CREATED);
	}

	@Override
	public ResponseEntity<Void> removeBookFromPromotion(Long id, Long bookId) {
		log.info("DELETE /promotions/{}/books/{}: baja de libro de la promocion recibida", id, bookId);
		promotionService.removeBook(id, bookId);
		return ResponseEntity.noContent().build();
	}

	@Override
	public ResponseEntity<Void> deletePromotion(Long id) {
		log.info("DELETE /promotions/{}: borrado logico de promocion recibido", id);
		promotionService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
