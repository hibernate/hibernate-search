/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.runtime.impl;

import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentFieldValueConvertContext;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;

public final class PojoValueBridgeFromDocumentFieldValueConverter<F, V>
		implements FromDocumentFieldValueConverter<F, V> {

	private final ValueBridge<V, F> bridge;

	public PojoValueBridgeFromDocumentFieldValueConverter(ValueBridge<V, F> bridge) {
		this.bridge = bridge;
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
	public boolean isCompatibleWith(FromDocumentFieldValueConverter<?, ?> other) {
		if ( other == null || !getClass().equals( other.getClass() ) ) {
			return false;
		}
		PojoValueBridgeFromDocumentFieldValueConverter<?, ?> castedOther =
				(PojoValueBridgeFromDocumentFieldValueConverter<?, ?>) other;
		return bridge.isCompatibleWith( castedOther.bridge );
	}
}
