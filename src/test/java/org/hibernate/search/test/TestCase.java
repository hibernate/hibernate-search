//$Id$
package org.hibernate.search.test;

import java.io.InputStream;

import org.apache.lucene.analysis.StopAnalyzer;

import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.search.store.RAMDirectoryProvider;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.event.FlushEventListener;
import org.hibernate.event.def.DefaultFlushEventListener;
import org.hibernate.search.event.FullTextIndexEventListener;

/**
 * A modified base class for tests without annotations.
 *
 * @author Hardy Ferentschik
 */
public abstract class TestCase extends junit.framework.TestCase {

	protected static SessionFactory sessions;
	protected static Configuration cfg;
	protected static Dialect dialect;
	protected static Class lastTestClass;
	protected Session session;

	public TestCase() {
		super();
	}

	public TestCase(String x) {
		super( x );
	}

	protected void buildSessionFactory(String[] xmlFiles) throws Exception {

		if ( getSessions() != null ) {
			getSessions().close();
		}
		try {
			setCfg( new Configuration() );
			configure( cfg );
			if ( recreateSchema() ) {
				cfg.setProperty( Environment.HBM2DDL_AUTO, "create-drop" );
			}
			for ( String xmlFile : xmlFiles ) {
				InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream( xmlFile );
				getCfg().addInputStream( is );
			}
			setDialect( Dialect.getDialect() );
			setSessions( getCfg().buildSessionFactory() );
		}
		catch ( Exception e ) {
			e.printStackTrace();
			throw e;
		}
	}

	protected void setUp() throws Exception {
		if ( getSessions() == null || getSessions().isClosed() || lastTestClass != getClass() ) {
			buildSessionFactory( getXmlFiles() );
			lastTestClass = getClass();
		}
	}

	protected void runTest() throws Throwable {
		try {
			super.runTest();
			if ( session != null && session.isOpen() ) {
				if ( session.isConnected() ) {
					session.connection().rollback();
				}
				session.close();
				session = null;
				fail( "unclosed session" );
			}
			else {
				session = null;
			}
		}
		catch ( Throwable e ) {
			try {
				if ( session != null && session.isOpen() ) {
					if ( session.isConnected() ) {
						session.connection().rollback();
					}
					session.close();
				}
			}
			catch ( Exception ignore ) {
			}
			try {
				if ( sessions != null ) {
					sessions.close();
					sessions = null;
				}
			}
			catch ( Exception ignore ) {
			}
			throw e;
		}
	}

	public Session openSession() throws HibernateException {
		session = getSessions().openSession();
		return session;
	}

	public Session openSession(Interceptor interceptor) throws HibernateException {
		session = getSessions().openSession( interceptor );
		return session;
	}

	protected String[] getXmlFiles() {
		return new String[] { };
	}

	protected void setSessions(SessionFactory sessions) {
		TestCase.sessions = sessions;
	}

	protected SessionFactory getSessions() {
		return sessions;
	}

	protected void setDialect(Dialect dialect) {
		TestCase.dialect = dialect;
	}

	protected Dialect getDialect() {
		return dialect;
	}

	protected static void setCfg(Configuration cfg) {
		TestCase.cfg = cfg;
	}

	protected static Configuration getCfg() {
		return cfg;
	}

	protected void configure(Configuration cfg) {
		//needs to register all event listeners:
		cfg.setListener( "post-update", "org.hibernate.search.event.FullTextIndexEventListener" );
		cfg.setListener( "post-insert", "org.hibernate.search.event.FullTextIndexEventListener" );
		cfg.setListener( "post-delete", "org.hibernate.search.event.FullTextIndexEventListener" );
		cfg.setListener( "post-collection-recreate", "org.hibernate.search.event.FullTextIndexEventListener" );
		cfg.setListener( "post-collection-remove", "org.hibernate.search.event.FullTextIndexEventListener" );
		cfg.setListener( "post-collection-update", "org.hibernate.search.event.FullTextIndexEventListener" );
		
		cfg.setListeners( "flush", new FlushEventListener[]{new DefaultFlushEventListener(), new FullTextIndexEventListener()} );

		cfg.setProperty( "hibernate.search.default.directory_provider", RAMDirectoryProvider.class.getName() );
		cfg.setProperty( org.hibernate.search.Environment.ANALYZER_CLASS, StopAnalyzer.class.getName() );
	}

	protected boolean recreateSchema() {
		return true;
	}
}