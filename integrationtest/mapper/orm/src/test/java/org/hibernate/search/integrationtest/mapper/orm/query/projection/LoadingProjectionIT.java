/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.query.projection;

import java.util.Arrays;
import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.assertj.core.api.Assertions;
import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.hibernate.FullTextQuery;
import org.hibernate.search.mapper.orm.hibernate.FullTextSearchTarget;
import org.hibernate.search.mapper.orm.hibernate.FullTextSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.rule.StubSearchWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendUtils;
import org.hibernate.search.util.impl.integrationtest.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.orm.OrmUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class LoadingProjectionIT {

	private static final String TITLE_4_3_2_1 = "4 3 2 1";
	private static final String AUTHOR_4_3_2_1 = "Paul Auster";

	private static final String TITLE_CIDER_HOUSE = "The Cider House Rules";
	private static final String AUTHOR_CIDER_HOUSE = "John Irving";

	private static final String TITLE_AVENUE_OF_MYSTERIES = "Avenue of Mysteries";
	private static final String AUTHOR_AVENUE_OF_MYSTERIES = "John Irving";

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public OrmSetupHelper ormSetupHelper = new OrmSetupHelper();

	private SessionFactory sessionFactory;

	@Before
	public void setup() {
		backendMock.expectAnySchema( Book.INDEX );

		sessionFactory = ormSetupHelper.withBackendMock( backendMock )
				.setup( Book.class );

		backendMock.verifyExpectationsMet();

		initData();
	}

	@Test
	public void search() {
		OrmUtils.withinSession( sessionFactory, session -> {
			FullTextSession ftSession = Search.getFullTextSession( session );
			FullTextSearchTarget<Book> searchTarget = ftSession.search( Book.class );

			FullTextQuery<Book_Author_Score> query = ftSession.search( Book.class )
					.query()
					.asProjection( searchTarget.projection().composite(
							Book_Author_Score::new,
							searchTarget.projection().composite(
									Book_Author::new,
									searchTarget.projection().object().toProjection(),
									searchTarget.projection().field( "author", String.class ).toProjection()
							).toProjection(),
							searchTarget.projection().score().toProjection()
					).toProjection() )
					.predicate( f -> f.matchAll().toPredicate() )
					.build();

			backendMock.expectSearchProjection(
					Arrays.asList( Book.INDEX ),
					b -> { },
					StubSearchWorkBehavior.of(
							3L,
							Arrays.asList(
									Arrays.asList( StubBackendUtils.reference( Book.INDEX, "1" ), AUTHOR_4_3_2_1 ),
									4.0F
							),
							Arrays.asList(
									Arrays.asList( StubBackendUtils.reference( Book.INDEX, "2" ), AUTHOR_CIDER_HOUSE ),
									5.0F
							),
							Arrays.asList(
									Arrays.asList( StubBackendUtils.reference( Book.INDEX, "3" ), AUTHOR_AVENUE_OF_MYSTERIES ),
									6.0F
							)
					)
			);

			Assertions.assertThat( query.list() ).containsExactlyInAnyOrder(
					new Book_Author_Score( new Book_Author( session.get( Book.class, 1 ), AUTHOR_4_3_2_1 ), 4.0F ),
					new Book_Author_Score( new Book_Author( session.get( Book.class, 2 ), AUTHOR_CIDER_HOUSE ), 5.0F ),
					new Book_Author_Score( new Book_Author( session.get( Book.class, 3 ), AUTHOR_AVENUE_OF_MYSTERIES ), 6.0F )
			);
		} );
	}

	private void initData() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			session.persist( new Book( 1, TITLE_4_3_2_1, AUTHOR_4_3_2_1 ) );
			session.persist( new Book( 2, TITLE_CIDER_HOUSE, AUTHOR_CIDER_HOUSE ) );
			session.persist( new Book( 3, TITLE_AVENUE_OF_MYSTERIES, AUTHOR_AVENUE_OF_MYSTERIES ) );

			backendMock.expectWorks( Book.INDEX )
					.add( "1", b -> b
							.field( "title", TITLE_4_3_2_1 )
							.field( "author", AUTHOR_4_3_2_1 )
					)
					.add( "2", b -> b
							.field( "title", TITLE_CIDER_HOUSE )
							.field( "author", AUTHOR_CIDER_HOUSE )
					)
					.add( "3", b -> b
							.field( "title", TITLE_AVENUE_OF_MYSTERIES )
							.field( "author", AUTHOR_AVENUE_OF_MYSTERIES )
					)
					.preparedThenExecuted();
		} );

		backendMock.verifyExpectationsMet();
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

	private static class Book_Author {

		private Book book;

		private String author;

		public Book_Author(Book book, String author) {
			this.book = book;
			this.author = author;
		}

		@Override
		public boolean equals(Object obj) {
			if ( !(obj instanceof Book_Author) ) {
				return false;
			}
			Book_Author other = (Book_Author) obj;
			return Objects.equals( book, other.book )
					&& Objects.equals( author, other.author );
		}

		@Override
		public int hashCode() {
			return Objects.hash( book, author );
		}

		@Override
		public String toString() {
			return book + " - " + author;
		}
	}

	private static class Book_Author_Score {

		private Book_Author book_author;

		private Float score;

		public Book_Author_Score(Book_Author book_author, Float score) {
			this.book_author = book_author;
			this.score = score;
		}

		@Override
		public boolean equals(Object obj) {
			if ( !(obj instanceof Book_Author_Score) ) {
				return false;
			}
			Book_Author_Score other = (Book_Author_Score) obj;
			return Objects.equals( book_author, other.book_author )
					&& Objects.equals( score, other.score );
		}

		@Override
		public int hashCode() {
			return Objects.hash( book_author, score );
		}

		@Override
		public String toString() {
			return book_author + " - " + score;
		}
	}
}
