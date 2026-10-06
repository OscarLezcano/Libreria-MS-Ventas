package com.bigobooks.model;

import java.time.LocalDate;
import java.util.List;

import com.bigobooks.entities.BaseEntity;
import com.bigobooks.entities.book.Book;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Entidad propia del microservicio de ventas: promocion con un descuento
 * aplicado sobre un conjunto de libros (relacion ManyToMany con Book).
 */
@Entity
@Table(name = "sales_promotion")
@Getter
@Setter
public class Promotion extends BaseEntity {

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private int discountPercent;

	@Column(nullable = false)
	private LocalDate startDate;

	@Column(nullable = false)
	private LocalDate endDate;

	@ManyToMany
	@JoinTable(name = "sales_promotion_book", joinColumns = @JoinColumn(name = "promotion_id"), inverseJoinColumns = @JoinColumn(name = "book_id"))
	private List<Book> books;
}
