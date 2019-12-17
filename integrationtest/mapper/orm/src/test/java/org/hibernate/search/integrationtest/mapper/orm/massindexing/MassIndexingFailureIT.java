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
import java.util.function.Consumer;
import java.util.function.Function;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.cfg.EngineSettings;
import org.hibernate.search.engine.cfg.spi.EngineSpiSettings;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.automaticindexing.AutomaticIndexingStrategyName;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.rule.ThreadSpy;
import org.hibernate.search.util.impl.integrationtest.common.stub.StubFailureHandler;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubIndexScopeWork;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils;
import org.hibernate.search.util.impl.test.ExceptionMatcherBuilder;
import org.hibernate.search.util.impl.test.SubTest;
import org.hibernate.search.util.impl.test.rule.ExpectedLog4jLog;
import org.hibernate.search.util.impl.test.rule.StaticCounters;

import org.junit.Rule;
import org.junit.Test;

import org.apache.log4j.Level;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.awaitility.Awaitility;

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
	public ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	@Rule
	public StaticCounters staticCounters = new StaticCounters();

	@Rule
	public ThreadSpy threadSpy = new ThreadSpy();

	@Test
	public void indexing_defaultHandler() {
		SessionFactory sessionFactory = setup( null );

		logged.expectEvent(
				Level.ERROR,
				ExceptionMatcherBuilder.isException( SimulatedFailure.class )
						.withMessage( "Indexing failure" )
						.build(),
				"Indexing instance of entity '" + Book.NAME + "'",
				"Entities that could not be indexed correctly:",
				Book.NAME + "#2"
		)
				.once();

		doMassIndexingWithFailure(
				sessionFactory,
				ThreadExpectation.CREATED_AND_TERMINATED,
				throwable -> assertThat( throwable ).isInstanceOf( SearchException.class )
						.hasMessageContainingAll(
								"1 entities could not be indexed",
								"See the logs for details.",
								"First failure on entity 'Book#2': ",
								"Indexing failure"
						)
						.hasCauseInstanceOf( SimulatedFailure.class ),
				expectIndexScopeWork( StubIndexScopeWork.Type.PURGE, ExecutionExpectation.SUCCEED ),
				expectIndexScopeWork( StubIndexScopeWork.Type.OPTIMIZE, ExecutionExpectation.SUCCEED ),
				expectIndexingWorks( ExecutionExpectation.FAIL ),
				expectIndexScopeWork( StubIndexScopeWork.Type.FLUSH, ExecutionExpectation.SUCCEED )
		);
	}

	@Test
	public void indexing_customHandler() {
		assertThat( staticCounters.get( StubFailureHandler.CREATE ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_INDEX_CONTEXT ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_GENERIC_CONTEXT ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_ENTITY_INDEXING_CONTEXT ) ).isEqualTo( 0 );

		SessionFactory sessionFactory = setup( StubFailureHandler.class.getName() );

		assertThat( staticCounters.get( StubFailureHandler.CREATE ) ).isEqualTo( 1 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_INDEX_CONTEXT ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_GENERIC_CONTEXT ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_ENTITY_INDEXING_CONTEXT ) ).isEqualTo( 0 );

		doMassIndexingWithFailure(
				sessionFactory,
				ThreadExpectation.CREATED_AND_TERMINATED,
				throwable -> assertThat( throwable ).isInstanceOf( SearchException.class )
						.hasMessageContainingAll(
								"1 entities could not be indexed",
								"See the logs for details.",
								"First failure on entity 'Book#2': ",
								"Indexing failure"
						),
				expectIndexScopeWork( StubIndexScopeWork.Type.PURGE, ExecutionExpectation.SUCCEED ),
				expectIndexScopeWork( StubIndexScopeWork.Type.OPTIMIZE, ExecutionExpectation.SUCCEED ),
				expectIndexingWorks( ExecutionExpectation.FAIL ),
				expectIndexScopeWork( StubIndexScopeWork.Type.FLUSH, ExecutionExpectation.SUCCEED )
		);

		assertThat( staticCounters.get( StubFailureHandler.CREATE ) ).isEqualTo( 1 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_INDEX_CONTEXT ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_GENERIC_CONTEXT ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_ENTITY_INDEXING_CONTEXT ) ).isEqualTo( 1 );
	}

	@Test
	public void getId_defaultHandler() {
		SessionFactory sessionFactory = setup( null );

		logged.expectEvent(
				Level.ERROR,
				ExceptionMatcherBuilder.isException( SearchException.class )
						.withMessage( "Exception while invoking" )
						.causedBy( SimulatedFailure.class )
								.withMessage( "getId failure" )
						.build(),
				"Indexing instance of entity '" + Book.NAME + "'",
				"Entities that could not be indexed correctly:",
				Book.NAME + "#2"

		)
				.once();

		doMassIndexingWithBook2GetIdFailure( sessionFactory );
	}

	@Test
	public void getId_customHandler() {
		assertThat( staticCounters.get( StubFailureHandler.CREATE ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_INDEX_CONTEXT ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_GENERIC_CONTEXT ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_ENTITY_INDEXING_CONTEXT ) ).isEqualTo( 0 );

		SessionFactory sessionFactory = setup( StubFailureHandler.class.getName() );

		assertThat( staticCounters.get( StubFailureHandler.CREATE ) ).isEqualTo( 1 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_INDEX_CONTEXT ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_GENERIC_CONTEXT ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_ENTITY_INDEXING_CONTEXT ) ).isEqualTo( 0 );

		doMassIndexingWithBook2GetIdFailure( sessionFactory );

		assertThat( staticCounters.get( StubFailureHandler.CREATE ) ).isEqualTo( 1 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_INDEX_CONTEXT ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_GENERIC_CONTEXT ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_ENTITY_INDEXING_CONTEXT ) ).isEqualTo( 1 );
	}

	@Test
	public void getTitle_defaultHandler() {
		SessionFactory sessionFactory = setup( null );

		logged.expectEvent(
				Level.ERROR,
				ExceptionMatcherBuilder.isException( SearchException.class )
						.withMessage( "Exception while invoking" )
						.causedBy( SimulatedFailure.class )
								.withMessage( "getTitle failure" )
						.build(),
				"Indexing instance of entity '" + Book.NAME + "'",
				"Entities that could not be indexed correctly:",
				Book.NAME + "#2"
		)
				.once();

		doMassIndexingWithBook2GetTitleFailure( sessionFactory );
	}

	@Test
	public void getTitle_customHandler() {
		assertThat( staticCounters.get( StubFailureHandler.CREATE ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_INDEX_CONTEXT ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_GENERIC_CONTEXT ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_ENTITY_INDEXING_CONTEXT ) ).isEqualTo( 0 );

		SessionFactory sessionFactory = setup( StubFailureHandler.class.getName() );

		assertThat( staticCounters.get( StubFailureHandler.CREATE ) ).isEqualTo( 1 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_INDEX_CONTEXT ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_GENERIC_CONTEXT ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_ENTITY_INDEXING_CONTEXT ) ).isEqualTo( 0 );

		doMassIndexingWithBook2GetTitleFailure( sessionFactory );

		assertThat( staticCounters.get( StubFailureHandler.CREATE ) ).isEqualTo( 1 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_INDEX_CONTEXT ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_GENERIC_CONTEXT ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_ENTITY_INDEXING_CONTEXT ) ).isEqualTo( 1 );
	}

	@Test
	public void purge_defaultHandler() {
		SessionFactory sessionFactory = setup( null );

		logged.expectEvent(
				Level.ERROR,
				ExceptionMatcherBuilder.isException( SimulatedFailure.class )
						.withMessage( "PURGE failure" )
						.build(),
				"MassIndexer operation"
		)
				.once();

		doMassIndexingWithFailure(
				sessionFactory,
				ThreadExpectation.NOT_CREATED,
				throwable -> assertThat( throwable ).isInstanceOf( SimulatedFailure.class )
						.hasMessageContaining( "PURGE failure" ),
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

		doMassIndexingWithFailure(
				sessionFactory,
				ThreadExpectation.NOT_CREATED,
				throwable -> assertThat( throwable ).isInstanceOf( SimulatedFailure.class )
						.hasMessageContaining( "PURGE failure" ),
				expectIndexScopeWork( StubIndexScopeWork.Type.PURGE, ExecutionExpectation.FAIL )
		);

		assertThat( staticCounters.get( StubFailureHandler.CREATE ) ).isEqualTo( 1 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_INDEX_CONTEXT ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_GENERIC_CONTEXT ) ).isEqualTo( 1 );
	}

	@Test
	public void optimizeBefore_defaultHandler() {
		SessionFactory sessionFactory = setup( null );

		logged.expectEvent(
				Level.ERROR,
				ExceptionMatcherBuilder.isException( SimulatedFailure.class )
						.withMessage( "OPTIMIZE failure" )
						.build(),
				"MassIndexer operation"
		)
				.once();

		doMassIndexingWithFailure(
				sessionFactory,
				ThreadExpectation.NOT_CREATED,
				throwable -> assertThat( throwable ).isInstanceOf( SimulatedFailure.class )
						.hasMessageContaining( "OPTIMIZE failure" ),
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

		doMassIndexingWithFailure(
				sessionFactory,
				ThreadExpectation.NOT_CREATED,
				throwable -> assertThat( throwable ).isInstanceOf( SimulatedFailure.class )
						.hasMessageContaining( "OPTIMIZE failure" ),
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

		logged.expectEvent(
				Level.ERROR,
				ExceptionMatcherBuilder.isException( SimulatedFailure.class )
						.withMessage( "OPTIMIZE failure" )
						.build(),
				"MassIndexer operation"
		)
				.once();

		doMassIndexingWithFailure(
				sessionFactory,
				searchSession -> searchSession.massIndexer().optimizeOnFinish( true ),
				ThreadExpectation.CREATED_AND_TERMINATED,
				throwable -> assertThat( throwable ).isInstanceOf( SimulatedFailure.class )
						.hasMessageContaining( "OPTIMIZE failure" ),
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

		doMassIndexingWithFailure(
				sessionFactory,
				searchSession -> searchSession.massIndexer().optimizeOnFinish( true ),
				ThreadExpectation.CREATED_AND_TERMINATED,
				throwable -> assertThat( throwable ).isInstanceOf( SimulatedFailure.class )
						.hasMessageContaining( "OPTIMIZE failure" ),
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

		logged.expectEvent(
				Level.ERROR,
				ExceptionMatcherBuilder.isException( SimulatedFailure.class )
						.withMessage( "FLUSH failure" )
						.build(),
				"MassIndexer operation"
		)
				.once();

		doMassIndexingWithFailure(
				sessionFactory,
				ThreadExpectation.CREATED_AND_TERMINATED,
				throwable -> assertThat( throwable ).isInstanceOf( SimulatedFailure.class )
						.hasMessageContaining( "FLUSH failure" ),
				expectIndexScopeWork( StubIndexScopeWork.Type.PURGE, ExecutionExpectation.SUCCEED ),
				expectIndexScopeWork( StubIndexScopeWork.Type.OPTIMIZE, ExecutionExpectation.SUCCEED ),
				expectIndexingWorks( ExecutionExpectation.SUCCEED ),
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

		doMassIndexingWithFailure(
				sessionFactory,
				ThreadExpectation.CREATED_AND_TERMINATED,
				throwable -> assertThat( throwable ).isInstanceOf( SimulatedFailure.class )
						.hasMessageContaining( "FLUSH failure" ),
				expectIndexScopeWork( StubIndexScopeWork.Type.PURGE, ExecutionExpectation.SUCCEED ),
				expectIndexScopeWork( StubIndexScopeWork.Type.OPTIMIZE, ExecutionExpectation.SUCCEED ),
				expectIndexingWorks( ExecutionExpectation.SUCCEED ),
				expectIndexScopeWork( StubIndexScopeWork.Type.FLUSH, ExecutionExpectation.FAIL )
		);

		assertThat( staticCounters.get( StubFailureHandler.CREATE ) ).isEqualTo( 1 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_INDEX_CONTEXT ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_GENERIC_CONTEXT ) ).isEqualTo( 1 );
	}

	@Test
	public void indexingAndFlush_defaultHandler() {
		SessionFactory sessionFactory = setup( null );

		logged.expectEvent(
				Level.ERROR,
				ExceptionMatcherBuilder.isException( SimulatedFailure.class )
						.withMessage( "Indexing failure" )
						.build(),
				"Indexing instance of entity '" + Book.NAME + "'",
				"Entities that could not be indexed correctly:",
				Book.NAME + "#2"
		)
				.once();

		logged.expectEvent(
				Level.ERROR,
				ExceptionMatcherBuilder.isException( SimulatedFailure.class )
						.withMessage( "FLUSH failure" )
						.build(),
				"MassIndexer operation"
		)
				.once();

		doMassIndexingWithFailure(
				sessionFactory,
				ThreadExpectation.CREATED_AND_TERMINATED,
				throwable -> assertThat( throwable ).isInstanceOf( SimulatedFailure.class )
						.hasMessageContaining( "FLUSH failure" )
						// Indexing failure should also be mentioned as a suppressed exception
						.extracting( Throwable::getSuppressed ).asInstanceOf( InstanceOfAssertFactories.ARRAY )
						.anySatisfy( suppressed -> assertThat( suppressed ).asInstanceOf( InstanceOfAssertFactories.THROWABLE )
								.isInstanceOf( SearchException.class )
								.hasMessageContainingAll(
										"1 entities could not be indexed",
										"See the logs for details.",
										"First failure on entity 'Book#2': ",
										"Indexing failure"
								)
								.hasCauseInstanceOf( SimulatedFailure.class )
						),
				expectIndexScopeWork( StubIndexScopeWork.Type.PURGE, ExecutionExpectation.SUCCEED ),
				expectIndexScopeWork( StubIndexScopeWork.Type.OPTIMIZE, ExecutionExpectation.SUCCEED ),
				expectIndexingWorks( ExecutionExpectation.FAIL ),
				expectIndexScopeWork( StubIndexScopeWork.Type.FLUSH, ExecutionExpectation.FAIL )
		);
	}

	@Test
	public void indexingAndFlush_customHandler() {
		assertThat( staticCounters.get( StubFailureHandler.CREATE ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_INDEX_CONTEXT ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_GENERIC_CONTEXT ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_ENTITY_INDEXING_CONTEXT ) ).isEqualTo( 0 );

		SessionFactory sessionFactory = setup( StubFailureHandler.class.getName() );

		assertThat( staticCounters.get( StubFailureHandler.CREATE ) ).isEqualTo( 1 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_INDEX_CONTEXT ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_GENERIC_CONTEXT ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_ENTITY_INDEXING_CONTEXT ) ).isEqualTo( 0 );

		doMassIndexingWithFailure(
				sessionFactory,
				ThreadExpectation.CREATED_AND_TERMINATED,
				throwable -> assertThat( throwable ).isInstanceOf( SimulatedFailure.class )
						.hasMessageContaining( "FLUSH failure" )
						// Indexing failure should also be mentioned as a suppressed exception
						.extracting( Throwable::getSuppressed ).asInstanceOf( InstanceOfAssertFactories.ARRAY )
						.anySatisfy( suppressed -> assertThat( suppressed ).asInstanceOf( InstanceOfAssertFactories.THROWABLE )
								.isInstanceOf( SearchException.class )
								.hasMessageContainingAll(
										"1 entities could not be indexed",
										"See the logs for details.",
										"First failure on entity 'Book#2': ",
										"Indexing failure"
								)
								.hasCauseInstanceOf( SimulatedFailure.class )
						),
				expectIndexScopeWork( StubIndexScopeWork.Type.PURGE, ExecutionExpectation.SUCCEED ),
				expectIndexScopeWork( StubIndexScopeWork.Type.OPTIMIZE, ExecutionExpectation.SUCCEED ),
				expectIndexingWorks( ExecutionExpectation.FAIL ),
				expectIndexScopeWork( StubIndexScopeWork.Type.FLUSH, ExecutionExpectation.FAIL )
		);

		assertThat( staticCounters.get( StubFailureHandler.CREATE ) ).isEqualTo( 1 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_INDEX_CONTEXT ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_GENERIC_CONTEXT ) ).isEqualTo( 1 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_ENTITY_INDEXING_CONTEXT ) ).isEqualTo( 1 );
	}

	private void doMassIndexingWithFailure(SessionFactory sessionFactory,
			ThreadExpectation threadExpectation,
			Consumer<Throwable> thrownExpectation,
			Runnable ... expectationSetters) {
		doMassIndexingWithFailure(
				sessionFactory,
				searchSession -> searchSession.massIndexer(),
				threadExpectation,
				thrownExpectation,
				expectationSetters
		);
	}

	private void doMassIndexingWithFailure(SessionFactory sessionFactory,
			Function<SearchSession, MassIndexer> indexerProducer,
			ThreadExpectation threadExpectation,
			Consumer<Throwable> thrownExpectation,
			Runnable ... expectationSetters) {
		doMassIndexingWithFailure(
				sessionFactory,
				indexerProducer,
				threadExpectation,
				thrownExpectation,
				ExecutionExpectation.SUCCEED, ExecutionExpectation.SUCCEED,
				expectationSetters
		);
	}

	private void doMassIndexingWithBook2GetIdFailure(SessionFactory sessionFactory) {
		doMassIndexingWithFailure(
				sessionFactory,
				searchSession -> searchSession.massIndexer(),
				ThreadExpectation.CREATED_AND_TERMINATED,
				throwable -> assertThat( throwable ).isInstanceOf( SearchException.class )
						.hasMessageContainingAll(
								"1 entities could not be indexed",
								"See the logs for details.",
								"First failure on entity 'Book#2': ",
								"Exception while invoking"
						)
						.extracting( Throwable::getCause ).asInstanceOf( InstanceOfAssertFactories.THROWABLE )
						.isInstanceOf( SearchException.class )
						.hasMessageContaining( "Exception while invoking" ),
				ExecutionExpectation.FAIL, ExecutionExpectation.SKIP,
				expectIndexScopeWork( StubIndexScopeWork.Type.PURGE, ExecutionExpectation.SUCCEED ),
				expectIndexScopeWork( StubIndexScopeWork.Type.OPTIMIZE, ExecutionExpectation.SUCCEED ),
				expectIndexingWorks( ExecutionExpectation.SKIP ),
				expectIndexScopeWork( StubIndexScopeWork.Type.FLUSH, ExecutionExpectation.SUCCEED )
		);
	}

	private void doMassIndexingWithBook2GetTitleFailure(SessionFactory sessionFactory) {
		doMassIndexingWithFailure(
				sessionFactory,
				searchSession -> searchSession.massIndexer(),
				ThreadExpectation.CREATED_AND_TERMINATED,
				throwable -> assertThat( throwable ).isInstanceOf( SearchException.class )
						.hasMessageContainingAll(
								"1 entities could not be indexed",
								"See the logs for details.",
								"First failure on entity 'Book#2': ",
								"Exception while invoking"
						)
						.extracting( Throwable::getCause ).asInstanceOf( InstanceOfAssertFactories.THROWABLE )
						.isInstanceOf( SearchException.class )
						.hasMessageContaining( "Exception while invoking" ),
				ExecutionExpectation.SUCCEED, ExecutionExpectation.FAIL,
				expectIndexScopeWork( StubIndexScopeWork.Type.PURGE, ExecutionExpectation.SUCCEED ),
				expectIndexScopeWork( StubIndexScopeWork.Type.OPTIMIZE, ExecutionExpectation.SUCCEED ),
				expectIndexingWorks( ExecutionExpectation.SKIP ),
				expectIndexScopeWork( StubIndexScopeWork.Type.FLUSH, ExecutionExpectation.SUCCEED )
		);
	}

	private void doMassIndexingWithFailure(SessionFactory sessionFactory,
			Function<SearchSession, MassIndexer> indexerProducer,
			ThreadExpectation threadExpectation,
			Consumer<Throwable> thrownExpectation,
			ExecutionExpectation book2GetIdExpectation, ExecutionExpectation book2GetTitleExpectation,
			Runnable ... expectationSetters) {
		Book.failOnBook2GetId.set( ExecutionExpectation.FAIL.equals( book2GetIdExpectation ) );
		Book.failOnBook2GetTitle.set( ExecutionExpectation.FAIL.equals( book2GetTitleExpectation ) );
		AssertionError assertionError = null;
		try {
			OrmUtils.withinSession( sessionFactory, session -> {
				SearchSession searchSession = Search.session( session );
				MassIndexer indexer = indexerProducer.apply( searchSession );

				for ( Runnable expectationSetter : expectationSetters ) {
					expectationSetter.run();
				}

				// TODO HSEARCH-3728 simplify this when even indexing exceptions are propagated
				Runnable runnable = () -> {
					try {
						indexer.startAndWait();
					}
					catch (InterruptedException e) {
						fail( "Unexpected InterruptedException: " + e.getMessage() );
					}
				};
				if ( thrownExpectation == null ) {
					runnable.run();
				}
				else {
					SubTest.expectException( runnable )
							.assertThrown()
							.satisfies( thrownExpectation );
				}
			} );
			backendMock.verifyExpectationsMet();
		}
		catch (AssertionError e) {
			assertionError = e;
			throw e;
		}
		finally {
			Book.failOnBook2GetId.set( false );
			Book.failOnBook2GetTitle.set( false );

			if ( assertionError == null ) {
				switch ( threadExpectation ) {
					case CREATED_AND_TERMINATED:
						Awaitility.await().untilAsserted(
								() -> assertThat( threadSpy.getCreatedThreads( "mass index" ) )
										.as( "Mass indexing threads" )
										.isNotEmpty()
										.allSatisfy( t -> assertThat( t )
												.extracting( Thread::getState )
												.isEqualTo( Thread.State.TERMINATED )
										)
						);
						break;
					case NOT_CREATED:
						assertThat( threadSpy.getCreatedThreads( "mass index" ) )
								.as( "Mass indexing threads" )
								.isEmpty();
						break;
				}
			}
		}
	}

	private Runnable expectIndexScopeWork(StubIndexScopeWork.Type type, ExecutionExpectation executionExpectation) {
		return () -> {
			switch ( executionExpectation ) {
				case SUCCEED:
					backendMock.expectIndexScopeWorks( Book.NAME )
							.indexScopeWork( type );
					break;
				case FAIL:
					CompletableFuture<?> failingFuture = new CompletableFuture<>();
					failingFuture.completeExceptionally( new SimulatedFailure( type.name() + " failure" ) );
					backendMock.expectIndexScopeWorks( Book.NAME )
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
							Book.NAME, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE
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
							Book.NAME, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE
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
							Book.NAME, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE
					)
							.add( "2", b -> b
									.field( "title", TITLE_2 )
									.field( "author", AUTHOR_2 )
							)
							.processedThenExecuted( failingFuture );
					break;
				case SKIP:
					backendMock.expectWorksAnyOrder(
							Book.NAME, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE
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
		backendMock.expectAnySchema( Book.NAME );

		SessionFactory sessionFactory = ormSetupHelper.start()
				.withPropertyRadical( HibernateOrmMapperSettings.Radicals.AUTOMATIC_INDEXING_STRATEGY, AutomaticIndexingStrategyName.NONE )
				.withPropertyRadical( EngineSettings.Radicals.BACKGROUND_FAILURE_HANDLER, failureHandler )
				.withPropertyRadical( EngineSpiSettings.Radicals.THREAD_PROVIDER, threadSpy.getThreadProvider() )
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

	private enum ThreadExpectation {
		CREATED_AND_TERMINATED,
		NOT_CREATED;
	}

	@Entity(name = Book.NAME)
	@Indexed(index = Book.NAME)
	public static class Book {

		public static final String NAME = "Book";

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
