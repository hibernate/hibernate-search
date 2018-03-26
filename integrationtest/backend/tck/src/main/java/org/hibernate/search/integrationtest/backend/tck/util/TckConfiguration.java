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
 * Allows to run the tests with different backends depending on the content
 * of the property file at {@value PROPERTIES_PATH} in the classpath.
 */
public final class TckConfiguration {

	private static final String PROPERTIES_PATH = "/backend-tck.properties";

	private static final String BACKEND_LUCENE_ROOT_DIRECTORY_PROPERTY = "backend.lucene.root_directory";

	private static TckConfiguration instance;

	public static TckConfiguration get() {
		if ( instance == null ) {
			instance = new TckConfiguration();
		}
		return instance;
	}

	private final ConfigurationPropertySource source;

	private TckConfiguration() {
		Properties properties = new Properties();
		try ( InputStream propertiesInputStream = getClass().getResourceAsStream( PROPERTIES_PATH ) ) {
			if ( propertiesInputStream == null ) {
				throw new IllegalStateException( "Missing TCK properties file in the classpath: " + PROPERTIES_PATH );
			}
			properties.load( propertiesInputStream );
		}
		catch (IOException e) {
			throw new IllegalStateException( "Error loading TCK properties file: " + PROPERTIES_PATH );
		}

		addTimestampToLuceneRootDirectory( properties );

		source = ConfigurationPropertySource.fromProperties( properties );
	}

	public ConfigurationPropertySource getBackendProperties() {
		return source.withMask( "backend" );
	}

	private void addTimestampToLuceneRootDirectory(Properties properties) {
		String baseLuceneRootDirectory = properties.getProperty( BACKEND_LUCENE_ROOT_DIRECTORY_PROPERTY );

		if ( baseLuceneRootDirectory != null ) {
			StringBuilder timestampedLuceneRootDirectoryBuilder = new StringBuilder( baseLuceneRootDirectory )
					.append( '/' )
					.append( new SimpleDateFormat( "yyyy-MM-dd-HH-mm-ss.SSS" ).format( new Date() ) );

			properties.put( BACKEND_LUCENE_ROOT_DIRECTORY_PROPERTY, timestampedLuceneRootDirectoryBuilder.toString() );
		}
	}
}
