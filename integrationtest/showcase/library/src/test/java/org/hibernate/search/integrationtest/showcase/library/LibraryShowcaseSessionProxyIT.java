/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.showcase.library;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.persistence.EntityManager;

import org.hibernate.search.integrationtest.showcase.library.model.Book;
import org.hibernate.search.integrationtest.showcase.library.service.TestDataService;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.util.common.impl.Futures;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Service;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
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
@ActiveProfiles(resolver = TestActiveProfilesResolver.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class LibraryShowcaseSessionProxyIT {

	private static boolean needsInit;

	@Autowired
	private TestDataService testDataService;

	@Autowired
	private SingleSearchSessionService singleSearchSessionService;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@BeforeClass
	public static void beforeClass() {
		needsInit = true;
	}

	@Before
	public void before() {
		if ( needsInit ) {
			testDataService.initDefaultDataSet();
			needsInit = false;
		}
	}

	@Test
	public void search_library() {
		TransactionTemplate template = new TransactionTemplate( transactionManager );

		template.execute( status -> {
			Book bookFromThread1_1 = singleSearchSessionService.simulateSearch();
			assertThat( bookFromThread1_1 ).returns( true, singleSearchSessionService.entityManager::contains );

			// Same call from the same thread and transaction:
			// the same underlying session is used, so we should get the same entity instance
			Book bookFromThread1_2 = singleSearchSessionService.simulateSearch();
			assertThat( bookFromThread1_2 ).returns( bookFromThread1_1.getId(), Book::getId );
			assertThat( bookFromThread1_2 ).isSameAs( bookFromThread1_1 );

			// Same call from another thread: another underlying session is used, so we should a different entity instance
			ThreadPoolExecutor executorService = new ThreadPoolExecutor(
					1, 1, 0L, TimeUnit.MILLISECONDS,
					new LinkedBlockingQueue<>( 10 )
			);
			CompletableFuture<Book> future = CompletableFuture.supplyAsync(
					() -> template.execute( status2 -> {
						Book bookFromThread2 = singleSearchSessionService.simulateSearch();
						assertThat( bookFromThread2 ).returns( true, singleSearchSessionService.entityManager::contains );
						assertThat( bookFromThread2 ).returns( bookFromThread1_1.getId(), Book::getId );
						assertThat( bookFromThread2 ).isNotSameAs( bookFromThread1_1 );
						return bookFromThread2;
					} ),
					executorService
			);
			Book bookFromThread2 = Futures.unwrappedExceptionJoin( future );
			assertThat( bookFromThread2 ).returns( false, singleSearchSessionService.entityManager::contains );

			return null;
		} );
	}

	@Service
	public static class SingleSearchSessionService {

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

		public Book simulateSearch() {
			return searchSession.search( Book.class ).where( f -> f.matchAll() )
					.sort( f -> f.field( "title_sort" ) )
					.fetchHits( 1 ).get( 0 );
		}

	}
}
