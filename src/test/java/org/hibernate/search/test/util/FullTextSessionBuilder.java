// $Id$
package org.hibernate.search.test.util;

import org.apache.lucene.analysis.StopAnalyzer;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Environment;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.store.RAMDirectoryProvider;

/**
 * Use the builder pattern to provide a SessionFactory.
 * This is meant to use only ram-based index and databases, for those test
 * which need to use several differently configured SessionFactories.
 * 
 * @author Sanne Grinovero
 * @author Hardy Ferentschik
 */
public class FullTextSessionBuilder {
	
	private AnnotationConfiguration cfg;
	private SessionFactory sessionFactory;
	private Session session;
	
	public FullTextSessionBuilder() {
		cfg = new AnnotationConfiguration();
		cfg.setProperty( Environment.HBM2DDL_AUTO, "create-drop" );
		
		//cache:
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "true" );
		cfg.setProperty( Environment.CACHE_PROVIDER,
				org.hibernate.cache.HashtableCacheProvider.class.getCanonicalName() );
		cfg.setProperty( Environment.USE_QUERY_CACHE, "true" );
		
		//search specific:
		cfg.setProperty( org.hibernate.search.Environment.ANALYZER_CLASS,
				StopAnalyzer.class.getName() );
		cfg.setProperty( "hibernate.search.default.directory_provider",
				RAMDirectoryProvider.class.getName() );
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
		cfg.addAnnotatedClass( annotatedClass );
		return this;
	}
	
	/**
	 * Creates a new FullTextSession based upon the configuration built so far.
	 * @return new FullTextSession based upon the configuration built so far.
	 */
	public FullTextSession build() {
		if ( session != null || sessionFactory != null ) {
			throw new java.lang.IllegalStateException( "session is open already" );
		}
		sessionFactory = cfg.buildSessionFactory();
		session = sessionFactory.openSession();
		return Search.getFullTextSession( session );
	}
	
	/**
	 * Closes the provided FullTextSession and the SessionFactory
	 */
	public void close() {
		if ( session == null || sessionFactory == null ) {
			throw new java.lang.IllegalStateException( "session not yet built" );
		}
		session.close();
		session = null;
		sessionFactory.close();
		sessionFactory = null;
	}
}
