/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.converter.spi;

import org.hibernate.search.engine.backend.document.converter.ToIndexFieldValueConverter;
import org.hibernate.search.util.impl.common.Contracts;

public final class PassThroughToIndexFieldValueConverter<F> implements ToIndexFieldValueConverter<F, F> {
	private final Class<F> valueType;

	public PassThroughToIndexFieldValueConverter(Class<F> valueType) {
		Contracts.assertNotNull( valueType, "valueType" );
		this.valueType = valueType;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + valueType + "]";
	}

	@Override
	public F convert(F value) {
		return value;
	}

	@Override
	public F convertUnknown(Object value) {
		return valueType.cast( value );
	}

	@Override
	public boolean isCompatibleWith(ToIndexFieldValueConverter<?, ?> other) {
		if ( !getClass().equals( other.getClass() ) ) {
			return false;
		}
		PassThroughToIndexFieldValueConverter<?> castedOther = (PassThroughToIndexFieldValueConverter<?>) other;
		return valueType.equals( castedOther.valueType );
	}
}
