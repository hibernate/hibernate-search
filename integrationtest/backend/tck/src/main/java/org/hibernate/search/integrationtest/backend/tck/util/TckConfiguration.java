/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.util;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.function.Function;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;

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

	private final String startupTimestamp;

	private final TckBackendFeatures backendFeatures;

	private TckConfiguration() {
		this.startupTimestamp = new SimpleDateFormat( "yyyy-MM-dd-HH-mm-ss.SSS", Locale.ROOT )
				.format( new Date() );

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

	public ConfigurationPropertySource getBackendProperties(String testId, String configurationId) {
		String propertiesPath =
				configurationId == null
						? DEFAULT_PROPERTIES_PATH
						: SPECIFIC_PROPERTIES_PATH_FUNCTION.apply( configurationId );
		return getPropertySourceFromFile( propertiesPath, testId );
	}

	private ConfigurationPropertySource getPropertySourceFromFile(String propertyFilePath, String testId) {
		Properties properties = new Properties();
		try ( InputStream propertiesInputStream = getClass().getResourceAsStream( propertyFilePath ) ) {
			if ( propertiesInputStream == null ) {
				throw new IllegalStateException( "Missing TCK properties file in the classpath: " + propertyFilePath );
			}
			properties.load( propertiesInputStream );
		}
		catch (IOException e) {
			throw new IllegalStateException( "Error loading TCK properties file: " + propertyFilePath );
		}

		Properties overriddenProperties = new Properties();

		properties.forEach( (k, v) -> {
			if ( v instanceof String ) {
				overriddenProperties.put( k, ( (String) v ).replace( "#{tck.test.id}", testId ).replace( "#{tck.startup.timestamp}", startupTimestamp ) );
			}
			else {
				overriddenProperties.put( k, v );
			}
		} );

		return ConfigurationPropertySource.fromProperties( overriddenProperties ).withMask( "backend" );
	}
}
