/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.types.converter.spi;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.backend.types.converter.runtime.spi.FromDocumentIdentifierValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.ToDocumentIdentifierValueConvertContext;
import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class StringDocumentIdentifierValueConverter implements DocumentIdentifierValueConverter<String> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	@Override
	public String convertToDocument(String value, ToDocumentIdentifierValueConvertContext context) {
		return value;
	}

	@Override
	public String convertToDocumentUnknown(Object value, ToDocumentIdentifierValueConvertContext context) {
		return (String) value;
	}

	@Override
	public void checkSourceTypeAssignableTo(Class<?> requiredType) {
		if ( !requiredType.isAssignableFrom( String.class ) ) {
			throw log.wrongRequiredIdentifierType( requiredType, String.class );
		}
	}

	@Override
	public String convertToSource(String documentId, FromDocumentIdentifierValueConvertContext context) {
		return documentId;
	}

	@Override
	public boolean isCompatibleWith(DocumentIdentifierValueConverter<?> other) {
		return getClass().equals( other.getClass() );
	}
}
