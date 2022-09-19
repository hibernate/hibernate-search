/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.hibernateormapis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.runInTransaction;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.lang.reflect.Proxy;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.Session;
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
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.ReusableOrmSetupHolder;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

/**
 * @author Emmanuel Bernard
 */
@PortedFromSearch5(original = "org.hibernate.search.test.session.SessionTest")
@TestForIssue(jiraKey = "HSEARCH-4108")
public class ToSearchSessionFromSessionProxyIT {

	//EventSource, org.hibernate.Session, LobCreationContext
	private static final Class<?>[] SESS_PROXY_INTERFACES = new Class[] {
			Session.class,
			LobCreationContext.class,
			EventSource.class,
			SessionImplementor.class,
			SharedSessionContract.class
	};

	@ClassRule
	public static BackendMock backendMock = new BackendMock();

	@ClassRule
	public static ReusableOrmSetupHolder setupHolder = ReusableOrmSetupHolder.withBackendMock( backendMock );

	@Rule
	public MethodRule setupHolderMethodRule = setupHolder.methodRule();

	@ReusableOrmSetupHolder.Setup
	public void setup(OrmSetupHelper.SetupContext setupContext) {
		backendMock.expectAnySchema( IndexedEntity.NAME );
		setupContext
				// for this test we explicitly set the auto commit mode since we are not explicitly starting a transaction
				// which could be a problem in some databases.
				.withProperty( "hibernate.connection.autocommit", "true" )
				//needed for testThreadBoundSessionWrappingOutOfTransaction
				.withProperty( "hibernate.current_session_context_class", "thread" )
				.withAnnotatedTypes( IndexedEntity.class );
	}

	@Test
	public void testSessionWrapper() {
		setupHolder.runNoTransaction( s -> {
			DelegationWrapper wrapper = new DelegationWrapper( s );
			Session wrapped = (Session) Proxy.newProxyInstance(
					Session.class.getClassLoader(),
					SESS_PROXY_INTERFACES,
					wrapper
			);
			try {
				SearchSession searchSession = Search.session( wrapped );
				assertNotNull( searchSession );
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
	public void testThreadBoundSessionWrappingInTransaction() {
		final Session sessionFromFirstThread = setupHolder.sessionFactory().getCurrentSession();
		try {
			runInTransaction( sessionFromFirstThread, ignored -> {
				SearchSession searchSessionFromFirstThread = Search.session( sessionFromFirstThread );
				assertNotNull( searchSessionFromFirstThread );
				assertThat( searchSessionFromFirstThread.toEntityManager() ).isSameAs( sessionFromFirstThread );
				assertThat( searchSessionFromFirstThread.toOrmSession() ).isSameAs( sessionFromFirstThread );
				ThreadPoolExecutor executorService = new ThreadPoolExecutor(
						1, 1, 0L, TimeUnit.MILLISECONDS,
						new LinkedBlockingQueue<>( 10 )
				);
				CompletableFuture<?> future = Futures.runAsync(
						() -> {
							Session sessionFromOtherThread = setupHolder.sessionFactory().getCurrentSession();
							assertThat( sessionFromOtherThread ).isNotSameAs( sessionFromFirstThread );
							SearchSession searchSessionFromOtherThread = Search.session( sessionFromOtherThread );
							assertNotNull( searchSessionFromOtherThread );
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
			ThreadLocalSessionContext.unbind( setupHolder.sessionFactory() );
		}
	}

	@Test
	public void testThreadBoundSessionWrappingOutOfTransaction() {
		final Session sessionFromFirstThread = setupHolder.sessionFactory().getCurrentSession();
		try {
			SearchSession searchSessionFromFirstThread = Search.session( sessionFromFirstThread );
			assertNotNull( searchSessionFromFirstThread );
			assertThat( searchSessionFromFirstThread.toEntityManager() ).isSameAs( sessionFromFirstThread );
			assertThat( searchSessionFromFirstThread.toOrmSession() ).isSameAs( sessionFromFirstThread );
			ThreadPoolExecutor executorService = new ThreadPoolExecutor(
					1, 1, 0L, TimeUnit.MILLISECONDS,
					new LinkedBlockingQueue<>( 10 )
			);
			CompletableFuture<?> future = Futures.runAsync(
					() -> {
						Session sessionFromOtherThread = setupHolder.sessionFactory().getCurrentSession();
						assertThat( sessionFromOtherThread ).isNotSameAs( sessionFromFirstThread );
						SearchSession searchSessionFromOtherThread = Search.session( sessionFromOtherThread );
						assertNotNull( searchSessionFromOtherThread );
						assertThat( searchSessionFromOtherThread.toEntityManager() ).isSameAs( sessionFromOtherThread );
						assertThat( searchSessionFromOtherThread.toOrmSession() ).isSameAs( sessionFromOtherThread );
					},
					executorService
			);
			Futures.unwrappedExceptionJoin( future );
		}
		finally {
			//clean up after the mess
			ThreadLocalSessionContext.unbind( setupHolder.sessionFactory() );
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
