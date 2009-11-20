package org.hibernate.search.test.id.providedId;

import java.util.Iterator;
import java.util.Properties;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import org.apache.lucene.analysis.StopAnalyzer;

import org.hibernate.search.cfg.SearchConfiguration;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.store.RAMDirectoryProvider;
import org.hibernate.search.Environment;
import org.hibernate.annotations.common.reflection.ReflectionManager;

/**
 * @author Emmanuel Bernard
 */
public class StandaloneConf implements SearchConfiguration {
	final Map<String,Class<?>>  classes;
	final Properties properties;

	public StandaloneConf() {
		classes = new HashMap<String,Class<?>>(2);
		classes.put( ProvidedIdPerson.class.getName(), ProvidedIdPerson.class );
		classes.put( ProvidedIdPersonSub.class.getName(), ProvidedIdPersonSub.class );

		properties = new Properties( );
		properties.setProperty( "hibernate.search.default.directory_provider", RAMDirectoryProvider.class.getName() );
		properties.setProperty( Environment.ANALYZER_CLASS, StopAnalyzer.class.getName() );
		properties.setProperty( "hibernate.search.default.transaction.merge_factor", "100" );
		properties.setProperty( "hibernate.search.default.batch.max_buffered_docs", "1000" );
	}

	public Iterator<Class<?>> getClassMappings() {
		return classes.values().iterator();
	}

	public Class<?> getClassMapping(String name) {
		return classes.get( name );
	}

	public String getProperty(String propertyName) {
		return properties.getProperty( propertyName );
	}

	public Properties getProperties() {
		return properties;
	}

	public ReflectionManager getReflectionManager() {
		return null;
	}

	public SearchMapping getProgrammaticMapping() {
		return null;
	}
}
