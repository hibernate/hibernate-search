/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.massindexing;

import static org.assertj.core.api.Fail.fail;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.mapper.javabean.mapping.SearchMapping;
import org.hibernate.search.mapper.javabean.massindexing.MassIndexer;
import org.hibernate.search.mapper.javabean.massindexing.loader.JavaBeanIndexingStrategies;
import org.hibernate.search.mapper.javabean.session.SearchSession;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.IdentifierBridgeRef;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeFromDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeToDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

/**
 * Test mass indexing of an entity type using an {@link EmbeddedId}.
 */
public class MassIndexingEmbeddedIdIT {

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
	public final MockitoRule mockito = MockitoJUnit.rule().strictness( Strictness.STRICT_STUBS );

	private SearchMapping mapping;

	private Book book1;
	private Book book2;
	private Book book3;

	private Map<BookId, Book> booksmap = new LinkedHashMap<>();

	@Before
	public void setup() {
		backendMock.expectAnySchema( Book.INDEX );

		mapping = setupHelper.start()
				.expectCustomBeans()
				.setup( Book.class );

		backendMock.verifyExpectationsMet();

		initData();
	}

	@Test
	public void defaultMassIndexerStartAndWait() {
		try ( SearchSession searchSession = createSessionFromMap() ) {
			MassIndexer indexer = searchSession.massIndexer();

			// add operations on indexes can follow any random order,
			// since they are executed by different threads
			backendMock.expectWorks(
					Book.INDEX, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE
			)
					.add( book1.getIdentity().toString(), b -> b
					.field( "title", TITLE_1 )
					.field( "author", AUTHOR_1 )
					)
					.add( book2.getIdentity().toString(), b -> b
					.field( "title", TITLE_2 )
					.field( "author", AUTHOR_2 )
					)
					.add( book3.getIdentity().toString(), b -> b
					.field( "title", TITLE_3 )
					.field( "author", AUTHOR_3 )
					)
					.createdThenExecuted();

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

	private SearchSession createSessionFromMap() {
		return mapping.createSessionWithOptions().loading( (o) -> {
			o.registerLoader( Book.class, (identifiers) -> {
				return identifiers.stream()
						.map( (identifier) -> booksmap.get( (BookId) identifier ) )
						.collect( Collectors.toList() );
			} );

			o.massIndexingLoadingStrategy( Book.class, JavaBeanIndexingStrategies.from( booksmap ) );
		} ).build();
	}

	private void initData() {
		persist( book1 = new Book( 1, TITLE_1, AUTHOR_1 ) );
		persist( book2 = new Book( 2, TITLE_2, AUTHOR_2 ) );
		persist( book3 = new Book( 3, TITLE_3, AUTHOR_3 ) );
	}

	private void persist(Book book) {
		booksmap.put( book.identity, book );
	}

	@Indexed(index = Book.INDEX)
	public static class Book {
		public static final String INDEX = "Book";

		@DocumentId(identifierBridge = @IdentifierBridgeRef(type = BookIdIndexBridge.class))
		private BookId identity;

		private Long id;

		@GenericField
		private String title;

		@GenericField
		private String author;

		private Book() {
		}

		public Book(Integer identity, String title, String author) {
			this.identity = new BookId( identity, title );
			this.title = title;
			this.author = author;
		}

		public BookId getIdentity() {
			return identity;
		}

		public Long getId() {
			return id;
		}

		public String getTitle() {
			return title;
		}

		public String getAuthor() {
			return author;
		}
	}

	public static class BookId implements Serializable {
		private static final String SEPARATOR = "###";

		public static BookId fromString(String documentIdentifier) {
			String[] split = documentIdentifier.split( SEPARATOR );
			return new BookId( Integer.parseInt( split[0] ), split[1], split[2] );
		}

		private Integer identity;
		private String code;
		private String title;

		public BookId(Integer identity, String title) {
			this.identity = identity;
			this.code = UUID.randomUUID().toString();
			this.title = title;
		}

		public BookId() {
		}

		private BookId(Integer identity, String code, String title) {
			this.identity = identity;
			this.code = code;
			this.title = title;
		}

		public Integer getIdentity() {
			return identity;
		}

		public String getCode() {
			return code;
		}

		public String getTitle() {
			return title;
		}

		@Override
		public String toString() {
			return identity + SEPARATOR + getCode() + SEPARATOR + getTitle();
		}
	}

	public static class BookIdIndexBridge implements IdentifierBridge<BookId> {

		@Override
		public String toDocumentIdentifier(BookId propertyValue, IdentifierBridgeToDocumentIdentifierContext context) {
			return propertyValue.toString();
		}

		@Override
		public BookId fromDocumentIdentifier(String documentIdentifier, IdentifierBridgeFromDocumentIdentifierContext context) {
			return BookId.fromString( documentIdentifier );
		}
	}
}
