package com.bigobooks.mappers;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.bigobooks.dto.RentCouponDto;
import com.bigobooks.dto.RentDetailDto;
import com.bigobooks.dto.RentDto;
import com.bigobooks.entities.rents.Rent;

@Component
public class RentMapper {

	private final RentDetailMapper rentDetailMapper;
	private final RentCouponMapper rentCouponMapper;

	public RentMapper(RentDetailMapper rentDetailMapper, RentCouponMapper rentCouponMapper) {
		this.rentDetailMapper = rentDetailMapper;
		this.rentCouponMapper = rentCouponMapper;
	}

	public RentDto toDto(Rent rent) {
		RentDto dto = new RentDto();
		dto.setId(rent.getId());
		dto.setUserId(rent.getUserAccount() != null ? rent.getUserAccount().getId() : null);
		dto.setTotalPrice(rent.getTotalPrice());
		dto.setCreatedAt(rent.getCreatedAt());
		dto.setRentDetails(toDetailDtos(rent));
		dto.setRentCoupons(toCouponDtos(rent));
		return dto;
	}

	private List<RentDetailDto> toDetailDtos(Rent rent) {
		if (rent.getRentDetails() == null) {
			return new ArrayList<>();
		}
		return rent.getRentDetails().stream().map(rentDetailMapper::toDto).toList();
	}

	private List<RentCouponDto> toCouponDtos(Rent rent) {
		if (rent.getRentCoupons() == null) {
			return new ArrayList<>();
		}
		return rent.getRentCoupons().stream().map(rentCouponMapper::toDto).toList();
	}
}