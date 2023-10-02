/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common.data.spi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;

import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.impl.Throwables;

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

	public static <K, V> KeyValueProvider<K, V> create(Map<K, ? extends V> content,
			BiFunction<? super K, ? super Set<K>, SearchException> missingValueExceptionFactory) {
		return new KeyValueProvider<>( content, missingValueExceptionFactory,
				toMultiMissingValue( missingValueExceptionFactory ) );
	}

	public static <K, V> KeyValueProvider<K, V> create(Map<K, ? extends V> content,
			BiFunction<? super K, ? super Set<K>, SearchException> singleMissingValueExceptionFactory,
			BiFunction<? super Set<K>, ? super Set<K>, SearchException> multiMissingValueExceptionFactory) {
		return new KeyValueProvider<>( content, singleMissingValueExceptionFactory,
				multiMissingValueExceptionFactory );
	}

	public static <K, V> KeyValueProvider<K, V> createWithMultiKeyException(Map<K, ? extends V> content,
			BiFunction<? super Set<K>, ? super Set<K>, SearchException> missingValueExceptionFactory) {
		return new KeyValueProvider<>( content, toSingleMissingValue( missingValueExceptionFactory ),
				missingValueExceptionFactory );
	}

	private static <K> BiFunction<K, Set<K>, SearchException> toSingleMissingValue(
			BiFunction<? super Set<K>, ? super Set<K>, SearchException> missingValueExceptionFactory) {
		return (invalidKey, allValidKeys) -> missingValueExceptionFactory.apply( Set.of( invalidKey ), allValidKeys );
	}

	private static <K> BiFunction<Set<K>, Set<K>, SearchException> toMultiMissingValue(
			BiFunction<? super K, ? super Set<K>, SearchException> singleMissingValueExceptionFactory) {
		return (invalidKeys, allValidKeys) -> {
			SearchException exception = null;
			for ( var invalidKey : invalidKeys ) {
				exception = Throwables.combine( exception,
						singleMissingValueExceptionFactory.apply( invalidKey, allValidKeys ) );
			}
			return exception;
		};
	}

	private final Map<K, ? extends V> content;
	private final BiFunction<? super K, ? super Set<K>, SearchException> singleMissingValueExceptionFactory;
	private final BiFunction<? super Set<K>, ? super Set<K>, SearchException> multiMissingValueExceptionFactory;

	public KeyValueProvider(Map<K, ? extends V> content,
			BiFunction<? super K, ? super Set<K>, SearchException> singleMissingValueExceptionFactory) {
		this( content, singleMissingValueExceptionFactory,
				toMultiMissingValue( singleMissingValueExceptionFactory ) );
	}

	private KeyValueProvider(Map<K, ? extends V> content,
			BiFunction<? super K, ? super Set<K>, SearchException> singleMissingValueExceptionFactory,
			BiFunction<? super Set<K>, ? super Set<K>, SearchException> multiMissingValueExceptionFactory) {
		this.content = Collections.unmodifiableMap( content );
		this.singleMissingValueExceptionFactory = singleMissingValueExceptionFactory;
		this.multiMissingValueExceptionFactory = multiMissingValueExceptionFactory;
	}

	public V getOrFail(K key) {
		V result = content.get( key );
		if ( result == null ) {
			throw singleMissingValueExceptionFactory.apply( key, content.keySet() );
		}
		return result;
	}

	public List<V> getAllOrFail(Collection<? extends K> keys) {
		return getAllOrFail( keys, (k, v) -> v );
	}

	public <V2> List<V2> getAllOrFail(Collection<? extends K> keys, BiFunction<K, V, V2> transform) {
		List<V2> result = new ArrayList<>();
		Set<K> missing = null;
		for ( K key : keys ) {
			V value = content.get( key );
			if ( value == null ) {
				if ( missing == null ) {
					missing = new LinkedHashSet<>();
				}
				missing.add( key );
			}
			else {
				result.add( transform.apply( key, value ) );
			}
		}
		if ( missing != null ) {
			throw multiMissingValueExceptionFactory.apply( missing, content.keySet() );
		}
		return result;
	}

	public Optional<? extends V> getOptional(K key) {
		return Optional.ofNullable( content.get( key ) );
	}

	public V getOrNull(K key) {
		return content.get( key );
	}

	public Set<K> keys() {
		return content.keySet();
	}

	public Collection<? extends V> values() {
		return content.values();
	}

}
