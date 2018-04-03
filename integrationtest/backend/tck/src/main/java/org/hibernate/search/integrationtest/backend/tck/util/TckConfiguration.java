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
import java.util.Properties;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;

/**
 * Allows to run the tests with different backends depending on the content of the property file at
 * {@value PROPERTIES_PATH} in the classpath.
 */
public final class TckConfiguration {

	private static final String PROPERTIES_PATH = "/backend-tck.properties";

	private static final String MULTI_TENANCY_PROPERTIES_PATH = "/backend-tck-multi-tenancy.properties";

	private static TckConfiguration instance;

	public static TckConfiguration get() {
		if ( instance == null ) {
			instance = new TckConfiguration();
		}
		return instance;
	}

	private final String startupTimestamp;

	private TckConfiguration() {
		this.startupTimestamp = new SimpleDateFormat( "yyyy-MM-dd-HH-mm-ss.SSS" ).format( new Date() );
	}

	public ConfigurationPropertySource getBackendProperties(String testId) {
		return getPropertySourceFromFile( PROPERTIES_PATH, testId );
	}

	public ConfigurationPropertySource getMultiTenancyBackendProperties(String testId) {
		return getPropertySourceFromFile( MULTI_TENANCY_PROPERTIES_PATH, testId );
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
