/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.binding.identifierbridge.simple;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;

import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.IdentifierBridgeRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

// tag::include[]
@Entity
@Indexed
public class Book {

	@EmbeddedId
	@DocumentId( // <1>
			identifierBridge = @IdentifierBridgeRef(type = BookIdBridge.class) // <2>
	)
	private BookId id = new BookId();

	private String title;

	// Getters and setters
	// ...

	// tag::getters-setters[]
	public BookId getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}
	// end::getters-setters[]
}
// end::include[]
