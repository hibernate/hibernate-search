/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.IdentifierBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.IdentifierBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeFromDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeToDocumentIdentifierContext;

public final class DefaultEnumIdentifierBridge<T extends Enum<T>> implements IdentifierBridge<T> {

	private final Class<T> enumType;

	public DefaultEnumIdentifierBridge(Class<T> enumType) {
		this.enumType = enumType;
	}

	@Override
	public String toDocumentIdentifier(T propertyValue,
			IdentifierBridgeToDocumentIdentifierContext context) {
		return propertyValue.name();
	}

	@Override
	public T fromDocumentIdentifier(String documentIdentifier, IdentifierBridgeFromDocumentIdentifierContext context) {
		return Enum.valueOf( enumType, documentIdentifier );
	}

	@Override
	public boolean isCompatibleWith(IdentifierBridge<?> other) {
		if ( !getClass().equals( other.getClass() ) ) {
			return false;
		}
		DefaultEnumIdentifierBridge<?> castedOther = (DefaultEnumIdentifierBridge<?>) other;
		return enumType.equals( castedOther.enumType );
	}

	public static class Binder implements IdentifierBinder {
		@Override
		@SuppressWarnings("unchecked") // The bridge resolver performs the checks using reflection
		public void bind(IdentifierBindingContext<?> context) {
			doBind( context, (Class) context.getBridgedElement().getRawType() );
		}

		private <V extends Enum<V>> void doBind(IdentifierBindingContext<?> context, Class<V> enumType) {
			context.setBridge( enumType, new DefaultEnumIdentifierBridge<>( enumType ) );
		}
	}

}