/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.binding.impl;

import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentFieldValueConvertContext;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;

final class PojoValueBridgeFromDocumentFieldValueConverter<F, V>
		implements FromDocumentFieldValueConverter<F, V> {

	private final ValueBridge<V, F> bridge;
	private final Class<? super V> rawValueType;

	PojoValueBridgeFromDocumentFieldValueConverter(ValueBridge<V, F> bridge, Class<? super V> rawValueType) {
		this.bridge = bridge;
		this.rawValueType = rawValueType;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + bridge + "]";
	}

	@Override
	public V convert(F value, FromDocumentFieldValueConvertContext context) {
		return bridge.fromIndexedValue( value, context.extension( PojoValueBridgeContextExtension.INSTANCE ) );
	}

	@Override
	public boolean isConvertedTypeAssignableTo(Class<?> superTypeCandidate) {
		return superTypeCandidate.isAssignableFrom( rawValueType );
	}

	@Override
	public boolean isCompatibleWith(FromDocumentFieldValueConverter<?, ?> other) {
		if ( other == null || !getClass().equals( other.getClass() ) ) {
			return false;
		}
		PojoValueBridgeFromDocumentFieldValueConverter<?, ?> castedOther =
				(PojoValueBridgeFromDocumentFieldValueConverter<?, ?>) other;
		return bridge.isCompatibleWith( castedOther.bridge )
				&& rawValueType.equals( castedOther.rawValueType );
	}
}
