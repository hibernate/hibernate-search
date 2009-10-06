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
package org.hibernate.search.test;

import java.io.File;
import java.io.InputStream;

import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.store.Directory;
import org.slf4j.Logger;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Dialect;
import org.hibernate.event.PostInsertEventListener;
import org.hibernate.impl.SessionFactoryImpl;
import org.hibernate.search.Environment;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.event.FullTextIndexEventListener;
import org.hibernate.search.store.RAMDirectoryProvider;
import org.hibernate.tool.hbm2ddl.SchemaExport;

/**
 * Base class for Hibernate Search unit tests.
 *
 * @author Emmanuel Bernard
 */
public abstract class SearchTestCase extends TestCase {

	private static final Logger log = org.hibernate.search.util.LoggerFactory.make();

	private static File indexDir;
	
	private SearchFactory searchFactory;

	static {
		String buildDir = System.getProperty( "build.dir" );
		if ( buildDir == null ) {
			buildDir = ".";
		}
		File current = new File( buildDir );
		indexDir = new File( current, "indextemp" );
		log.debug( "Using {} as index directory.", indexDir.getAbsolutePath() );
	}

	protected void setUp() throws Exception {
		buildSessionFactory( getMappings(), getAnnotatedPackages(), getXmlFiles() );
		ensureIndexesAreEmpty();
	}

	protected void tearDown() throws Exception {
		SchemaExport export = new SchemaExport( cfg );
		export.drop( false, true );
		searchFactory = null;
	}

	protected Directory getDirectory(Class<?> clazz) {
		return getLuceneEventListener().getSearchFactoryImplementor().getDirectoryProviders( clazz )[0].getDirectory();
	}

	private FullTextIndexEventListener getLuceneEventListener() {
		PostInsertEventListener[] listeners = ( ( SessionFactoryImpl ) getSessions() ).getEventListeners()
				.getPostInsertEventListeners();
		FullTextIndexEventListener listener = null;
		//FIXME this sucks since we mandante the event listener use
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

	protected void ensureIndexesAreEmpty() {
		if ( "jms".equals( getCfg().getProperty( "hibernate.search.worker.backend" ) ) ) {
			log.debug( "JMS based test. Skipping index emptying" );
			return;
		}
		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx;
		tx = s.beginTransaction();
		for ( Class<?> clazz : getMappings() ) {
			if ( clazz.getAnnotation( Indexed.class ) != null ) {
				s.purgeAll( clazz );
			}
		}
		tx.commit();
		s.close();
	}
	
	protected SearchFactory getSearchFactory() {
		if ( searchFactory == null ) {
			Session session = openSession();
			FullTextSession fullTextSession = Search.getFullTextSession( session );
			searchFactory = fullTextSession.getSearchFactory();
			fullTextSession.close();
		}
		return searchFactory;
	}

	protected void configure(Configuration cfg) {
		cfg.setProperty( "hibernate.search.default.directory_provider", RAMDirectoryProvider.class.getName() );
		cfg.setProperty( "hibernate.search.default.indexBase", indexDir.getAbsolutePath() );
		cfg.setProperty( Environment.ANALYZER_CLASS, StopAnalyzer.class.getName() );
		cfg.setProperty( "hibernate.search.default.transaction.merge_factor", "100" );
		cfg.setProperty( "hibernate.search.default.batch.max_buffered_docs", "1000" );
	}

	protected File getBaseIndexDir() {
		return indexDir;
	}

	protected void buildSessionFactory(Class<?>[] classes, String[] packages, String[] xmlFiles) throws Exception {
		if ( getSessions() != null ) {
			getSessions().close();
		}
		try {
			setCfg( new AnnotationConfiguration() );
			configure( cfg );
			if ( recreateSchema() ) {
				cfg.setProperty( org.hibernate.cfg.Environment.HBM2DDL_AUTO, "create-drop" );
			}
			for ( String aPackage : packages ) {
				( ( AnnotationConfiguration ) getCfg() ).addPackage( aPackage );
			}
			for ( Class<?> aClass : classes ) {
				( ( AnnotationConfiguration ) getCfg() ).addAnnotatedClass( aClass );
			}
			for ( String xmlFile : xmlFiles ) {
				InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream( xmlFile );
				getCfg().addInputStream( is );
			}
			setDialect( Dialect.getDialect() );
			setSessions( getCfg().buildSessionFactory( /*new TestInterceptor()*/ ) );
		}
		catch ( Exception e ) {
			e.printStackTrace();
			throw e;
		}
	}

	protected abstract Class<?>[] getMappings();

	protected String[] getAnnotatedPackages() {
		return new String[] { };
	}

	protected static File getIndexDir() {
		return indexDir;
	}
}
