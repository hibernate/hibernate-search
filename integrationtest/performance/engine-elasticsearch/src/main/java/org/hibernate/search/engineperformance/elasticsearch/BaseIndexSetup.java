/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engineperformance.elasticsearch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.hibernate.search.engineperformance.elasticsearch.setuputilities.ConnectionInfo;

class BaseIndexSetup {

	private static final String HOST_PROPERTY = "host";

	private static final String USERNAME_PROPERTY = "username";

	private static final String PASSWORD_PROPERTY = "password";

	private static final String AWS_ACCESS_KEY_PROPERTY = "aws.access-key";

	private static final String AWS_SECRET_KEY_PROPERTY = "aws.secret-key";

	private static final String AWS_REGION_PROPERTY = "aws.region";

	/**
	 * Set this system property to an alternative path if you want
	 * cache files (downloads) to be stored in a specific place
	 * (other than "/tmp/" + TEST_DIR_PREFIX + "cache").
	 */
	private static final String CACHE_PROPERTY = "cache-path";

	/**
	 * Prefix used to identify the generated temporary directories for
	 * running tests which need writing to a filesystem.
	 */
	private static final String TEST_DIR_PREFIX = "HibernateSearch-Perftests-";

	protected ConnectionInfo getConnectionInfo() {
		String host = System.getProperty( HOST_PROPERTY );
		if ( host == null ) {
			host = "localhost:9200";
		}
		String username = System.getProperty( USERNAME_PROPERTY );
		String password = System.getProperty( PASSWORD_PROPERTY );
		String awsAccessKey = System.getProperty( AWS_ACCESS_KEY_PROPERTY );
		String awsSecretKey = System.getProperty( AWS_SECRET_KEY_PROPERTY );
		String awsRegion = System.getProperty( AWS_REGION_PROPERTY );
		return new ConnectionInfo( host, username, password, awsAccessKey, awsSecretKey, awsRegion );
	}

	protected Path pickCacheDirectory() throws IOException {
		String userSelectedPath = System.getProperty( CACHE_PROPERTY );
		Path path;
		if ( userSelectedPath != null ) {
			path = Paths.get( userSelectedPath );
		}
		else {
			path = Paths.get( System.getProperty( "java.io.tmpdir" ) )
					.resolve( TEST_DIR_PREFIX + "cache" );
		}
		if ( ! Files.exists( path ) ) {
			Files.createDirectory( path );
		}
		return path;
	}

}
