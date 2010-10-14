package org.hibernate.search.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.java.JavaReflectionManager;
import org.hibernate.search.cfg.SearchConfiguration;
import org.hibernate.search.cfg.SearchMapping;

/**
 * @author Emmanuel Bernard
 */
public class IncrementalSearchConfiguration implements SearchConfiguration {
	private final ReflectionManager reflectionManager = new JavaReflectionManager();
	private final List<Class<?>> classes;
	private final Map<String, Class<?>> classesByName = new HashMap<String, Class<?>>();
	private Properties properties;

	public IncrementalSearchConfiguration(List<Class<?>> classes, Properties properties) {
		this.properties = properties;
		this.classes = classes;
		for ( Class<?> entity : classes ) {
			classesByName.put( entity.getName(), entity );
		}
	}
	public Iterator<Class<?>> getClassMappings() {
		return classes.iterator();
	}

	public Class<?> getClassMapping(String name) {
		return classesByName.get( name );
	}

	public String getProperty(String propertyName) {
		return properties.getProperty(propertyName );
	}

	public Properties getProperties() {
		return properties;
	}

	public ReflectionManager getReflectionManager() {
		return reflectionManager;
	}

	public SearchMapping getProgrammaticMapping() {
		return null;
	}
}
