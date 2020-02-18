/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.search.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.persistence.EntityManagerFactory;
import javax.persistence.SharedCacheMode;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.search.backend.elasticsearch.ElasticsearchExtension;
import org.hibernate.search.backend.elasticsearch.search.query.ElasticsearchSearchQuery;
import org.hibernate.search.backend.elasticsearch.search.query.ElasticsearchSearchResult;
import org.hibernate.search.backend.lucene.LuceneExtension;
import org.hibernate.search.backend.lucene.search.query.LuceneSearchQuery;
import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.ElasticsearchBackendConfiguration;
import org.hibernate.search.documentation.testsupport.LuceneBackendConfiguration;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.automaticindexing.AutomaticIndexingSynchronizationStrategyName;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.search.loading.EntityLoadingCacheLookupStrategy;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.util.common.SearchTimeoutException;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils;
import org.hibernate.stat.Statistics;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.lucene.search.Explanation;

@RunWith(Parameterized.class)
public class QueryDslIT {

	private static final int BOOK1_ID = 1;
	private static final int BOOK2_ID = 2;
	private static final int BOOK3_ID = 3;
	private static final int BOOK4_ID = 4;

	private static final int MANAGER1_ID = 1;
	private static final int MANAGER2_ID = 2;
	private static final int ASSOCIATE1_ID = 1;
	private static final int ASSOCIATE2_ID = 2;

	@Parameterized.Parameters(name = "{0}")
	public static Object[] backendConfigurations() {
		return BackendConfigurations.simple().toArray();
	}

	@Rule
	public OrmSetupHelper setupHelper;

	private final BackendConfiguration backendConfiguration;

	private EntityManagerFactory entityManagerFactory;

	public QueryDslIT(BackendConfiguration backendConfiguration) {
		this.setupHelper = OrmSetupHelper.withSingleBackend( backendConfiguration );
		this.backendConfiguration = backendConfiguration;
	}

	@Before
	public void setup() {
		entityManagerFactory = setupHelper.start()
				.withProperty(
						HibernateOrmMapperSettings.AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY,
						AutomaticIndexingSynchronizationStrategyName.SYNC
				)
				.withProperty( AvailableSettings.JPA_SHARED_CACHE_MODE, SharedCacheMode.ENABLE_SELECTIVE.name() )
				.setup( Book.class, Manager.class, Associate.class );
		initData();
	}

