/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.cfg.impl;

import java.util.Optional;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.util.common.impl.Contracts;

public class PrefixedConfigurationPropertySource implements ConfigurationPropertySource {
	private final ConfigurationPropertySource propertiesToPrefix;
	private final String radix;
	private final int radixLength;

	public PrefixedConfigurationPropertySource(ConfigurationPropertySource propertiesToPrefix, String prefix) {
		Contracts.assertNotNull( propertiesToPrefix, "propertiesToPrefix" );
		Contracts.assertNotNull( prefix, "prefix" );
		this.propertiesToPrefix = propertiesToPrefix;
		this.radix = prefix + ".";
		this.radixLength = radix.length();
	}

	@Override
	public Optional<?> get(String key) {
		if ( key.startsWith( radix ) ) {
			return propertiesToPrefix.get( key.substring( radixLength ) );
		}
		else {
			return Optional.empty();
		}
	}

	@Override
	public Optional<String> resolve(String key) {
		if ( key.startsWith( radix ) ) {
			return propertiesToPrefix.resolve( key.substring( radixLength ) );
		}
		else {
			return Optional.empty();
		}
	}

	@Override
	public ConfigurationPropertySource withPrefix(String prefix) {
		return new PrefixedConfigurationPropertySource( propertiesToPrefix, prefix + radix.substring( 0, radix.length() - 1 ) );
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "prefix=" ).append( radix )
				.append( ", propertiesToPrefix=" ).append( propertiesToPrefix )
				.append( "]" );
		return sb.toString();
	}
}
