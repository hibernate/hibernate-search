/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.searchdsl.predicate;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.persistence.EntityManagerFactory;

import org.hibernate.search.backend.elasticsearch.ElasticsearchExtension;
import org.hibernate.search.backend.lucene.LuceneExtension;
import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.ElasticsearchBackendConfiguration;
import org.hibernate.search.documentation.testsupport.LuceneBackendConfiguration;
import org.hibernate.search.engine.search.common.BooleanOperator;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoBoundingBox;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.engine.spatial.GeoPolygon;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.automaticindexing.AutomaticIndexingSynchronizationStrategyName;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.scope.SearchScope;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.orm.OrmUtils;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.RegexpQuery;

@RunWith(Parameterized.class)
public class PredicateDslIT {

	private static final int ASIMOV_ID = 1;
	private static final int MARTINEZ_ID = 2;

	private static final int BOOK1_ID = 1;
	private static final int BOOK2_ID = 2;
	private static final int BOOK3_ID = 3;
	private static final int BOOK4_ID = 4;

	@Parameterized.Parameters(name = "{0}")
	public static Object[] backendConfigurations() {
		return BackendConfigurations.simple().toArray();
	}

	@Rule
	public OrmSetupHelper setupHelper;

	private BackendConfiguration backendConfiguration;

	private EntityManagerFactory entityManagerFactory;

	public PredicateDslIT(BackendConfiguration backendConfiguration) {
		this.setupHelper = OrmSetupHelper.withSingleBackend( backendConfiguration );
		this.backendConfiguration = backendConfiguration;
	}

	@Before
	public void setup() {
		entityManagerFactory = setupHelper.start()
				.withProperty(
						HibernateOrmMapperSettings.AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY,
						AutomaticIndexingSynchronizationStrategyName.SEARCHABLE
				)
				.setup( Book.class, Author.class, EmbeddableGeoPoint.class );
		initData();
	}

