/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.realbackend.limitations;


import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl.OutboxPollingOutboxEventAdditionalJaxbMappingProducer.ENTITY_NAME;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.dialect.CockroachDB192Dialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.search.integrationtest.mapper.orm.realbackend.testsupport.BackendConfigurations;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl.OutboxEvent;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategyNames;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.Rule;
import org.junit.Test;

import org.awaitility.Awaitility;
import org.awaitility.core.ThrowingRunnable;

/**
 * See "limitations-parallel-embedded-update" in the documentation.
 */
public class ConcurrentEmbeddedUpdateLimitationIT {

	@Rule
	public OrmSetupHelper setupHelper = OrmSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	private SessionFactory sessionFactory;
	private boolean synchronizationAsync;

	@Test
	public void indexingStrategySession() throws Throwable {
		synchronizationAsync = false;
		sessionFactory = setupHelper.start()
				// This is absolutely necessary to avoid false positives in this test
				.withProperty( HibernateOrmMapperSettings.INDEXING_PLAN_SYNCHRONIZATION_STRATEGY,
						IndexingPlanSynchronizationStrategyNames.SYNC )
				.skipTestForDialect( SQLServerDialect.class,
						"The execution could provoke a failure caused by a deadlock on SQLServer, "
						+ "which will abort our requests and will make the tests fail." )
				.skipTestForDialect( CockroachDB192Dialect.class,
						"The execution could provoke a 'failed preemptive refresh due to a conflict' on CockroachDB,"
						+ " which will abort our requests and will make the tests fail." )
				.setup( Book.class, Author.class, BookEdition.class );

		reproducer();

		// The previous data isn't there: good.
		assertThat( countByEditionAndAuthor( "12th", "asimov" ) ).isEqualTo( 0L );

		// The new data isn't there either: bad!
		assertThat( countByEditionAndAuthor( "13th", "vonnegut" ) ).isEqualTo( 0L );

		// Turns out only half of the update came through...
		assertThat( countByEditionAndAuthor( "13th", "asimov" ) ).isEqualTo( 1L );
	}

	@Test
	public void indexingStrategyOutbox() throws Throwable {
		synchronizationAsync = true;
		sessionFactory = setupHelper.start()
				.withProperty( "hibernate.search.coordination.strategy", "outbox-polling" )
				.setup( Book.class, Author.class, BookEdition.class );

		reproducer();

		Awaitility.await()
				.timeout( 5, TimeUnit.SECONDS )
				.until( () -> noMoreOutboxEvents( sessionFactory ) );

		verify( () -> assertThat( countByEditionAndAuthor( "12th", "asimov" ) ).isEqualTo( 0L ) );
		verify( () -> assertThat( countByEditionAndAuthor( "13th", "vonnegut" ) ).isEqualTo( 1L ) );
		verify( () -> assertThat( countByEditionAndAuthor( "13th", "asimov" ) ).isEqualTo( 0L ) );
	}

