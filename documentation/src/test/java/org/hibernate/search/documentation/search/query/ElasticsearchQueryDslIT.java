package org.hibernate.search.documentation.search.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.backend.elasticsearch.ElasticsearchExtension;
import org.hibernate.search.backend.elasticsearch.json.jackson.JacksonJsonHandlers;
import org.hibernate.search.backend.elasticsearch.search.query.ElasticsearchSearchQuery;
import org.hibernate.search.backend.elasticsearch.search.query.ElasticsearchSearchResult;
import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

class ElasticsearchQueryDslIT {

	private static final int BOOK1_ID = 1;
	private static final int BOOK2_ID = 2;
	private static final int BOOK3_ID = 3;
	private static final int BOOK4_ID = 4;

	@RegisterExtension
	public DocumentationSetupHelper setupHelper = DocumentationSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	private EntityManagerFactory entityManagerFactory;

	@BeforeEach
	void setup() {
		entityManagerFactory = setupHelper.start()
				.setup( Book.class );
		initData();
	}

	@Test
	void explain() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			// tag::explain-elasticsearch[]
			ElasticsearchSearchQuery<Book> query = searchSession.search( Book.class )
					.extension( ElasticsearchExtension.get() ) // <1>
					.where( f -> f.match()
							.field( "title" )
							.matching( "robot" ) )
					.toQuery(); // <2>

			ObjectNode explanation1 = query.explain( JacksonJsonHandlers.object(), 1 ); // <3>
			ObjectNode explanation2 = query.explain( JacksonJsonHandlers.object(), "Book", 1 ); // <4>

			ElasticsearchSearchQuery<Book> elasticsearchQuery = query.extension( ElasticsearchExtension.get() ); // <5>
			// end::explain-elasticsearch[]

			assertThat( explanation1 ).asString().contains( "title" );
			assertThat( explanation2 ).asString().contains( "title" );
			assertThat( elasticsearchQuery ).isNotNull();
		} );
	}

	@Test
	void json() {
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

						context.transformBody( JacksonJsonHandlers.object(), body -> { // <4>
							body.put( "min_score", 0.2f );
							return body;
						} );
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
						context.transformBody( JacksonJsonHandlers.object(), body -> {
							body.set( "suggest", jsonObject( suggest -> { // <3>
								suggest.set( "my-suggest", jsonObject( mySuggest -> {
									mySuggest.put( "text", "robt" );
									mySuggest.set( "term", jsonObject( term -> {
										term.put( "field", "title" );
									} ) );
								} ) );
							} ) );
							return body;
						} );
					} )
					.fetch( 20 ); // <4>

			ObjectNode responseBody = result.responseBody( JacksonJsonHandlers.object() ); // <5>
			JsonNode mySuggestResults = responseBody.path( "suggest" ) // <6>
					.path( "my-suggest" );
			// end::elasticsearch-responseBody[]

			assertThat( mySuggestResults.size() ).isGreaterThanOrEqualTo( 1 );
			JsonNode mySuggestResult0 = mySuggestResults.get( 0 );
			assertThat( mySuggestResult0.get( "text" ).asString() )
					.isEqualTo( "robt" );
			JsonNode mySuggestResult0Option0 = mySuggestResult0.path( "options" ).get( 0 );
			assertThat( mySuggestResult0Option0.get( "text" ).asString() )
					.isEqualTo( "robot" );

		} );
	}

	// tag::elasticsearch-responseBody-helper[]
	private static ObjectNode jsonObject(Consumer<ObjectNode> instructions) {
		ObjectNode object = JsonNodeFactory.instance.objectNode();
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
