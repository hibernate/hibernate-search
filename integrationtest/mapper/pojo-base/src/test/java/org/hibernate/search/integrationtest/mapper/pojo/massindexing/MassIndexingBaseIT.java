/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.massindexing;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Fail.fail;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.loading.PersistenceTypeKey;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.loading.StubLoadingContext;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.loading.StubMassLoadingStrategy;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.massindexing.MassIndexer;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubSchemaManagementWork;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Very basic test to probe an use of {@link MassIndexer} api.
 */
public class MassIndexingBaseIT {

	public static final String TITLE_1 = "Oliver Twist";
	public static final String AUTHOR_1 = "Charles Dickens";
	public static final String TITLE_2 = "Ulysses";
	public static final String AUTHOR_2 = "James Joyce";
	public static final String TITLE_3 = "Frankenstein";
	public static final String AUTHOR_3 = "Mary Shelley";

	@Rule
	public final BackendMock backendMock = new BackendMock();

	@Rule
	public final StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	private SearchMapping mapping;

	private final StubLoadingContext loadingContext = new StubLoadingContext();

	@Before
	public void setup() {
		backendMock.expectAnySchema( Book.INDEX );

		mapping = setupHelper.start()
				.withConfiguration( b -> {
					b.addEntityType( Book.class, c -> c
							.massLoadingStrategy( new StubMassLoadingStrategy<>( Book.PERSISTENCE_KEY ) ) );
				} )
				.setup( Book.class );

		backendMock.verifyExpectationsMet();

		initData();
	}

	@Test
	public void defaultMassIndexerStartAndWait() throws Exception {
		try ( SearchSession searchSession = mapping.createSession() ) {
			MassIndexer indexer = searchSession.massIndexer()
					// Simulate passing information to connect to a DB, ...
					.context( StubLoadingContext.class, loadingContext );

			// add operations on indexes can follow any random order,
			// since they are executed by different threads
			backendMock.expectWorks(
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
					);

			// purgeAtStart and mergeSegmentsAfterPurge are enabled by default,
			// so we expect 1 purge, 1 mergeSegments and 1 flush calls in this order:
			backendMock.expectIndexScaleWorks( Book.INDEX, searchSession.tenantIdentifier() )
					.purge()
					.mergeSegments()
					.flush()
					.refresh();

			try {
				indexer.startAndWait();
			}
			catch (InterruptedException e) {
				fail( "Unexpected InterruptedException: " + e.getMessage() );
			}

		}

		backendMock.verifyExpectationsMet();
	}

	@Test
	public void dropAndCreateSchemaOnStart() {
		try ( SearchSession searchSession = mapping.createSession() ) {
			MassIndexer indexer = searchSession.massIndexer()
					// Simulate passing information to connect to a DB, ...
					.context( StubLoadingContext.class, loadingContext )
					.dropAndCreateSchemaOnStart( true );

			// add operations on indexes can follow any random order,
			// since they are executed by different threads
			backendMock.expectWorks(
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
					);

			backendMock.expectSchemaManagementWorks( Book.INDEX )
					.work( StubSchemaManagementWork.Type.DROP_AND_CREATE );

			// because we set dropAndCreateSchemaOnStart = true and do not explicitly set the purge value
			// it means that purge will default to false hence only flush and refresh are expected:
			backendMock.expectIndexScaleWorks( Book.INDEX, searchSession.tenantIdentifier() )
					.flush()
					.refresh();

			try {
				indexer.startAndWait();
			}
			catch (InterruptedException e) {
				fail( "Unexpected InterruptedException: " + e.getMessage() );
			}

		}

		backendMock.verifyExpectationsMet();
	}

