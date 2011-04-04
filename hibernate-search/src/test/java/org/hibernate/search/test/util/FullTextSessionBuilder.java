/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.test.util;

import java.io.File;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.lucene.analysis.StopAnalyzer;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.util.FileHelper;
import org.slf4j.Logger;

/**
 * Use the builder pattern to provide a SessionFactory.
 * This is meant to use only ram-based index and databases, for those test
 * which need to use several differently configured SessionFactories.
 * 
 * @author Sanne Grinovero
 * @author Hardy Ferentschik
 */
public class FullTextSessionBuilder {
	
	private static final Logger log = org.hibernate.search.util.LoggerFactory.make();

	private static final File indexDir;

	private final Properties cfg = new Properties();
	private final Set<Class<?>> annotatedClasses = new HashSet<Class<?>>();
	private SessionFactory sessionFactory;
	private boolean usingFileSystem = false;
	
	static {
		String buildDir = System.getProperty( "build.dir" );
		if ( buildDir == null ) {
			buildDir = ".";
		}
		File current = new File( buildDir );
		indexDir = new File( current, "indextemp" );
		log.debug( "Using {} as index directory.", indexDir.getAbsolutePath() );
	}
	
	public FullTextSessionBuilder() {
		cfg.setProperty( Environment.HBM2DDL_AUTO, "create-drop" );
		
		//cache:
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "true" );
		cfg.setProperty( Environment.CACHE_PROVIDER,
				org.hibernate.cache.HashtableCacheProvider.class.getCanonicalName() );
		cfg.setProperty( Environment.USE_QUERY_CACHE, "true" );
		
		//search specific:
		cfg.setProperty( org.hibernate.search.Environment.ANALYZER_CLASS,
				StopAnalyzer.class.getName() );
		useRAMDirectoryProvider( true );
	}
	
	/**
	 * @param use if true, use indexes in RAM otherwise use FSDirectoryProvider
	 * @return the same builder (this).
	 */
	public FullTextSessionBuilder useRAMDirectoryProvider(boolean use) {
		 if ( use ) {
			 cfg.setProperty( "hibernate.search.default.directory_provider", "ram" );
			 usingFileSystem = false;
		 }
		 else {
			 cfg.setProperty( "hibernate.search.default.directory_provider", "filesystem" );
			 usingFileSystem = true;
		 }
		return this;
	}
	
	/**
	 * Override before building any parameter, or add new ones.
	 * @param key Property name.
	 * @param value Property value.
	 * @return the same builder (this).
	 */
	public FullTextSessionBuilder setProperty(String key, String value) {
		cfg.setProperty( key, value );
		return this;
	}
	
	/**
	 * Adds classes to the SessionFactory being built.
	 * @param annotatedClass The annotated class to add to the configuration.
	 * @return the same builder (this)
	 */
	public FullTextSessionBuilder addAnnotatedClass(Class annotatedClass) {
		annotatedClasses.add( annotatedClass );
		return this;
	}
	
	/**
	 * @return a new FullTextSession based upon the built configuration.
	 */
	public FullTextSession openFullTextSession() {
		if ( sessionFactory == null ) {
			build();
		}
		Session session = sessionFactory.openSession();
		return Search.getFullTextSession( session );
	}
	
	/**
	 * Closes the SessionFactory.
	 * Make sure you close all sessions first
	 */
	public void close() {
		if ( sessionFactory == null ) {
			throw new java.lang.IllegalStateException( "sessionFactory not yet built" );
		}
		sessionFactory.close();
		if ( usingFileSystem ) {
			FileHelper.delete( indexDir );
		}
		sessionFactory = null;
	}

	/**
	 * Builds the sessionFactory as configured so far.
	 */
	public FullTextSessionBuilder build() {
		Configuration hibConfiguration = new Configuration();
		for ( Class<?> annotatedClass : annotatedClasses ) {
			hibConfiguration.addAnnotatedClass( annotatedClass );
		}
		hibConfiguration.getProperties().putAll( cfg );
		sessionFactory = hibConfiguration.buildSessionFactory();
		return this;
	}

	/**
	 * @return the SearchFactory
	 */
	public SearchFactory getSearchFactory() {
		FullTextSession fullTextSession = openFullTextSession();
		try {
			return fullTextSession.getSearchFactory();
		}
		finally {
			fullTextSession.close();
		}
	}

	/**
	 * Defines a programmatic configuration to be used by Search
	 * @return the enabled SearchMapping. change it to define the mapping programmatically.
	 */
	public SearchMapping fluentMapping() {
		SearchMapping mapping = (SearchMapping) cfg.get( org.hibernate.search.Environment.MODEL_MAPPING );
		if ( mapping == null ) {
			mapping = new SearchMapping();
			cfg.put( org.hibernate.search.Environment.MODEL_MAPPING, mapping );
		}
		return mapping;
	}
	
}
