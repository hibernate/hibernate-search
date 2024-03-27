/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.cfg.impl;

import java.util.Optional;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;

public class EmptyConfigurationPropertySource implements ConfigurationPropertySource {

	private static final EmptyConfigurationPropertySource INSTANCE = new EmptyConfigurationPropertySource();

	public static ConfigurationPropertySource get() {
		return INSTANCE;
	}

	private EmptyConfigurationPropertySource() {
		// Private constructor, use get() instead
	}

	@Override
	public Optional<Object> get(String key) {
		return Optional.empty();
	}

	@Override
	public Optional<String> resolve(String key) {
		return Optional.of( key );
	}

	@Override
	public ConfigurationPropertySource withMask(String mask) {
		return this;
	}

	@Override
	public ConfigurationPropertySource withFallback(ConfigurationPropertySource fallback) {
		return fallback;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( getClass().getSimpleName() )
				.append( "[]" );
		return sb.toString();
	}
}
