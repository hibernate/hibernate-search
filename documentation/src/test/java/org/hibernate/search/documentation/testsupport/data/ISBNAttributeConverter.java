/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.testsupport.data;

import jakarta.persistence.AttributeConverter;

public class ISBNAttributeConverter implements AttributeConverter<ISBN, String> {

	@Override
	public String convertToDatabaseColumn(ISBN attribute) {
		return attribute == null ? null : attribute.getStringValue();
	}

	@Override
	public ISBN convertToEntityAttribute(String dbData) {
		return dbData == null ? null : ISBN.parse( dbData );
	}
}
