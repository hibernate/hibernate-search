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
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySourceScopeUtils;
import org.hibernate.search.engine.environment.bean.BeanResolver;

public final class EngineConfigurationUtils {

	private EngineConfigurationUtils() {
	}

	public static ConfigurationPropertySourceExtractor extractorForBackend(BeanResolver beanResolver,
			Optional<String> backendNameOptional) {
		if ( !backendNameOptional.isPresent() ) {
			return engineSource -> engineSource.withMask( EngineSettings.Radicals.BACKEND )
					.withFallback(
							ConfigurationPropertySourceScopeUtils.fallback(
									beanResolver,
									ConfigurationPropertySourceScopeUtils.backend()
							)
					);
		}
		else {
			return engineSource -> engineSource.withMask( EngineSettings.Radicals.BACKENDS )
					.withFallback(
							ConfigurationPropertySourceScopeUtils.fallback(
									beanResolver,
									ConfigurationPropertySourceScopeUtils.backend()
							)
					)
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
			BeanResolver beanResolver, ConfigurationPropertySourceExtractor extractorForBackend,
			String backendName, String indexName) {
		return engineSource -> {
			ConfigurationPropertySource backendSource = extractorForBackend.extract( engineSource );
			return backendSource.withMask( BackendSettings.INDEXES )
					.withFallback(
							ConfigurationPropertySourceScopeUtils.fallback(
									beanResolver,
									ConfigurationPropertySourceScopeUtils.index( backendName )
							)
					)
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
