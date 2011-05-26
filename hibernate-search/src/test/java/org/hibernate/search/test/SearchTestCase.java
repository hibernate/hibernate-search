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
package org.hibernate.search.test;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;

import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.event.PostInsertEventListener;
import org.hibernate.impl.SessionFactoryImpl;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.SearchException;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.event.FullTextIndexEventListener;
import org.hibernate.search.util.FileHelper;
import org.hibernate.search.util.logging.Log;
import org.hibernate.search.util.logging.LoggerFactory;
import org.hibernate.testing.junit.functional.annotations.HibernateTestCase;

/**
 * Base class for Hibernate Search unit tests.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public abstract class SearchTestCase extends HibernateTestCase {

	private static final Log log = LoggerFactory.make();

	public static final Analyzer standardAnalyzer = new StandardAnalyzer( getTargetLuceneVersion() );
	public static final Analyzer stopAnalyzer = new StopAnalyzer( getTargetLuceneVersion() );
	public static final Analyzer simpleAnalyzer = new SimpleAnalyzer( getTargetLuceneVersion() );
	public static final Analyzer keywordAnalyzer = new KeywordAnalyzer();

	protected static final File indexDir;
	protected static SessionFactory sessions;
	protected Session session;

	private static File targetDir;
	private SearchFactoryImplementor searchFactory;

	static {
		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		// get a URL reference to something we now is part of the classpath (us)
		URL myUrl = contextClassLoader.getResource( SearchTestCase.class.getName().replace( '.', '/' ) + ".class" );
		File myPath = new File( myUrl.getFile() );
		// navigate back to '/target'
		targetDir = myPath
				.getParentFile()  // target/classes/org/hibernate/search/test
				.getParentFile()  // target/classes/org/hibernate/search
				.getParentFile()  // target/classes/org/hibernate/
				.getParentFile()  // target/classes/org
				.getParentFile()  // target/classes/
				.getParentFile(); // target

		indexDir = new File( targetDir, "indextemp" );
		log.debugf( "Using %s as index directory.", indexDir.getAbsolutePath() );
	}

	public SearchTestCase() {
		super();
	}

	public SearchTestCase(String x) {
		super( x );
	}

	@Override
	protected void handleUnclosedResources() {
		if ( session != null && session.isOpen() ) {
			if ( session.isConnected() ) {
				session.doWork( new RollbackWork() );
			}
			session.close();
			session = null;
			log.debug("Closing open session. Make sure to close sessions explicitly in your tests!");
		}
		else {
			session = null;
		}
	}

	@Override
	protected void closeResources() {
	}

	public Session openSession() throws HibernateException {
		session = getSessions().openSession();
		return session;
	}

	public Session openSession(Interceptor interceptor) throws HibernateException {
		session = getSessions().openSession( interceptor );
		return session;
	}

	protected void setSessions(SessionFactory sessions) {
		SearchTestCase.sessions = sessions;
	}

	protected SessionFactory getSessions() {
		if ( sessions == null ) {
			buildConfiguration();
		}
		return sessions;
	}

	protected void configure(Configuration cfg) {
		super.configure( cfg );

		cfg.setProperty( "hibernate.search.default.directory_provider", "ram" );
		cfg.setProperty( "hibernate.search.default.indexBase", indexDir.getAbsolutePath() );
		cfg.setProperty( org.hibernate.search.Environment.ANALYZER_CLASS, StopAnalyzer.class.getName() );

		cfg.setProperty( "hibernate.search.default.indexwriter.merge_factor", "100" );
		cfg.setProperty( "hibernate.search.default.indexwriter.max_buffered_docs", "1000" );
	}

	protected Directory getDirectory(Class<?> clazz) {
		return getLuceneEventListener().getSearchFactoryImplementor().getDirectoryProviders( clazz )[0].getDirectory();
	}

	private FullTextIndexEventListener getLuceneEventListener() {
		PostInsertEventListener[] listeners = ( ( SessionFactoryImpl ) getSessions() ).getEventListeners()
				.getPostInsertEventListeners();
		FullTextIndexEventListener listener = null;
		//FIXME this sucks since we mandate the event listener use
		for ( PostInsertEventListener candidate : listeners ) {
			if ( candidate instanceof FullTextIndexEventListener ) {
				listener = ( FullTextIndexEventListener ) candidate;
				break;
			}
		}
		if ( listener == null ) {
			throw new HibernateException( "Lucene event listener not initialized" );
		}
		return listener;
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		if ( sessions != null ) {
			sessions.close();
			sessions = null;
		}
		ensureIndexesAreEmpty();
	}

	protected void ensureIndexesAreEmpty() {
		if ( "jms".equals( getCfg().getProperty( "hibernate.search.worker.backend" ) ) ) {
			log.debug( "JMS based test. Skipping index emptying" );
			return;
		}
		FileHelper.delete( indexDir );
	}

	protected SearchFactory getSearchFactory() {
		if ( searchFactory == null ) {
			Session session = openSession();
			FullTextSession fullTextSession = Search.getFullTextSession( session );
			searchFactory = ( SearchFactoryImplementor ) fullTextSession.getSearchFactory();
			fullTextSession.close();
		}
		return searchFactory;
	}

	protected File getBaseIndexDir() {
		return indexDir;
	}

	protected void buildConfiguration() {
		try {
			setCfg( new Configuration() );
			configure( cfg );
			if ( recreateSchema() ) {
				cfg.setProperty( org.hibernate.cfg.Environment.HBM2DDL_AUTO, "create-drop" );
			}
			for ( String aPackage : getAnnotatedPackages() ) {
				( ( Configuration ) getCfg() ).addPackage( aPackage );
			}
			for ( Class<?> aClass : getAnnotatedClasses() ) {
				( ( Configuration ) getCfg() ).addAnnotatedClass( aClass );
			}
			for ( String xmlFile : getXmlFiles() ) {
				InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream( xmlFile );
				getCfg().addInputStream( is );
			}
			setSessions( getCfg().buildSessionFactory( /*new TestInterceptor()*/ ) );
		}
		catch ( HibernateException e ) {
			e.printStackTrace();
			throw e;
		}
		catch ( Exception e ) {
			e.printStackTrace();
			throw new SearchException( e );
		}
	}

	protected String[] getAnnotatedPackages() {
		return new String[] { };
	}

	public static Version getTargetLuceneVersion() {
		return Version.LUCENE_31;
	}
	
	protected SearchFactoryImplementor getSearchFactoryImpl() {
		FullTextSession s = Search.getFullTextSession( openSession() );
		s.close();
		SearchFactory searchFactory = s.getSearchFactory();
		return (SearchFactoryImplementor) searchFactory;
	}

	/**
	 * Returns the target directory of the build.
	 *
	 * @return the target directory of the build
	 */
	public static File getTargetDir() {
		return targetDir;
	}
}
