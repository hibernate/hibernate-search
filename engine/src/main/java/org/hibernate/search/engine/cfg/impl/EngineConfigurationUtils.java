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

public final class EngineConfigurationUtils {

	private EngineConfigurationUtils() {
	}

	public static ConfigurationPropertySourceExtractor extractorForBackend(Optional<String> backendNameOptional) {
		if ( !backendNameOptional.isPresent() ) {
			return engineSource -> engineSource
					.withMask( EngineSettings.Radicals.BACKEND )
					.withScope( ConfigurationScopeNamespace.BACKEND );
		}
		else {
			return engineSource -> engineSource
					.withMask( EngineSettings.Radicals.BACKENDS )
					// First we want to get the defaults for the default backend
					.withScope( ConfigurationScopeNamespace.BACKEND )
					.withMask( backendNameOptional.get() )
					// and then on top of that put the backend specific ones (for a particular backend name)
					.withScope( ConfigurationScopeNamespace.BACKEND, backendNameOptional.get() );
		}
	}

	public static ConfigurationPropertySourceExtractor extractorForIndex(
			ConfigurationPropertySourceExtractor extractorForBackend,
			String indexName) {
		return engineSource -> {
			ScopedConfigurationPropertySource backendSource = extractorForBackend.extract( engineSource );
			return backendSource
					.withMask( BackendSettings.INDEXES )
					// First we want to get the defaults for the default index
					.withScope( ConfigurationScopeNamespace.INDEX )
					.withMask( indexName )
					// and then on top of that put the index specific ones (for a particular index name)
					.withScope( ConfigurationScopeNamespace.INDEX, indexName )
					.withFallback( backendSource );
		};
	}

}
