/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.binding.valuebridge.string;

import jakarta.persistence.AttributeConverter;

public class ISBNAttributeConverter implements AttributeConverter<ISBN, Long> {

	@Override
	public Long convertToDatabaseColumn(ISBN attribute) {
		return attribute == null ? null : attribute.getId();
	}

	@Override
	public ISBN convertToEntityAttribute(Long dbData) {
		return dbData == null ? null : new ISBN( dbData );
	}
}
