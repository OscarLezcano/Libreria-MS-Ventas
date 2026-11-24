package com.bigobooks.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import com.bigobooks.dto.Envelope;
import com.bigobooks.dto.PromotionBookAdd;
import com.bigobooks.dto.PromotionCreateRequest;
import com.bigobooks.dto.PromotionDto;
import com.bigobooks.dto.PromotionUpdateRequest;
import com.bigobooks.mappers.PromotionMapper;
import com.bigobooks.model.Promotion;
import com.bigobooks.services.PromotionQueryService;
import com.bigobooks.services.PromotionService;
import com.bigobooks.util.Envelopes;

/**
 * Controlador de promociones: implementa la interfaz generada desde
 * openapi.yaml.
 */
@RestController
public class PromotionsController implements PromotionsApi {

	private static final Logger log = LoggerFactory.getLogger(PromotionsController.class);

	private final PromotionService promotionService;
	private final PromotionQueryService promotionQueryService;
	private final PromotionMapper promotionMapper;

	public PromotionsController(PromotionService promotionService, PromotionQueryService promotionQueryService,
			PromotionMapper promotionMapper) {
		this.promotionService = promotionService;
		this.promotionQueryService = promotionQueryService;
		this.promotionMapper = promotionMapper;
	}

	@Override
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Envelope> createPromotion(PromotionCreateRequest promotionCreateRequest) {
		log.info("POST /promotions: creacion de promocion recibida");
		Promotion created = promotionService.createFromRequest(promotionCreateRequest);
		return new ResponseEntity<>(Envelopes.single(promotionMapper.toDto(created)), HttpStatus.CREATED);
	}

	@Override
	@PreAuthorize("hasAnyRole('ADMIN','SELLER','WAREHOUSE','READER')")
	public ResponseEntity<Envelope> listPromotions(String name, Boolean active, Integer minDiscountPercent,
			Long bookId, Integer page, Integer size, String sort) {
		log.info("GET /promotions: listado de promociones (page={}, size={}, sort={})", page, size, sort);
		Page<Promotion> promotions = promotionService.list(name, active, minDiscountPercent, bookId, page, size, sort);
		List<PromotionDto> data = promotions.map(promotionMapper::toDto).getContent();
		return ResponseEntity.ok(Envelopes.page(promotions, data));
	}

	@Override
	@PreAuthorize("hasAnyRole('ADMIN','SELLER','WAREHOUSE','READER')")
	public ResponseEntity<Envelope> getPromotionById(Long id) {
		log.info("GET /promotions/{}: detalle de promocion", id);
		return ResponseEntity.ok(Envelopes.single(promotionQueryService.getById(id)));
	}

	@Override
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Envelope> updatePromotion(Long id,
			PromotionUpdateRequest promotionUpdateRequest) {
		log.info("PUT /promotions/{}: actualizacion de promocion recibida", id);
		return ResponseEntity.ok(Envelopes.single(promotionMapper.toDto(promotionService.update(id, promotionUpdateRequest))));
	}

	@Override
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Envelope> addBooksToPromotion(Long id, PromotionBookAdd promotionBookAdd) {
		log.info("POST /promotions/{}/books: alta de libros en la promocion recibida", id);
		return new ResponseEntity<>(Envelopes.single(promotionMapper.toDto(promotionService.addBooks(id, promotionBookAdd))),
				HttpStatus.CREATED);
	}

	@Override
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> removeBookFromPromotion(Long id, Long bookId) {
		log.info("DELETE /promotions/{}/books/{}: baja de libro de la promocion recibida", id, bookId);
		promotionService.removeBook(id, bookId);
		return ResponseEntity.noContent().build();
	}

	@Override
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> deletePromotion(Long id) {
		log.info("DELETE /promotions/{}: borrado logico de promocion recibido", id);
		promotionService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
