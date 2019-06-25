/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg.impl;

import java.util.Optional;

import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;

/**
 * This class is very similar to {@link FallbackConfigurationPropertySource}.
 * We could actually use {@link FallbackConfigurationPropertySource} wherever we use this class,
 * simply by inverting the constructor parameters, if it wasn't for one detail:
 * the implementation of {@link #resolve(String)} would not work as expected,
 * returning the key resolved using the wrong source.
 */
public class OverriddenConfigurationPropertySource implements ConfigurationPropertySource {
	private final ConfigurationPropertySource main;
	private final ConfigurationPropertySource override;

	public OverriddenConfigurationPropertySource(ConfigurationPropertySource main, ConfigurationPropertySource override) {
		this.main = main;
		this.override = override;
	}

	@Override
	public Optional<?> get(String key) {
		Optional<?> value = override.get( key );
		if ( !value.isPresent() ) {
			return main.get( key );
		}
		else {
			return value;
		}
	}

	@Override
	public Optional<String> resolve(String key) {
		return main.resolve( key );
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "main=" ).append( main )
				.append( ", override=" ).append( override )
				.append( "]" );
		return sb.toString();
	}
}
