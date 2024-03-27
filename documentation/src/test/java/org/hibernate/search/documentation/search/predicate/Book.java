/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.search.predicate;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;

import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;

@Entity
@Indexed
public class Book {

	@Id
	private Integer id;

	@FullTextField(analyzer = "english")
	@FullTextField(name = "title_autocomplete", analyzer = "autocomplete_indexing")
	private String title;

	@FullTextField(analyzer = "english")
	private String description;

	@GenericField
	private Integer pageCount;

	@KeywordField
	private Genre genre;

	@FullTextField(analyzer = "english")
	private String comment;

	private float[] coverImageEmbeddings;
	private float[] alternativeCoverImageEmbeddings;

	@ManyToMany
	@IndexedEmbedded(structure = ObjectStructure.NESTED)
	private List<Author> authors = new ArrayList<>();

	public Book() {
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

	public Integer getPageCount() {
		return pageCount;
	}

	public void setPageCount(Integer pageCount) {
		this.pageCount = pageCount;
	}

	public Genre getGenre() {
		return genre;
	}

	public void setGenre(Genre genre) {
		this.genre = genre;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public float[] getCoverImageEmbeddings() {
		return coverImageEmbeddings;
	}

	public void setCoverImageEmbeddings(float[] coverImageEmbeddings) {
		this.coverImageEmbeddings = coverImageEmbeddings;
	}

	public float[] getAlternativeCoverImageEmbeddings() {
		return alternativeCoverImageEmbeddings;
	}

	public void setAlternativeCoverImageEmbeddings(float[] alternativeCoverImageEmbeddings) {
		this.alternativeCoverImageEmbeddings = alternativeCoverImageEmbeddings;
	}

	public List<Author> getAuthors() {
		return authors;
	}

	public void setAuthors(List<Author> authors) {
		this.authors = authors;
	}
}
