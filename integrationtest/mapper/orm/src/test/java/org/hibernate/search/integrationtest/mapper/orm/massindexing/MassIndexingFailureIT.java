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
import java.util.concurrent.atomic.AtomicBoolean;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.SessionFactory;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.cfg.EngineSettings;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.automaticindexing.AutomaticIndexingStrategyName;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.stub.StubFailureHandler;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubIndexScopeWork;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils;
import org.hibernate.search.util.impl.test.ExceptionMatcherBuilder;
import org.hibernate.search.util.impl.test.rule.ExpectedLog4jLog;
import org.hibernate.search.util.impl.test.rule.StaticCounters;

import org.junit.Rule;
import org.junit.Test;

import org.apache.log4j.Level;

public class MassIndexingFailureIT {

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
	public void indexing_defaultHandler() {
		SessionFactory sessionFactory = setup( null );

		log.expectEvent(
				Level.ERROR,
				ExceptionMatcherBuilder.isException( SimulatedFailure.class )
						.withMessage( "Indexing failure" )
						.build(),
				"Indexing instance of type " + Book.class.getName()
		)
				.once();

		doMassIndexing(
				sessionFactory,
				expectIndexScopeWork( StubIndexScopeWork.Type.PURGE, ExecutionExpectation.SUCCEED ),
				expectIndexScopeWork( StubIndexScopeWork.Type.OPTIMIZE, ExecutionExpectation.SUCCEED ),
				expectIndexingWorks( ExecutionExpectation.FAIL ),
				expectIndexScopeWork( StubIndexScopeWork.Type.OPTIMIZE, ExecutionExpectation.SUCCEED ),
				expectIndexScopeWork( StubIndexScopeWork.Type.FLUSH, ExecutionExpectation.SUCCEED )
		);
	}

	@Test
	public void indexing_customHandler() {
		assertThat( staticCounters.get( StubFailureHandler.CREATE ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_INDEX_CONTEXT ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_GENERIC_CONTEXT ) ).isEqualTo( 0 );

		SessionFactory sessionFactory = setup( StubFailureHandler.class.getName() );

		assertThat( staticCounters.get( StubFailureHandler.CREATE ) ).isEqualTo( 1 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_INDEX_CONTEXT ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_GENERIC_CONTEXT ) ).isEqualTo( 0 );

		doMassIndexing(
				sessionFactory,
				expectIndexScopeWork( StubIndexScopeWork.Type.PURGE, ExecutionExpectation.SUCCEED ),
				expectIndexScopeWork( StubIndexScopeWork.Type.OPTIMIZE, ExecutionExpectation.SUCCEED ),
				expectIndexingWorks( ExecutionExpectation.FAIL ),
				expectIndexScopeWork( StubIndexScopeWork.Type.OPTIMIZE, ExecutionExpectation.SUCCEED ),
				expectIndexScopeWork( StubIndexScopeWork.Type.FLUSH, ExecutionExpectation.SUCCEED )
		);

