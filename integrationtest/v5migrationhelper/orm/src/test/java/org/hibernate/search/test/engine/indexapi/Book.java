/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.engine.indexapi;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.DateBridge;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Fields;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;

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
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Fields({
			@Field(store = Store.YES),
			@Field(name = "summary_forSort", analyze = Analyze.NO, store = Store.YES)
	})
	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	@Field(analyze = Analyze.NO, store = Store.YES)
	@DateBridge(resolution = Resolution.SECOND)
	public Date getPublicationDate() {
		return publicationDate;
	}

	public void setPublicationDate(Date publicationDate) {
		this.publicationDate = publicationDate;
	}
}
