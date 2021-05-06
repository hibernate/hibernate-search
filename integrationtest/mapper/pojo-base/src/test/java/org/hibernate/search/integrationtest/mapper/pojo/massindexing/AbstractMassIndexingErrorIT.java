/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.massindexing;

import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Fail.fail;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.cfg.EngineSettings;
import org.hibernate.search.engine.cfg.spi.EngineSpiSettings;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingFailureHandler;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubIndexScaleWork;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubSchemaManagementWork;

import org.junit.Rule;
import org.junit.Test;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.awaitility.Awaitility;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.mapper.javabean.mapping.SearchMapping;
import org.hibernate.search.mapper.javabean.massindexing.MassIndexer;
import org.hibernate.search.mapper.javabean.loading.MassLoadingStrategies;
import org.hibernate.search.mapper.javabean.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.util.impl.integrationtest.common.rule.ThreadSpy;

public abstract class AbstractMassIndexingErrorIT {

	public static final String TITLE_1 = "Oliver Twist";
	public static final String AUTHOR_1 = "Charles Dickens";
	public static final String TITLE_2 = "Ulysses";
	public static final String AUTHOR_2 = "James Joyce";
	public static final String TITLE_3 = "Frankenstein";
	public static final String AUTHOR_3 = "Mary Shelley";

	@Rule
	public final BackendMock backendMock = new BackendMock();

	@Rule
	public final JavaBeanMappingSetupHelper setupHelper
			= JavaBeanMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@Rule
	public ThreadSpy threadSpy = new ThreadSpy();

	private Map<Integer, Book> booksmap = new LinkedHashMap<>();

	@Test
	public void indexing() {
		SearchMapping mapping = setup();

		String errorMessage = "Indexing error";

		expectNoFailureHandling();

		doMassIndexingWithError(
				session( mapping ).massIndexer( Object.class ),
				ThreadExpectation.CREATED_AND_TERMINATED,
				throwable -> assertThat( throwable ).isInstanceOf( SimulatedError.class )
						.hasMessage( errorMessage ),
				expectIndexScaleWork( StubIndexScaleWork.Type.PURGE, ExecutionExpectation.SUCCEED ),
				expectIndexScaleWork( StubIndexScaleWork.Type.MERGE_SEGMENTS, ExecutionExpectation.SUCCEED ),
				expectIndexingWorks( ExecutionExpectation.ERROR )
		);

		assertNoFailureHandling();
	}

	@Test
	public void getId() {
		SearchMapping mapping = setup();

		String errorMessage = "getId error";

		expectNoFailureHandling();

		doMassIndexingWithError(
				session( mapping ).massIndexer( Object.class ),
				ThreadExpectation.CREATED_AND_TERMINATED,
				throwable -> assertThat( throwable ).isInstanceOf( SimulatedError.class )
						.hasMessage( errorMessage ),
				ExecutionExpectation.ERROR, ExecutionExpectation.STOP,
				expectIndexScaleWork( StubIndexScaleWork.Type.PURGE, ExecutionExpectation.SUCCEED ),
				expectIndexScaleWork( StubIndexScaleWork.Type.MERGE_SEGMENTS, ExecutionExpectation.SUCCEED ),
				expectIndexingWorks( ExecutionExpectation.STOP )
		// no final refresh && flush
		);

		assertNoFailureHandling();
	}

	@Test
	public void getTitle() {
		SearchMapping mapping = setup();

		String errorMessage = "getTitle error";

		expectNoFailureHandling();

		doMassIndexingWithError(
				session( mapping ).massIndexer( Object.class ),
				ThreadExpectation.CREATED_AND_TERMINATED,
				throwable -> assertThat( throwable ).isInstanceOf( SimulatedError.class )
						.hasMessage( errorMessage ),
				ExecutionExpectation.SUCCEED, ExecutionExpectation.ERROR,
				expectIndexScaleWork( StubIndexScaleWork.Type.PURGE, ExecutionExpectation.SUCCEED ),
				expectIndexScaleWork( StubIndexScaleWork.Type.MERGE_SEGMENTS, ExecutionExpectation.SUCCEED ),
				expectIndexingWorks( ExecutionExpectation.STOP )
		);

		assertNoFailureHandling();
	}

