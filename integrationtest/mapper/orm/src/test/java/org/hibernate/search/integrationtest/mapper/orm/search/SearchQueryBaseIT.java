/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendUtils.reference;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import org.hibernate.SessionFactory;
import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.hibernate.search.mapper.orm.scope.SearchScope;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.common.spi.PojoEntityReference;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.util.common.SearchTimeoutException;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.extension.StubSearchWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendUtils;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.aggregation.impl.StubSearchAggregation;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.predicate.impl.StubSearchPredicate;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl.StubSearchProjection;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.sort.impl.StubSearchSort;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.SlowerLoadingListener;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.TimeoutLoadingListener;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.assertj.core.api.Assertions;

/**
 * Test everything related to the search query itself.
 * <p>
 * Does not test sorts and predicates, or other features that only involve the backend.
 * Those should be tested in the backend integration tests.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SearchQueryBaseIT {

	private static final String TITLE_4_3_2_1 = "4 3 2 1";
	private static final String AUTHOR_4_3_2_1 = "Paul Auster";

	private static final String TITLE_CIDER_HOUSE = "The Cider House Rules";
	private static final String AUTHOR_CIDER_HOUSE = "John Irving";

	private static final String TITLE_AVENUE_OF_MYSTERIES = "Avenue of Mysteries";
	private static final String AUTHOR_AVENUE_OF_MYSTERIES = "John Irving";

	@RegisterExtension
	public static BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public static OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );
	private SessionFactory sessionFactory;

	@BeforeAll
	void setup() {
		backendMock.expectAnySchema( Book.NAME );
		backendMock.expectAnySchema( Author.NAME );

		sessionFactory = ormSetupHelper.start().withAnnotatedTypes( Book.class, Author.class, NotIndexed.class )
				.dataClearing(
						config -> config.clearOrder( Book.class, Author.class, NotIndexed.class ) )
				.setup();
	}

	@BeforeEach
	void initData() {
		with( sessionFactory ).runInTransaction( session -> {
			Author author4321 = new Author( 1, AUTHOR_4_3_2_1 );
			Author authorCiderHouse = new Author( 2, AUTHOR_CIDER_HOUSE );
			Author authorAvenueOfMysteries = new Author( 3, AUTHOR_AVENUE_OF_MYSTERIES );

			Book book4321 = new Book( 1, TITLE_4_3_2_1 );
			book4321.setAuthor( author4321 );
			author4321.getBooks().add( book4321 );

			Book bookCiderHouse = new Book( 2, TITLE_CIDER_HOUSE );
			bookCiderHouse.setAuthor( authorCiderHouse );
			authorCiderHouse.getBooks().add( bookCiderHouse );

			Book bookAvenueOfMysteries = new Book( 3, TITLE_AVENUE_OF_MYSTERIES );
			bookAvenueOfMysteries.setAuthor( authorAvenueOfMysteries );
			authorAvenueOfMysteries.getBooks().add( bookAvenueOfMysteries );

			session.persist( author4321 );
			session.persist( authorCiderHouse );
			session.persist( authorAvenueOfMysteries );
			session.persist( book4321 );
			session.persist( bookCiderHouse );
			session.persist( bookAvenueOfMysteries );

			backendMock.expectWorks( Book.NAME )
					.add( "1", b -> b
							.field( "title", TITLE_4_3_2_1 )
							.objectField( "author", b2 -> b2
									.field( "name", AUTHOR_4_3_2_1 )
							)

					)
					.add( "2", b -> b
							.field( "title", TITLE_CIDER_HOUSE )
							.objectField( "author", b2 -> b2
									.field( "name", AUTHOR_CIDER_HOUSE )
							)
					)
					.add( "3", b -> b
							.field( "title", TITLE_AVENUE_OF_MYSTERIES )
							.objectField( "author", b2 -> b2
									.field( "name", AUTHOR_CIDER_HOUSE )
							)
					);
			backendMock.expectWorks( Author.NAME )
					.add( "1", b -> b
							.field( "name", AUTHOR_4_3_2_1 )
					)
					.add( "2", b -> b
							.field( "name", AUTHOR_CIDER_HOUSE )
					)
					.add( "3", b -> b
							.field( "name", AUTHOR_AVENUE_OF_MYSTERIES )
					);
		} );

		backendMock.verifyExpectationsMet();
	}

	@Test
	void target_byClass_singleType() {
		with( sessionFactory ).runInTransaction( session -> {
			SearchSession searchSession = Search.session( session );

			SearchQuery<Book> query = searchSession.search( Book.class )
					.where( f -> f.matchAll() )
					.toQuery();

			backendMock.expectSearchObjects(
					Arrays.asList( Book.NAME ),
					b -> {},
					StubSearchWorkBehavior.of(
							3L,
							reference( Book.NAME, "1" ),
							reference( Book.NAME, "2" ),
							reference( Book.NAME, "3" )
					)
			);

			assertThat( query.fetchAllHits() ).containsExactly(
					session.getReference( Book.class, 1 ),
					session.getReference( Book.class, 2 ),
					session.getReference( Book.class, 3 )
			);
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3896")
	void target_byClass_singleType_reuseQueryInstance() {
		with( sessionFactory ).runInTransaction( session -> {
			SearchSession searchSession = Search.session( session );

			SearchQuery<Book> query = searchSession.search( Book.class )
					.where( f -> f.matchAll() )
					.toQuery();

			// reuse three times the same query instance
			for ( int i = 0; i < 3; i++ ) {
				backendMock.expectSearchObjects(
						Arrays.asList( Book.NAME ),
						b -> {},
						StubSearchWorkBehavior.of(
								3L,
								reference( Book.NAME, "1" ),
								reference( Book.NAME, "2" ),
								reference( Book.NAME, "3" )
						)
				);

				assertThat( query.fetchAllHits() ).containsExactly(
						session.getReference( Book.class, 1 ),
						session.getReference( Book.class, 2 ),
						session.getReference( Book.class, 3 )
				);
			}
		} );
	}

	@Test
	void target_byClass_multipleTypes() {
		with( sessionFactory ).runInTransaction( session -> {
			SearchSession searchSession = Search.session( session );

			SearchQuery<Object> query = searchSession.search( Arrays.asList( Book.class, Author.class ) )
					.where( f -> f.matchAll() )
					.toQuery();

			backendMock.expectSearchObjects(
					Arrays.asList( Book.NAME, Author.NAME ),
					b -> {},
					StubSearchWorkBehavior.of(
							2L,
							reference( Book.NAME, "1" ),
							reference( Author.NAME, "2" )
					)
			);

			assertThat( query.fetchAllHits() ).containsExactly(
					session.getReference( Book.class, 1 ),
					session.getReference( Author.class, 2 )
			);
		} );
	}

	@Test
	void target_byClass_multipleTypes_entityLoadingTimeout_clientSideTimeout() {
		with( sessionFactory ).runInTransaction( session -> {
			SearchSession searchSession = Search.session( session );
			SlowerLoadingListener.registerSlowerLoadingListener( session, 100 );

			SearchQuery<Object> query = searchSession.search( Arrays.asList( Book.class, Author.class ) )
					.where( f -> f.matchAll() )
					.failAfter( 1L, TimeUnit.MILLISECONDS )
					.toQuery();

			backendMock.expectSearchObjects(
					Arrays.asList( Book.NAME, Author.NAME ),
					b -> b.failAfter( 1L, TimeUnit.MILLISECONDS ),
					StubSearchWorkBehavior.of(
							2L,
							reference( Book.NAME, "1" ),
							reference( Author.NAME, "2" )
					)
			);

			assertThatThrownBy( () -> query.fetchAllHits() )
					.isInstanceOf( SearchTimeoutException.class )
					.hasMessageContaining( "Operation exceeded the timeout of 0s, 1ms and 0ns" );
		} );
	}

	@Test
	void target_byClass_multipleTypes_entityLoadingTimeout_jdbcTimeout() {
		with( sessionFactory ).runInTransaction( session -> {
			SearchSession searchSession = Search.session( session );
			TimeoutLoadingListener.registerTimingOutLoadingListener( session );

			SearchQuery<Object> query = searchSession.search( Arrays.asList( Book.class, Author.class ) )
					.where( f -> f.matchAll() )
					.failAfter( 1L, TimeUnit.MILLISECONDS )
					.toQuery();

			backendMock.expectSearchObjects(
					Arrays.asList( Book.NAME, Author.NAME ),
					b -> b.failAfter( 1L, TimeUnit.MILLISECONDS ),
					StubSearchWorkBehavior.of(
							2L,
							reference( Book.NAME, "1" ),
							reference( Author.NAME, "2" )
					)
			);

			assertThatThrownBy( () -> query.fetchAllHits() )
					.isInstanceOf( SearchTimeoutException.class )
					.hasMessageContaining( "Operation exceeded the timeout of 0s, 1ms and 0ns" );
		} );
	}

	@Test
	void target_byClass_invalidClass_noEntitySubType() {
		with( sessionFactory ).runInTransaction( session -> {
			SearchSession searchSession = Search.session( session );

			Class<?> invalidClass = String.class;

			assertThatThrownBy( () -> searchSession.scope( invalidClass ) )
					.hasMessageContainingAll( "No matching indexed entity types for classes [" + invalidClass.getName() + "]",
							"Neither these classes nor any of their subclasses are indexed in Hibernate Search",
							"Valid classes are: ["
									+ Object.class.getName() + ", "
									+ Author.class.getName() + ", "
									+ Book.class.getName()
									+ "]" );
		} );
	}

	@Test
	void target_byClass_invalidClass_noIndexedSubtype() {
		with( sessionFactory ).runInTransaction( session -> {
			SearchSession searchSession = Search.session( session );

			Class<?> invalidClass = NotIndexed.class;

			assertThatThrownBy( () -> searchSession.scope( invalidClass ) )
					.hasMessageContainingAll( "No matching indexed entity types for classes [" + invalidClass.getName() + "]",
							"Neither these classes nor any of their subclasses are indexed in Hibernate Search",
							"Valid classes are: ["
									+ Object.class.getName() + ", "
									+ Author.class.getName() + ", "
									+ Book.class.getName()
									+ "]" );
		} );
	}

	@Test
	void target_byName_invalidType() {
		with( sessionFactory ).runInTransaction( session -> {
			SearchSession searchSession = Search.session( session );

			Class<?> invalidClass = String.class;

			assertThatThrownBy( () -> searchSession.scope(
					invalidClass, Book.NAME
			) )
					.hasMessageContainingAll(
							"Invalid type for '" + Book.NAME + "'",
							"the entity type must extend '" + invalidClass.getName()
									+ "', but entity type '" + Book.class.getName() + "' does not"
					);
		} );
	}

	@Test
	void target_byName_invalidName() {
		with( sessionFactory ).runInTransaction( session -> {
			SearchSession searchSession = Search.session( session );

			String invalidName = "foo";

			assertThatThrownBy( () -> searchSession.scope(
					Book.class, invalidName
			) )
					.hasMessageContainingAll(
							"No matching indexed entity types for entity names [" + invalidName + "]",
							"Either these are not the names of entity types",
							"or neither the entity types nor any of their subclasses are indexed in Hibernate Search",
							"Valid entity names are: ["
									// JPA entity names + Hibernate ORM entity names
									+ Author.NAME + ", "
									+ Author.class.getName() + ", "
									+ Book.NAME + ", "
									+ Book.class.getName()
			// NotIndexed should not be mentioned here
									+ "]"
					);
		} );
	}

	@Test
	void selectEntity() {
		with( sessionFactory ).runInTransaction( session -> {
			SearchSession searchSession = Search.session( session );

			SearchQuery<Book> query = searchSession.search( Book.class )
					.selectEntity()
					.where( f -> f.matchAll() )
					.toQuery();

			backendMock.expectSearchObjects(
					Arrays.asList( Book.NAME ),
					b -> {},
					StubSearchWorkBehavior.of(
							3L,
							reference( Book.NAME, "1" ),
							reference( Book.NAME, "2" ),
							reference( Book.NAME, "3" )
					)
			);

			assertThat( query.fetchAllHits() ).containsExactly(
					session.getReference( Book.class, 1 ),
					session.getReference( Book.class, 2 ),
					session.getReference( Book.class, 3 )
			);
		} );
	}

	@Test
	void select_searchProjection_single() {
		with( sessionFactory ).runInTransaction( session -> {
			SearchSession searchSession = Search.session( session );

			SearchScope<Book> scope = searchSession.scope( Book.class );

			SearchQuery<String> query = searchSession.search( scope )
					.select(
							scope.projection().field( "title", String.class ).toProjection()
					)
					.where( f -> f.matchAll() )
					.toQuery();

			backendMock.expectSearchProjection(
					Book.NAME,
					StubSearchWorkBehavior.of(
							3L,
							TITLE_4_3_2_1,
							TITLE_CIDER_HOUSE,
							TITLE_AVENUE_OF_MYSTERIES
					)
			);

			assertThat( query.fetchAllHits() ).containsExactly(
					TITLE_4_3_2_1,
					TITLE_CIDER_HOUSE,
					TITLE_AVENUE_OF_MYSTERIES
			);
		} );
	}

	@Test
	void select_searchProjection_multiple() {
		with( sessionFactory ).runInTransaction( session -> {
			SearchSession searchSession = Search.session( session );

			SearchScope<Book> scope = searchSession.scope( Book.class );

			SearchQuery<List<?>> query = searchSession.search( scope )
					.select(
							scope.projection().field( "title", String.class ).toProjection(),
							scope.projection().id( Integer.class ).toProjection(),
							scope.projection().entityReference().toProjection(),
							scope.projection().documentReference().toProjection(),
							scope.projection().field( "author.name", String.class ).toProjection()
					)
					.where( f -> f.matchAll() )
					.toQuery();

			backendMock.expectSearchProjection(
					Book.NAME,
					StubSearchWorkBehavior.of(
							3L,
							Arrays.asList(
									TITLE_4_3_2_1,
									"1",
									reference( Book.NAME, "1" ),
									reference( Book.NAME, "1" ),
									AUTHOR_4_3_2_1
							),
							Arrays.asList(
									TITLE_CIDER_HOUSE,
									"2",
									reference( Book.NAME, "2" ),
									reference( Book.NAME, "2" ),
									AUTHOR_CIDER_HOUSE
							),
							Arrays.asList(
									TITLE_AVENUE_OF_MYSTERIES,
									"3",
									reference( Book.NAME, "3" ),
									reference( Book.NAME, "3" ),
									AUTHOR_AVENUE_OF_MYSTERIES
							)
					)
			);

			assertThat( query.fetchAllHits() ).containsExactly(
					Arrays.asList(
							TITLE_4_3_2_1,
							1,
							PojoEntityReference.withName( Book.class, Book.NAME, 1 ),
							reference( Book.NAME, "1" ),
							AUTHOR_4_3_2_1
					),
					Arrays.asList(
							TITLE_CIDER_HOUSE,
							2,
							PojoEntityReference.withName( Book.class, Book.NAME, 2 ),
							reference( Book.NAME, "2" ),
							AUTHOR_CIDER_HOUSE
					),
					Arrays.asList(
							TITLE_AVENUE_OF_MYSTERIES,
							3,
							PojoEntityReference.withName( Book.class, Book.NAME, 3 ),
							reference( Book.NAME, "3" ),
							AUTHOR_AVENUE_OF_MYSTERIES
					)
			);
		} );
	}

	@Test
	void select_lambda() {
		with( sessionFactory ).runInTransaction( session -> {
			SearchSession searchSession = Search.session( session );

			SearchQuery<Book_Author_Score> query = searchSession.search( Book.class )
					.select( f -> f.composite()
							.from( f.composite()
									.from( f.entity(), f.field( "author.name", String.class ) )
									.as( Book_Author::new ),
									f.score() )
							.as( Book_Author_Score::new ) )
					.where( f -> f.matchAll() )
					.toQuery();

			backendMock.expectSearchProjection(
					Book.NAME,
					StubSearchWorkBehavior.of(
							3L,
							Arrays.asList(
									Arrays.asList( StubBackendUtils.reference( Book.NAME, "1" ), AUTHOR_4_3_2_1 ),
									4.0F
							),
							Arrays.asList(
									Arrays.asList( StubBackendUtils.reference( Book.NAME, "2" ), AUTHOR_CIDER_HOUSE ),
									5.0F
							),
							Arrays.asList(
									Arrays.asList( StubBackendUtils.reference( Book.NAME, "3" ), AUTHOR_AVENUE_OF_MYSTERIES ),
									6.0F
							)
					)
			);

			assertThat( query.fetchAllHits() ).containsExactlyInAnyOrder(
					new Book_Author_Score( new Book_Author( session.get( Book.class, 1 ), AUTHOR_4_3_2_1 ), 4.0F ),
					new Book_Author_Score( new Book_Author( session.get( Book.class, 2 ), AUTHOR_CIDER_HOUSE ), 5.0F ),
					new Book_Author_Score( new Book_Author( session.get( Book.class, 3 ), AUTHOR_AVENUE_OF_MYSTERIES ), 6.0F )
			);
		} );
	}

	@Test
	void select_compositeAndLoading() {
		with( sessionFactory ).runInTransaction( session -> {
			SearchSession searchSession = Search.session( session );

			SearchQuery<Book_Author_Score> query = searchSession.search( Book.class )
					.select( f -> f.composite()
							.from( f.composite()
									.from( f.entity(), f.field( "author.name", String.class ) )
									.as( Book_Author::new ),
									f.score() )
							.as( Book_Author_Score::new )
					)
					.where( f -> f.matchAll() )
					.toQuery();

			backendMock.expectSearchProjection(
					Book.NAME,
					StubSearchWorkBehavior.of(
							3L,
							Arrays.asList(
									Arrays.asList( StubBackendUtils.reference( Book.NAME, "1" ), AUTHOR_4_3_2_1 ),
									4.0F
							),
							Arrays.asList(
									Arrays.asList( StubBackendUtils.reference( Book.NAME, "2" ), AUTHOR_CIDER_HOUSE ),
									5.0F
							),
							Arrays.asList(
									Arrays.asList( StubBackendUtils.reference( Book.NAME, "3" ), AUTHOR_AVENUE_OF_MYSTERIES ),
									6.0F
							)
					)
			);

			assertThat( query.fetchAllHits() ).containsExactlyInAnyOrder(
					new Book_Author_Score( new Book_Author( session.getReference( Book.class, 1 ), AUTHOR_4_3_2_1 ), 4.0F ),
					new Book_Author_Score( new Book_Author( session.getReference( Book.class, 2 ), AUTHOR_CIDER_HOUSE ), 5.0F ),
					new Book_Author_Score( new Book_Author( session.getReference( Book.class, 3 ), AUTHOR_AVENUE_OF_MYSTERIES ),
							6.0F )
			);
		} );
	}

	/**
	 * A smoke test for components (predicate, sort, ...) created from the mapping without a session
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3671")
	void componentsFromMappingWithoutSession() {
		SearchMapping mapping = Search.mapping( sessionFactory );
		SearchScope<Book> scope = mapping.scope( Book.class );

		/*
		 * The backend is a stub, so these components are stub too:
		 * we simply test that the mapper correctly delegates to the backend.
		 */
		SearchProjection<? extends EntityReference> projection = scope.projection().entityReference().toProjection();
		SearchPredicate predicate = scope.predicate().matchAll().toPredicate();
		SearchSort sort = scope.sort().field( "title" ).toSort();
		SearchAggregation<Map<String, Long>> aggregation = scope.aggregation().terms()
				.field( "title", String.class ).toAggregation();
		assertSoftly( a -> {
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
		with( sessionFactory ).runInTransaction( session -> {
			SearchSession searchSession = Search.session( session );

			SearchQuery<? extends EntityReference> query = searchSession.search( scope )
					.select( projection )
					.where( predicate )
					.sort( sort )
					.aggregation( aggregationKey, aggregation )
					.toQuery();

			backendMock.expectSearchProjection(
					Book.NAME,
					StubSearchWorkBehavior.of(
							3L,
							StubBackendUtils.reference( Book.NAME, "1" ),
							StubBackendUtils.reference( Book.NAME, "2" ),
							StubBackendUtils.reference( Book.NAME, "3" )
					)
			);

			Assertions.<EntityReference>assertThat( query.fetchAllHits() ).containsExactlyInAnyOrder(
					PojoEntityReference.withName( Book.class, Book.NAME, 1 ),
					PojoEntityReference.withName( Book.class, Book.NAME, 2 ),
					PojoEntityReference.withName( Book.class, Book.NAME, 3 )
			);
		} );
	}

	@Test
	void select_searchProjection_entityReference() {
		with( sessionFactory ).runInTransaction( session -> {
			SearchSession searchSession = Search.session( session );

			SearchScope<Book> scope = searchSession.scope( Book.class );

			SearchQuery<? extends EntityReference> query = searchSession.search( scope )
					.select( f -> f.entityReference() )
					.where( f -> f.matchAll() )
					.toQuery();

			backendMock.expectSearchProjection(
					Book.NAME,
					StubSearchWorkBehavior.of(
							3L,
							StubBackendUtils.reference( Book.NAME, "1" ),
							StubBackendUtils.reference( Book.NAME, "2" ),
							StubBackendUtils.reference( Book.NAME, "3" )
					)
			);

			Assertions.<EntityReference>assertThat( query.fetchAllHits() ).containsExactly(
					PojoEntityReference.withName( Book.class, Book.NAME, 1 ),
					PojoEntityReference.withName( Book.class, Book.NAME, 2 ),
					PojoEntityReference.withName( Book.class, Book.NAME, 3 )
			);
		} );
	}

	@Test
	@SuppressWarnings("deprecation")
	void select_searchProjection_entityReference_deprecatedEntityReferenceType() {
		with( sessionFactory ).runInTransaction( session -> {
			SearchSession searchSession = Search.session( session );

			SearchScope<Book> scope = searchSession.scope( Book.class );

			SearchQuery<org.hibernate.search.mapper.orm.common.EntityReference> query = searchSession.search( scope )
					.select( f -> f.entityReference() )
					.where( f -> f.matchAll() )
					.toQuery();

			backendMock.expectSearchProjection(
					Book.NAME,
					StubSearchWorkBehavior.of(
							3L,
							StubBackendUtils.reference( Book.NAME, "1" ),
							StubBackendUtils.reference( Book.NAME, "2" ),
							StubBackendUtils.reference( Book.NAME, "3" )
					)
			);

			assertThat( query.fetchAllHits() ).containsExactly(
					org.hibernate.search.mapper.orm.common.impl.HibernateOrmEntityReference.withName( Book.class, Book.NAME,
							1 ),
					org.hibernate.search.mapper.orm.common.impl.HibernateOrmEntityReference.withName( Book.class, Book.NAME,
							2 ),
					org.hibernate.search.mapper.orm.common.impl.HibernateOrmEntityReference.withName( Book.class, Book.NAME, 3 )
			);
		} );
	}

	@Entity(name = Book.NAME)
	@Indexed(index = Book.NAME)
	public static class Book {

		public static final String NAME = "Book";

		@Id
		private Integer id;

		@GenericField
		private String title;

		@ManyToOne
		@IndexedEmbedded
		private Author author;

		public Book() {
		}

		public Book(Integer id, String title) {
			this.id = id;
			this.title = title;
		}

		public Integer getId() {
			return id;
		}

		public String getTitle() {
			return title;
		}

		public Author getAuthor() {
			return author;
		}

		public void setAuthor(Author author) {
			this.author = author;
		}
	}

	@Entity(name = Author.NAME)
	@Indexed(index = Author.NAME)
	public static class Author {

		public static final String NAME = "Author";

		@Id
		private Integer id;

		@GenericField
		private String name;

		@OneToMany(mappedBy = "author")
		private final List<Book> books = new ArrayList<>();

		public Author() {
		}

		public Author(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public List<Book> getBooks() {
			return books;
		}
	}

	@Entity(name = NotIndexed.NAME)
	public static class NotIndexed {

		public static final String NAME = "NotInd";

		@Id
		private Integer id;

		private String name;

		public NotIndexed() {
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
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
			if ( !( obj instanceof Book_Author ) ) {
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
			if ( !( obj instanceof Book_Author_Score ) ) {
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
