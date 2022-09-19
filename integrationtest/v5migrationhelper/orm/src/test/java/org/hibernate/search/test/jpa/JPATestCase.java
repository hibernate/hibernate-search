/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.jpa;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.search.test.testsupport.V5MigrationHelperJPASetupHelper;
import org.hibernate.search.testsupport.TestConstants;

import org.junit.Before;
import org.junit.Rule;

import org.apache.lucene.util.Version;

/**
 * @author Emmanuel Bernard
 */
public abstract class JPATestCase {

	@Rule
	public final V5MigrationHelperJPASetupHelper setupHelper = V5MigrationHelperJPASetupHelper.create();

	protected EntityManagerFactory factory;

	@Before
	public void setUp() {
		factory = setupHelper.start().withProperties( getConfig() ).setup( getPersistenceUnitName() );
	}

	public abstract Class[] getAnnotatedClasses();

	protected String getPersistenceUnitName() {
		return getClass().getSimpleName() + "PU";
	}

	public Map<Class, String> getCachedClasses() {
		return new HashMap<Class, String>();
	}

	public Map<String, String> getCachedCollections() {
		return new HashMap<String, String>();
	}

	public static Properties loadProperties() {
		Properties props = new Properties();
		InputStream stream = Persistence.class.getResourceAsStream( "/hibernate.properties" );
		if ( stream != null ) {
			try {
				props.load( stream );
			}
			catch (Exception e) {
				throw new RuntimeException( "could not load hibernate.properties" );
			}
			finally {
				try {
					stream.close();
				}
				catch (IOException ioe) {
				}
			}
		}
		props.setProperty( Environment.HBM2DDL_AUTO, "create-drop" );
		return props;
	}

	public Map getConfig() {
		Map<Object, Object> config = loadProperties();
		ArrayList<Class> classes = new ArrayList<Class>();

		classes.addAll( Arrays.asList( getAnnotatedClasses() ) );
		config.put( AvailableSettings.LOADED_CLASSES, classes );
		for ( Map.Entry<Class, String> entry : getCachedClasses().entrySet() ) {
			config.put(
					AvailableSettings.CLASS_CACHE_PREFIX + "." + entry.getKey().getName(),
					entry.getValue()
			);
		}
		for ( Map.Entry<String, String> entry : getCachedCollections().entrySet() ) {
			config.put(
					AvailableSettings.COLLECTION_CACHE_PREFIX + "." + entry.getKey(),
					entry.getValue()
			);
		}

		config.put( AvailableSettings.SESSION_FACTORY_NAME, "Test" + getClass() );

		//Search config
		configure( config );

		return config;
	}

	public static Version getTargetLuceneVersion() {
		return TestConstants.getTargetLuceneVersion();
	}

	protected void configure(Map cfg) {
		// for extensions
	}

}
