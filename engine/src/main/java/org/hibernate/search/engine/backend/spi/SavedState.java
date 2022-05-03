/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.spi;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.hibernate.search.util.common.impl.Contracts;

public class SavedState {

	private static final SavedState EMPTY = new SavedState.Builder().build();

	public static SavedState empty() {
		return EMPTY;
	}

	private final Map<Key<?>, Object> content;

	private SavedState(Builder builder) {
		this.content = builder.content;
	}

	@SuppressWarnings("unchecked") // values have always the corresponding key generic type
	public <T> Optional<T> get(Key<T> key) {
		T value = (T) content.get( key );
		return Optional.ofNullable( value );
	}

	public static <T> Key<T> key(String name) {
		Contracts.assertNotNullNorEmpty( name, "name" );
		return new Key<>( name );
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

	public static final class Builder {
		private final Map<Key<?>, Object> content = new LinkedHashMap<>();

		private Builder() {
		}

		// values have always the corresponding key generic type
		public <T> Builder put(Key<T> key, T value) {
			content.put( key, value );
			return this;
		}

		public SavedState build() {
			return new SavedState( this );
		}
	}
}
