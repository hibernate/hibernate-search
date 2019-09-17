/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.search;

import static org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendUtils.reference;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;

import org.hibernate.SessionFactory;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.mapper.orm.common.EntityReference;
import org.hibernate.search.mapper.orm.common.impl.EntityReferenceImpl;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.hibernate.search.mapper.orm.scope.SearchScope;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.rule.StubSearchWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendUtils;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.aggregation.impl.StubSearchAggregation;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.predicate.impl.StubSearchPredicate;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl.StubSearchProjection;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.sort.StubSearchSort;
import org.hibernate.search.util.impl.integrationtest.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.orm.OrmUtils;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test everything related to the search query itself.
 * <p>
 * Does not test sorts and predicates, or other features that only involve the backend.
 * Those should be tested in the backend integration tests.
 */
public class SearchQueryBaseIT {

	private static final String TITLE_4_3_2_1 = "4 3 2 1";
	private static final String AUTHOR_4_3_2_1 = "Paul Auster";

	private static final String TITLE_CIDER_HOUSE = "The Cider House Rules";
	private static final String AUTHOR_CIDER_HOUSE = "John Irving";

	private static final String TITLE_AVENUE_OF_MYSTERIES = "Avenue of Mysteries";
	private static final String AUTHOR_AVENUE_OF_MYSTERIES = "John Irving";

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	private SessionFactory sessionFactory;

	@Before
	public void setup() {
		backendMock.expectAnySchema( Book.INDEX );

		sessionFactory = ormSetupHelper.start()
				.setup( Book.class );

		backendMock.verifyExpectationsMet();

		initData();
	}

	@Test
	public void asEntity() {
		OrmUtils.withinSession( sessionFactory, session -> {
			SearchSession searchSession = Search.session( session );

			SearchQuery<Book> query = searchSession.search( Book.class )
					.asEntity()
					.predicate( f -> f.matchAll() )
					.toQuery();

			backendMock.expectSearchObjects(
					Arrays.asList( Book.INDEX ),
					b -> { },
					StubSearchWorkBehavior.of(
							3L,
							reference( Book.INDEX, "1" ),
							reference( Book.INDEX, "2" ),
							reference( Book.INDEX, "3" )
					)
			);

			Assertions.assertThat( query.fetchAllHits() ).containsExactly(
					session.load( Book.class, 1 ),
					session.load( Book.class, 2 ),
					session.load( Book.class, 3 )
			);
		} );
	}

	@Test
	public void asProjection_searchProjectionObject_single() {
		OrmUtils.withinSession( sessionFactory, session -> {
			SearchSession searchSession = Search.session( session );

			SearchScope<Book> scope = searchSession.scope( Book.class );

			SearchQuery<String> query = searchSession.search( scope )
					.asProjection(
							scope.projection().field( "title", String.class ).toProjection()
					)
					.predicate( f -> f.matchAll() )
					.toQuery();

			backendMock.expectSearchProjection(
					Arrays.asList( Book.INDEX ),
					b -> { },
					StubSearchWorkBehavior.of(
							3L,
							TITLE_4_3_2_1,
							TITLE_CIDER_HOUSE,
							TITLE_AVENUE_OF_MYSTERIES
					)
			);

			Assertions.assertThat( query.fetchAllHits() ).containsExactly(
					TITLE_4_3_2_1,
					TITLE_CIDER_HOUSE,
					TITLE_AVENUE_OF_MYSTERIES
			);
		} );
	}

	@Test
	public void asProjection_searchProjectionObject_multiple() {
		OrmUtils.withinSession( sessionFactory, session -> {
			SearchSession searchSession = Search.session( session );

			SearchScope<Book> scope = searchSession.scope( Book.class );

			SearchQuery<List<?>> query = searchSession.search( scope )
					.asProjections(
							scope.projection().field( "title", String.class ).toProjection(),
							scope.projection().entityReference().toProjection(),
							scope.projection().documentReference().toProjection(),
							scope.projection().field( "author", String.class ).toProjection()
					)
					.predicate( f -> f.matchAll() )
					.toQuery();

			backendMock.expectSearchProjection(
					Arrays.asList( Book.INDEX ),
					b -> { },
					StubSearchWorkBehavior.of(
							3L,
							Arrays.asList(
									TITLE_4_3_2_1,
									reference( Book.INDEX, "1" ),
									reference( Book.INDEX, "1" ),
									AUTHOR_4_3_2_1
							),
							Arrays.asList(
									TITLE_CIDER_HOUSE,
									reference( Book.INDEX, "2" ),
									reference( Book.INDEX, "2" ),
									AUTHOR_CIDER_HOUSE
							),
							Arrays.asList(
									TITLE_AVENUE_OF_MYSTERIES,
									reference( Book.INDEX, "3" ),
									reference( Book.INDEX, "3" ),
									AUTHOR_AVENUE_OF_MYSTERIES
							)
					)
			);

			Assertions.assertThat( query.fetchAllHits() ).containsExactly(
					Arrays.asList(
							TITLE_4_3_2_1,
							new EntityReferenceImpl( Book.class, 1 ),
							reference( Book.INDEX, "1" ),
							AUTHOR_4_3_2_1
					),
					Arrays.asList(
							TITLE_CIDER_HOUSE,
							new EntityReferenceImpl( Book.class, 2 ),
							reference( Book.INDEX, "2" ),
							AUTHOR_CIDER_HOUSE
					),
					Arrays.asList(
							TITLE_AVENUE_OF_MYSTERIES,
							new EntityReferenceImpl( Book.class, 3 ),
							reference( Book.INDEX, "3" ),
							AUTHOR_AVENUE_OF_MYSTERIES
					)
			);
		} );
	}

