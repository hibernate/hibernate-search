/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.spring.sessionproxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendUtils.reference;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;

import org.hibernate.search.integrationtest.spring.testsupport.AbstractSpringITConfig;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.rule.StubSearchWorkBehavior;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Test that when using an EntityManager proxy that relies on a different underlying EntityManager for each thread,
 *  one can create a single SearchSession for all threads, and it will correctly use the correct EntityManager
 *  depending on the current thread.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class SessionProxyIT {

	@Configuration
	@EntityScan
	@ComponentScan(basePackageClasses = SessionProxyIT.class)
	public static class SpringConfig extends AbstractSpringITConfig {
	}

	private static boolean needsInit;

	@Autowired
	@Rule
	public BackendMock backendMock;

	@Autowired
	private HelperService helperService;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@BeforeClass
	public static void beforeClass() {
		needsInit = true;
	}

	@Before
	public void before() {
		if ( needsInit ) {
			TransactionTemplate template = new TransactionTemplate( transactionManager );
			backendMock.inLenientMode( () -> template.execute( ignored -> {
				helperService.initData();
				return null;
			} ) );
			needsInit = false;
		}
	}

	@Test
	public void useSingleSearchSessionFromMultipleThreads() {
		TransactionTemplate template = new TransactionTemplate( transactionManager );

		template.execute( status -> {
			IndexedEntity entityFromThread1_1 = helperService.simulateSearch( backendMock );
			assertThat( entityFromThread1_1 ).returns( true, helperService.entityManager::contains );

			// Same call from the same thread and transaction:
			// the same underlying session is used, so we should get the same entity instance
			IndexedEntity entityFromThread1_2 = helperService.simulateSearch( backendMock );
			assertThat( entityFromThread1_2 ).returns( entityFromThread1_1.getId(), IndexedEntity::getId );
			assertThat( entityFromThread1_2 ).isSameAs( entityFromThread1_1 );

			// Same call from another thread: another underlying session is used, so we should get a different entity instance
			ThreadPoolExecutor executorService = new ThreadPoolExecutor(
					1, 1, 0L, TimeUnit.MILLISECONDS,
					new LinkedBlockingQueue<>( 10 )
			);
			CompletableFuture<IndexedEntity> future = CompletableFuture.supplyAsync(
					() -> template.execute( status2 -> {
						IndexedEntity entityFromThread2 = helperService.simulateSearch( backendMock );
						assertThat( entityFromThread2 ).returns( true, helperService.entityManager::contains );
						assertThat( entityFromThread2 ).returns( entityFromThread1_1.getId(), IndexedEntity::getId );
						assertThat( entityFromThread2 ).isNotSameAs( entityFromThread1_1 );
						return entityFromThread2;
					} ),
					executorService
			);
			IndexedEntity entityFromThread2 = Futures.unwrappedExceptionJoin( future );
			assertThat( entityFromThread2 ).returns( false, helperService.entityManager::contains );

			return null;
		} );
	}

	@Service
	public static class HelperService {

		public EntityManager entityManager;

		// Use the SAME SearchSession instance from all threads
		// This will only work if Hibernate Search takes care of fetching the thread-local ORM Session
		// every time a method is called on the SearchSession.
		public SearchSession searchSession;

		@Autowired
		public void init(EntityManager entityManager) {
			this.entityManager = entityManager;
			this.searchSession = Search.session( entityManager );
		}

		public void initData() {
			entityManager.persist( new IndexedEntity( 1 ) );
		}

		public IndexedEntity simulateSearch(BackendMock backendMock) {
			backendMock.expectSearchObjects( IndexedEntity.NAME,
					StubSearchWorkBehavior.of( 1, reference( IndexedEntity.NAME, "1" ) ) );
			return searchSession.search( IndexedEntity.class ).where( f -> f.matchAll() )
					.fetchAllHits().get( 0 );
		}
	}

	@Entity(name = IndexedEntity.NAME)
	@Indexed
	public static class IndexedEntity {

		public static final String NAME = "Indexed";

		@Id
		private Integer id;

		IndexedEntity() {
		}

		public IndexedEntity(Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}
}
