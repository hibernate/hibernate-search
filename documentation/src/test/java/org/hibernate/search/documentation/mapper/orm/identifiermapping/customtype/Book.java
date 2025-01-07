/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.identifiermapping.customtype;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.Type;
import org.hibernate.search.documentation.testsupport.data.ISBN;
import org.hibernate.search.documentation.testsupport.data.ISBNUserType;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.IdentifierBridgeRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

//tag::include[]
@Entity
@Indexed
public class Book {

	@Id
	@Type(ISBNUserType.class)
	@DocumentId(identifierBridge = @IdentifierBridgeRef(type = ISBNIdentifierBridge.class))
	private ISBN isbn;

	public Book() {
	}

	// Getters and setters
	// ...

	//tag::getters-setters[]
	public ISBN getIsbn() {
		return isbn;
	}

	public void setIsbn(ISBN isbn) {
		this.isbn = isbn;
	}
	//end::getters-setters[]
}
//end::include[]
