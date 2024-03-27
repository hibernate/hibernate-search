/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.pojo.standalone.gettingstarted.withouthsearch;

// tag::include[]
import java.util.HashSet;
import java.util.Set;

public class Book {

	private Integer id;

	private String title;

	private String isbn;

	private int pageCount;

	private Set<Author> authors = new HashSet<>();

	public Book() {
	}

	// Getters and setters
	// ...

	// tag::getters-setters[]
	public Integer getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getIsbn() {
		return isbn;
	}

	public void setIsbn(String isbn) {
		this.isbn = isbn;
	}

	public int getPageCount() {
		return pageCount;
	}

	public void setPageCount(int pageCount) {
		this.pageCount = pageCount;
	}

	public Set<Author> getAuthors() {
		return authors;
	}
	// end::getters-setters[]
}
// end::include[]
