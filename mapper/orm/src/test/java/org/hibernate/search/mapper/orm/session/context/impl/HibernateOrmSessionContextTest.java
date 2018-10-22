/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.session.context.impl;

import static org.junit.Assert.assertSame;

import javax.persistence.EntityManager;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.engine.mapper.session.context.SessionContext;
import org.hibernate.search.mapper.orm.session.context.HibernateOrmSessionContext;
import org.hibernate.search.util.SearchException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.easymock.EasyMockSupport;

public class HibernateOrmSessionContextTest extends EasyMockSupport {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private final SessionImplementor sessionMock = createMock( SessionImplementor.class );

	@Test
	public void unwrap_hibernateOrmSessionContext() {
		SessionContext sessionContext = new HibernateOrmSessionContextImpl( sessionMock );

		resetAll();
		replayAll();
		HibernateOrmSessionContext result = sessionContext.unwrap( HibernateOrmSessionContext.class );
		verifyAll();
		assertSame( sessionContext, result );
	}

	@Test
	public void unwrap_entityManager() {
		SessionContext sessionContext = new HibernateOrmSessionContextImpl( sessionMock );

		resetAll();
		replayAll();
		EntityManager result = sessionContext.unwrap( EntityManager.class );
		verifyAll();
		assertSame( sessionMock, result );
	}

	@Test
	public void unwrap_session() {
		SessionContext sessionContext = new HibernateOrmSessionContextImpl( sessionMock );

		resetAll();
		replayAll();
		Session result = sessionContext.unwrap( Session.class );
		verifyAll();
		assertSame( sessionMock, result );
	}

	@Test
	public void unwrap_spiType() {
		SessionContext sessionContext = new HibernateOrmSessionContextImpl( sessionMock );

		resetAll();
		replayAll();
		thrown.expect( SearchException.class );
		thrown.expectMessage( "'" + HibernateOrmSessionContext.class.getName() + "'" );
		thrown.expectMessage( "cannot be unwrapped to '" + SessionImplementor.class.getName() + "'" );
		try {
			sessionContext.unwrap( SessionImplementor.class );
		}
		finally {
			verifyAll();
		}
	}

	@Test
	public void unwrap_customSessionSubtype() {
		SessionContext sessionContext = new HibernateOrmSessionContextImpl( sessionMock );

		resetAll();
		replayAll();
		thrown.expect( SearchException.class );
		thrown.expectMessage( "'" + HibernateOrmSessionContext.class.getName() + "'" );
		thrown.expectMessage( "cannot be unwrapped to '" + MySession.class.getName() + "'" );
		try {
			sessionContext.unwrap( MySession.class );
		}
		finally {
			verifyAll();
		}
	}

	@Test
	public void getSession() {
		HibernateOrmSessionContext sessionContext = new HibernateOrmSessionContextImpl( sessionMock );
		resetAll();
		replayAll();
		Session result = sessionContext.getSession();
		verifyAll();
		assertSame( sessionMock, result );
	}

	interface MySession extends Session {

	}
}