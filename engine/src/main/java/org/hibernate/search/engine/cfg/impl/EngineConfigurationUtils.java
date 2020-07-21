/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg.impl;

import java.lang.invoke.MethodHandles;
import java.util.Optional;

import org.hibernate.search.engine.cfg.BackendSettings;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.EngineSettings;
import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class EngineConfigurationUtils {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private EngineConfigurationUtils() {
	}

	public static ConfigurationPropertySourceExtractor extractorForBackend(Optional<String> backendNameOptional,
			String defaultBackendName) {
		if ( !backendNameOptional.isPresent() ) {
			if ( defaultBackendName == null ) {
				return engineSource -> engineSource.withMask( EngineSettings.Radicals.BACKEND );
			}
			else {
				return engineSource -> engineSource.withMask( EngineSettings.Radicals.BACKEND )
						// Fall back to the syntax "hibernate.search.backends.<defaultBackendName>>.foo".
						// Mostly supported for consistency and backward compatibility.
						// We'll drop this when we remove the ability to assign a name to the default backend.
						.withFallback( engineSource.withMask( EngineSettings.Radicals.BACKENDS )
								.withMask( defaultBackendName ) );
			}
		}
		else {
			return engineSource -> engineSource.withMask( EngineSettings.Radicals.BACKENDS )
					.withMask( backendNameOptional.get() );
		}
	}

	@SuppressWarnings("deprecation")
	public static ConfigurationPropertySourceExtractor extractorForIndex(
			ConfigurationPropertySourceExtractor extractorForBackend, String indexName) {
		return engineSource -> {
			ConfigurationPropertySource backendSource = extractorForBackend.extract( engineSource );
			return backendSource.withMask( BackendSettings.INDEXES ).withMask( indexName )
					.withFallback( backendSource )
					.withFallback( backendSource.withMask( BackendSettings.INDEX_DEFAULTS )
							.onGet( log::deprecatedIndexDefaultsPrefix ) );
		};
	}

}
