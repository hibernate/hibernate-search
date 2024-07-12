/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.search.projection;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;

import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Entity
@Indexed
public class Author {

	@Id
	private Integer id;

	@FullTextField(analyzer = "name", projectable = Projectable.YES)
	private String firstName;

	@FullTextField(analyzer = "name", projectable = Projectable.YES)
	private String lastName;

	@GenericField(projectable = Projectable.YES)
	private LocalDate birthDate;

	@Embedded
	@GenericField(projectable = Projectable.YES)
	private EmbeddableGeoPoint placeOfBirth;

	@ManyToMany(mappedBy = "authors")
	private List<Book> books = new ArrayList<>();

	@ManyToMany(mappedBy = "flattenedAuthors")
	private List<Book> flattenedBooks = new ArrayList<>();

	public Author() {
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public LocalDate getBirthDate() {
		return birthDate;
	}

	public void setBirthDate(LocalDate birthDate) {
		this.birthDate = birthDate;
	}

	public EmbeddableGeoPoint getPlaceOfBirth() {
		return placeOfBirth;
	}

	public void setPlaceOfBirth(EmbeddableGeoPoint placeOfBirth) {
		this.placeOfBirth = placeOfBirth;
	}

	public List<Book> getBooks() {
		return books;
	}

	public void setBooks(List<Book> books) {
		this.books = books;
	}

	public List<Book> getFlattenedBooks() {
		return flattenedBooks;
	}

	public void setFlattenedBooks(List<Book> flattenedBooks) {
		this.flattenedBooks = flattenedBooks;
	}
}
