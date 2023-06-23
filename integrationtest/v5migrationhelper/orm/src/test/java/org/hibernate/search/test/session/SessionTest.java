/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.session;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.lang.reflect.Proxy;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.SharedSessionContract;
import org.hibernate.context.internal.ThreadLocalSessionContext;
import org.hibernate.engine.jdbc.LobCreationContext;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.junit.PortedToSearch6;

import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * @author Emmanuel Bernard
 */
@Category(PortedToSearch6.class)
public class SessionTest extends SearchTestBase {

	//EventSource, org.hibernate.Session, LobCreationContext
	private static final Class<?>[] SESS_PROXY_INTERFACES = new Class[] {
			org.hibernate.Session.class,
			LobCreationContext.class,
			EventSource.class,
			SessionImplementor.class,
			SharedSessionContract.class
	};

	@Test
	public void testSessionWrapper() throws Exception {
		Session s = openSession();
		DelegationWrapper wrapper = new DelegationWrapper( s );
		Session wrapped = (Session) Proxy.newProxyInstance(
				org.hibernate.Session.class.getClassLoader(),
				SESS_PROXY_INTERFACES,
				wrapper
		);
		try {
			Search.getFullTextSession( wrapped );
		}
		catch (ClassCastException e) {
			e.printStackTrace();
			fail( e.toString() );
		}
		wrapped.close();
	}

	@Test
	public void testThreadBoundSessionWrappingOutOfTransaction() throws Exception {
		final Session session = getSessionFactory().getCurrentSession();
		try {
			FullTextSession fts = Search.getFullTextSession( session );
			assertNotNull( fts );
			//success
		}
		finally {
			//clean up after the mess
			ThreadLocalSessionContext.unbind( getSessionFactory() );
		}
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Email.class,
				Domain.class
		};
	}

	@Override
	public void configure(Map<String, Object> cfg) {
		// for this test we explicitly set the auto commit mode since we are not explicitly starting a transaction
		// which could be a problem in some databases.
		cfg.put( "hibernate.connection.autocommit", "true" );
		//needed for testThreadBoundSessionWrappingOutOfTransaction
		cfg.put( "hibernate.current_session_context_class", "thread" );
	}
}
