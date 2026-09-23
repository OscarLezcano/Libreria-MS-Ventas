package com.bigobooks.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bigobooks.dto.RentCreateRequest;
import com.bigobooks.dto.RentItemCreate;
import com.bigobooks.dto.RentUpdateRequest;
import com.bigobooks.entities.auth.UserAccount;
import com.bigobooks.entities.book.Book;
import com.bigobooks.entities.rents.Rent;
import com.bigobooks.exception.NotFoundException;
import com.bigobooks.repositories.RentDetailRepository;
import com.bigobooks.repositories.RentRepository;

@ExtendWith(MockitoExtension.class)
class RentServiceTest {

	@Mock
	private RentRepository repository;
	@Mock
	private BookService bookService;
	@Mock
	private UserAccountService userAccountService;
	@Mock
	private RentCouponService rentCouponService;
	@Mock
	private RentDetailRepository rentDetailRepository;

	@InjectMocks
	private RentService rentService;

	@Test
	void createFromRequestCalculaPrecioYDevolucion() {
		when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
		when(rentDetailRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		Book book = new Book();
		book.setId(10L);
		book.setRentPrice(1000L);
		when(bookService.requireById(10L)).thenReturn(book);

		RentItemCreate item = new RentItemCreate();
		item.setBookId(10L);
		item.setMonthsRented(2);
		RentCreateRequest request = new RentCreateRequest();
		request.setItems(List.of(item));

		Rent rent = rentService.createFromRequest(request);

		assertEquals(2000, rent.getTotalPrice());
		assertEquals(1, rent.getRentDetails().size());
		assertEquals(2000L, rent.getRentDetails().get(0).getPrice());
		assertEquals(LocalDate.now().plusMonths(2), rent.getRentDetails().get(0).getReturnDate());
		assertNull(rent.getUserAccount());
		verify(rentCouponService, never()).applyCoupon(any(), any());
	}

	@Test
	void createFromRequestRequiereItems() {
		RentCreateRequest request = new RentCreateRequest();
		request.setItems(List.of());

		assertThrows(IllegalArgumentException.class, () -> rentService.createFromRequest(request));
	}

	@Test
	void createFromRequestConCuponInvocaAplicacionConCodigo() {
		when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
		when(rentDetailRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		Book book = new Book();
		book.setId(10L);
		book.setRentPrice(1000L);
		when(bookService.requireById(10L)).thenReturn(book);

		RentItemCreate item = new RentItemCreate();
		item.setBookId(10L);
		item.setMonthsRented(1);
		RentCreateRequest request = new RentCreateRequest();
		request.setItems(List.of(item));
		request.setCouponCode("VERANO10");

		Rent rent = rentService.createFromRequest(request);

		verify(rentCouponService).applyCoupon(eq(rent), eq("VERANO10"));
	}

	@Test
	void createFromRequestNoEncuentraLibro() {
		when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
		when(bookService.requireById(99L)).thenThrow(new NotFoundException("No existe el libro con id 99"));

		RentItemCreate item = new RentItemCreate();
		item.setBookId(99L);
		item.setMonthsRented(1);
		RentCreateRequest request = new RentCreateRequest();
		request.setItems(List.of(item));

		assertThrows(NotFoundException.class, () -> rentService.createFromRequest(request));
	}

	@Test
	void updateUserAsignaYEliminaUsuario() {
		Rent rent = new Rent();
		when(repository.findById(1L)).thenReturn(Optional.of(rent));
		when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		UserAccount user = new UserAccount();
		user.setId(5L);
		when(userAccountService.requireById(5L)).thenReturn(user);

		RentUpdateRequest withUser = new RentUpdateRequest();
		withUser.setUserId(5L);
		Rent updated = rentService.updateUser(1L, withUser);
		assertEquals(user, updated.getUserAccount());

		RentUpdateRequest withoutUser = new RentUpdateRequest();
		withoutUser.setUserId(null);
		Rent cleared = rentService.updateUser(1L, withoutUser);
		assertNull(cleared.getUserAccount());
	}

	@Test
	void deleteMarcaEliminacionLogica() {
		Rent rent = new Rent();
		when(repository.findById(1L)).thenReturn(Optional.of(rent));
		when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		rentService.delete(1L);

		assertTrue(rent.isDeleted());
		verify(repository).save(any());
	}

	@Test
	void requireByIdNoEncontradoLanzaNotFoundException() {
		when(repository.findById(anyLong())).thenReturn(Optional.empty());

		assertThrows(NotFoundException.class, () -> rentService.requireById(404L));
	}
}