	@Test
	public void entryPoint() {
		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			// tag::entryPoint[]
			// Not shown: get the entity manager and open a transaction
			SearchSession searchSession = Search.session( entityManager ); // <1>

			SearchResult<Book> result = searchSession.search( Book.class ) // <2>
					.where( f -> f.match() // <3>
							.field( "title" )
							.matching( "robot" ) )
					.fetch( 20 ); // <4>

			long totalHitCount = result.getTotalHitCount(); // <5>
			List<Book> hits = result.getHits(); // <6>
			// Not shown: commit the transaction and close the entity manager
			// end::entryPoint[]

			assertThat( totalHitCount ).isEqualTo( 2 );
			assertThat( hits ).extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK3_ID );
		} );
	}

	@Test
	public void targetingMultipleEntityTypes() {
		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			// tag::targeting-multiple[]
			SearchResult<Person> result = searchSession.search( Arrays.asList( // <1>
							Manager.class, Associate.class
					) )
					.where( f -> f.match() // <2>
							.field( "name" )
							.matching( "james" ) )
					.fetch( 20 ); // <3>
			// end::targeting-multiple[]
			List<Person> hits = result.getHits();
			assertThat( hits )
					.containsExactlyInAnyOrder(
							entityManager.getReference( Manager.class, MANAGER1_ID ),
							entityManager.getReference( Associate.class, ASSOCIATE1_ID )
					);
		} );
	}

	@Test
	public void targetingByEntityName() {
		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			// tag::targeting-entityName[]
			SearchResult<Person> result = searchSession.search( // <1>
							searchSession.scope( // <2>
									Person.class,
									Arrays.asList( "Manager", "Associate" )
							)
					)
					.where( f -> f.match() // <3>
							.field( "name" )
							.matching( "james" ) )
					.fetch( 20 ); // <4>
			// end::targeting-entityName[]
			List<Person> hits = result.getHits();
			assertThat( hits )
					.containsExactlyInAnyOrder(
							entityManager.getReference( Manager.class, MANAGER1_ID ),
							entityManager.getReference( Associate.class, ASSOCIATE1_ID )
					);
		} );
	}

	@Test
	public void fetchingBasics() {
		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			// tag::fetching-searchResult[]
			SearchResult<Book> result = searchSession.search( Book.class ) // <1>
					.where( f -> f.matchAll() )
					.fetch( 20 ); // <2>

			long totalHitCount = result.getTotalHitCount(); // <3>
			List<Book> hits = result.getHits(); // <4>
			// ... // <5>
			// end::fetching-searchResult[]

			assertThat( totalHitCount ).isEqualTo( 4 );
			assertThat( hits ).extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK2_ID, BOOK3_ID, BOOK4_ID );
		} );

		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			// tag::fetching-totalHitCount[]
			long totalHitCount = searchSession.search( Book.class )
					.where( f -> f.matchAll() )
					.fetchTotalHitCount();
			// end::fetching-totalHitCount[]

			assertThat( totalHitCount ).isEqualTo( 4 );
		} );

		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			// tag::fetching-hits[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.matchAll() )
					.fetchHits( 20 );
			// end::fetching-hits[]

			assertThat( hits ).extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK2_ID, BOOK3_ID, BOOK4_ID );
		} );

		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			// tag::fetching-singleHit[]
			Optional<Book> hit = searchSession.search( Book.class )
					.where( f -> f.id().matching( 1 ) )
					.fetchSingleHit();
			// end::fetching-singleHit[]

			assertThat( hit ).get().extracting( Book::getId )
					.isEqualTo( BOOK1_ID );
		} );
	}

	@Test
	public void fetchingAllHits() {
		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			// tag::fetching-all-searchResult[]
			SearchResult<Book> result = searchSession.search( Book.class )
					.where( f -> f.id().matchingAny( Arrays.asList( 1, 2 ) ) )
					.fetchAll();

			long totalHitCount = result.getTotalHitCount();
			List<Book> hits = result.getHits();
			// end::fetching-all-searchResult[]

			assertThat( totalHitCount ).isEqualTo( 2 );
			assertThat( hits ).extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK2_ID );
		} );

		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			// tag::fetching-all-hits[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.id().matchingAny( Arrays.asList( 1, 2 ) ) )
					.fetchAllHits();
			// end::fetching-all-hits[]

			assertThat( hits ).extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK2_ID );
		} );
	}

	@Test
	public void pagination() {
		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			// tag::fetching-pagination-searchResult[]
			SearchResult<Book> result = searchSession.search( Book.class )
					.where( f -> f.matchAll() )
					.fetch( 40, 20 ); // <1>
			// end::fetching-pagination-searchResult[]
			long totalHitCount = result.getTotalHitCount();
			List<Book> hits = result.getHits();

			assertThat( totalHitCount ).isEqualTo( 4 );
			assertThat( hits ).isEmpty();
		} );

		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			// tag::fetching-pagination-hits[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.matchAll() )
					.fetchHits( 40, 20 ); // <1>
			// end::fetching-pagination-hits[]

			assertThat( hits ).isEmpty();
		} );
	}

	@Test
	public void searchQuery() {
		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			// tag::searchQuery[]
			SearchQuery<Book> query = searchSession.search( Book.class ) // <1>
					.where( f -> f.matchAll() )
					.toQuery(); // <2>
			List<Book> hits = query.fetchHits( 20 ); // <3>
			// end::searchQuery[]

			assertThat( hits ).extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK2_ID, BOOK3_ID, BOOK4_ID );
		} );

		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			// tag::searchQuery-toORM[]
			SearchQuery<Book> query = searchSession.search( Book.class ) // <1>
					.where( f -> f.matchAll() )
					.toQuery(); // <2>
			javax.persistence.TypedQuery<Book> jpaQuery = Search.toJpaQuery( query ); // <3>
			org.hibernate.query.Query<Book> ormQuery = Search.toOrmQuery( query ); // <4>
			// end::searchQuery-toORM[]
			List<Book> hits = jpaQuery.getResultList();
			assertThat( hits ).extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK2_ID, BOOK3_ID, BOOK4_ID );
			hits = ormQuery.list();
			assertThat( hits ).extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK2_ID, BOOK3_ID, BOOK4_ID );
		} );
	}

	@Test
	public void explain_lucene() {
		Assume.assumeTrue( backendConfiguration instanceof LuceneBackendConfiguration );

		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			// tag::explain-lucene[]
			LuceneSearchQuery<Book> query = searchSession.search( Book.class )
					.extension( LuceneExtension.get() ) // <1>
					.where( f -> f.match()
							.field( "title" )
							.matching( "robot" ) )
					.toQuery(); // <2>

			Explanation explanation1 = query.explain( "1" ); // <3>
			Explanation explanation2 = query.explain( "Book", "1" ); // <4>

			LuceneSearchQuery<Book> luceneQuery = query.extension( LuceneExtension.get() ); // <5>
			// end::explain-lucene[]

			assertThat( explanation1 ).asString()
					.contains( "title" );
			assertThat( explanation2 ).asString()
					.contains( "title" );
			assertThat( luceneQuery ).isNotNull();
		} );
	}

	@Test
	public void explain_elasticsearch() {
		Assume.assumeTrue( backendConfiguration instanceof ElasticsearchBackendConfiguration );

		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			// tag::explain-elasticsearch[]
			ElasticsearchSearchQuery<Book> query = searchSession.search( Book.class )
					.extension( ElasticsearchExtension.get() ) // <1>
					.where( f -> f.match()
							.field( "title" )
							.matching( "robot" ) )
					.toQuery(); // <2>

			JsonObject explanation1 = query.explain( "1" ); // <3>
			JsonObject explanation2 = query.explain( "Book", "1" ); // <4>

			ElasticsearchSearchQuery<Book> elasticsearchQuery = query.extension( ElasticsearchExtension.get() ); // <5>
			// end::explain-elasticsearch[]

			assertThat( explanation1 ).asString().contains( "title" );
			assertThat( explanation2 ).asString().contains( "title" );
			assertThat( elasticsearchQuery ).isNotNull();
		} );
	}

	@Test
	public void tookAndTimedOut() {
		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			// tag::took-timedOut[]
			SearchQuery<Book> query = searchSession.search( Book.class )
					.where( f -> f.match()
							.field( "title" )
							.matching( "robot" ) )
					.toQuery();

			SearchResult<Book> result = query.fetch( 20 ); // <1>

			Duration took = result.getTook(); // <2>
			Boolean timedOut = result.isTimedOut(); // <3>
			// end::took-timedOut[]

			assertThat( took ).isNotNull();
			assertThat( timedOut ).isNotNull();
		} );
	}

	@Test
	public void truncateAfter() {
		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			// tag::truncateAfter[]
			SearchResult<Book> result = searchSession.search( Book.class ) // <1>
					.where( f -> f.match()
							.field( "title" )
							.matching( "robot" ) )
					.truncateAfter( 500, TimeUnit.MILLISECONDS ) // <2>
					.fetch( 20 ); // <3>

			Duration took = result.getTook(); // <4>
			Boolean timedOut = result.isTimedOut(); // <5>
			// end::truncateAfter[]

			assertThat( took ).isNotNull();
			assertThat( timedOut ).isNotNull();
		} );
	}

	@Test
	public void failAfter() {
		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			// tag::failAfter[]
			try {
				SearchResult<Book> result = searchSession.search( Book.class ) // <1>
						.where( f -> f.match()
								.field( "title" )
								.matching( "robot" ) )
						.failAfter( 500, TimeUnit.MILLISECONDS ) // <2>
						.fetch( 20 ); // <3>
			}
			catch (SearchTimeoutException e) { // <4>
				// ...
			}
			// end::failAfter[]
		} );
	}

	@Test
	public void cacheLookupStrategy() {
		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			Statistics statistics = entityManagerFactory.unwrap( SessionFactory.class ).getStatistics();
			statistics.setStatisticsEnabled( true );
			statistics.clear();

			SearchSession searchSession = Search.session( entityManager );

			// tag::cacheLookupStrategy-persistenceContextThenSecondLevelCache[]
			SearchResult<Book> result = searchSession.search( Book.class ) // <1>
					.where( f -> f.match()
							.field( "title" )
							.matching( "robot" ) )
					.loading( o -> o.cacheLookupStrategy( // <2>
							EntityLoadingCacheLookupStrategy.PERSISTENCE_CONTEXT_THEN_SECOND_LEVEL_CACHE
					) )
					.fetch( 20 ); // <3>
			// end::cacheLookupStrategy-persistenceContextThenSecondLevelCache[]

			assertThat( result.getHits() ).extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK3_ID );
			assertThat( statistics.getSecondLevelCacheHitCount() ).isEqualTo( 2 );
		} );
	}

	@Test
	public void fetchSize() {
		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			// tag::fetchSize[]
			SearchResult<Book> result = searchSession.search( Book.class ) // <1>
					.where( f -> f.match()
							.field( "title" )
							.matching( "robot" ) )
					.loading( o -> o.fetchSize( 50 ) ) // <2>
					.fetch( 200 ); // <3>
			// end::fetchSize[]

			assertThat( result.getHits() ).extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK3_ID );
		} );
	}

	@Test
	public void json_elasticsearch() {
		Assume.assumeTrue( backendConfiguration instanceof ElasticsearchBackendConfiguration );

		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			// tag::elasticsearch-requestTransformer[]
			List<Book> hits = searchSession.search( Book.class )
					.extension( ElasticsearchExtension.get() ) // <1>
					.where( f -> f.match()
							.field( "title" )
							.matching( "robot" ) )
					.requestTransformer( context -> { // <2>
						Map<String, String> parameters = context.getParametersMap(); // <3>
						parameters.put( "search_type", "dfs_query_then_fetch" );

						JsonObject body = context.getBody(); // <4>
						body.addProperty( "min_score", 0.5f );
					} )
					.fetchHits( 20 ); // <5>
			// end::elasticsearch-requestTransformer[]

			assertThat( hits ).extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK3_ID );
		} );

		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			// tag::elasticsearch-responseBody[]
			ElasticsearchSearchResult<Book> result = searchSession.search( Book.class )
					.extension( ElasticsearchExtension.get() ) // <1>
					.where( f -> f.match()
							.field( "title" )
							.matching( "robt" ) )
					.requestTransformer( context -> { // <2>
						JsonObject body = context.getBody();
						body.add( "suggest", jsonObject( suggest -> { // <3>
							suggest.add( "my-suggest", jsonObject( mySuggest -> {
								mySuggest.addProperty( "text", "robt" );
								mySuggest.add( "term", jsonObject( term -> {
									term.addProperty( "field", "title" );
								} ) );
							} ) );
						} ) );
					} )
					.fetch( 20 ); // <4>

			JsonObject responseBody = result.getResponseBody(); // <5>
			JsonArray mySuggestResults = responseBody.getAsJsonObject( "suggest" ) // <6>
					.getAsJsonArray( "my-suggest" );
			// end::elasticsearch-responseBody[]

			assertThat( mySuggestResults.size() ).isGreaterThanOrEqualTo( 1 );
			JsonObject mySuggestResult0 = mySuggestResults.get( 0 ).getAsJsonObject();
			assertThat( mySuggestResult0.get( "text" ).getAsString() )
					.isEqualTo( "robt" );
			JsonObject mySuggestResult0Option0 = mySuggestResult0.getAsJsonArray( "options" ).get( 0 )
					.getAsJsonObject();
			assertThat( mySuggestResult0Option0.get( "text" ).getAsString() )
					.isEqualTo( "robot" );

		} );
	}

	// tag::elasticsearch-responseBody-helper[]
	private static JsonObject jsonObject(Consumer<JsonObject> instructions) {
		JsonObject object = new JsonObject();
		instructions.accept( object );
		return object;
	}
	// end::elasticsearch-responseBody-helper[]

	private void initData() {
		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			Book book1 = new Book();
			book1.setId( BOOK1_ID );
			book1.setTitle( "I, Robot" );

			Book book2 = new Book();
			book2.setId( BOOK2_ID );
			book2.setTitle( "The Caves of Steel" );

			Book book3 = new Book();
			book3.setId( BOOK3_ID );
			book3.setTitle( "The Robots of Dawn" );

			Book book4 = new Book();
			book4.setId( BOOK4_ID );
			book4.setTitle( "The Automatic Detective" );

			entityManager.persist( book1 );
			entityManager.persist( book2 );
			entityManager.persist( book3 );
			entityManager.persist( book4 );
		} );

		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			Manager manager1 = new Manager();
			manager1.setId( MANAGER1_ID );
			manager1.setName( "James Green" );

			Manager manager2 = new Manager();
			manager2.setId( MANAGER2_ID );
			manager2.setName( "John Doe" );

			Associate associate1 = new Associate();
			associate1.setId( ASSOCIATE1_ID );
			associate1.setName( "James Harper" );
			associate1.setManager( manager1 );
			manager1.getAssociates().add( associate1 );

			Associate associate2 = new Associate();
			associate2.setId( ASSOCIATE2_ID );
			associate2.setName( "John Sutherland" );
			associate2.setManager( manager2 );
			manager2.getAssociates().add( associate2 );

			entityManager.persist( manager1 );
			entityManager.persist( manager2 );
			entityManager.persist( associate1 );
			entityManager.persist( associate2 );
		} );
	}


}
