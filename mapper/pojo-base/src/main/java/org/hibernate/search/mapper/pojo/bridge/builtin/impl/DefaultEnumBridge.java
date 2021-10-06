/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import org.hibernate.search.engine.cfg.spi.ParseUtils;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.IdentifierBindingContext;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.IdentifierBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBinder;

public final class DefaultEnumBridge<T extends Enum<T>> extends AbstractStringBasedDefaultBridge<T> {

	private final Class<T> enumType;

	public DefaultEnumBridge(Class<T> enumType) {
		this.enumType = enumType;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + enumType.getName() + "]";
	}

	@Override
	public boolean isCompatibleWith(IdentifierBridge<?> other) {
		return isCompatibleWith( (Object) other );
	}

	@Override
	public boolean isCompatibleWith(ValueBridge<?, ?> other) {
		return isCompatibleWith( (Object) other );
	}

	private boolean isCompatibleWith(Object other) {
		if ( !getClass().equals( other.getClass() ) ) {
			return false;
		}
		DefaultEnumBridge<?> castedOther = (DefaultEnumBridge<?>) other;
		return enumType.equals( castedOther.enumType );
	}

	@Override
	protected String toString(T value) {
		return value.name();
	}

	@Override
	protected T fromString(String value) {
		return ParseUtils.parseEnum( enumType, value );
	}

	public static class Binder implements IdentifierBinder, ValueBinder {
		@Override
		@SuppressWarnings("unchecked") // The bridge resolver performs the checks using reflection
		public void bind(IdentifierBindingContext<?> context) {
			doBind( context, (Class) context.bridgedElement().rawType() );
		}

		private <V extends Enum<V>> void doBind(IdentifierBindingContext<?> context, Class<V> enumType) {
			context.bridge( enumType, new DefaultEnumBridge<>( enumType ) );
		}

		@Override
		@SuppressWarnings("unchecked") // The bridge resolver performs the checks using reflection
		public void bind(ValueBindingContext<?> context) {
			doBind( context, (Class) context.bridgedElement().rawType() );
		}

		private <V extends Enum<V>> void doBind(ValueBindingContext<?> context, Class<V> enumType) {
			context.bridge( enumType, new DefaultEnumBridge<>( enumType ) );
		}
	}

}