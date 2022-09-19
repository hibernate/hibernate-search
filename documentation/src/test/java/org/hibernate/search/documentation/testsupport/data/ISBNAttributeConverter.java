/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
