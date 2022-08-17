/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.common.data.spi;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;

import org.hibernate.search.util.common.SearchException;

/**
 * A simpler, safer, read-only wrapper around {@link java.util.Map}
 * that exposes only a few methods to retrieve a value from a given key.
 * <p>
 * Methods either makes it clear there might be no value (through the method name or {@link Optional} return type)
 * or are guaranteed to throw an exception when no value can be found,
 * with a meaningful message (including a list of all available keys).
 * <p>
 * This class only makes sense where there is a relatively low number of keys,
 * as exception messages will include a list of all available keys.
 *
 * @param <K> The type of keys.
 * @param <V> The type of values.
 */
public final class KeyValueProvider<K, V> {

	private final Map<K, ? extends V> content;
	private final BiFunction<? super K, ? super Set<K>, SearchException> missingValueExceptionFactory;

	public KeyValueProvider(Map<K, ? extends V> content,
			BiFunction<? super K, ? super Set<K>, SearchException> missingValueExceptionFactory) {
		this.content = Collections.unmodifiableMap( content );
		this.missingValueExceptionFactory = missingValueExceptionFactory;
	}

	public V getOrFail(K key) {
		V result = content.get( key );
		if ( result == null ) {
			throw missingValueExceptionFactory.apply( key, content.keySet() );
		}
		return result;
	}

	public Optional<V> getOptional(K key) {
		return Optional.ofNullable( content.get( key ) );
	}

	public V getOrNull(K key) {
		return content.get( key );
	}

	public Collection<? extends V> values() {
		return content.values();
	}

}
