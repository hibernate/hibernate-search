/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.junit.runner.Description;

public final class TestHelper {

	private static final String STARTUP_TIMESTAMP = new SimpleDateFormat( "yyyy-MM-dd-HH-mm-ss.SSS", Locale.ROOT )
			.format( new Date() );

	public static TestHelper create(Description description) {
		return new TestHelper( description );
	}

	private final String testId;

	private TestHelper(Description description) {
		this.testId = description.getTestClass().getSimpleName() + "-" + description.getMethodName();
	}

	public Map<String, Object> getPropertiesFromFile(String propertyFilePath) {
		Properties properties = new Properties();
		try ( InputStream propertiesInputStream = getClass().getResourceAsStream( propertyFilePath ) ) {
			if ( propertiesInputStream == null ) {
				throw new IllegalStateException( "Missing test properties file in the classpath: " + propertyFilePath );
			}
			properties.load( propertiesInputStream );
		}
		catch (IOException e) {
			throw new IllegalStateException( "Error loading test properties file: " + propertyFilePath, e );
		}

		Map<String, Object> overriddenProperties = new LinkedHashMap<>();

		properties.forEach( (k, v) -> {
			if ( v instanceof String ) {
				overriddenProperties.put(
						(String) k,
						( (String) v ).replace( "#{tck.test.id}", testId )
								.replace( "#{tck.startup.timestamp}", STARTUP_TIMESTAMP ) );
			}
			else {
				overriddenProperties.put( (String) k, v );
			}
		} );

		return overriddenProperties;
	}
}
