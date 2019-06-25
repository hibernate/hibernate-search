/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.util;

import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.function.Function;

import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.util.impl.integrationtest.common.TestConfigurationProvider;

/**
 * Allows to run the tests with different backends depending on the content of the property file at
 * {@value DEFAULT_PROPERTIES_PATH} in the classpath.
 */
public final class TckConfiguration {

	private static final String DEFAULT_PROPERTIES_PATH = "/backend-tck.properties";

	private static final Function<String, String> SPECIFIC_PROPERTIES_PATH_FUNCTION =
			configurationId -> "/backend-tck-" + configurationId + ".properties";

	private static TckConfiguration instance;

	public static TckConfiguration get() {
		if ( instance == null ) {
			instance = new TckConfiguration();
		}
		return instance;
	}

	private final TckBackendFeatures backendFeatures;

	private TckConfiguration() {
		Iterator<TckBackendFeatures> featuresIterator = ServiceLoader.load( TckBackendFeatures.class ).iterator();
		if ( featuresIterator.hasNext() ) {
			this.backendFeatures = featuresIterator.next();
			if ( featuresIterator.hasNext() ) {
				throw new IllegalStateException( "Multiple backend features services found" );
			}
		}
		else {
			this.backendFeatures = new TckBackendFeatures();
		}
	}

	public TckBackendFeatures getBackendFeatures() {
		return backendFeatures;
	}

	public ConfigurationPropertySource getBackendProperties(TestConfigurationProvider configurationProvider, String configurationId) {
		String propertiesPath =
				configurationId == null
						? DEFAULT_PROPERTIES_PATH
						: SPECIFIC_PROPERTIES_PATH_FUNCTION.apply( configurationId );
		return ConfigurationPropertySource.fromMap( configurationProvider.getPropertiesFromFile( propertiesPath ) )
				.withMask( "backend" );
	}
}
