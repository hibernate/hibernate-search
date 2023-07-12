/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProvider;
import org.hibernate.search.engine.cfg.spi.ConfigurationScope;
import org.hibernate.search.engine.cfg.spi.ScopedConfigurationPropertySource;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class RescopableConfigurationPropertySource extends FallbackConfigurationPropertySource {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final ConfigurationScope scope;

	public RescopableConfigurationPropertySource(BeanResolver beanResolver, ConfigurationPropertySource delegate) {
		this(
				ConfigurationScope.GLOBAL,
				delegate,
				scopeFallback( beanResolver, ConfigurationScope.GLOBAL )
		);
	}

	public RescopableConfigurationPropertySource(
			ConfigurationScope scope,
			ConfigurationPropertySource main,
			ConfigurationPropertySource fallback) {
		super( main, fallback );
		this.scope = scope;
	}

	@Override
	public ScopedConfigurationPropertySource withScope(BeanResolver beanResolver, String namespace, String name) {
		ConfigurationScope reduced = scope.reduce( namespace, name );

		return new RescopableConfigurationPropertySource(
				reduced,
				main,
				scopeFallback( beanResolver, reduced )
		);
	}

	private static ConfigurationPropertySource scopeFallback(BeanResolver beanResolver, ConfigurationScope scope) {
		try ( BeanHolder<List<ConfigurationProvider>> configurationProviderHolders =
				beanResolver.resolve( beanResolver.allConfiguredForRole( ConfigurationProvider.class ) ) ) {
			List<ConfigurationProvider> configurationProviders = configurationProviderHolders.get()
					.stream().sorted().collect( Collectors.toList() );

			if ( configurationProviders.size() > 1 ) {
				log.multipleConfigurationProvidersAvailable( scope.toString(), configurationProviders );
			}

			ConfigurationPropertySource fallback = EmptyConfigurationPropertySource.get();
			ConfigurationScope scopeIterator = scope;
			do {
				for ( ConfigurationProvider provider : configurationProviders ) {
					fallback = provider.get( scopeIterator ).map( fallback::withFallback ).orElse( fallback );
				}
				scopeIterator = scopeIterator.parent();
			}
			while ( scopeIterator != null );

			return fallback;
		}
	}
}