	@Test
	public void entryPoint() {
		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			// tag::entryPoint-lambdas[]
			SearchSession searchSession = Search.session( entityManager );

			List<Book> result = searchSession.search( Book.class ) // <1>
					.predicate( f -> f.match().field( "title" ) // <2>
							.matching( "robot" ) )
					.fetchHits( 20 ); // <3>
			// end::entryPoint-lambdas[]
			assertThat( result )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK3_ID );
		} );

		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			// tag::entryPoint-objects[]
			SearchSession searchSession = Search.session( entityManager );

			SearchScope<Book> scope = searchSession.scope( Book.class );

			List<Book> result = scope.search()
					.predicate( scope.predicate().match().field( "title" )
							.matching( "robot" )
							.toPredicate() )
					.fetchHits( 20 );
			// end::entryPoint-objects[]
			assertThat( result )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK3_ID );
		} );
	}


	@Test
	public void matchAll() {
		withinSearchSession( searchSession -> {
			// tag::matchAll[]
			List<Book> hits = searchSession.search( Book.class )
					.predicate( f -> f.matchAll() )
					.fetchHits( 20 );
			// end::matchAll[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK2_ID, BOOK3_ID, BOOK4_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::matchAll-except[]
			List<Book> hits = searchSession.search( Book.class )
					.predicate( f -> f.matchAll()
							.except( f.match().field( "title" )
									.matching( "robot" ) )
					)
					.fetchHits( 20 );
			// end::matchAll-except[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK2_ID, BOOK4_ID );
		} );
	}

	@Test
	public void id() {
		// DO NOT USE THE BOOKX_ID CONSTANTS INSIDE TAGS BELOW:
		// we don't want the constants to appear in the documentation.

		withinSearchSession( searchSession -> {
			// tag::id[]
			List<Book> hits = searchSession.search( Book.class )
					.predicate( f -> f.id().matching( 1 ) )
					.fetchHits( 20 );
			// end::id[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::id-matchingAny[]
			List<Integer> ids = new ArrayList<>();
			ids.add( 1 );
			ids.add( 2 );
			List<Book> hits = searchSession.search( Book.class )
					.predicate( f -> f.id().matchingAny( ids ) )
					.fetchHits( 20 );
			// end::id-matchingAny[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK2_ID );
		} );
	}

	@Test
	public void bool() {
		withinSearchSession( searchSession -> {
			// tag::bool-or[]
			List<Book> hits = searchSession.search( Book.class )
					.predicate( f -> f.bool()
							.should( f.match().field( "title" )
									.matching( "robot" ) ) // <1>
							.should( f.match().field( "description" )
									.matching( "investigation" ) ) // <2>
					)
					.fetchHits( 20 ); // <3>
			// end::bool-or[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK2_ID, BOOK3_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::bool-and[]
			List<Book> hits = searchSession.search( Book.class )
					.predicate( f -> f.bool()
							.must( f.match().field( "title" )
									.matching( "robot" ) ) // <1>
							.must( f.match().field( "description" )
									.matching( "crime" ) ) // <2>
					)
					.fetchHits( 20 ); // <3>
			// end::bool-and[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK3_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::bool-mustNot[]
			List<Book> hits = searchSession.search( Book.class )
					.predicate( f -> f.bool()
							.must( f.match().field( "title" )
									.matching( "robot" ) ) // <1>
							.mustNot( f.match().field( "description" )
									.matching( "investigation" ) ) // <2>
					)
					.fetchHits( 20 ); // <3>
			// end::bool-mustNot[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK3_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::bool-filter[]
			List<Book> hits = searchSession.search( Book.class )
					.predicate( f -> f.bool() // <1>
							.should( f.bool() // <2>
									.filter( f.match().field( "genre" )
											.matching( Genre.SCIENCE_FICTION ) ) // <3>
									.must( f.match().fields( "description" )
											.matching( "crime" ) ) // <4>
							)
							.should( f.bool() // <5>
									.filter( f.match().field( "genre" )
											.matching( Genre.CRIME_FICTION ) ) // <6>
									.must( f.match().fields( "description" )
											.matching( "robot" ) ) // <7>
							)
					)
					.fetchHits( 20 ); // <8>
			// end::bool-filter[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK3_ID, BOOK4_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::bool-mustAndShould[]
			List<Book> hits = searchSession.search( Book.class )
					.predicate( f -> f.bool()
							.must( f.match().field( "title" )
									.matching( "robot" ) ) // <1>
							.should( f.match().field( "description" )
									.matching( "crime" ) ) // <2>
							.should( f.match().field( "description" )
									.matching( "investigation" ) ) // <3>
					)
					.fetchHits( 20 ); // <4>
			// end::bool-mustAndShould[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK3_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::bool-minimumShouldMatchNumber[]
			List<Book> hits = searchSession.search( Book.class )
					.predicate( f -> f.bool()
							.minimumShouldMatchNumber( 2 ) // <1>
							.should( f.match().field( "description" )
									.matching( "robot" ) ) // <2>
							.should( f.match().field( "description" )
									.matching( "investigation" ) ) // <3>
							.should( f.match().field( "description" )
									.matching( "disappearance" ) ) // <4>
					)
					.fetchHits( 20 ); // <5>
			// end::bool-minimumShouldMatchNumber[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK2_ID, BOOK4_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::bool-dynamicParameters[]
			MySearchParameters searchParameters = getSearchParameters(); // <1>
			List<Book> hits = searchSession.search( Book.class )
					.predicate( f -> f.bool( b -> { // <2>
						b.must( f.matchAll() ); // <3>
						if ( searchParameters.getGenreFilter() != null ) { // <4>
							b.must( f.match().field( "genre" )
									.matching( searchParameters.getGenreFilter() ) );
						}
						if ( searchParameters.getFullTextFilter() != null ) {
							b.must( f.match().fields( "title", "description" )
									.matching( searchParameters.getFullTextFilter() ) );
						}
						if ( searchParameters.getPageCountMaxFilter() != null ) {
							b.must( f.range().field( "pageCount" )
									.below( searchParameters.getPageCountMaxFilter() ) );
						}
					} ) )
					.fetchHits( 20 ); // <5>
			// end::bool-dynamicParameters[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK2_ID );
		} );
	}

	@Test
	public void match() {
		withinSearchSession( searchSession -> {
			// tag::match[]
			List<Book> hits = searchSession.search( Book.class )
					.predicate( f -> f.match().field( "title" )
							.matching( "robot" ) )
					.fetchHits( 20 );
			// end::match[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK3_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::match-multipleTerms[]
			List<Book> hits = searchSession.search( Book.class )
					.predicate( f -> f.match().field( "title" )
							.matching( "robot dawn" ) ) // <1>
					.fetchHits( 20 ); // <2>
			// end::match-multipleTerms[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK3_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::match-orField[]
			List<Book> hits = searchSession.search( Book.class )
					.predicate( f -> f.match()
							.field( "title" ).field( "description" )
							.matching( "robot" ) )
					.fetchHits( 20 );
			// end::match-orField[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK2_ID, BOOK3_ID, BOOK4_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::match-fields[]
			List<Book> hits = searchSession.search( Book.class )
					.predicate( f -> f.match()
							.fields( "title", "description" )
							.matching( "robot" ) )
					.fetchHits( 20 );
			// end::match-fields[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK2_ID, BOOK3_ID, BOOK4_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::match-fuzzy[]
			List<Book> hits = searchSession.search( Book.class )
					.predicate( f -> f.match()
							.field( "title" )
							.matching( "robto" )
							.fuzzy() )
					.fetchHits( 20 );
			// end::match-fuzzy[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK3_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::match-analyzer[]
			List<Book> hits = searchSession.search( Book.class )
					.predicate( f -> f.match()
							.field( "title_autocomplete" )
							.matching( "robo" )
							.analyzer( "autocomplete_query" ) )
					.fetchHits( 20 );
			// end::match-analyzer[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK3_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::match-skipAnalysis[]
			List<Book> hits = searchSession.search( Book.class )
					.predicate( f -> f.match()
							.field( "title" )
							.matching( "robot" )
							.skipAnalysis() )
					.fetchHits( 20 );
			// end::match-skipAnalysis[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK3_ID );
		} );
	}

	@Test
	public void range() {
		withinSearchSession( searchSession -> {
			// tag::range-between[]
			List<Book> hits = searchSession.search( Book.class )
					.predicate( f -> f.range().field( "pageCount" )
							.from( 210 ).to( 250 ) )
					.fetchHits( 20 );
			// end::range-between[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK4_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::range-above[]
			List<Book> hits = searchSession.search( Book.class )
					.predicate( f -> f.range().field( "pageCount" )
							.above( 400 ) )
					.fetchHits( 20 );
			// end::range-above[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK3_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::range-below[]
			List<Book> hits = searchSession.search( Book.class )
					.predicate( f -> f.range().field( "pageCount" )
							.below( 400 ) )
					.fetchHits( 20 );
			// end::range-below[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK2_ID, BOOK4_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::range-excludeLimit[]
			List<Book> hits = searchSession.search( Book.class )
					.predicate( f -> f.range().field( "pageCount" )
							.from( 200 ).excludeLimit()
							.to( 250 ).excludeLimit() )
					.fetchHits( 20 );
			// end::range-excludeLimit[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK2_ID, BOOK4_ID );
		} );
	}

	@Test
	public void phrase() {
		withinSearchSession( searchSession -> {
			// tag::phrase[]
			List<Book> hits = searchSession.search( Book.class )
					.predicate( f -> f.phrase().field( "title" )
							.matching( "robots of dawn" ) )
					.fetchHits( 20 );
			// end::phrase[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK3_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::phrase-slop[]
			List<Book> hits = searchSession.search( Book.class )
					.predicate( f -> f.phrase().field( "title" )
							.matching( "dawn robot" )
							.slop( 3 ) )
					.fetchHits( 20 );
			// end::phrase-slop[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK3_ID );
		} );
	}

	@Test
	public void simpleQueryString() {
		withinSearchSession( searchSession -> {
			// tag::simpleQueryString-boolean[]
			List<Book> hits = searchSession.search( Book.class )
					.predicate( f -> f.simpleQueryString().field( "description" )
							.matching( "robots + (crime | investigation | disappearance)" ) )
					.fetchHits( 20 );
			// end::simpleQueryString-boolean[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK2_ID, BOOK4_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::simpleQueryString-not[]
			List<Book> hits = searchSession.search( Book.class )
					.predicate( f -> f.simpleQueryString().field( "description" )
							.matching( "robots + -investigation" ) )
					.fetchHits( 20 );
			// end::simpleQueryString-not[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK4_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::simpleQueryString-defaultOperator-and[]
			List<Book> hits = searchSession.search( Book.class )
					.predicate( f -> f.simpleQueryString().field( "description" )
							.matching( "robots investigation" )
							.defaultOperator( BooleanOperator.AND ) )
					.fetchHits( 20 );
			// end::simpleQueryString-defaultOperator-and[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK2_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::simpleQueryString-prefix[]
			List<Book> hits = searchSession.search( Book.class )
					.predicate( f -> f.simpleQueryString().field( "description" )
							.matching( "rob*" ) )
					.fetchHits( 20 );
			// end::simpleQueryString-prefix[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK2_ID, BOOK3_ID, BOOK4_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::simpleQueryString-fuzzy[]
			List<Book> hits = searchSession.search( Book.class )
					.predicate( f -> f.simpleQueryString().field( "description" )
							.matching( "robto~2" ) )
					.fetchHits( 20 );
			// end::simpleQueryString-fuzzy[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK2_ID, BOOK4_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::simpleQueryString-phrase[]
			List<Book> hits = searchSession.search( Book.class )
					.predicate( f -> f.simpleQueryString().field( "title" )
							.matching( "\"robots of dawn\"" ) )
					.fetchHits( 20 );
			// end::simpleQueryString-phrase[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK3_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::simpleQueryString-phrase-slop[]
			List<Book> hits = searchSession.search( Book.class )
					.predicate( f -> f.simpleQueryString().field( "title" )
							.matching( "\"dawn robot\"~3" ) )
					.fetchHits( 20 );
			// end::simpleQueryString-phrase-slop[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK3_ID );
		} );
	}

	@Test
	public void exists() {
		withinSearchSession( searchSession -> {
			// tag::exists[]
			List<Book> hits = searchSession.search( Book.class )
					.predicate( f -> f.exists().field( "comment" ) )
					.fetchHits( 20 );
			// end::exists[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK2_ID );
		} );
	}

	@Test
	public void wildcard() {
		withinSearchSession( searchSession -> {
			// tag::wildcard[]
			List<Book> hits = searchSession.search( Book.class )
					.predicate( f -> f.wildcard().field( "description" )
							.matching( "rob*t" ) )
					.fetchHits( 20 );
			// end::wildcard[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK2_ID, BOOK4_ID );
		} );
	}

	@Test
	public void nested() {
		withinSearchSession( searchSession -> {
			// tag::nested[]
			List<Book> hits = searchSession.search( Book.class )
					.predicate( f -> f.nested().objectField( "authors" ) // <1>
							.nest( f.bool()
									.must( f.match().field( "authors.firstName" )
											.matching( "isaac" ) ) // <2>
									.must( f.match().field( "authors.lastName" )
											.matching( "asimov" ) ) // <3>
							) )
					.fetchHits( 20 ); // <4>
			// end::nested[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK2_ID, BOOK3_ID );
		} );
	}

	@Test
	public void within() {
		withinSearchSession( searchSession -> {
			// tag::within-circle[]
			GeoPoint center = GeoPoint.of( 53.970000, 32.150000 );
			List<Author> hits = searchSession.search( Author.class )
					.predicate( f -> f.spatial().within().field( "placeOfBirth" )
							.circle( center, 50, DistanceUnit.KILOMETERS ) )
					.fetchHits( 20 );
			// end::within-circle[]
			assertThat( hits )
					.extracting( Author::getId )
					.containsExactlyInAnyOrder( ASIMOV_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::within-box[]
			GeoBoundingBox box = GeoBoundingBox.of(
					53.99, 32.13,
					53.95, 32.17
			);
			List<Author> hits = searchSession.search( Author.class )
					.predicate( f -> f.spatial().within().field( "placeOfBirth" )
							.boundingBox( box ) )
					.fetchHits( 20 );
			// end::within-box[]
			assertThat( hits )
					.extracting( Author::getId )
					.containsExactlyInAnyOrder( ASIMOV_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::within-polygon[]
			GeoPolygon polygon = GeoPolygon.of(
					GeoPoint.of( 53.976177, 32.138627 ),
					GeoPoint.of( 53.986177, 32.148627 ),
					GeoPoint.of( 53.979177, 32.168627 ),
					GeoPoint.of( 53.876177, 32.159627 ),
					GeoPoint.of( 53.956177, 32.155627 ),
					GeoPoint.of( 53.976177, 32.138627 )
			);
			List<Author> hits = searchSession.search( Author.class )
					.predicate( f -> f.spatial().within().field( "placeOfBirth" )
							.polygon( polygon ) )
					.fetchHits( 20 );
			// end::within-polygon[]
			assertThat( hits )
					.extracting( Author::getId )
					.containsExactlyInAnyOrder( ASIMOV_ID );
		} );
	}

	@Test
	public void lucene() {
		Assume.assumeTrue( backendConfiguration instanceof LuceneBackendConfiguration );

		withinSearchSession( searchSession -> {
			// tag::lucene-fromLuceneQuery[]
			List<Book> hits = searchSession.search( Book.class )
					.extension( LuceneExtension.get() )
					.predicate( f -> f.fromLuceneQuery(
							new RegexpQuery( new Term( "description", "neighbor|neighbour" ) )
					) )
					.fetchHits( 20 );
			// end::lucene-fromLuceneQuery[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK4_ID );
		} );
	}

	@Test
	public void elasticsearch() {
		Assume.assumeTrue( backendConfiguration instanceof ElasticsearchBackendConfiguration );

		withinSearchSession( searchSession -> {
			// tag::elasticsearch-fromJson[]
			List<Book> hits = searchSession.search( Book.class )
					.extension( ElasticsearchExtension.get() )
					.predicate( f -> f.fromJson( "{"
									+ "\"regexp\": {"
											+ "\"description\": \"neighbor|neighbour\""
									+ "}"
							+ "}" ) )
					.fetchHits( 20 );
			// end::elasticsearch-fromJson[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK4_ID );
		} );
	}

	private MySearchParameters getSearchParameters() {
		return new MySearchParameters() {
			@Override
			public Genre getGenreFilter() {
				return Genre.SCIENCE_FICTION;
			}

			@Override
			public String getFullTextFilter() {
				return "robot";
			}

			@Override
			public Integer getPageCountMaxFilter() {
				return 400;
			}
		};
	}

	private void withinSearchSession(Consumer<SearchSession> action) {
		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			action.accept( searchSession );
		} );
	}

	private void initData() {
		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			Author isaacAsimov = new Author();
			isaacAsimov.setId( ASIMOV_ID );
			isaacAsimov.setFirstName( "Isaac" );
			isaacAsimov.setLastName( "Asimov" );
			isaacAsimov.setPlaceOfBirth( EmbeddableGeoPoint.of( 53.976177, 32.158627 ) );

			Author aLeeMartinez = new Author();
			aLeeMartinez.setId( MARTINEZ_ID );
			aLeeMartinez.setFirstName( "A. Lee" );
			aLeeMartinez.setLastName( "Martinez" );
			aLeeMartinez.setPlaceOfBirth( EmbeddableGeoPoint.of( 31.814315, -106.475524 ) );

			Book book1 = new Book();
			book1.setId( BOOK1_ID );
			book1.setTitle( "I, Robot" );
			book1.setDescription( "A robot becomes self-aware." );
			book1.setPageCount( 250 );
			book1.setGenre( Genre.SCIENCE_FICTION );
			book1.getAuthors().add( isaacAsimov );
			isaacAsimov.getBooks().add( book1 );

			Book book2 = new Book();
			book2.setId( BOOK2_ID );
			book2.setTitle( "The Caves of Steel" );
			book2.setDescription( "A robot helps investigate a murder on an extrasolar colony." );
			book2.setPageCount( 206 );
			book2.setGenre( Genre.SCIENCE_FICTION );
			book2.setComment( "Really liked this one!" );
			book2.getAuthors().add( isaacAsimov );
			isaacAsimov.getBooks().add( book2 );

			Book book3 = new Book();
			book3.setId( BOOK3_ID );
			book3.setTitle( "The Robots of Dawn" );
			book3.setDescription( "A crime story about the first \"roboticide\"." );
			book3.setPageCount( 435 );
			book3.setGenre( Genre.SCIENCE_FICTION );
			book3.getAuthors().add( isaacAsimov );
			isaacAsimov.getBooks().add( book3 );

			Book book4 = new Book();
			book4.setId( BOOK4_ID );
			book4.setTitle( "The Automatic Detective" );
			book4.setDescription( "A robot cab driver turns PI after the disappearance of a neighboring family." );
			book4.setPageCount( 222 );
			book4.setGenre( Genre.CRIME_FICTION );
			book4.getAuthors().add( aLeeMartinez );
			aLeeMartinez.getBooks().add( book3 );

			entityManager.persist( isaacAsimov );
			entityManager.persist( aLeeMartinez );

			entityManager.persist( book1 );
			entityManager.persist( book2 );
			entityManager.persist( book3 );
			entityManager.persist( book4 );
		} );
	}

	private interface MySearchParameters {
		Genre getGenreFilter();
		String getFullTextFilter();
		Integer getPageCountMaxFilter();
	}
}
