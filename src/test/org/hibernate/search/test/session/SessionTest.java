//$Id$
package org.hibernate.search.test.session;

import java.lang.reflect.Proxy;

import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.Search;
import org.hibernate.search.FullTextSession;
import org.hibernate.Session;

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
		Session s = openSession( );
		DelegationWrapper wrapper = new DelegationWrapper( s );
		Session wrapped = (Session) Proxy.newProxyInstance(
				org.hibernate.classic.Session.class.getClassLoader(),
		        SESS_PROXY_INTERFACES,
		        wrapper
			);
		try {
			FullTextSession fts = Search.createFullTextSession( wrapped );
		}
		catch( ClassCastException e ) {
			e.printStackTrace( );
			fail(e.toString());
		}
		wrapped.close();
	}

	protected Class[] getMappings() {
		return new Class[] {
				Email.class
		};
	}
}
