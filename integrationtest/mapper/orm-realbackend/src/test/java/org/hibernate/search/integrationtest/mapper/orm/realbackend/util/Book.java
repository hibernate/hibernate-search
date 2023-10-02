/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.realbackend.util;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Entity(name = Book.NAME)
@Indexed
public class Book {
	public static final String NAME = "book";
	@Id
	private Integer id;

	private String title;

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

}
