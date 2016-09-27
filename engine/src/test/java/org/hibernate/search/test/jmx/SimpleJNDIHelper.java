/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.jmx;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;

import org.hibernate.search.cfg.spi.SearchConfiguration;

/**
 * A common place for helper methods useful for all tests using SimpleJNDI
 * @author Sanne Grinovero
 */
class SimpleJNDIHelper {

	private SimpleJNDIHelper() {
		//not to be constructed
	}

	/**
	 * @param testClass the current test class is needed to define were to best store the temporary data
	 * @return the Path for SimpleJNDI to store its data
	 */
	public static Path makeTestingJndiDirectory(Class<?> testClass) {
		Path targetDir = getTargetDir();
		Path simpleJndiDir = targetDir.resolve( "simpleJndi" );
		if ( !Files.exists( simpleJndiDir ) ) {
			try {
				Files.createDirectory( simpleJndiDir );
			}
			catch (IOException e) {
				throw new RuntimeException( e );
			}
		}
		return simpleJndiDir;
	}

	public static void enableSimpleJndi(SearchConfiguration configuration, Path jndiStorage) {
		Properties p = configuration.getProperties();
		enableSimpleJndi( p, jndiStorage );
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void enableSimpleJndi(Map p, Path jndiStorage) {
		p.put( "hibernate.jndi.class", "org.osjava.sj.SimpleContextFactory" );
		p.put( "hibernate.jndi.org.osjava.sj.root", jndiStorage.toAbsolutePath().toString() );
		p.put( "hibernate.jndi.org.osjava.sj.jndi.shared", "true" );
	}

	private static Path getTargetDir() {
		URI classesDirUri;
		try {
			classesDirUri = SimpleJNDIHelper.class.getProtectionDomain()
					.getCodeSource()
					.getLocation()
					.toURI();
		}
		catch (URISyntaxException e) {
			throw new RuntimeException( e );
		}

		return Paths.get( classesDirUri ).getParent();
	}
}
