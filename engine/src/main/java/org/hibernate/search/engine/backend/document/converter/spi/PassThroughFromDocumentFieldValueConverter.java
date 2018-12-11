/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.converter.spi;

import org.hibernate.search.engine.backend.document.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.document.converter.runtime.FromDocumentFieldValueConvertContext;

public final class PassThroughFromDocumentFieldValueConverter<F> implements FromDocumentFieldValueConverter<F, F> {

	private final Class<F> fieldType;

	public PassThroughFromDocumentFieldValueConverter(Class<F> fieldType) {
		this.fieldType = fieldType;
	}

	@Override
	public F convert(F value, FromDocumentFieldValueConvertContext context) {
		return value;
	}

	@Override
	public boolean isConvertedTypeAssignableTo(Class<?> superTypeCandidate) {
		return superTypeCandidate.isAssignableFrom( fieldType );
	}

	@Override
	public boolean isCompatibleWith(FromDocumentFieldValueConverter<?, ?> other) {
		if ( !getClass().equals( other.getClass() ) ) {
			return false;
		}
		PassThroughFromDocumentFieldValueConverter<?> castedOther = (PassThroughFromDocumentFieldValueConverter<?>) other;
		return fieldType.equals( castedOther.fieldType );
	}
}
