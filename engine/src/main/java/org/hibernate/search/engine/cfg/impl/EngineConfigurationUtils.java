/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg.impl;

import org.hibernate.search.engine.cfg.BackendSettings;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.EngineSettings;

public final class EngineConfigurationUtils {

	private EngineConfigurationUtils() {
	}

	public static ConfigurationPropertySource getBackend(ConfigurationPropertySource engineSource, String backendName) {
		return engineSource.withMask( EngineSettings.BACKENDS ).withMask( backendName );
	}

	public static ConfigurationPropertySource getIndexWithoutDefaults(ConfigurationPropertySource engineSource, String indexName) {
		return engineSource.withMask( EngineSettings.INDEXES ).withMask( indexName );
	}

	public static ConfigurationPropertySource getIndexDefaults(ConfigurationPropertySource backendSource) {
		return backendSource.withMask( BackendSettings.INDEX_DEFAULTS );
	}

	public static ConfigurationPropertySource addIndexDefaults(ConfigurationPropertySource indexSource,
			ConfigurationPropertySource indexDefaultsSource) {
		return indexSource.withFallback( indexDefaultsSource );
	}

}
