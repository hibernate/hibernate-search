/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.identifiertovalue.impl;

import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeFromIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

final class IdentifierBridgeToValueBridgeAdapter<I> implements ValueBridge<I, String> {
	private final IdentifierBridge<I> delegate;

	@Override
	public String toString() {
		return "IdentifierBridgeValueBridgeAdapter[" + "delegate=" + delegate + "]";
	}

	public IdentifierBridgeToValueBridgeAdapter(IdentifierBridge<I> delegate) {
		this.delegate = delegate;
	}

	@Override
	public String toIndexedValue(I value, ValueBridgeToIndexedValueContext context) {
		return value == null ? null : delegate.toDocumentIdentifier( value,
				context.extension( IdentifierBridgeContextToValueBridgeContextAdapterExtension.INSTANCE ) );
	}

	@Override
	public I fromIndexedValue(String value, ValueBridgeFromIndexedValueContext context) {
		return value == null ? null : delegate.fromDocumentIdentifier( value,
				context.extension( IdentifierBridgeContextToValueBridgeContextAdapterExtension.INSTANCE ) );
	}

	@Override
	public boolean isCompatibleWith(ValueBridge<?, ?> other) {
		if ( !(other instanceof IdentifierBridgeToValueBridgeAdapter ) ) {
			return false;
		}
		IdentifierBridgeToValueBridgeAdapter<?> castedOther = (IdentifierBridgeToValueBridgeAdapter<?>) other;
		return delegate.isCompatibleWith( castedOther.delegate );
	}

	@Override
	public void close() {
		delegate.close();
	}
}
