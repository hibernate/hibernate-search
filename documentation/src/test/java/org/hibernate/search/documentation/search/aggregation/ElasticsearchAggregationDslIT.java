/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.search.aggregation;

import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;
import static org.hibernate.search.util.impl.test.JsonHelper.assertJsonEquals;

import java.sql.Date;
import java.util.function.Consumer;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.backend.elasticsearch.ElasticsearchExtension;
import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class ElasticsearchAggregationDslIT {

	private static final int BOOK1_ID = 1;
	private static final int BOOK2_ID = 2;
	private static final int BOOK3_ID = 3;
	private static final int BOOK4_ID = 4;

	@Rule
	public DocumentationSetupHelper setupHelper = DocumentationSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	private EntityManagerFactory entityManagerFactory;

	@Before
	public void setup() {
		entityManagerFactory = setupHelper.start().setup( Book.class, BookEdition.class );
		initData();
	}

	@Test
	public void elasticsearch() {
		withinSearchSession( searchSession -> {
			// tag::elasticsearch-fromJson-jsonObject[]
			JsonObject jsonObject =
					// end::elasticsearch-fromJson-jsonObject[]
					new Gson().fromJson(
							"{"
									+ "\"histogram\": {"
											+ "\"field\": \"price\","
											+ "\"interval\": 10"
									+ "}"
							+ "}",
							JsonObject.class
					)
					// tag::elasticsearch-fromJson-jsonObject[]
					/* ... */;
			AggregationKey<JsonObject> countsByPriceHistogramKey = AggregationKey.of( "countsByPriceHistogram" );
			SearchResult<Book> result = searchSession.search( Book.class )
					.extension( ElasticsearchExtension.get() )
					.where( f -> f.matchAll() )
					.aggregation( countsByPriceHistogramKey, f -> f.fromJson( jsonObject ) )
					.fetch( 20 );
			JsonObject countsByPriceHistogram = result.aggregation( countsByPriceHistogramKey ); // <1>
			// end::elasticsearch-fromJson-jsonObject[]
			assertJsonEquals(
					"{"
							+ "\"buckets\": ["
									+ "{"
											+ "\"key\": 0.0,"
											+ "\"doc_count\": 1"
									+ "},"
									+ "{"
											+ "\"key\": 10.0,"
											+ "\"doc_count\": 2"
									+ "},"
									+ "{"
											+ "\"key\": 20.0,"
											+ "\"doc_count\": 1"
									+ "}"
							+ "]"
					+ "}",
					countsByPriceHistogram.toString()
			);
		} );

		withinSearchSession( searchSession -> {
			// tag::elasticsearch-fromJson-string[]
			AggregationKey<JsonObject> countsByPriceHistogramKey = AggregationKey.of( "countsByPriceHistogram" );
			SearchResult<Book> result = searchSession.search( Book.class )
					.extension( ElasticsearchExtension.get() )
					.where( f -> f.matchAll() )
					.aggregation( countsByPriceHistogramKey, f -> f.fromJson( "{"
									+ "\"histogram\": {"
											+ "\"field\": \"price\","
											+ "\"interval\": 10"
									+ "}"
							+ "}" ) )
					.fetch( 20 );
			JsonObject countsByPriceHistogram = result.aggregation( countsByPriceHistogramKey ); // <1>
			// end::elasticsearch-fromJson-string[]
			assertJsonEquals(
					"{"
							+ "\"buckets\": ["
									+ "{"
											+ "\"key\": 0.0,"
											+ "\"doc_count\": 1"
									+ "},"
									+ "{"
											+ "\"key\": 10.0,"
											+ "\"doc_count\": 2"
									+ "},"
									+ "{"
											+ "\"key\": 20.0,"
											+ "\"doc_count\": 1"
									+ "}"
							+ "]"
					+ "}",
					countsByPriceHistogram.toString()
			);
		} );
	}

	private void withinSearchSession(Consumer<SearchSession> action) {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			action.accept( searchSession );
		} );
	}

	private void initData() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			Book book1 = new Book();
			book1.setId( BOOK1_ID );
			book1.setTitle( "I, Robot" );
			book1.setPrice( 24.99 );
			book1.setGenre( Genre.SCIENCE_FICTION );
			book1.setReleaseDate( Date.valueOf( "1950-12-02" ) );
			addEdition( book1, "Mass Market Paperback, 1st Edition", 9.99 );
			addEdition( book1, "Kindle", 9.99 );

			Book book2 = new Book();
			book2.setId( BOOK2_ID );
			book2.setTitle( "The Caves of Steel" );
			book2.setPrice( 19.99 );
			book2.setGenre( Genre.SCIENCE_FICTION );
			book2.setReleaseDate( Date.valueOf( "1953-10-01" ) );
			addEdition( book2, "Mass Market Paperback, 12th Edition", 4.99 );
			addEdition( book2, "Kindle", 19.99 );

			Book book3 = new Book();
			book3.setId( BOOK3_ID );
			book3.setTitle( "The Robots of Dawn" );
			book3.setPrice( 15.99 );
			book3.setGenre( Genre.SCIENCE_FICTION );
			book3.setReleaseDate( Date.valueOf( "1983-01-01" ) );
			addEdition( book3, "Mass Market Paperback, 59th Edition", 3.99 );
			addEdition( book3, "Kindle", 5.99 );

			Book book4 = new Book();
			book4.setId( BOOK4_ID );
			book4.setTitle( "The Automatic Detective" );
			book4.setPrice( 7.99 );
			book4.setGenre( Genre.CRIME_FICTION );
			book4.setReleaseDate( Date.valueOf( "2008-02-05" ) );
			addEdition( book4, "Mass Market Paperback, 2nd Edition", 10.99 );
			addEdition( book4, "Kindle", 12.99 );

			entityManager.persist( book1 );
			entityManager.persist( book2 );
			entityManager.persist( book3 );
			entityManager.persist( book4 );
		} );
	}

	private void addEdition(Book book, String label, double price) {
		BookEdition edition = new BookEdition();
		edition.setBook( book );
		edition.setLabel( label );
		edition.setPrice( price );
		book.getEditions().add( edition );
	}
}
