package com.bigobooks.services;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bigobooks.entities.rents.RentDetail;
import com.bigobooks.entities.rents.RentExtension;
import com.bigobooks.exception.NotFoundException;
import com.bigobooks.repositories.RentDetailRepository;
import com.bigobooks.repositories.RentExtensionRepository;

/**
 * Detalles de una renta. Sobre un detalle se aplican las extensiones del
 * alquiler (calculo del precio extra y nuevas fechas de devolucion).
 */
@Service
public class RentDetailService extends BaseService<RentDetail, Long, RentDetailRepository> {

	private final RentExtensionRepository extensionRepository;

	public RentDetailService(RentDetailRepository repository, RentExtensionRepository extensionRepository) {
		super(repository);
		this.extensionRepository = extensionRepository;
	}

	public RentDetail requireById(Long id) {
		try {
			return getById(id);
		} catch (IllegalArgumentException e) {
			throw new NotFoundException("No existe el detalle de renta con id " + id);
		}
	}

	@Transactional
	public RentExtension extend(Long rentDetailId, int monthsExtended) {
		if (monthsExtended <= 0) {
			throw new IllegalArgumentException("monthsExtended debe ser mayor a 0");
		}
		RentDetail detail = requireById(rentDetailId);

		long monthly = detail.getMonthsRented() > 0 ? detail.getPrice() / detail.getMonthsRented() : 0L;
		long extraPrice = monthly * monthsExtended;

		RentExtension extension = new RentExtension();
		extension.setRentDetail(detail);
		extension.setMonthsExtended(monthsExtended);
		extension.setExtraPrice(extraPrice);
		extension.setNewReturnDate(detail.getReturnDate() == null ? LocalDate.now().plusMonths(monthsExtended)
				: detail.getReturnDate().plusMonths(monthsExtended));
		extensionRepository.save(extension);

		detail.setReturnDate(extension.getNewReturnDate());
		detail.setPrice(detail.getPrice() + extraPrice);
		repository.save(detail);
		return extension;
	}
}