		assertThat( staticCounters.get( StubFailureHandler.CREATE ) ).isEqualTo( 1 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_INDEX_CONTEXT ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_GENERIC_CONTEXT ) ).isEqualTo( 1 );
	}

	@Test
	public void getId_defaultHandler() {
		SessionFactory sessionFactory = setup( null );

		log.expectEvent(
				Level.ERROR,
				ExceptionMatcherBuilder.isException( SearchException.class )
						.withMessage( "Exception while invoking" )
						.causedBy( SimulatedFailure.class )
								.withMessage( "getId failure" )
						.build(),
				"Indexing instance of type " + Book.class.getName()
		)
				.once();

		doMassIndexingWithBook2GetIdFailure( sessionFactory );
	}

	@Test
	public void getId_customHandler() {
		assertThat( staticCounters.get( StubFailureHandler.CREATE ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_INDEX_CONTEXT ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_GENERIC_CONTEXT ) ).isEqualTo( 0 );

		SessionFactory sessionFactory = setup( StubFailureHandler.class.getName() );

		assertThat( staticCounters.get( StubFailureHandler.CREATE ) ).isEqualTo( 1 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_INDEX_CONTEXT ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_GENERIC_CONTEXT ) ).isEqualTo( 0 );

		doMassIndexingWithBook2GetIdFailure( sessionFactory );

		assertThat( staticCounters.get( StubFailureHandler.CREATE ) ).isEqualTo( 1 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_INDEX_CONTEXT ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_GENERIC_CONTEXT ) ).isEqualTo( 1 );
	}

	@Test
	public void getTitle_defaultHandler() {
		SessionFactory sessionFactory = setup( null );

		log.expectEvent(
				Level.ERROR,
				ExceptionMatcherBuilder.isException( SearchException.class )
						.withMessage( "Exception while invoking" )
						.causedBy( SimulatedFailure.class )
								.withMessage( "getTitle failure" )
						.build(),
				"Indexing instance of type " + Book.class.getName()
		)
				.once();

		doMassIndexingWithBook2GetTitleFailure( sessionFactory );
	}

	@Test
	public void getTitle_customHandler() {
		assertThat( staticCounters.get( StubFailureHandler.CREATE ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_INDEX_CONTEXT ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_GENERIC_CONTEXT ) ).isEqualTo( 0 );

		SessionFactory sessionFactory = setup( StubFailureHandler.class.getName() );

		assertThat( staticCounters.get( StubFailureHandler.CREATE ) ).isEqualTo( 1 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_INDEX_CONTEXT ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_GENERIC_CONTEXT ) ).isEqualTo( 0 );

		doMassIndexingWithBook2GetTitleFailure( sessionFactory );

		assertThat( staticCounters.get( StubFailureHandler.CREATE ) ).isEqualTo( 1 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_INDEX_CONTEXT ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_GENERIC_CONTEXT ) ).isEqualTo( 1 );
	}

	@Test
	public void purge_defaultHandler() {
		SessionFactory sessionFactory = setup( null );

		log.expectEvent(
				Level.ERROR,
				ExceptionMatcherBuilder.isException( SimulatedFailure.class )
						.withMessage( "Index-scope operation failure" )
						.build(),
				"MassIndexer operation"
		)
				.once();

		doMassIndexing(
				sessionFactory,
				expectIndexScopeWork( StubIndexScopeWork.Type.PURGE, ExecutionExpectation.FAIL )
		);
	}

	@Test
	public void purge_customHandler() {
		assertThat( staticCounters.get( StubFailureHandler.CREATE ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_INDEX_CONTEXT ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_GENERIC_CONTEXT ) ).isEqualTo( 0 );

		SessionFactory sessionFactory = setup( StubFailureHandler.class.getName() );

		assertThat( staticCounters.get( StubFailureHandler.CREATE ) ).isEqualTo( 1 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_INDEX_CONTEXT ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_GENERIC_CONTEXT ) ).isEqualTo( 0 );

		doMassIndexing(
				sessionFactory,
				expectIndexScopeWork( StubIndexScopeWork.Type.PURGE, ExecutionExpectation.FAIL )
		);

		assertThat( staticCounters.get( StubFailureHandler.CREATE ) ).isEqualTo( 1 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_INDEX_CONTEXT ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_GENERIC_CONTEXT ) ).isEqualTo( 1 );
	}

	@Test
	public void optimizeBefore_defaultHandler() {
		SessionFactory sessionFactory = setup( null );

		log.expectEvent(
				Level.ERROR,
				ExceptionMatcherBuilder.isException( SimulatedFailure.class )
						.withMessage( "Index-scope operation failure" )
						.build(),
				"MassIndexer operation"
		)
				.once();

		doMassIndexing(
				sessionFactory,
				expectIndexScopeWork( StubIndexScopeWork.Type.PURGE, ExecutionExpectation.SUCCEED ),
				expectIndexScopeWork( StubIndexScopeWork.Type.OPTIMIZE, ExecutionExpectation.FAIL )
		);
	}

	@Test
	public void optimizeBefore_customHandler() {
		assertThat( staticCounters.get( StubFailureHandler.CREATE ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_INDEX_CONTEXT ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_GENERIC_CONTEXT ) ).isEqualTo( 0 );

		SessionFactory sessionFactory = setup( StubFailureHandler.class.getName() );

		assertThat( staticCounters.get( StubFailureHandler.CREATE ) ).isEqualTo( 1 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_INDEX_CONTEXT ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_GENERIC_CONTEXT ) ).isEqualTo( 0 );

		doMassIndexing(
				sessionFactory,
				expectIndexScopeWork( StubIndexScopeWork.Type.PURGE, ExecutionExpectation.SUCCEED ),
				expectIndexScopeWork( StubIndexScopeWork.Type.OPTIMIZE, ExecutionExpectation.FAIL )
		);

		assertThat( staticCounters.get( StubFailureHandler.CREATE ) ).isEqualTo( 1 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_INDEX_CONTEXT ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_GENERIC_CONTEXT ) ).isEqualTo( 1 );
	}

	@Test
	public void optimizeAfter_defaultHandler() {
		SessionFactory sessionFactory = setup( null );

		log.expectEvent(
				Level.ERROR,
				ExceptionMatcherBuilder.isException( SimulatedFailure.class )
						.withMessage( "Index-scope operation failure" )
						.build(),
				"MassIndexer operation"
		)
				.once();

		doMassIndexing(
				sessionFactory,
				expectIndexScopeWork( StubIndexScopeWork.Type.PURGE, ExecutionExpectation.SUCCEED ),
				expectIndexScopeWork( StubIndexScopeWork.Type.OPTIMIZE, ExecutionExpectation.SUCCEED ),
				expectIndexingWorks( ExecutionExpectation.SUCCEED ),
				expectIndexScopeWork( StubIndexScopeWork.Type.OPTIMIZE, ExecutionExpectation.FAIL )
		);
	}

	@Test
	public void optimizeAfter_customHandler() {
		assertThat( staticCounters.get( StubFailureHandler.CREATE ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_INDEX_CONTEXT ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_GENERIC_CONTEXT ) ).isEqualTo( 0 );

		SessionFactory sessionFactory = setup( StubFailureHandler.class.getName() );

		assertThat( staticCounters.get( StubFailureHandler.CREATE ) ).isEqualTo( 1 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_INDEX_CONTEXT ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_GENERIC_CONTEXT ) ).isEqualTo( 0 );

		doMassIndexing(
				sessionFactory,
				expectIndexScopeWork( StubIndexScopeWork.Type.PURGE, ExecutionExpectation.SUCCEED ),
				expectIndexScopeWork( StubIndexScopeWork.Type.OPTIMIZE, ExecutionExpectation.SUCCEED ),
				expectIndexingWorks( ExecutionExpectation.SUCCEED ),
				expectIndexScopeWork( StubIndexScopeWork.Type.OPTIMIZE, ExecutionExpectation.FAIL )
		);

		assertThat( staticCounters.get( StubFailureHandler.CREATE ) ).isEqualTo( 1 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_INDEX_CONTEXT ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_GENERIC_CONTEXT ) ).isEqualTo( 1 );
	}

	@Test
	public void flush_defaultHandler() {
		SessionFactory sessionFactory = setup( null );

		log.expectEvent(
				Level.ERROR,
				ExceptionMatcherBuilder.isException( SimulatedFailure.class )
						.withMessage( "Index-scope operation failure" )
						.build(),
				"MassIndexer operation"
		)
				.once();

		doMassIndexing(
				sessionFactory,
				expectIndexScopeWork( StubIndexScopeWork.Type.PURGE, ExecutionExpectation.SUCCEED ),
				expectIndexScopeWork( StubIndexScopeWork.Type.OPTIMIZE, ExecutionExpectation.SUCCEED ),
				expectIndexingWorks( ExecutionExpectation.SUCCEED ),
				expectIndexScopeWork( StubIndexScopeWork.Type.OPTIMIZE, ExecutionExpectation.SUCCEED ),
				expectIndexScopeWork( StubIndexScopeWork.Type.FLUSH, ExecutionExpectation.FAIL )
		);
	}

	@Test
	public void flush_customHandler() {
		assertThat( staticCounters.get( StubFailureHandler.CREATE ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_INDEX_CONTEXT ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_GENERIC_CONTEXT ) ).isEqualTo( 0 );

		SessionFactory sessionFactory = setup( StubFailureHandler.class.getName() );

		assertThat( staticCounters.get( StubFailureHandler.CREATE ) ).isEqualTo( 1 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_INDEX_CONTEXT ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_GENERIC_CONTEXT ) ).isEqualTo( 0 );

		doMassIndexing(
				sessionFactory,
				expectIndexScopeWork( StubIndexScopeWork.Type.PURGE, ExecutionExpectation.SUCCEED ),
				expectIndexScopeWork( StubIndexScopeWork.Type.OPTIMIZE, ExecutionExpectation.SUCCEED ),
				expectIndexingWorks( ExecutionExpectation.SUCCEED ),
				expectIndexScopeWork( StubIndexScopeWork.Type.OPTIMIZE, ExecutionExpectation.SUCCEED ),
				expectIndexScopeWork( StubIndexScopeWork.Type.FLUSH, ExecutionExpectation.FAIL )
		);

		assertThat( staticCounters.get( StubFailureHandler.CREATE ) ).isEqualTo( 1 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_INDEX_CONTEXT ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_GENERIC_CONTEXT ) ).isEqualTo( 1 );
	}

	private void doMassIndexing(SessionFactory sessionFactory, Runnable ... expectationSetters) {
		doMassIndexing(
				sessionFactory,
				ExecutionExpectation.SUCCEED, ExecutionExpectation.SUCCEED,
				expectationSetters
		);
	}

	private void doMassIndexingWithBook2GetIdFailure(SessionFactory sessionFactory) {
		doMassIndexing(
				sessionFactory,
				ExecutionExpectation.FAIL, ExecutionExpectation.SKIP,
				expectIndexScopeWork( StubIndexScopeWork.Type.PURGE, ExecutionExpectation.SUCCEED ),
				expectIndexScopeWork( StubIndexScopeWork.Type.OPTIMIZE, ExecutionExpectation.SUCCEED ),
				expectIndexingWorks( ExecutionExpectation.SKIP ),
				expectIndexScopeWork( StubIndexScopeWork.Type.OPTIMIZE, ExecutionExpectation.SUCCEED ),
				expectIndexScopeWork( StubIndexScopeWork.Type.FLUSH, ExecutionExpectation.SUCCEED )
		);
	}

	private void doMassIndexingWithBook2GetTitleFailure(SessionFactory sessionFactory) {
		doMassIndexing(
				sessionFactory,
				ExecutionExpectation.SUCCEED, ExecutionExpectation.FAIL,
				expectIndexScopeWork( StubIndexScopeWork.Type.PURGE, ExecutionExpectation.SUCCEED ),
				expectIndexScopeWork( StubIndexScopeWork.Type.OPTIMIZE, ExecutionExpectation.SUCCEED ),
				expectIndexingWorks( ExecutionExpectation.SKIP ),
				expectIndexScopeWork( StubIndexScopeWork.Type.OPTIMIZE, ExecutionExpectation.SUCCEED ),
				expectIndexScopeWork( StubIndexScopeWork.Type.FLUSH, ExecutionExpectation.SUCCEED )
		);
	}

	private void doMassIndexing(SessionFactory sessionFactory,
			ExecutionExpectation book2GetIdExpectation, ExecutionExpectation book2GetTitleExpectation,
			Runnable ... expectationSetters) {
		Book.failOnBook2GetId.set( ExecutionExpectation.FAIL.equals( book2GetIdExpectation ) );
		Book.failOnBook2GetTitle.set( ExecutionExpectation.FAIL.equals( book2GetTitleExpectation ) );
		try {
			OrmUtils.withinSession( sessionFactory, session -> {
				SearchSession searchSession = Search.session( session );
				MassIndexer indexer = searchSession.massIndexer();

				for ( Runnable expectationSetter : expectationSetters ) {
					expectationSetter.run();
				}

				try {
					indexer.startAndWait();
				}
				catch (InterruptedException e) {
					fail( "Unexpected InterruptedException: " + e.getMessage() );
				}
			} );
			backendMock.verifyExpectationsMet();
		}
		finally {
			Book.failOnBook2GetId.set( false );
			Book.failOnBook2GetTitle.set( false );
		}
	}

	private Runnable expectIndexScopeWork(StubIndexScopeWork.Type type, ExecutionExpectation executionExpectation) {
		return () -> {
			switch ( executionExpectation ) {
				case SUCCEED:
					backendMock.expectIndexScopeWorks( Book.INDEX )
							.indexScopeWork( type );
					break;
				case FAIL:
					CompletableFuture<?> failingFuture = new CompletableFuture<>();
					failingFuture.completeExceptionally( new SimulatedFailure( "Index-scope operation failure" ) );
					backendMock.expectIndexScopeWorks( Book.INDEX )
							.indexScopeWork( type, failingFuture );
					break;
				case SKIP:
					break;
			}
		};
	}

	private Runnable expectIndexingWorks(ExecutionExpectation workTwoExecutionExpectation) {
		return () -> {
			switch ( workTwoExecutionExpectation ) {
				case SUCCEED:
					backendMock.expectWorksAnyOrder(
							Book.INDEX, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE
					)
							.add( "1", b -> b
									.field( "title", TITLE_1 )
									.field( "author", AUTHOR_1 )
							)
							.add( "2", b -> b
									.field( "title", TITLE_2 )
									.field( "author", AUTHOR_2 )
							)
							.add( "3", b -> b
									.field( "title", TITLE_3 )
									.field( "author", AUTHOR_3 )
							)
							.processedThenExecuted();
					break;
				case FAIL:
					CompletableFuture<?> failingFuture = new CompletableFuture<>();
					failingFuture.completeExceptionally( new SimulatedFailure( "Indexing failure" ) );
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
							.processedThenExecuted( failingFuture );
					break;
				case SKIP:
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
					break;
			}
		};
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

	private enum ExecutionExpectation {
		SUCCEED,
		FAIL,
		SKIP;
	}

	@Entity
	@Table(name = "book")
	@Indexed(index = Book.INDEX)
	public static class Book {

		public static final String INDEX = "Book";

		private static final AtomicBoolean failOnBook2GetId = new AtomicBoolean( false );
		private static final AtomicBoolean failOnBook2GetTitle = new AtomicBoolean( false );

		private Integer id;

		private String title;

		private String author;

		public Book() {
		}

		public Book(Integer id, String title, String author) {
			this.id = id;
			this.title = title;
			this.author = author;
		}

		@Id // This must be on the getter, so that Hibernate Search uses getters instead of direct field access
		public Integer getId() {
			if ( id == 2 && failOnBook2GetId.get() ) {
				throw new SimulatedFailure( "getId failure" );
			}
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@GenericField
		public String getTitle() {
			if ( id == 2 && failOnBook2GetTitle.get() ) {
				throw new SimulatedFailure( "getTitle failure" );
			}
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		@GenericField
		public String getAuthor() {
			return author;
		}

		public void setAuthor(String author) {
			this.author = author;
		}
	}

	private static class SimulatedFailure extends RuntimeException {
		SimulatedFailure(String message) {
			super( message );
		}
	}
}
