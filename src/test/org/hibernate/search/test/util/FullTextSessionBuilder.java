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
 */
public class FullTextSessionBuilder {
	
	private AnnotationConfiguration cfg = new AnnotationConfiguration();
	private SessionFactory sessionFactory;
	private Session session;
	
	public FullTextSessionBuilder() {
		cfg.setProperty( Environment.HBM2DDL_AUTO, "create-drop" );
		//DB type:
		cfg.setProperty( Environment.URL, "jdbc:hsqldb:mem:." );
		cfg.setProperty( Environment.DRIVER,
				org.hsqldb.jdbcDriver.class.getCanonicalName() );
		cfg.setProperty( Environment.DIALECT,
				org.hibernate.dialect.HSQLDialect.class.getCanonicalName() );
		//connection:
		cfg.setProperty( Environment.USER, "sa" );
		cfg.setProperty( Environment.PASS, "" );
		cfg.setProperty( Environment.ISOLATION, "2" );
		cfg.setProperty( Environment.POOL_SIZE, "1" );
		cfg.setProperty( Environment.ORDER_UPDATES, "true" );
		//cache:
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "true" );
		cfg.setProperty( Environment.CACHE_PROVIDER,
				org.hibernate.cache.HashtableCacheProvider.class.getCanonicalName() );
		cfg.setProperty( Environment.USE_QUERY_CACHE, "true" );
		//debugging/logging:
		cfg.setProperty( Environment.SHOW_SQL, "false" );
		cfg.setProperty( Environment.USE_SQL_COMMENTS, "true" );
		cfg.setProperty( Environment.FORMAT_SQL, "true" );
		cfg.setProperty( Environment.USE_STRUCTURED_CACHE, "true" );
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
		//search specific:
		cfg.setProperty( org.hibernate.search.Environment.ANALYZER_CLASS,
				StopAnalyzer.class.getName() );
		cfg.setProperty( "hibernate.search.default.directory_provider",
				RAMDirectoryProvider.class.getName() );
	}
	
	/**
	 * Override before building any parameter, or add new ones.
	 * @param key
	 * @param value
	 * @return the same builder (this)
	 */
	public FullTextSessionBuilder setProperty(String key, String value) {
		cfg.setProperty( key, value );
		return this;
	}
	
	/**
	 * Adds classes to the SessionFactory being built
	 * @param annotatedClass
	 * @return the same builder (this)
	 */
	public FullTextSessionBuilder addAnnotatedClass(Class annotatedClass) {
		cfg.addAnnotatedClass( annotatedClass );
		return this;
	}
	
	/**
	 * Creates a new FullTextSession based upon the configuration built so far.
	 * @return
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
