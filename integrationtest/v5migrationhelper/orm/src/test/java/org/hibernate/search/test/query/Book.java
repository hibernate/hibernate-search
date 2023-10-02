/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.query;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.DateBridge;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Fields;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.annotations.SortableField;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Indexed(index = "Book")
public class Book {

	private Integer id;
	private String body;
	private String summary;
	private Set<Author> authors = new HashSet<Author>();
	private Author mainAuthor;
	private Date publicationDate;

	@IndexedEmbedded
	@ManyToOne
	@IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
	public Author getMainAuthor() {
		return mainAuthor;
	}

	public void setMainAuthor(Author mainAuthor) {
		this.mainAuthor = mainAuthor;
	}

	@ManyToMany
	public Set<Author> getAuthors() {
		return authors;
	}

	public void setAuthors(Set<Author> authors) {
		this.authors = authors;
	}

	public Book() {
	}

	public Book(Integer id, String summary, String body) {
		this.id = id;
		this.summary = summary;
		this.body = body;
	}

	@Field
	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	@Id
	@DocumentId
	@Field(store = Store.YES)
	@Field(
			name = "id_forIntegerSort",
			store = Store.NO,
			index = Index.NO
	)
	@NumericField
	@SortableField(forField = "id_forIntegerSort")
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Transient
	@Field(name = "id_forStringSort", analyze = Analyze.NO)
	@SortableField(forField = "id_forStringSort")
	@IndexingDependency(derivedFrom = @ObjectPath(@PropertyValue(propertyName = "id")))
	public String getIdAsString() {
		return String.valueOf( id );
	}

	@Fields({
			@Field(store = Store.YES),
			@Field(name = "summary_forSort", analyze = Analyze.NO, store = Store.YES)
	})
	@SortableField(forField = "summary_forSort")
	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	@SortableField
	@Field(analyze = Analyze.NO, store = Store.YES)
	@DateBridge(resolution = Resolution.SECOND)
	public Date getPublicationDate() {
		return publicationDate;
	}

	public void setPublicationDate(Date publicationDate) {
		this.publicationDate = publicationDate;
	}
}
