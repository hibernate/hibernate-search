/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.massindexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.concurrent.CompletableFuture;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.SessionFactory;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.cfg.EngineSettings;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingMonitor;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.rule.StaticCounters;

import org.junit.Rule;
import org.junit.Test;

public class MassIndexingMonitorIT {

	public static final String TITLE_1 = "Oliver Twist";
	public static final String AUTHOR_1 = "Charles Dickens";
	public static final String TITLE_2 = "Ulysses";
	public static final String AUTHOR_2 = "James Joyce";
	public static final String TITLE_3 = "Frankenstein";
	public static final String AUTHOR_3 = "Mary Shelley";

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	@Rule
	public StaticCounters staticCounters = new StaticCounters();

	@Test
	public void simple() {
		SessionFactory sessionFactory = setup( null );

		with( sessionFactory ).runNoTransaction( session -> {
			SearchSession searchSession = Search.session( session );
			MassIndexer indexer = searchSession.massIndexer();

			CompletableFuture<?> indexingFuture = new CompletableFuture<>();
			indexingFuture.completeExceptionally( new SimulatedFailure( "Indexing error" ) );

			// add operations on indexes can follow any random order,
			// since they are executed by different threads
			backendMock.expectWorks(
					Book.INDEX, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE
			)
					.add( "1", b -> b
							.field( "title", TITLE_1 )
							.field( "author", AUTHOR_1 )
					)
					.add( "3", b -> b
							.field( "title", TITLE_3 )
							.field( "author", AUTHOR_3 )
					)
					.createAndExecuteFollowingWorks( indexingFuture )
					.add( "2", b -> b
							.field( "title", TITLE_2 )
							.field( "author", AUTHOR_2 )
					);

			// purgeAtStart and mergeSegmentsAfterPurge are enabled by default,
			// so we expect 1 purge, 1 mergeSegments and 1 flush calls in this order:
			backendMock.expectIndexScaleWorks( Book.INDEX, session.getTenantIdentifier() )
					.purge()
					.mergeSegments()
					.flush()
					.refresh();

			try {
				indexer.monitor( new StaticCountersMonitor() )
						.startAndWait();
			}
			catch (SearchException ignored) {
				// Expected, but not relevant to this test
			}
			catch (InterruptedException e) {
				fail( "Unexpected InterruptedException: " + e.getMessage() );
			}
		} );

		backendMock.verifyExpectationsMet();

		assertThat( staticCounters.get( StaticCountersMonitor.LOADED ) ).isEqualTo( 3 );
		assertThat( staticCounters.get( StaticCountersMonitor.BUILT ) ).isEqualTo( 3 );
		assertThat( staticCounters.get( StaticCountersMonitor.ADDED ) ).isEqualTo( 2 );
		assertThat( staticCounters.get( StaticCountersMonitor.TOTAL ) ).isEqualTo( 3 );
		assertThat( staticCounters.get( StaticCountersMonitor.INDEXING_COMPLETED ) ).isEqualTo( 1 );
	}

	private SessionFactory setup(String failureHandler) {
		backendMock.expectAnySchema( Book.INDEX );

		SessionFactory sessionFactory = ormSetupHelper.start()
				.withPropertyRadical( HibernateOrmMapperSettings.Radicals.INDEXING_LISTENERS_ENABLED, false )
				.withPropertyRadical( EngineSettings.BACKGROUND_FAILURE_HANDLER, failureHandler )
				.setup( Book.class );

		backendMock.verifyExpectationsMet();

		with( sessionFactory ).runInTransaction( session -> {
			session.persist( new Book( 1, TITLE_1, AUTHOR_1 ) );
			session.persist( new Book( 2, TITLE_2, AUTHOR_2 ) );
			session.persist( new Book( 3, TITLE_3, AUTHOR_3 ) );
		} );

		return sessionFactory;
	}

	@Entity
	@Table(name = "book")
	@Indexed(index = Book.INDEX)
	public static class Book {

		public static final String INDEX = "Book";

		@Id
		private Integer id;

		@GenericField
		private String title;

		@GenericField
		private String author;

		public Book() {
		}

		public Book(Integer id, String title, String author) {
			this.id = id;
			this.title = title;
			this.author = author;
		}

		public Integer getId() {
			return id;
		}

		public String getTitle() {
			return title;
		}

		public String getAuthor() {
			return author;
		}
	}

	public static class StaticCountersMonitor implements MassIndexingMonitor {

		public static StaticCounters.Key ADDED = StaticCounters.createKey();
		public static StaticCounters.Key BUILT = StaticCounters.createKey();
		public static StaticCounters.Key LOADED = StaticCounters.createKey();
		public static StaticCounters.Key TOTAL = StaticCounters.createKey();
		public static StaticCounters.Key INDEXING_COMPLETED = StaticCounters.createKey();

		@Override
		public void documentsAdded(long increment) {
			StaticCounters.get().add( ADDED, (int) increment );
		}

		@Override
		public void documentsBuilt(long increment) {
			StaticCounters.get().add( BUILT, (int) increment );
		}

		@Override
		public void entitiesLoaded(long increment) {
			StaticCounters.get().add( LOADED, (int) increment );
		}

		@Override
		public void addToTotalCount(long increment) {
			StaticCounters.get().add( TOTAL, (int) increment );
		}

		@Override
		public void indexingCompleted() {
			StaticCounters.get().increment( INDEXING_COMPLETED );
		}
	}

	private static class SimulatedFailure extends RuntimeException {
		SimulatedFailure(String message) {
			super( message );
		}
	}
}
