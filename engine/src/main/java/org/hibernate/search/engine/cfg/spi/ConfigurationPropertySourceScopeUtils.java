/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg.spi;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.impl.EmptyConfigurationPropertySource;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.util.common.annotation.Incubating;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * Provides shortcuts to create scopes, as well as a quick way to create a fallback property source for a given scope.
 */
@Incubating
public final class ConfigurationPropertySourceScopeUtils {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private ConfigurationPropertySourceScopeUtils() {
	}

	public static ConfigurationScope global() {
		return ConfigurationScope.GLOBAL;
	}

	public static ConfigurationScope backend() {
		return global().reduce( ConfigurationScopeNamespace.BACKEND, null );
	}

	public static ConfigurationScope backend(String backendName) {
		return backend()
				.reduce( ConfigurationScopeNamespace.BACKEND, backendName );
	}

	public static ConfigurationScope index(String backendName) {
		return backend( backendName )
				.reduce( ConfigurationScopeNamespace.INDEX, null );
	}

	public static ConfigurationScope index(String backendName, String indexName) {
		return index( backendName )
				.reduce( ConfigurationScopeNamespace.INDEX, indexName );
	}

	/**
	 * Helps to create the fallback from the configuration providers for an exact scope.
	 * If multiple providers are available, they will be sorted and configurations from them
	 * will be added one by one as fallbacks.
	 */
	public static ConfigurationPropertySource fallback(BeanResolver beanResolver, ConfigurationScope scope) {
		try ( BeanHolder<List<ConfigurationProvider>> configurationProviderHolders =
				beanResolver.resolve( beanResolver.allConfiguredForRole( ConfigurationProvider.class ) ) ) {
			List<ConfigurationProvider> configurationProviders = configurationProviderHolders.get()
					.stream().sorted().collect( Collectors.toList() );

			if ( configurationProviders.size() > 1 ) {
				log.multipleConfigurationProvidersAvailable( scope.toString(), configurationProviders );
			}

			ConfigurationPropertySource fallback = EmptyConfigurationPropertySource.get();
			ConfigurationScope scopeIterator = scope;

			for ( ConfigurationProvider provider : configurationProviders ) {
				fallback = provider.get( scopeIterator ).map( fallback::withFallback ).orElse( fallback );
			}

			return fallback;
		}
	}
}
