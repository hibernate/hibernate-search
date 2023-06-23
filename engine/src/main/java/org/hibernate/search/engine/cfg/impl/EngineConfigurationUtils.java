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
			return engineSource -> engineSource.withMask( EngineSettings.Radicals.BACKEND );
		}
		else {
			return engineSource -> engineSource.withMask( EngineSettings.Radicals.BACKENDS )
					.withMask( backendNameOptional.get() );
		}
	}

	public static ConfigurationPropertySourceExtractor extractorForIndex(
			ConfigurationPropertySourceExtractor extractorForBackend, String indexName) {
		return engineSource -> {
			ConfigurationPropertySource backendSource = extractorForBackend.extract( engineSource );
			return backendSource.withMask( BackendSettings.INDEXES ).withMask( indexName )
					.withFallback( backendSource );
		};
	}

}
