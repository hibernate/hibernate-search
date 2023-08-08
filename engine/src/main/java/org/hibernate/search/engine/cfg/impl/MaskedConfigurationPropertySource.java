/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg.impl;

import java.util.Optional;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.util.common.impl.Contracts;

public class MaskedConfigurationPropertySource implements ConfigurationPropertySource {
	private final ConfigurationPropertySource propertiesToMask;
	private final String radix;

	public MaskedConfigurationPropertySource(ConfigurationPropertySource propertiesToMask, String mask) {
		Contracts.assertNotNull( propertiesToMask, "propertiesToMask" );
		Contracts.assertNotNull( mask, "mask" );
		this.propertiesToMask = propertiesToMask;
		this.radix = mask + ".";
	}

	@Override
	public Optional<?> get(String key) {
		return propertiesToMask.get( radix + key );
	}

	@Override
	public Optional<String> resolve(String key) {
		return propertiesToMask.resolve( radix + key );
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
