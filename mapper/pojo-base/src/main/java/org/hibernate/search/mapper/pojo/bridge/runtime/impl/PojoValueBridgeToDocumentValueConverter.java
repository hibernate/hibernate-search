/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.runtime.impl;


import org.hibernate.search.engine.backend.types.converter.ToDocumentValueConverter;
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentValueConvertContext;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;

public final class PojoValueBridgeToDocumentValueConverter<V, F>
		implements ToDocumentValueConverter<V, F> {

	private final ValueBridge<V, F> bridge;

	public PojoValueBridgeToDocumentValueConverter(ValueBridge<V, F> bridge) {
		this.bridge = bridge;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[bridge=" + bridge + "]";
	}

	@Override
	public F toDocumentValue(V value, ToDocumentValueConvertContext context) {
		return bridge.toIndexedValue( value, context.extension( PojoValueBridgeContextExtension.INSTANCE ) );
	}

	@Override
	public boolean isCompatibleWith(ToDocumentValueConverter<?, ?> other) {
		if ( other == null || !getClass().equals( other.getClass() ) ) {
			return false;
		}
		PojoValueBridgeToDocumentValueConverter<?, ?> castedOther =
				(PojoValueBridgeToDocumentValueConverter<?, ?>) other;
		return bridge.isCompatibleWith( castedOther.bridge );
	}
}
