/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.massindexing;

import static org.assertj.core.api.Fail.fail;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.io.Serializable;
import java.util.UUID;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import org.hibernate.SessionFactory;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.IdentifierBridgeRef;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeFromDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeToDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

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
	public BackendMock backendMock = new BackendMock();

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	private SessionFactory sessionFactory;
	private Book book1;
	private Book book2;
	private Book book3;

	@Before
	public void setup() {
		backendMock.expectAnySchema( Book.INDEX );

		sessionFactory = ormSetupHelper.start()
				.withPropertyRadical( HibernateOrmMapperSettings.Radicals.AUTOMATIC_INDEXING_ENABLED, false )
				.setup( Book.class );

		backendMock.verifyExpectationsMet();

		initData();
	}

	@Test
	public void defaultMassIndexerStartAndWait() {
		with( sessionFactory ).runNoTransaction( session -> {
			SearchSession searchSession = Search.session( session );
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
					);

			// purgeAtStart and mergeSegmentsAfterPurge are enabled by default,
			// so we expect 1 purge, 1 mergeSegments and 1 flush calls in this order:
			backendMock.expectIndexScaleWorks( Book.INDEX, session.getTenantIdentifier() )
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

		} );

		backendMock.verifyExpectationsMet();
	}

	private void initData() {
		with( sessionFactory ).runInTransaction( session -> {
			book1 = new Book( 1, TITLE_1, AUTHOR_1 );
			session.persist( book1 );
			book2 = new Book( 2, TITLE_2, AUTHOR_2 );
			session.persist( book2 );
			book3 = new Book( 3, TITLE_3, AUTHOR_3 );
			session.persist( book3 );
		} );
	}

	@Entity
	@Table(name = "book")
	@Indexed(index = Book.INDEX)
	public static class Book {
		public static final String INDEX = "Book";

		@EmbeddedId
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

		@DocumentId(identifierBridge = @IdentifierBridgeRef(type = BookIdIndexBridge.class))
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

	@Embeddable
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

		private BookId() {
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
