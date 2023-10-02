/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.search.paths;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Entity
@Indexed
public class Author {

	@Id
	private Integer id;

	@FullTextField(analyzer = "name")
	private String firstName;

	@FullTextField(analyzer = "name")
	private String lastName;

	@ManyToMany(mappedBy = "writers")
	private List<Book> booksAsWriter = new ArrayList<>();

	@ManyToMany(mappedBy = "artists")
	private List<Book> booksAsArtist = new ArrayList<>();

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

	public List<Book> getBooksAsWriter() {
		return booksAsWriter;
	}

	public void setBooksAsWriter(List<Book> booksAsWriter) {
		this.booksAsWriter = booksAsWriter;
	}

	public List<Book> getBooksAsArtist() {
		return booksAsArtist;
	}

	public void setBooksAsArtist(List<Book> booksAsArtist) {
		this.booksAsArtist = booksAsArtist;
	}

}
