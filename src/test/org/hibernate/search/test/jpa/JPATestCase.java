//$Id$
package org.hibernate.search.test.jpa;

import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import java.util.ArrayList;import java.util.Arrays;
import java.io.InputStream;
import java.io.IOException;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.hibernate.cfg.Environment;
import org.hibernate.ejb.HibernatePersistence;
import org.hibernate.search.store.RAMDirectoryProvider;
import org.apache.lucene.analysis.StopAnalyzer;

/**
 * @author Emmanuel Bernard
 */
public abstract class JPATestCase extends junit.framework.TestCase {
	protected EntityManagerFactory factory;

	public JPATestCase() {
		super();
	}

	public JPATestCase(String name) {
		super( name );
	}

	public void setUp() {
		factory = new HibernatePersistence().createEntityManagerFactory( getConfig() );
	}

	public void tearDown() {
		factory.close();
	}

	public abstract Class[] getAnnotatedClasses();

	public String[] getEjb3DD() {
		return new String[]{};
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
		Map config = loadProperties();
		ArrayList<Class> classes = new ArrayList<Class>();

		classes.addAll( Arrays.asList( getAnnotatedClasses() ) );
		config.put( HibernatePersistence.LOADED_CLASSES, classes );
		for ( Map.Entry<Class, String> entry : getCachedClasses().entrySet() ) {
			config.put(
					HibernatePersistence.CLASS_CACHE_PREFIX + "." + entry.getKey().getName(),
					entry.getValue()
			);
		}
		for ( Map.Entry<String, String> entry : getCachedCollections().entrySet() ) {
			config.put(
					HibernatePersistence.COLLECTION_CACHE_PREFIX + "." + entry.getKey(),
					entry.getValue()
			);
		}
		if ( getEjb3DD().length > 0 ) {
			ArrayList<String> dds = new ArrayList<String>();
			dds.addAll( Arrays.asList( getEjb3DD() ) );
			config.put( HibernatePersistence.XML_FILE_NAMES, dds );
		}

		//Search config
		config.put( "hibernate.search.default.directory_provider", RAMDirectoryProvider.class.getName() );
		config.put( org.hibernate.search.Environment.ANALYZER_CLASS, StopAnalyzer.class.getName() );

		return config;
	}
}

