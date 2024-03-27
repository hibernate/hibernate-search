/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.search.highlighting;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.search.engine.backend.types.Highlightable;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.TermVector;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

// tag::basics[]
@Entity(name = Book.NAME)
@Indexed
public class Book {

	public static final String NAME = "Book";

	@Id
	private Integer id;

	@FullTextField(analyzer = "english") // <1>
	private String author;

	@FullTextField(analyzer = "english",
			highlightable = { Highlightable.PLAIN, Highlightable.UNIFIED }) // <2>
	private String title;

	@FullTextField(analyzer = "english",
			highlightable = Highlightable.ANY) // <3>
	@Column(length = 10000)
	private String description;

	@FullTextField(analyzer = "english",
			projectable = Projectable.YES,
			termVector = TermVector.WITH_POSITIONS_OFFSETS) // <4>
	@Column(length = 10000)
	@ElementCollection
	private List<String> text;

	@GenericField // <5>
	@Column(length = 10000)
	@ElementCollection
	private List<String> keywords;


	// end::basics[]
	public Book() {
	}

	public Book(Integer id, String author, String title, String description, List<String> text, List<String> keywords) {
		this.id = id;
		this.author = author;
		this.title = title;
		this.description = description;
		this.text = text;
		this.keywords = keywords;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
	// tag::basics[]
}
// end::basics[]
