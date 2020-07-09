/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg.impl;

import org.hibernate.search.engine.cfg.BackendSettings;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.EngineSettings;

public final class EngineConfigurationUtils {

	private EngineConfigurationUtils() {
	}

	public static ConfigurationPropertySource getDefaultBackend(ConfigurationPropertySource engineSource) {
		return engineSource.withMask( EngineSettings.Radicals.BACKEND );
	}

	public static ConfigurationPropertySource getBackendByName(ConfigurationPropertySource engineSource, String backendName) {
		return engineSource.withMask( EngineSettings.Radicals.BACKENDS ).withMask( backendName );
	}

	public static ConfigurationPropertySource getIndex(ConfigurationPropertySource backendSource,
			ConfigurationPropertySource indexDefaultsSource, String indexName) {
		return backendSource.withMask( BackendSettings.INDEXES ).withMask( indexName ).withFallback( indexDefaultsSource );
	}

	public static ConfigurationPropertySource getIndexDefaults(ConfigurationPropertySource backendSource) {
		return backendSource.withMask( BackendSettings.INDEX_DEFAULTS );
	}

}
