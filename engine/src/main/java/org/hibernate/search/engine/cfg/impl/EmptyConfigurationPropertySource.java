/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg.impl;

import java.util.Optional;

import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;

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
