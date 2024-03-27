/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.proxy;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import org.hibernate.annotations.Proxy;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Fields;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Store;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Indexed(index = "Book")
@Proxy(proxyClass = IBook.class)
public class Book implements IBook {

	private Integer id;
	private String body;
	private String summary;
	private Set<IAuthor> authors = new HashSet<IAuthor>();

	public Book() {
	}

	public Book(Integer id, String summary, String body) {
		this.id = id;
		this.summary = summary;
		this.body = body;
	}

	@Id
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@OneToMany(targetEntity = Author.class, mappedBy = "book", cascade = CascadeType.ALL)
	@IndexedEmbedded(targetElement = Author.class)
	public Set<IAuthor> getAuthors() {
		return authors;
	}

	public void setAuthors(Set<IAuthor> authors) {
		this.authors = authors;
	}

	@Field
	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
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
}
