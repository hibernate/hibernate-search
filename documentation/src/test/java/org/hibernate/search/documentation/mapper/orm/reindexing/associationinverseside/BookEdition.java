/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.reindexing.associationinverseside;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;

//tag::include[]
@Entity
public class BookEdition {

	@Id
	@GeneratedValue
	private Integer id;

	@ManyToOne // <4>
	private Book book;

	@FullTextField(analyzer = "english")
	private String label;

	public BookEdition() {
	}

	// Getters and setters
	// ...

	//tag::getters-setters[]
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Book getBook() {
		return book;
	}

	public void setBook(Book book) {
		this.book = book;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}
	//end::getters-setters[]
}
//end::include[]
