/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.testsupport.setup;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Provides access to the defaults used in tests.
 *
 * @author Yoann Rodiere
 */
public final class TestDefaults {

	private static final Log LOG = LoggerFactory.make( Log.class );

	private static final String DEFAULT_PROPERTIES_PATH = "/test-defaults.properties";

	private TestDefaults() {
		// Private constructor
	}

	public static Properties getProperties() {
		Properties result = new Properties();
		try ( InputStream stream = TestDefaults.class.getResourceAsStream( DEFAULT_PROPERTIES_PATH ) ) {
			if ( stream != null ) {
				result.load( stream );
			}
		}
		catch (IOException | RuntimeException e) {
			LOG.warn( "Unable to retrieve from properties file [" + DEFAULT_PROPERTIES_PATH + "]", e );
		}
		return result;
	}

}
