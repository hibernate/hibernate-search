/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

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
import org.hibernate.search.testsupport.junit.Tags;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * @author Emmanuel Bernard
 */
@Tag(Tags.PORTED_TO_SEARCH_6)
class SessionTest extends SearchTestBase {

	//EventSource, org.hibernate.Session, LobCreationContext
	private static final Class<?>[] SESS_PROXY_INTERFACES = new Class[] {
			org.hibernate.Session.class,
			LobCreationContext.class,
			EventSource.class,
			SessionImplementor.class,
			SharedSessionContract.class
	};

	@Test
	void testSessionWrapper() {
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
	void testThreadBoundSessionWrappingOutOfTransaction() {
		final Session session = getSessionFactory().getCurrentSession();
		try {
			FullTextSession fts = Search.getFullTextSession( session );
			assertThat( fts ).isNotNull();
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
