/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.types.converter.spi;

import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentFieldValueConvertContext;
import org.hibernate.search.util.common.impl.Contracts;

public final class PassThroughToDocumentFieldValueConverter<F> implements ToDocumentFieldValueConverter<F, F> {
	private final Class<F> valueType;

	public PassThroughToDocumentFieldValueConverter(Class<F> valueType) {
		Contracts.assertNotNull( valueType, "valueType" );
		this.valueType = valueType;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + valueType + "]";
	}

	@Override
	public boolean isValidInputType(Class<?> inputTypeCandidate) {
		return valueType.isAssignableFrom( inputTypeCandidate );
	}

	@Override
	public F convert(F value, ToDocumentFieldValueConvertContext context) {
		return value;
	}

	@Override
	public F convertUnknown(Object value, ToDocumentFieldValueConvertContext context) {
		return valueType.cast( value );
	}

	@Override
	public boolean isCompatibleWith(ToDocumentFieldValueConverter<?, ?> other) {
		if ( !getClass().equals( other.getClass() ) ) {
			return false;
		}
		PassThroughToDocumentFieldValueConverter<?> castedOther = (PassThroughToDocumentFieldValueConverter<?>) other;
		return valueType.equals( castedOther.valueType );
	}
}
