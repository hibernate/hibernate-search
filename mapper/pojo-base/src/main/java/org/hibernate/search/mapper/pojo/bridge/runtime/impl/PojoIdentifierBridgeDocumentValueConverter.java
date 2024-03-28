/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.runtime.impl;

import org.hibernate.search.engine.backend.types.converter.FromDocumentValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentValueConverter;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentValueConvertContext;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;

public final class PojoIdentifierBridgeDocumentValueConverter<I>
		implements ToDocumentValueConverter<I, String>, FromDocumentValueConverter<String, I> {

	private final IdentifierBridge<I> bridge;

	public PojoIdentifierBridgeDocumentValueConverter(IdentifierBridge<I> bridge) {
		this.bridge = bridge;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + bridge + "]";
	}

	@Override
	public String toDocumentValue(I value, ToDocumentValueConvertContext context) {
		return bridge.toDocumentIdentifier( value, context.extension( PojoIdentifierBridgeContextExtension.INSTANCE ) );
	}

	@Override
	public I fromDocumentValue(String value, FromDocumentValueConvertContext context) {
		return bridge.fromDocumentIdentifier( value, context.extension( PojoIdentifierBridgeContextExtension.INSTANCE ) );
	}

	@Override
	public boolean isCompatibleWith(ToDocumentValueConverter<?, ?> other) {
		if ( other == null || !getClass().equals( other.getClass() ) ) {
			return false;
		}
		PojoIdentifierBridgeDocumentValueConverter<?> castedOther =
				(PojoIdentifierBridgeDocumentValueConverter<?>) other;
		return bridge.isCompatibleWith( castedOther.bridge );
	}

	@Override
	public boolean isCompatibleWith(FromDocumentValueConverter<?, ?> other) {
		if ( other == null || !getClass().equals( other.getClass() ) ) {
			return false;
		}
		PojoIdentifierBridgeDocumentValueConverter<?> castedOther =
				(PojoIdentifierBridgeDocumentValueConverter<?>) other;
		return bridge.isCompatibleWith( castedOther.bridge );
	}

}
