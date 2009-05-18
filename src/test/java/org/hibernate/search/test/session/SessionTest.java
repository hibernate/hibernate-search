//$Id$
package org.hibernate.search.test.session;

import java.lang.reflect.Proxy;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestCase;

/**
 * @author Emmanuel Bernard
 */
public class SessionTest extends SearchTestCase {

	private static final Class[] SESS_PROXY_INTERFACES = new Class[] {
			org.hibernate.classic.Session.class,
			org.hibernate.engine.SessionImplementor.class,
			org.hibernate.jdbc.JDBCContext.Context.class,
			org.hibernate.event.EventSource.class
	};

	public void testSessionWrapper() throws Exception {
		Session s = openSession();
		DelegationWrapper wrapper = new DelegationWrapper( s );
		Session wrapped = ( Session ) Proxy.newProxyInstance(
				org.hibernate.classic.Session.class.getClassLoader(),
				SESS_PROXY_INTERFACES,
				wrapper
		);
		try {
			Search.getFullTextSession( wrapped );
		}
		catch ( ClassCastException e ) {
			e.printStackTrace();
			fail( e.toString() );
		}
		wrapped.close();
	}

	public void testDetachedCriteria() throws Exception {
		FullTextSession s = Search.getFullTextSession( openSession() );
		DetachedCriteria dc = DetachedCriteria.forClass( Email.class );
		try {
			Criteria c = dc.getExecutableCriteria( s ).setMaxResults( 10 );
			c.list();
		}
		catch ( ClassCastException e ) {
			e.printStackTrace();
			fail( e.toString() );
		}
		s.close();
	}

	protected Class[] getMappings() {
		return new Class[] {
				Email.class,
				Domain.class
		};
	}

	protected void configure(Configuration cfg) {
		super.configure( cfg );
		// for this test we explcitly set the auto commit mode since we are not explcitly starting a transaction
		// which could be a problem in some databases.
		cfg.setProperty( "hibernate.connection.autocommit", "true" );
	}
}
