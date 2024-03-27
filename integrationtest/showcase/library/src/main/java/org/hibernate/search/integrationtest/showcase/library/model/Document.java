/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.showcase.library.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.OneToMany;

import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.integrationtest.showcase.library.analysis.LibraryAnalyzers;
import org.hibernate.search.integrationtest.showcase.library.bridge.annotation.MultiKeywordStringBinding;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;

/**
 * Information about a document (book, video, ...) that can be available in a library catalog.
 *
 * @param <C> The type of document copies.
 */
@Entity
@Indexed
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class Document<C extends DocumentCopy<?>> extends AbstractEntity<Integer> {

	@Id
	private Integer id;

	@Basic
	@FullTextField
	@KeywordField(
			name = "title_sort",
			normalizer = LibraryAnalyzers.NORMALIZER_SORT,
			sortable = Sortable.YES
	)
	private String title;

	@GenericField(projectable = Projectable.YES, sortable = Sortable.YES)
	private String author;

	@Basic
	@FullTextField
	private String summary;

	/**
	 * Comma-separated tags.
	 */
	@Basic
	@MultiKeywordStringBinding(fieldName = "tags")
	private String tags;

	@OneToMany(mappedBy = "document", targetEntity = DocumentCopy.class)
	@IndexedEmbedded(includePaths = { "medium", "library.location", "library.services" }, structure = ObjectStructure.NESTED)
	private List<C> copies = new ArrayList<>();

	public Document() {
	}

	public Document(int id, String title, String author, String summary, String tags) {
		this.id = id;
		this.title = title;
		this.author = author;
		this.summary = summary;
		this.tags = tags;
	}

	@Override
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

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	public String getTags() {
		return tags;
	}

	public void setTags(String tags) {
		this.tags = tags;
	}

	public List<C> getCopies() {
		return copies;
	}

	public void setCopies(List<C> copies) {
		this.copies = copies;
	}

	@Override
	protected String getDescriptionForToString() {
		return getTitle();
	}
}
