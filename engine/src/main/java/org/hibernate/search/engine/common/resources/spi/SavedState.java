/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.resources.spi;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.Contracts;
import org.hibernate.search.util.common.spi.ClosingOperator;

public class SavedState implements AutoCloseable {

	private static final SavedState EMPTY = new SavedState.Builder().build();

	public static SavedState empty() {
		return EMPTY;
	}

	private final Map<Key<?>, SavedValue<?, ?>> content;

	private SavedState(Builder builder) {
		this.content = builder.content;
	}

	@SuppressWarnings("unchecked") // values have always the corresponding key generic type
	public <T> Optional<T> get(Key<T> key) {
		SavedValue<?, ?> savedValue = content.get( key );
		if ( savedValue == null ) {
			return Optional.empty();
		}

		T value = (T) savedValue.value();
		return Optional.ofNullable( value );
	}

	public static <T> Key<T> key(String name) {
		Contracts.assertNotNullNorEmpty( name, "name" );
		return new Key<>( name );
	}

	@Override
	public void close() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.pushAll( SavedValue::close, content.values() );
		}
	}

	public static final class Key<T> {

		private final String name;

		private Key(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + "[" + name + "]";
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			Key<?> key = (Key<?>) o;
			return Objects.equals( name, key.name );
		}

		@Override
		public int hashCode() {
			return Objects.hash( name );
		}
	}

	public static Builder builder() {
		return new Builder();
	}

	private static void closeAll(Map<?, SavedState> map) {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.pushAll( SavedState::close, map.values() );
		}
	}

	public static final class Builder {

		private final Map<Key<?>, SavedValue<?, ?>> content = new LinkedHashMap<>();

		private Builder() {
		}

		public Builder put(Key<SavedState> key, SavedState value) {
			return put( key, value, SavedState::close );
		}

		public Builder put(SavedState.Key<Map<String, SavedState>> key, Map<String, SavedState> value) {
			return put( key, value, SavedState::closeAll );
		}

		// values have always the corresponding key generic type
		public <T, E extends Exception> Builder put(Key<T> key, T value, ClosingOperator<T, ? extends E> operator) {
			content.put( key, new SavedValue<>( value, operator ) );
			return this;
		}

		public SavedState build() {
			return new SavedState( this );
		}
	}

	public static final class SavedValue<T, E extends Exception> {

		private final T value;
		private final ClosingOperator<T, ? extends E> operator;

		private boolean close = true;

		public SavedValue(T value, ClosingOperator<T, ? extends E> operator) {
			this.value = value;
			this.operator = operator;
		}

		public T value() {
			close = false;
			return value;
		}

		public void close() {
			if ( !close ) {
				return;
			}

			try {
				operator.close( value );
			}
			catch (Exception e) {
				throw new RuntimeException( e );
			}
		}
	}
}
