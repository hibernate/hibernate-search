/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg.impl;

import java.util.Objects;
import java.util.Optional;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;

public class MaskedConfigurationPropertySource implements ConfigurationPropertySource {
	private final ConfigurationPropertySource propertiesToMask;
	private final String radix;

	public MaskedConfigurationPropertySource(ConfigurationPropertySource toMask, String mask) {
		this.propertiesToMask = Objects.requireNonNull( toMask );
		this.radix = Objects.requireNonNull( mask ) + ".";
	}

	@Override
	public Optional<?> get(String key) {
		String compositeKey = radix + key;
		return propertiesToMask.get( compositeKey );
	}

	@Override
	public ConfigurationPropertySource withMask(String mask) {
		return new MaskedConfigurationPropertySource( propertiesToMask, radix + mask );
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "mask=" ).append( radix )
				.append( ", propertiesToMask=" ).append( propertiesToMask )
				.append( "]" );
		return sb.toString();
	}
}
