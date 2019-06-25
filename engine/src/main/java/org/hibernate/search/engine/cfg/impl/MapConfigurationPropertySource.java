/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg.impl;

import java.util.Map;
import java.util.Optional;

import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;

public class MapConfigurationPropertySource implements ConfigurationPropertySource {

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
	public String toString() {
		StringBuilder sb = new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "map=" ).append( map )
				.append( "]" );
		return sb.toString();
	}
}
