/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.search.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.backend.elasticsearch.ElasticsearchExtension;
import org.hibernate.search.backend.elasticsearch.search.query.ElasticsearchSearchQuery;
import org.hibernate.search.backend.elasticsearch.search.query.ElasticsearchSearchResult;
import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class ElasticsearchQueryDslIT {

	private static final int BOOK1_ID = 1;
	private static final int BOOK2_ID = 2;
	private static final int BOOK3_ID = 3;
	private static final int BOOK4_ID = 4;

	@Rule
	public DocumentationSetupHelper setupHelper = DocumentationSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	private EntityManagerFactory entityManagerFactory;

	@Before
	public void setup() {
		entityManagerFactory = setupHelper.start()
				.setup( Book.class );
		initData();
	}

	@Test
	public void explain() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			// tag::explain-elasticsearch[]
			ElasticsearchSearchQuery<Book> query = searchSession.search( Book.class )
					.extension( ElasticsearchExtension.get() ) // <1>
					.where( f -> f.match()
							.field( "title" )
							.matching( "robot" ) )
					.toQuery(); // <2>

			JsonObject explanation1 = query.explain( 1 ); // <3>
			JsonObject explanation2 = query.explain( "Book", 1 ); // <4>

			ElasticsearchSearchQuery<Book> elasticsearchQuery = query.extension( ElasticsearchExtension.get() ); // <5>
			// end::explain-elasticsearch[]

			assertThat( explanation1 ).asString().contains( "title" );
			assertThat( explanation2 ).asString().contains( "title" );
			assertThat( elasticsearchQuery ).isNotNull();
		} );
	}

	@Test
	public void json() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			// tag::elasticsearch-requestTransformer[]
			List<Book> hits = searchSession.search( Book.class )
					.extension( ElasticsearchExtension.get() ) // <1>
					.where( f -> f.match()
							.field( "title" )
							.matching( "robot" ) )
					.requestTransformer( context -> { // <2>
						Map<String, String> parameters = context.parametersMap(); // <3>
						parameters.put( "search_type", "dfs_query_then_fetch" );

						JsonObject body = context.body(); // <4>
						body.addProperty( "min_score", 0.5f );
					} )
					.fetchHits( 20 ); // <5>
			// end::elasticsearch-requestTransformer[]

			assertThat( hits ).extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK3_ID );
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			// tag::elasticsearch-responseBody[]
			ElasticsearchSearchResult<Book> result = searchSession.search( Book.class )
					.extension( ElasticsearchExtension.get() ) // <1>
					.where( f -> f.match()
							.field( "title" )
							.matching( "robt" ) )
					.requestTransformer( context -> { // <2>
						JsonObject body = context.body();
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

			JsonObject responseBody = result.responseBody(); // <5>
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
		with( entityManagerFactory ).runInTransaction( entityManager -> {
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
	}


}
