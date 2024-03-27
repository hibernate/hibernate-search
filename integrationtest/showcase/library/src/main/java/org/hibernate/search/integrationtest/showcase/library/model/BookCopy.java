/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.showcase.library.model;

import jakarta.persistence.Entity;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;

/**
 * A concrete copy of a book document.
 *
 * @see DocumentCopy
 */
@Entity
public class BookCopy extends DocumentCopy<Book> {

	@GenericField
	private BookMedium medium;

	public BookMedium getMedium() {
		return medium;
	}

	public void setMedium(BookMedium medium) {
		this.medium = medium;
	}
}
