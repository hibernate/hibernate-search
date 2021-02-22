/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.types.converter.spi;

import org.hibernate.search.engine.backend.types.converter.runtime.spi.ToDocumentIdentifierValueConvertContext;

public final class StringDocumentIdentifierValueConverter implements DocumentIdentifierValueConverter<String> {

	@Override
	public String convertToDocument(String value, ToDocumentIdentifierValueConvertContext context) {
		return value;
	}

	@Override
	public String convertToDocumentUnknown(Object value, ToDocumentIdentifierValueConvertContext context) {
		return (String) value;
	}

	@Override
	public boolean isCompatibleWith(DocumentIdentifierValueConverter<?> other) {
		return getClass().equals( other.getClass() );
	}
}
