/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.cfg.impl;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import org.hibernate.search.engine.cfg.spi.AllAwareConfigurationPropertySource;

public class MapConfigurationPropertySource implements AllAwareConfigurationPropertySource {

	private final Map<String, ?> map;

	public MapConfigurationPropertySource(Map<String, ?> map) {
		this.map = map;
	}

	@Override
	public Optional<?> get(String key) {
		Object value = map.get( key );
		return Optional.ofNullable( value );
	}

	@Override
	public Optional<String> resolve(String key) {
		return Optional.of( key );
	}

	@Override
	public Set<String> resolveAll(BiPredicate<String, Object> predicate) {
		return map.entrySet().stream()
				.filter( e -> predicate.test( e.getKey(), e.getValue() ) )
				.map( Map.Entry::getKey )
				.collect( Collectors.toSet() );
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "map=" ).append( map )
				.append( "]" );
		return sb.toString();
	}
}
