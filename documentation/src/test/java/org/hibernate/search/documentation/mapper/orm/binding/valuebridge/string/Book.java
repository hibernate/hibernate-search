/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.binding.valuebridge.string;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBridgeRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

// tag::include[]
@Entity
@Indexed
public class Book {

	@Id
	@GeneratedValue
	private Integer id;

	@Convert(converter = ISBNAttributeConverter.class) // <1>
	@GenericField( // <2>
			valueBridge = @ValueBridgeRef(type = ISBNValueBridge.class), // <3>
			projectable = Projectable.YES // <4>
	)
	private ISBN isbn;

	// Getters and setters
	// ...

	// tag::getters-setters[]
	public Integer getId() {
		return id;
	}

	public ISBN getIsbn() {
		return isbn;
	}

	public void setIsbn(ISBN isbn) {
		this.isbn = isbn;
	}
	// end::getters-setters[]
}
// end::include[]
