/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.massindexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;

import java.util.concurrent.CompletableFuture;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.SessionFactory;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.cfg.EngineSettings;
import org.hibernate.search.engine.reporting.IndexFailureContext;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.automaticindexing.AutomaticIndexingStrategyName;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils;
import org.hibernate.search.util.impl.test.ExceptionMatcherBuilder;
import org.hibernate.search.util.impl.test.rule.ExpectedLog4jLog;
import org.hibernate.search.util.impl.test.rule.StaticCounters;

import org.junit.Rule;
import org.junit.Test;

import org.apache.log4j.Level;

public class MassIndexingFailureHandlerIT {

	public static final String TITLE_1 = "Oliver Twist";
	public static final String AUTHOR_1 = "Charles Dickens";
	public static final String TITLE_2 = "Ulysses";
	public static final String AUTHOR_2 = "James Joyce";
	public static final String TITLE_3 = "Frankenstein";
	public static final String AUTHOR_3 = "Mary Shelley";

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	@Rule
	public ExpectedLog4jLog log = ExpectedLog4jLog.create();

	@Rule
	public StaticCounters staticCounters = new StaticCounters();

	@Test
	public void defaultHandler() {
		SessionFactory sessionFactory = setup( null );

		OrmUtils.withinSession( sessionFactory, session -> {
			SearchSession searchSession = Search.session( session );
			MassIndexer indexer = searchSession.massIndexer();

			CompletableFuture<?> indexingFuture = new CompletableFuture<>();
			indexingFuture.completeExceptionally( new SimulatedFailure( "Indexing error" ) );

			// add operations on indexes can follow any random order,
			// since they are executed by different threads
			backendMock.expectWorksAnyOrder(
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
					.processedThenExecuted();
			backendMock.expectWorksAnyOrder(
					Book.INDEX, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE
			)
					.add( "2", b -> b
							.field( "title", TITLE_2 )
							.field( "author", AUTHOR_2 )
					)
					.processedThenExecuted( indexingFuture );

			// purgeAtStart, optimizeAfterPurge and purgeAtStart flags are active by default,
			// so we expect 1 purge, 2 optimize and 1 flush calls in this order:
			backendMock.expectIndexScopeWorks( Book.INDEX, session.getTenantIdentifier() )
					.purge()
					.optimize()
					.optimize()
					.flush();

			log.expectEvent(
					Level.ERROR,
					ExceptionMatcherBuilder.isException( SimulatedFailure.class )
							.withMessage( "Indexing error" )
							.build(),
					"Unable to index instance of type " + Book.class.getName()
			)
					.once();

			try {
				indexer.startAndWait();
			}
			catch (InterruptedException e) {
				fail( "Unexpected InterruptedException: " + e.getMessage() );
			}

		} );

		backendMock.verifyExpectationsMet();
	}

	@Test
	public void custom() {
		assertThat( staticCounters.get( CountingFailureHandler.CREATE ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( CountingFailureHandler.HANDLE_CONTEXT ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( CountingFailureHandler.HANDLE_EXCEPTION ) ).isEqualTo( 0 );

		SessionFactory sessionFactory = setup( CountingFailureHandler.class.getName() );

		assertThat( staticCounters.get( CountingFailureHandler.CREATE ) ).isEqualTo( 1 );
		assertThat( staticCounters.get( CountingFailureHandler.HANDLE_CONTEXT ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( CountingFailureHandler.HANDLE_EXCEPTION ) ).isEqualTo( 0 );

		OrmUtils.withinSession( sessionFactory, session -> {
			SearchSession searchSession = Search.session( session );
			MassIndexer indexer = searchSession.massIndexer();

			CompletableFuture<?> indexingFuture = new CompletableFuture<>();
			indexingFuture.completeExceptionally( new SimulatedFailure( "Indexing error" ) );

			// add operations on indexes can follow any random order,
			// since they are executed by different threads
			backendMock.expectWorksAnyOrder(
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
					.processedThenExecuted();
			backendMock.expectWorksAnyOrder(
					Book.INDEX, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE
			)
					.add( "2", b -> b
							.field( "title", TITLE_2 )
							.field( "author", AUTHOR_2 )
					)
					.processedThenExecuted( indexingFuture );

			// purgeAtStart, optimizeAfterPurge and purgeAtStart flags are active by default,
			// so we expect 1 purge, 2 optimize and 1 flush calls in this order:
			backendMock.expectIndexScopeWorks( Book.INDEX, session.getTenantIdentifier() )
					.purge()
					.optimize()
					.optimize()
					.flush();

			try {
				indexer.startAndWait();
			}
			catch (InterruptedException e) {
				fail( "Unexpected InterruptedException: " + e.getMessage() );
			}

		} );

		backendMock.verifyExpectationsMet();

		assertThat( staticCounters.get( CountingFailureHandler.CREATE ) ).isEqualTo( 1 );
		assertThat( staticCounters.get( CountingFailureHandler.HANDLE_CONTEXT ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( CountingFailureHandler.HANDLE_EXCEPTION ) ).isEqualTo( 1 );
	}

	private SessionFactory setup(String failureHandler) {
		backendMock.expectAnySchema( Book.INDEX );

		SessionFactory sessionFactory = ormSetupHelper.start()
				.withPropertyRadical( HibernateOrmMapperSettings.Radicals.AUTOMATIC_INDEXING_STRATEGY, AutomaticIndexingStrategyName.NONE )
				.withPropertyRadical( EngineSettings.Radicals.BACKGROUND_FAILURE_HANDLER, failureHandler )
				.setup( Book.class );

		backendMock.verifyExpectationsMet();

		OrmUtils.withinTransaction( sessionFactory, session -> {
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

	public static class CountingFailureHandler implements FailureHandler {

		public static StaticCounters.Key CREATE = StaticCounters.createKey();
		public static StaticCounters.Key HANDLE_CONTEXT = StaticCounters.createKey();
		public static StaticCounters.Key HANDLE_EXCEPTION = StaticCounters.createKey();

		public CountingFailureHandler() {
			StaticCounters.get().increment( CREATE );
		}

		@Override
		public void handle(IndexFailureContext context) {
			StaticCounters.get().increment( HANDLE_CONTEXT );
		}

		@Override
		public void handleException(String errorMsg, Throwable exception) {
			StaticCounters.get().increment( HANDLE_EXCEPTION );
		}
	}

	private static class SimulatedFailure extends RuntimeException {
		SimulatedFailure(String message) {
			super( message );
		}
	}
}
