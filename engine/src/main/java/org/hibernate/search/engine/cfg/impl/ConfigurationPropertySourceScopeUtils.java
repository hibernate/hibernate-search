/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.cfg.impl;

import java.lang.invoke.MethodHandles;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProvider;
import org.hibernate.search.engine.cfg.spi.ConfigurationScope;
import org.hibernate.search.engine.cfg.spi.ConfigurationScopeNamespaces;
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

	private static final Comparator<ConfigurationProvider> CONFIGURATION_PROVIDER_COMPARATOR =
			Comparator.comparing( ConfigurationProvider::priority )
					.thenComparing( cp -> cp.getClass().getName() );

	private ConfigurationPropertySourceScopeUtils() {
	}

	public static ConfigurationScope global() {
		return ConfigurationScope.GLOBAL;
	}

	public static ConfigurationScope backend() {
		return global().reduce( ConfigurationScopeNamespaces.BACKEND, null );
	}

	public static ConfigurationScope backend(String backendName) {
		return backend()
				.reduce( ConfigurationScopeNamespaces.BACKEND, backendName );
	}

	public static ConfigurationScope index(String backendName, String indexName) {
		return backend( backendName )
				.reduce( ConfigurationScopeNamespaces.INDEX, indexName );
	}

	/**
	 * Helps to create the fallback from the configuration providers for an exact scope.
	 * If multiple providers are available, they will be sorted and configurations from them
	 * will be added one by one as fallbacks.
	 */
	public static ConfigurationPropertySource fallback(BeanResolver beanResolver, ConfigurationScope scope) {
		try ( BeanHolder<List<ConfigurationProvider>> configurationProviderHolders =
				beanResolver.resolve( beanResolver.allConfiguredForRole( ConfigurationProvider.class ) ) ) {
			List<ConfigurationProvider> configurationProviders = configurationProviderHolders.get().stream()
					.sorted( CONFIGURATION_PROVIDER_COMPARATOR )
					.collect( Collectors.toList() );

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
