/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg.impl;

import java.util.Optional;

import org.hibernate.search.engine.cfg.BackendSettings;
import org.hibernate.search.engine.cfg.EngineSettings;
import org.hibernate.search.engine.cfg.spi.ConfigurationScopeNamespace;
import org.hibernate.search.engine.cfg.spi.ScopedConfigurationPropertySource;
import org.hibernate.search.engine.environment.bean.BeanResolver;

public final class EngineConfigurationUtils {

	private EngineConfigurationUtils() {
	}

	public static ConfigurationPropertySourceExtractor extractorForBackend(BeanResolver beanResolver,
			Optional<String> backendNameOptional) {
		if ( !backendNameOptional.isPresent() ) {
			return engineSource -> engineSource
					.withScope( beanResolver, ConfigurationScopeNamespace.BACKEND )
					.withMask( EngineSettings.Radicals.BACKEND );
		}
		else {
			return engineSource -> engineSource
					// First we want to get the defaults for the default backend
					.withScope( beanResolver, ConfigurationScopeNamespace.BACKEND )
					// and then on top of that put the backend specific ones (for a particular backend name)
					.withScope( beanResolver, ConfigurationScopeNamespace.BACKEND, backendNameOptional.get() )
					.withMask( EngineSettings.Radicals.BACKENDS )
					.withMask( backendNameOptional.get() );
		}
	}

	public static ConfigurationPropertySourceExtractor extractorForIndex(
			BeanResolver beanResolver,
			ConfigurationPropertySourceExtractor extractorForBackend,
			String indexName) {
		return engineSource -> {
			ScopedConfigurationPropertySource backendSource = extractorForBackend.extract( engineSource );
			return backendSource
					// First we want to get the defaults for the default index
					.withScope( beanResolver, ConfigurationScopeNamespace.INDEX )
					// First we want to get the defaults for the default index
					.withScope( beanResolver, ConfigurationScopeNamespace.INDEX, indexName )
					.withMask( BackendSettings.INDEXES ).withMask( indexName )
					.withFallback( backendSource );
		};
	}

}