	@Test
	public void asProjection_lambda() {
		OrmUtils.withinSession( sessionFactory, session -> {
			SearchSession searchSession = Search.session( session );

			SearchQuery<Book_Author_Score> query = searchSession.search( Book.class )
					.asProjection( f ->
							f.composite(
									Book_Author_Score::new,
									f.composite(
											Book_Author::new,
											f.entity().toProjection(),
											f.field( "author", String.class ).toProjection()
									).toProjection(),
									f.score().toProjection()
							)
					)
					.predicate( f -> f.matchAll() )
					.toQuery();

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

			Assertions.assertThat( query.fetchAllHits() ).containsExactlyInAnyOrder(
					new Book_Author_Score( new Book_Author( session.get( Book.class, 1 ), AUTHOR_4_3_2_1 ), 4.0F ),
					new Book_Author_Score( new Book_Author( session.get( Book.class, 2 ), AUTHOR_CIDER_HOUSE ), 5.0F ),
					new Book_Author_Score( new Book_Author( session.get( Book.class, 3 ), AUTHOR_AVENUE_OF_MYSTERIES ), 6.0F )
			);
		} );
	}

	@Test
	public void asProjection_compositeAndLoading() {
		OrmUtils.withinSession( sessionFactory, session -> {
			SearchSession searchSession = Search.session( session );

			SearchQuery<Book_Author_Score> query = searchSession.search( Book.class )
					.asProjection( f ->
							f.composite(
									Book_Author_Score::new,
									f.composite(
											Book_Author::new,
											f.entity(),
											f.field( "author", String.class )
									),
									f.score()
							)
					)
					.predicate( f -> f.matchAll() )
					.toQuery();

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

			Assertions.assertThat( query.fetchAllHits() ).containsExactlyInAnyOrder(
					new Book_Author_Score( new Book_Author( session.load( Book.class, 1 ), AUTHOR_4_3_2_1 ), 4.0F ),
					new Book_Author_Score( new Book_Author( session.load( Book.class, 2 ), AUTHOR_CIDER_HOUSE ), 5.0F ),
					new Book_Author_Score( new Book_Author( session.load( Book.class, 3 ), AUTHOR_AVENUE_OF_MYSTERIES ), 6.0F )
			);
		} );
	}

	/**
	 * A smoke test for components (predicate, sort, ...) created from the mapping without a session
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3671")
	public void componentsFromMappingWithoutSession() {
		SearchMapping mapping = Search.mapping( sessionFactory );
		SearchScope<Book> scope = mapping.scope( Book.class );

		/*
		 * The backend is a stub, so these components are stub too:
		 * we simply test that the mapper correctly delegates to the backend.
		 */
		SearchProjection<EntityReference> projection = scope.projection().entityReference().toProjection();
		SearchPredicate predicate = scope.predicate().matchAll().toPredicate();
		SearchSort sort = scope.sort().field( "title" ).toSort();
		SearchAggregation<Map<String, Long>> aggregation = scope.aggregation().terms()
				.field( "title", String.class ).toAggregation();
		SoftAssertions.assertSoftly( a -> {
			a.assertThat( projection ).isInstanceOf( StubSearchProjection.class );
			a.assertThat( predicate ).isInstanceOf( StubSearchPredicate.class );
			a.assertThat( sort ).isInstanceOf( StubSearchSort.class );
			a.assertThat( aggregation ).isInstanceOf( StubSearchAggregation.class );
		} );

		/*
		 * ... and below we test that passing these objects to the DSL will work correctly.
		 * Objects are casted explicitly by the stub backend,
		 * so if a wrong object was passed, the whole query would fail.
		 */
		AggregationKey<Map<String, Long>> aggregationKey = AggregationKey.of( "titleAgg" );
		OrmUtils.withinSession( sessionFactory, session -> {
			SearchSession searchSession = Search.session( session );

			SearchQuery<EntityReference> query = searchSession.search( scope )
					.asProjection( projection )
					.predicate( predicate )
					.sort( sort )
					.aggregation( aggregationKey, aggregation )
					.toQuery();

			backendMock.expectSearchProjection(
					Arrays.asList( Book.INDEX ),
					b -> { },
					StubSearchWorkBehavior.of(
							3L,
							StubBackendUtils.reference( Book.INDEX, "1" ),
							StubBackendUtils.reference( Book.INDEX, "2" ),
							StubBackendUtils.reference( Book.INDEX, "3" )
					)
			);

			Assertions.assertThat( query.fetchHits() ).containsExactlyInAnyOrder(
					new EntityReferenceImpl( Book.class, 1 ),
					new EntityReferenceImpl( Book.class, 2 ),
					new EntityReferenceImpl( Book.class, 3 )
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
					.processedThenExecuted();
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
