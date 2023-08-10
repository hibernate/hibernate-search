/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg.impl;

import java.util.Optional;

import org.hibernate.search.engine.cfg.BackendSettings;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.EngineSettings;

public final class EngineConfigurationUtils {

	private EngineConfigurationUtils() {
	}

	public static ConfigurationPropertySourceExtractor extractorForBackend(Optional<String> backendNameOptional) {
		if ( !backendNameOptional.isPresent() ) {
			return (beanResolver, engineSource) -> engineSource.withMask( EngineSettings.Radicals.BACKEND )
					.withFallback(
							ConfigurationPropertySourceScopeUtils.fallback(
									beanResolver,
									ConfigurationPropertySourceScopeUtils.backend()
							)
					);
		}
		else {
			return (beanResolver, engineSource) -> engineSource.withMask( EngineSettings.Radicals.BACKENDS )
					.withMask( backendNameOptional.get() )
					.withFallback(
							ConfigurationPropertySourceScopeUtils.fallback(
									beanResolver,
									ConfigurationPropertySourceScopeUtils.backend( backendNameOptional.get() )
							)
					);
		}
	}

	public static ConfigurationPropertySourceExtractor extractorForIndex(
			ConfigurationPropertySourceExtractor extractorForBackend,
			String backendName, String indexName) {
		return (beanResolver, engineSource) -> {
			ConfigurationPropertySource backendSource = extractorForBackend.extract( beanResolver, engineSource );
			return backendSource.withMask( BackendSettings.INDEXES )
					.withMask( indexName )
					.withFallback(
							ConfigurationPropertySourceScopeUtils.fallback(
									beanResolver,
									ConfigurationPropertySourceScopeUtils.index( backendName, indexName )
							)
					)
					.withFallback( backendSource );
		};
	}

}
