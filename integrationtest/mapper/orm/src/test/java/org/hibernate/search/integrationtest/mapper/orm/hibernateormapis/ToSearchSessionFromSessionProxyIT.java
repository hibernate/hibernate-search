/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.hibernateormapis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.runInTransaction;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.lang.reflect.Proxy;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.SharedSessionContract;
import org.hibernate.context.internal.ThreadLocalSessionContext;
import org.hibernate.engine.jdbc.LobCreationContext;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * @author Emmanuel Bernard
 */
@PortedFromSearch5(original = "org.hibernate.search.test.session.SessionTest")
@TestForIssue(jiraKey = "HSEARCH-4108")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ToSearchSessionFromSessionProxyIT {

	//EventSource, org.hibernate.Session, LobCreationContext
	private static final Class<?>[] SESS_PROXY_INTERFACES = new Class[] {
			Session.class,
			LobCreationContext.class,
			EventSource.class,
			SessionImplementor.class,
			SharedSessionContract.class
	};

	@RegisterExtension
	public static BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public static OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );
	private SessionFactory sessionFactory;


	@BeforeAll
	void setup() {
		backendMock.expectAnySchema( IndexedEntity.NAME );
		sessionFactory = ormSetupHelper.start()
				// for this test we explicitly set the auto commit mode since we are not explicitly starting a transaction
				// which could be a problem in some databases.
				.withProperty( "hibernate.connection.autocommit", "true" )
				//needed for testThreadBoundSessionWrappingOutOfTransaction
				.withProperty( "hibernate.current_session_context_class", "thread" )
				.withAnnotatedTypes( IndexedEntity.class )
				.setup();
	}

	@Test
	void testSessionWrapper() {
		with( sessionFactory ).runNoTransaction( s -> {
			DelegationWrapper wrapper = new DelegationWrapper( s );
			Session wrapped = (Session) Proxy.newProxyInstance(
					Session.class.getClassLoader(),
					SESS_PROXY_INTERFACES,
					wrapper
			);
			try {
				SearchSession searchSession = Search.session( wrapped );
				assertThat( searchSession ).isNotNull();
				assertThat( searchSession.toEntityManager() ).isSameAs( wrapped );
				assertThat( searchSession.toOrmSession() ).isSameAs( wrapped );
			}
			catch (ClassCastException e) {
				e.printStackTrace();
				fail( e.toString() );
			}
			wrapped.close();
		} );
	}

	@Test
	void testThreadBoundSessionWrappingInTransaction() {
		final Session sessionFromFirstThread = sessionFactory.getCurrentSession();
		try {
			runInTransaction( sessionFromFirstThread, ignored -> {
				SearchSession searchSessionFromFirstThread = Search.session( sessionFromFirstThread );
				assertThat( searchSessionFromFirstThread ).isNotNull();
				assertThat( searchSessionFromFirstThread.toEntityManager() ).isSameAs( sessionFromFirstThread );
				assertThat( searchSessionFromFirstThread.toOrmSession() ).isSameAs( sessionFromFirstThread );
				ThreadPoolExecutor executorService = new ThreadPoolExecutor(
						1, 1, 0L, TimeUnit.MILLISECONDS,
						new LinkedBlockingQueue<>( 10 )
				);
				CompletableFuture<?> future = Futures.runAsync(
						() -> {
							Session sessionFromOtherThread = sessionFactory.getCurrentSession();
							assertThat( sessionFromOtherThread ).isNotSameAs( sessionFromFirstThread );
							SearchSession searchSessionFromOtherThread = Search.session( sessionFromOtherThread );
							assertThat( searchSessionFromOtherThread ).isNotNull();
							assertThat( searchSessionFromOtherThread.toEntityManager() ).isSameAs( sessionFromOtherThread );
							assertThat( searchSessionFromOtherThread.toOrmSession() ).isSameAs( sessionFromOtherThread );
						},
						executorService
				);
				Futures.unwrappedExceptionJoin( future );
			} );
		}
		finally {
			//clean up after the mess
			ThreadLocalSessionContext.unbind( sessionFactory );
		}
	}

	@Test
	void testThreadBoundSessionWrappingOutOfTransaction() {
		final Session sessionFromFirstThread = sessionFactory.getCurrentSession();
		try {
			SearchSession searchSessionFromFirstThread = Search.session( sessionFromFirstThread );
			assertThat( searchSessionFromFirstThread ).isNotNull();
			assertThat( searchSessionFromFirstThread.toEntityManager() ).isSameAs( sessionFromFirstThread );
			assertThat( searchSessionFromFirstThread.toOrmSession() ).isSameAs( sessionFromFirstThread );
			ThreadPoolExecutor executorService = new ThreadPoolExecutor(
					1, 1, 0L, TimeUnit.MILLISECONDS,
					new LinkedBlockingQueue<>( 10 )
			);
			CompletableFuture<?> future = Futures.runAsync(
					() -> {
						Session sessionFromOtherThread = sessionFactory.getCurrentSession();
						assertThat( sessionFromOtherThread ).isNotSameAs( sessionFromFirstThread );
						SearchSession searchSessionFromOtherThread = Search.session( sessionFromOtherThread );
						assertThat( searchSessionFromOtherThread ).isNotNull();
						assertThat( searchSessionFromOtherThread.toEntityManager() ).isSameAs( sessionFromOtherThread );
						assertThat( searchSessionFromOtherThread.toOrmSession() ).isSameAs( sessionFromOtherThread );
					},
					executorService
			);
			Futures.unwrappedExceptionJoin( future );
		}
		finally {
			//clean up after the mess
			ThreadLocalSessionContext.unbind( sessionFactory );
		}
	}

	@Entity(name = IndexedEntity.NAME)
	@Indexed
	public static class IndexedEntity {

		public static final String NAME = "indexed";

		@Id
		private Integer id;

		@FullTextField
		private String text;

		@Override
		public String toString() {
			return "IndexedEntity[id=" + id + "]";
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}

	}
}
