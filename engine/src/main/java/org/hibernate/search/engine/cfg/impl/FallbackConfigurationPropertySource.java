/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg.impl;

import java.util.Optional;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ScopedConfigurationPropertySource;
import org.hibernate.search.engine.environment.bean.BeanResolver;

public class FallbackConfigurationPropertySource implements ScopedConfigurationPropertySource {
	protected final ConfigurationPropertySource main;
	protected final ConfigurationPropertySource fallback;

	public FallbackConfigurationPropertySource(ConfigurationPropertySource main, ConfigurationPropertySource fallback) {
		this.main = main;
		this.fallback = fallback;
	}

	@Override
	public Optional<?> get(String key) {
		Optional<?> value = main.get( key );
		if ( !value.isPresent() ) {
			return fallback.get( key );
		}
		else {
			return value;
		}
	}

	@Override
	public Optional<String> resolve(String key) {
		if ( !main.get( key ).isPresent() && fallback.get( key ).isPresent() ) {
			return fallback.resolve( key );
		}
		else {
			return main.resolve( key );
		}
	}

	@Override
	public ScopedConfigurationPropertySource withScope(BeanResolver beanResolver, String namespace, String name) {
		ConfigurationPropertySource scopedMain = main;
		ConfigurationPropertySource scopedFallback = fallback;
		if ( main instanceof ScopedConfigurationPropertySource ) {
			scopedMain = ( (ScopedConfigurationPropertySource) main ).withScope( beanResolver, namespace, name );
		}
		if ( fallback instanceof ScopedConfigurationPropertySource ) {
			scopedFallback = ( (ScopedConfigurationPropertySource) fallback ).withScope( beanResolver, namespace, name );
		}

		if ( scopedMain != main || scopedFallback != fallback ) {
			return new FallbackConfigurationPropertySource(
					scopedMain,
					scopedFallback
			);
		}
		return this;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "main=" ).append( main )
				.append( ", fallback=" ).append( fallback )
				.append( "]" );
		return sb.toString();
	}
}
