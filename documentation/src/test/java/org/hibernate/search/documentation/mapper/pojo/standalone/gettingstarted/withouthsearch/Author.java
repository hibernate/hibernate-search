/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.pojo.standalone.gettingstarted.withouthsearch;

// tag::include[]
import java.util.HashSet;
import java.util.Set;

public class Author {

	private Integer id;

	private String name;

	private Set<Book> books = new HashSet<>();

	public Author() {
	}

	// Getters and setters
	// ...

	// tag::getters-setters[]
	public Integer getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set<Book> getBooks() {
		return books;
	}
	// end::getters-setters[]
}
// end::include[]