	@Test
	public void dropAndCreateSchema_exception() {
		SearchMapping mapping = setup();

		String errorMessage = "DROP_AND_CREATE error";

		expectNoFailureHandling();

		doMassIndexingWithError(
				session( mapping ).massIndexer( Object.class ).dropAndCreateSchemaOnStart( true ),
				ThreadExpectation.NOT_CREATED,
				throwable -> assertThat( throwable ).isInstanceOf( SimulatedError.class )
						.hasMessage( errorMessage ),
				expectSchemaManagementWorkException( StubSchemaManagementWork.Type.DROP_AND_CREATE )
		);

		assertNoFailureHandling();
	}

	@Test
	public void purge() {
		SearchMapping mapping = setup();

		String errorMessage = "PURGE error";

		expectNoFailureHandling();

		doMassIndexingWithError(
				session( mapping ).massIndexer( Object.class ),
				ThreadExpectation.NOT_CREATED,
				throwable -> assertThat( throwable ).isInstanceOf( SimulatedError.class )
						.hasMessage( errorMessage ),
				expectIndexScaleWork( StubIndexScaleWork.Type.PURGE, ExecutionExpectation.ERROR )
		);

		assertNoFailureHandling();
	}

	@Test
	public void mergeSegmentsBefore() {
		SearchMapping mapping = setup();

		String errorMessage = "MERGE_SEGMENTS error";

		expectNoFailureHandling();

		doMassIndexingWithError(
				session( mapping ).massIndexer( Object.class ),
				ThreadExpectation.NOT_CREATED,
				throwable -> assertThat( throwable ).isInstanceOf( SimulatedError.class )
						.hasMessage( errorMessage ),
				expectIndexScaleWork( StubIndexScaleWork.Type.PURGE, ExecutionExpectation.SUCCEED ),
				expectIndexScaleWork( StubIndexScaleWork.Type.MERGE_SEGMENTS, ExecutionExpectation.ERROR )
		);

		assertNoFailureHandling();
	}

	@Test
	public void mergeSegmentsAfter() {
		SearchMapping mapping = setup();

		String errorMessage = "MERGE_SEGMENTS error";

		expectNoFailureHandling();

		doMassIndexingWithError(
				session( mapping ).massIndexer( Object.class )
						.mergeSegmentsOnFinish( true ),
				ThreadExpectation.CREATED_AND_TERMINATED,
				throwable -> assertThat( throwable ).isInstanceOf( SimulatedError.class )
						.hasMessage( errorMessage ),
				expectIndexScaleWork( StubIndexScaleWork.Type.PURGE, ExecutionExpectation.SUCCEED ),
				expectIndexScaleWork( StubIndexScaleWork.Type.MERGE_SEGMENTS, ExecutionExpectation.SUCCEED ),
				expectIndexingWorks( ExecutionExpectation.SUCCEED ),
				expectIndexScaleWork( StubIndexScaleWork.Type.MERGE_SEGMENTS, ExecutionExpectation.ERROR )
		);

		assertNoFailureHandling();
	}

	@Test
	public void flush() {
		SearchMapping mapping = setup();

		String errorMessage = "FLUSH error";

		expectNoFailureHandling();

		doMassIndexingWithError(
				session( mapping ).massIndexer( Object.class ),
				ThreadExpectation.CREATED_AND_TERMINATED,
				throwable -> assertThat( throwable ).isInstanceOf( SimulatedError.class )
						.hasMessage( errorMessage ),
				expectIndexScaleWork( StubIndexScaleWork.Type.PURGE, ExecutionExpectation.SUCCEED ),
				expectIndexScaleWork( StubIndexScaleWork.Type.MERGE_SEGMENTS, ExecutionExpectation.SUCCEED ),
				expectIndexingWorks( ExecutionExpectation.SUCCEED ),
				expectIndexScaleWork( StubIndexScaleWork.Type.FLUSH, ExecutionExpectation.ERROR )
		);

		assertNoFailureHandling();
	}

