/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.showcase.library.attributeconverter;

import jakarta.persistence.AttributeConverter;

import org.hibernate.search.integrationtest.showcase.library.model.ISBN;

public class ISBNAttributeConverter implements AttributeConverter<ISBN, String> {

	@Override
	public String convertToDatabaseColumn(ISBN attribute) {
		return attribute == null ? null : attribute.getStringValue();
	}

	@Override
	public ISBN convertToEntityAttribute(String dbData) {
		return dbData == null ? null : new ISBN( dbData );
	}
}
