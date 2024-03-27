/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.cfg.impl;

import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import org.hibernate.search.engine.cfg.spi.AllAwareConfigurationPropertySource;

public class SystemConfigurationPropertySource implements AllAwareConfigurationPropertySource {

	private static final SystemConfigurationPropertySource INSTANCE = new SystemConfigurationPropertySource();

	public static AllAwareConfigurationPropertySource get() {
		return INSTANCE;
	}

	private SystemConfigurationPropertySource() {
	}

	@Override
	public Optional<?> get(String key) {
		Object value = System.getProperty( key );
		return Optional.ofNullable( value );
	}

	@Override
	public Optional<String> resolve(String key) {
		return Optional.of( key );
	}

	@Override
	public Set<String> resolveAll(BiPredicate<String, Object> predicate) {
		return System.getProperties().entrySet().stream()
				.filter( e -> {
					Object key = e.getKey();
					if ( !( key instanceof String ) ) {
						return false;
					}
					return predicate.test( (String) key, e.getValue() );
				} )
				.map( e -> (String) e.getKey() )
				.collect( Collectors.toSet() );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}