	private void reproducer() throws Throwable {
		with( sessionFactory ).runInTransaction( session -> {
			Book book = new Book();
			book.setTitle( "The Caves Of Steel" );

			Author author = new Author();
			author.setName( "Isaac Asimov" );
			book.getAuthors().add( author );
			author.getBooks().add( book );

			BookEdition edition1 = new BookEdition();
			edition1.setLabel( "Mass Market Paperback, 12th Edition" );
			edition1.setBook( book );
			book.getEditions().add( edition1 );

			BookEdition edition2 = new BookEdition();
			edition2.setLabel( "Kindle Edition" );
			edition2.setBook( book );
			book.getEditions().add( edition2 );

			session.persist( edition1 );
			session.persist( edition2 );
			session.persist( author );
			session.persist( book );
		} );

		verify( () -> assertThat( countByEditionAndAuthor( "12th", "asimov" ) ).isEqualTo( 1L ) );

		try ( Session session1 = sessionFactory.openSession(); Session session2 = sessionFactory.openSession() ) {
			Transaction tx1 = null;
			Transaction tx2 = null;
			try {
				tx1 = session1.beginTransaction();
				tx2 = session2.beginTransaction();

				Author author = session1.createQuery( "select a from Author a", Author.class )
						.list().get( 0 );
				author.setName( "Kurt Vonnegut" );

				BookEdition bookEdition = session2.createQuery( "select e from BookEdition e", BookEdition.class )
						.list().get( 0 );
				bookEdition.setLabel( "Mass Market Paperback, 13th Edition" );

				// Simulate tx.commit starting.
				// Entities are loaded in session 2, including the author from the DB, still not modified.
				// Documents are created in session 2 based on this data: author is not updated in the documents.
				session2.flush();

				// Entities are loaded in session 1, including the bookEdition from the DB, still not modified.
				// Documents are created in session 1 based on this data: bookEdition is not updated in the documents.
				// Indexing is performed in session 1: author is up-to-date, but not bookEdition.
				tx1.commit();

				// Indexing is performed in session 2: bookEdition is up-to-date, but not author.
				// In particular, author is **restored to its previous state**!
				tx2.commit();
			}
			catch (RuntimeException e) {
				new SuppressingCloser( e )
						.push( Transaction::rollback, tx1 )
						.push( Transaction::rollback, tx2 );
				throw e;
			}
		}
	}

	private void verify(ThrowingRunnable runnable) throws Throwable {
		if ( !synchronizationAsync ) {
			runnable.run();
			return;
		}

		Awaitility.await()
				.timeout( 5, TimeUnit.SECONDS )
				.untilAsserted( runnable );
	}

	long countByEditionAndAuthor(String editionLabel, String authorName) {
		return with( sessionFactory ).applyInTransaction( session -> {
			SearchSession searchSession = Search.session( session );

			return searchSession.search( Book.class )
					.where( f -> f.bool()
							.must( f.match().field( "editions.label" ).matching( editionLabel ) )
							.must( f.match().field( "authors.name" ).matching( authorName ) ) )
					.fetchTotalHitCount();
		} );
	}

	private static boolean noMoreOutboxEvents(SessionFactory sessionFactory) {
		try ( Session session = sessionFactory.openSession() ) {
			return session.createQuery( "select e from " + ENTITY_NAME
					+ " e order by id", OutboxEvent.class ).list().isEmpty();
		}
	}

	@Entity(name = Author.NAME)
	public static class Author {
		public static final String NAME = "Author";

		@Id
		@GeneratedValue
		private Integer id;

		@FullTextField
		private String name;

		@ManyToMany(mappedBy = "authors")
		private Set<Book> books = new HashSet<>();

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Set<Book> getBooks() {
			return books;
		}
	}

	@Entity(name = Book.NAME)
	@Indexed
	public static class Book {
		public static final String NAME = "Book";

		@Id
		@GeneratedValue
		private Integer id;

		@FullTextField
		private String title;

		@ManyToMany
		@IndexedEmbedded
		private List<Author> authors = new ArrayList<>();

		@OneToMany(mappedBy = "book")
		@IndexedEmbedded
		private List<BookEdition> editions = new ArrayList<>();

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public List<Author> getAuthors() {
			return authors;
		}

		public List<BookEdition> getEditions() {
			return editions;
		}
	}

	@Entity(name = BookEdition.NAME)
	public static class BookEdition {
		public static final String NAME = "BookEdition";

		@Id
		@GeneratedValue
		private Integer id;

		@ManyToOne
		private Book book;

		@FullTextField
		private String label;

		public BookEdition() {
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Book getBook() {
			return book;
		}

		public void setBook(Book book) {
			this.book = book;
		}

		public String getLabel() {
			return label;
		}

		public void setLabel(String label) {
			this.label = label;
		}
	}
}