	@Test
	public void refresh() {
		SearchMapping mapping = setup();

		String errorMessage = "REFRESH error";

		expectNoFailureHandling();

		doMassIndexingWithError(
				session( mapping ).massIndexer( Object.class ),
				ThreadExpectation.CREATED_AND_TERMINATED,
				throwable -> assertThat( throwable ).isInstanceOf( SimulatedError.class )
						.hasMessage( errorMessage ),
				expectIndexScaleWork( StubIndexScaleWork.Type.PURGE, ExecutionExpectation.SUCCEED ),
				expectIndexScaleWork( StubIndexScaleWork.Type.MERGE_SEGMENTS, ExecutionExpectation.SUCCEED ),
				expectIndexingWorks( ExecutionExpectation.SUCCEED ),
				expectIndexScaleWork( StubIndexScaleWork.Type.FLUSH, ExecutionExpectation.SUCCEED ),
				expectIndexScaleWork( StubIndexScaleWork.Type.REFRESH, ExecutionExpectation.ERROR )
		);

		assertNoFailureHandling();
	}

	protected abstract String getBackgroundFailureHandlerReference();

	protected abstract MassIndexingFailureHandler getMassIndexingFailureHandler();

	protected void assertBeforeSetup() {
	}

	protected void assertAfterSetup() {
	}

	protected abstract void expectNoFailureHandling();

	protected abstract void assertNoFailureHandling();

	private void doMassIndexingWithError(MassIndexer massIndexer,
			ThreadExpectation threadExpectation,
			Consumer<Throwable> thrownExpectation,
			Runnable... expectationSetters) {
		doMassIndexingWithError(
				massIndexer,
				threadExpectation,
				thrownExpectation,
				ExecutionExpectation.SUCCEED, ExecutionExpectation.SUCCEED,
				expectationSetters
		);
	}

