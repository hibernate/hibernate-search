/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.common.resources.spi;

import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.Contracts;
import org.hibernate.search.util.common.impl.Throwables;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.spi.ClosingOperator;

public class SavedState implements AutoCloseable {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

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
			closer.pushAll( entry -> entry.getValue().close( entry.getKey() ), content.entrySet() );
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
			closer.pushAll( entry -> {
				try {
					entry.getValue().close();
				}
				catch (RuntimeException e) {
					throw log.unableToCloseSavedValue( Throwables.safeToString( e, entry.toString() ), e.getMessage(), e );
				}
			}, map.entrySet() );
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
		public <T> Builder put(Key<T> key, T value, ClosingOperator<T, ? extends Exception> closingOperator) {
			content.put( key, new SavedValue<>( value, closingOperator ) );
			return this;
		}

		public SavedState build() {
			return new SavedState( this );
		}
	}

	public static final class SavedValue<T, E extends Exception> {

		private final T value;
		private final ClosingOperator<T, ? extends Exception> closingOperator;

		private boolean close = true;

		public SavedValue(T value, ClosingOperator<T, ? extends Exception> closingOperator) {
			this.value = value;
			this.closingOperator = closingOperator;
		}

		public T value() {
			close = false;
			return value;
		}

		public void close(Key<?> key) {
			if ( !close ) {
				return;
			}

			try {
				closingOperator.close( value );
			}
			catch (Exception e) {
				throw log.unableToCloseSavedValue( key.name, e.getMessage(), e );
			}
		}
	}
}
