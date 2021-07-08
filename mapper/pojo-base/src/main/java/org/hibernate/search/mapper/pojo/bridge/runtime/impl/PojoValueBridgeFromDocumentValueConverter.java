/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.runtime.impl;

import org.hibernate.search.engine.backend.types.converter.FromDocumentValueConverter;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentValueConvertContext;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;

public final class PojoValueBridgeFromDocumentValueConverter<F, V>
		implements FromDocumentValueConverter<F, V> {

	private final ValueBridge<V, F> bridge;

	public PojoValueBridgeFromDocumentValueConverter(ValueBridge<V, F> bridge) {
		this.bridge = bridge;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + bridge + "]";
	}

	@Override
	public V fromDocumentValue(F value, FromDocumentValueConvertContext context) {
		return bridge.fromIndexedValue( value, context.extension( PojoValueBridgeContextExtension.INSTANCE ) );
	}

	@Override
	public boolean isCompatibleWith(FromDocumentValueConverter<?, ?> other) {
		if ( other == null || !getClass().equals( other.getClass() ) ) {
			return false;
		}
		PojoValueBridgeFromDocumentValueConverter<?, ?> castedOther =
				(PojoValueBridgeFromDocumentValueConverter<?, ?>) other;
		return bridge.isCompatibleWith( castedOther.bridge );
	}
}