	private void doMassIndexingWithError(MassIndexer massIndexer,
			ThreadExpectation threadExpectation,
			Consumer<Throwable> thrownExpectation,
			ExecutionExpectation book2GetIdExpectation, ExecutionExpectation book2GetTitleExpectation,
			Runnable... expectationSetters) {
		Book.errorOnBook2GetId.set( ExecutionExpectation.ERROR.equals( book2GetIdExpectation ) );
		Book.errorOnBook2GetTitle.set( ExecutionExpectation.ERROR.equals( book2GetTitleExpectation ) );
		AssertionError assertionError = null;
		try {
			MassIndexingFailureHandler massIndexingFailureHandler = getMassIndexingFailureHandler();
			if ( massIndexingFailureHandler != null ) {
				massIndexer.failureHandler( massIndexingFailureHandler );
			}

			for ( Runnable expectationSetter : expectationSetters ) {
				expectationSetter.run();
			}

			// TODO HSEARCH-3728 simplify this when even indexing exceptions are propagated
			Runnable runnable = () -> {
				try {
					massIndexer.startAndWait();
				}
				catch (InterruptedException e) {
					fail( "Unexpected InterruptedException: " + e.getMessage() );
				}
			};
			if ( thrownExpectation == null ) {
				runnable.run();
			}
			else {
				assertThatThrownBy( runnable::run )
						.asInstanceOf( InstanceOfAssertFactories.type( Throwable.class ) )
						.satisfies( thrownExpectation );
			}
			backendMock.verifyExpectationsMet();
		}
		catch (AssertionError e) {
			assertionError = e;
			throw e;
		}
		finally {
			Book.errorOnBook2GetId.set( false );
			Book.errorOnBook2GetTitle.set( false );

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

	private Runnable expectSchemaManagementWorkException(StubSchemaManagementWork.Type type) {
		return () -> {
			CompletableFuture<?> failingFuture = new CompletableFuture<>();
			failingFuture.completeExceptionally( new SimulatedError( type.name() + " error" ) );
			backendMock.expectSchemaManagementWorks( Book.NAME )
					.work( type, failingFuture );
		};
	}

	private Runnable expectIndexScaleWork(StubIndexScaleWork.Type type, ExecutionExpectation executionExpectation) {
		return () -> {
			switch ( executionExpectation ) {
				case SUCCEED:
					backendMock.expectIndexScaleWorks( Book.NAME )
							.indexScaleWork( type );
					break;
				case ERROR:
					CompletableFuture<?> failingFuture = new CompletableFuture<>();
					failingFuture.completeExceptionally( new SimulatedError( type.name() + " error" ) );
					backendMock.expectIndexScaleWorks( Book.NAME )
							.indexScaleWork( type, failingFuture );
					break;
				case STOP:
					break;
			}
		};
	}

	private Runnable expectIndexingWorks(ExecutionExpectation workTwoExecutionExpectation) {
		return () -> {
			switch ( workTwoExecutionExpectation ) {
				case SUCCEED:
					backendMock.expectWorks(
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
							.createdThenExecuted();
					break;
				case ERROR:
					CompletableFuture<?> failingFuture = new CompletableFuture<>();
					failingFuture.completeExceptionally( new SimulatedError( "Indexing error" ) );
					backendMock.expectWorks(
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
							.createdThenExecuted();
					backendMock.expectWorks(
							Book.NAME, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE
					)
							.add( "2", b -> b
							.field( "title", TITLE_2 )
							.field( "author", AUTHOR_2 )
							)
							.createdThenExecuted( failingFuture );
					break;
				case STOP:
					backendMock.expectWorks(
							Book.NAME, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE
					)
							.add( "1", b -> b
							.field( "title", TITLE_1 )
							.field( "author", AUTHOR_1 )
							)
							.createdThenExecuted();
					break;
			}
		};
	}

	private SearchMapping setup() {
		assertBeforeSetup();

		backendMock.expectAnySchema( Book.NAME );

		SearchMapping mapping = setupHelper.start()
				.expectCustomBeans()
				.withPropertyRadical( EngineSettings.Radicals.BACKGROUND_FAILURE_HANDLER, getBackgroundFailureHandlerReference() )
				.withPropertyRadical( EngineSpiSettings.Radicals.THREAD_PROVIDER, threadSpy.getThreadProvider() )
				.setup( Book.class );

		backendMock.verifyExpectationsMet();

		persist( new Book( 1, TITLE_1, AUTHOR_1 ) );
		persist( new Book( 2, TITLE_2, AUTHOR_2 ) );
		persist( new Book( 3, TITLE_3, AUTHOR_3 ) );

		assertAfterSetup();

		return mapping;
	}

	private SearchSession session(SearchMapping mapping) {
		return mapping.createSessionWithOptions().loading( (o) -> {
			o.massLoadingStrategy( Book.class, MassLoadingStrategies.from( booksmap ) );
		} ).build();
	}

	private void persist(Book book) {
		booksmap.put( book.id, book );
	}

	private enum ExecutionExpectation {
		SUCCEED,
		ERROR,
		STOP;
	}

	private enum ThreadExpectation {
		CREATED_AND_TERMINATED,
		NOT_CREATED;
	}

	@Indexed(index = Book.NAME)
	public static class Book {

		public static final String NAME = "Book";

		private static final AtomicBoolean errorOnBook2GetId = new AtomicBoolean( false );
		private static final AtomicBoolean errorOnBook2GetTitle = new AtomicBoolean( false );

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

		@DocumentId // This must be on the getter, so that Hibernate Search uses getters instead of direct field access
		public Integer getId() {
			if ( id == 2 && errorOnBook2GetId.getAndSet( false ) ) {
				throw new SimulatedError( "getId error" );
			}
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@GenericField
		public String getTitle() {
			if ( id == 2 && errorOnBook2GetTitle.get() ) {
				throw new SimulatedError( "getTitle error" );
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

	protected static class SimulatedError extends Error {
		SimulatedError(String message) {
			super( message );
		}
	}
}
