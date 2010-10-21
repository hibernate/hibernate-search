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
package org.hibernate.search.test.session;

import java.lang.reflect.Proxy;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.context.ThreadLocalSessionContext;
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

	public void testThreadBoundSessionWrappingOutOfTransaction() throws Exception {
		final Session session = getSessions().getCurrentSession();
		try {
			FullTextSession fts = Search.getFullTextSession( session );
			//success
		}
		finally {
			//clean up after the mess
			ThreadLocalSessionContext.unbind( getSessions() );
		}
	}

	protected Class<?>[] getAnnotatedClasses() {
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
		//needed for testThreadBoundSessionWrappingOutOfTransaction
		cfg.setProperty( "hibernate.current_session_context_class", "thread" );
	}
}
