/* $Id$
 * 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * Copyright (c) 2009, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 * 
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.test.id.providedId;

import java.util.Iterator;
import java.util.Properties;
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