	@Test
	public void mergeSegmentsOnFinish() {
		try ( SearchSession searchSession = mapping.createSession() ) {
			MassIndexer indexer = searchSession.massIndexer()
					// Simulate passing information to connect to a DB, ...
					.context( StubLoadingContext.class, loadingContext )
					.mergeSegmentsOnFinish( true );

			// add operations on indexes can follow any random order,
			// since they are executed by different threads
			backendMock.expectWorks(
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
					);

			// purgeAtStart and mergeSegmentsAfterPurge are enabled by default,
			// and optimizeOnFinish is enabled explicitly,
			// so we expect 1 purge, 2 optimize and 1 flush calls in this order:
			backendMock.expectIndexScaleWorks( Book.INDEX, searchSession.tenantIdentifier() )
					.purge()
					.mergeSegments()
					.mergeSegments()
					.flush()
					.refresh();

			try {
				indexer.startAndWait();
			}
			catch (InterruptedException e) {
				fail( "Unexpected InterruptedException: " + e.getMessage() );
			}

		}

		backendMock.verifyExpectationsMet();
	}

	@Test
	public void dropAndCreateSchemaOnStartAndPurgeBothEnabled() {
		try ( SearchSession searchSession = mapping.createSession() ) {
			MassIndexer indexer = searchSession.massIndexer()
					// Simulate passing information to connect to a DB, ...
					.context( StubLoadingContext.class, loadingContext )
					.dropAndCreateSchemaOnStart( true )
					.purgeAllOnStart( true );

			// add operations on indexes can follow any random order,
			// since they are executed by different threads
			backendMock.expectWorks(
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
					);

			backendMock.expectSchemaManagementWorks( Book.INDEX )
					.work( StubSchemaManagementWork.Type.DROP_AND_CREATE );

			// as purgeAllOnStart is explicitly set to true, and merge is true by default
			// it means that both purge and merge will be triggered:
			backendMock.expectIndexScaleWorks( Book.INDEX, searchSession.tenantIdentifier() )
					.purge()
					.mergeSegments()
					.flush()
					.refresh();

			try {
				indexer.startAndWait();
			}
			catch (InterruptedException e) {
				fail( "Unexpected InterruptedException: " + e.getMessage() );
			}

		}

		backendMock.verifyExpectationsMet();
	}

	@Test
	public void fromMappingWithoutSession() {
		MassIndexer indexer = mapping.scope( Object.class ).massIndexer()
				// Simulate passing information to connect to a DB, ...
				.context( StubLoadingContext.class, loadingContext );

		// add operations on indexes can follow any random order,
		// since they are executed by different threads
		backendMock.expectWorks(
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
				);

		// purgeAtStart and mergeSegmentsAfterPurge are enabled by default,
		// so we expect 1 purge, 1 mergeSegments and 1 flush calls in this order:
		backendMock.expectIndexScaleWorks( Book.INDEX )
				.purge()
				.mergeSegments()
				.flush()
				.refresh();

		try {
			indexer.startAndWait();
		}
		catch (InterruptedException e) {
			fail( "Unexpected InterruptedException: " + e.getMessage() );
		}

		backendMock.verifyExpectationsMet();
	}

	@Test
	public void reuseSearchSessionAfterSearchSessionIsClosed_createMassIndexer() {
		SearchSession searchSession = mapping.createSession();
		// a SearchSession instance is created lazily,
		// so we need to use it to have an instance of it
		searchSession.massIndexer();
		searchSession.close();

		assertThatThrownBy( () -> {
			searchSession.massIndexer();
		} )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Unable to access search session", "is closed" );
	}

	@Test
	public void lazyCreateSearchSessionAfterSearchSessionIsClosed_createMassIndexer() {
		// Search session is not created, since we don't use it
		SearchSession searchSession = mapping.createSession();
		searchSession.close();

		assertThatThrownBy( () -> {
			searchSession.massIndexer();
		} )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Unable to access search session", "is closed" );
	}

	private void initData() {
		persist( new Book( 1, TITLE_1, AUTHOR_1 ) );
		persist( new Book( 2, TITLE_2, AUTHOR_2 ) );
		persist( new Book( 3, TITLE_3, AUTHOR_3 ) );
	}

	private void persist(Book book) {
		loadingContext.persistenceMap( Book.PERSISTENCE_KEY ).put( book.id, book );
	}

	@Indexed(index = Book.INDEX)
	public static class Book {

		public static final String INDEX = "Book";
		public static final PersistenceTypeKey<Book, Integer> PERSISTENCE_KEY =
				new PersistenceTypeKey<>( Book.class, Integer.class );

		@DocumentId
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
}
