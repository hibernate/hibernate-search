/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.gettingstarted.withhsearch.customanalysis;

// tag::include[]
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;

@Entity
public class Author {

	@Id
	@GeneratedValue
	private Integer id;

	@FullTextField(analyzer = "name") // <1>
	private String name;

	@ManyToMany(mappedBy = "authors")
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
