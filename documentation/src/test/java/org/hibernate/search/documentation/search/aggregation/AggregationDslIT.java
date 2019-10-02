/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.search.aggregation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.function.Consumer;
import javax.persistence.EntityManagerFactory;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.automaticindexing.AutomaticIndexingSynchronizationStrategyName;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.scope.SearchScope;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.util.common.data.Range;
import org.hibernate.search.util.common.data.RangeBoundInclusion;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class AggregationDslIT {

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

	private EntityManagerFactory entityManagerFactory;

	public AggregationDslIT(BackendConfiguration backendConfiguration) {
		this.setupHelper = OrmSetupHelper.withSingleBackend( backendConfiguration );
	}

	@Before
	public void setup() {
		entityManagerFactory = setupHelper.start()
				.withProperty(
						HibernateOrmMapperSettings.AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY,
						AutomaticIndexingSynchronizationStrategyName.SEARCHABLE
				)
				.setup( Book.class );
		initData();
	}

	@Test
	public void entryPoint() {
		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			// tag::entryPoint-lambdas[]
			SearchSession searchSession = Search.session( entityManager );

			AggregationKey<Map<Genre, Long>> countsByGenreKey = AggregationKey.of( "countsByGenre" ); // <1>

			SearchResult<Book> result = searchSession.search( Book.class ) // <2>
					.predicate( f -> f.match().field( "title" ) // <3>
							.matching( "robot" ) )
					.aggregation( countsByGenreKey, f -> f.terms() // <4>
							.field( "genre", Genre.class ) )
					.fetch( 20 ); // <5>

			Map<Genre, Long> countsByGenre = result.getAggregation( countsByGenreKey ); // <6>
			// end::entryPoint-lambdas[]
			assertThat( countsByGenre )
					.containsExactly(
							entry( Genre.SCIENCE_FICTION, 2L )
					);
		} );

		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			// tag::entryPoint-objects[]
			SearchSession searchSession = Search.session( entityManager );

			SearchScope<Book> scope = searchSession.scope( Book.class );

			AggregationKey<Map<Genre, Long>> countsByGenreKey = AggregationKey.of( "countsByGenre" );

			SearchResult<Book> result = searchSession.search( scope )
					.predicate( scope.predicate().match().field( "title" )
							.matching( "robot" )
							.toPredicate() )
					.aggregation( countsByGenreKey, scope.aggregation().terms()
							.field( "genre", Genre.class )
							.toAggregation() )
					.fetch( 20 );

			Map<Genre, Long> countsByGenre = result.getAggregation( countsByGenreKey );
			// end::entryPoint-objects[]
			assertThat( countsByGenre )
					.containsExactly(
							entry( Genre.SCIENCE_FICTION, 2L )
					);
		} );
	}

	@Test
	public void terms() {
		withinSearchSession( searchSession -> {
			// tag::terms[]
			AggregationKey<Map<Genre, Long>> countsByGenreKey = AggregationKey.of( "countsByGenre" );
			SearchResult<Book> result = searchSession.search( Book.class )
					.predicate( f -> f.matchAll() )
					.aggregation( countsByGenreKey, f -> f.terms()
							.field( "genre", Genre.class ) ) // <1>
					.fetch( 20 );
			Map<Genre, Long> countsByGenre = result.getAggregation( countsByGenreKey );
			// end::terms[]
			assertThat( countsByGenre )
					.containsExactly(
							entry( Genre.SCIENCE_FICTION, 3L ),
							entry( Genre.CRIME_FICTION, 1L )
					);
		} );

		withinSearchSession( searchSession -> {
			// tag::terms-noConverter[]
			AggregationKey<Map<String, Long>> countsByGenreKey = AggregationKey.of( "countsByGenre" );
			SearchResult<Book> result = searchSession.search( Book.class )
					.predicate( f -> f.matchAll() )
					.aggregation( countsByGenreKey, f -> f.terms()
							.field( "genre", String.class, ValueConvert.NO ) )
					.fetch( 20 );
			Map<String, Long> countsByGenre = result.getAggregation( countsByGenreKey );
			// end::terms-noConverter[]
			assertThat( countsByGenre )
					.containsExactly(
							entry( Genre.SCIENCE_FICTION.name(), 3L ),
							entry( Genre.CRIME_FICTION.name(), 1L )
					);
		} );

		withinSearchSession( searchSession -> {
			// tag::terms-max-term-count[]
			AggregationKey<Map<Genre, Long>> countsByGenreKey = AggregationKey.of( "countsByGenre" );
			SearchResult<Book> result = searchSession.search( Book.class )
					.predicate( f -> f.matchAll() )
					.aggregation( countsByGenreKey, f -> f.terms()
							.field( "genre", Genre.class )
							.maxTermCount( 1 ) )
					.fetch( 20 );
			Map<Genre, Long> countsByGenre = result.getAggregation( countsByGenreKey );
			// end::terms-max-term-count[]
			assertThat( countsByGenre )
					.containsExactly(
							entry( Genre.SCIENCE_FICTION, 3L )
					);
		} );

		withinSearchSession( searchSession -> {
			// tag::terms-min-doc-count-zero[]
			AggregationKey<Map<Genre, Long>> countsByGenreKey = AggregationKey.of( "countsByGenre" );
			SearchResult<Book> result = searchSession.search( Book.class )
					.predicate( f -> f.matchAll() )
					.aggregation( countsByGenreKey, f -> f.terms()
							.field( "genre", Genre.class )
							.minDocumentCount( 0 ) )
					.fetch( 20 );
			Map<Genre, Long> countsByGenre = result.getAggregation( countsByGenreKey );
			// end::terms-min-doc-count-zero[]
			assertThat( countsByGenre )
					.containsExactly(
							entry( Genre.SCIENCE_FICTION, 3L ),
							entry( Genre.CRIME_FICTION, 1L )
					);
		} );

		withinSearchSession( searchSession -> {
			// tag::terms-min-doc-count-high[]
			AggregationKey<Map<Genre, Long>> countsByGenreKey = AggregationKey.of( "countsByGenre" );
			SearchResult<Book> result = searchSession.search( Book.class )
					.predicate( f -> f.matchAll() )
					.aggregation( countsByGenreKey, f -> f.terms()
							.field( "genre", Genre.class )
							.minDocumentCount( 2 ) )
					.fetch( 20 );
			Map<Genre, Long> countsByGenre = result.getAggregation( countsByGenreKey );
			// end::terms-min-doc-count-high[]
			assertThat( countsByGenre )
					.containsExactly(
							entry( Genre.SCIENCE_FICTION, 3L )
					);
		} );

		withinSearchSession( searchSession -> {
			// tag::terms-order-term-ascending[]
			AggregationKey<Map<Genre, Long>> countsByGenreKey = AggregationKey.of( "countsByGenre" );
			SearchResult<Book> result = searchSession.search( Book.class )
					.predicate( f -> f.matchAll() )
					.aggregation( countsByGenreKey, f -> f.terms()
							.field( "genre", Genre.class )
							.orderByTermAscending() )
					.fetch( 20 );
			Map<Genre, Long> countsByGenre = result.getAggregation( countsByGenreKey );
			// end::terms-order-term-ascending[]
			assertThat( countsByGenre )
					.containsExactly(
							entry( Genre.CRIME_FICTION, 1L ),
							entry( Genre.SCIENCE_FICTION, 3L )
					);
		} );

		withinSearchSession( searchSession -> {
			// tag::terms-order-term-descending[]
			AggregationKey<Map<Genre, Long>> countsByGenreKey = AggregationKey.of( "countsByGenre" );
			SearchResult<Book> result = searchSession.search( Book.class )
					.predicate( f -> f.matchAll() )
					.aggregation( countsByGenreKey, f -> f.terms()
							.field( "genre", Genre.class )
							.orderByTermDescending() )
					.fetch( 20 );
			Map<Genre, Long> countsByGenre = result.getAggregation( countsByGenreKey );
			// end::terms-order-term-descending[]
			assertThat( countsByGenre )
					.containsExactly(
							entry( Genre.SCIENCE_FICTION, 3L ),
							entry( Genre.CRIME_FICTION, 1L )
					);
		} );

		withinSearchSession( searchSession -> {
			// tag::terms-order-count-ascending[]
			AggregationKey<Map<Genre, Long>> countsByGenreKey = AggregationKey.of( "countsByGenre" );
			SearchResult<Book> result = searchSession.search( Book.class )
					.predicate( f -> f.matchAll() )
					.aggregation( countsByGenreKey, f -> f.terms()
							.field( "genre", Genre.class )
							.orderByCountAscending() )
					.fetch( 20 );
			Map<Genre, Long> countsByGenre = result.getAggregation( countsByGenreKey );
			// end::terms-order-count-ascending[]
			assertThat( countsByGenre )
					.containsExactly(
							entry( Genre.CRIME_FICTION, 1L ),
							entry( Genre.SCIENCE_FICTION, 3L )
					);
		} );
	}

	@Test
	public void range() {
		withinSearchSession( searchSession -> {
			// tag::range[]
			AggregationKey<Map<Range<Double>, Long>> countsByPriceKey = AggregationKey.of( "countsByPrice" );
			SearchResult<Book> result = searchSession.search( Book.class )
					.predicate( f -> f.matchAll() )
					.aggregation( countsByPriceKey, f -> f.range()
							.field( "price", Double.class ) // <1>
							.range( 0.0, 10.0 ) // <2>
							.range( 10.0, 20.0 )
							.range( 20.0, null ) // <3>
					)
					.fetch( 20 );
			Map<Range<Double>, Long> countsByPrice = result.getAggregation( countsByPriceKey );
			// end::range[]
			assertThat( countsByPrice )
					.containsExactly(
							entry( Range.canonical( 0.0, 10.0 ), 1L ),
							entry( Range.canonical( 10.0, 20.0 ), 2L ),
							entry( Range.canonical( 20.0, null ), 1L )
					);
		} );

		withinSearchSession( searchSession -> {
			// tag::range-objects[]
			AggregationKey<Map<Range<Double>, Long>> countsByPriceKey = AggregationKey.of( "countsByPrice" );
			SearchResult<Book> result = searchSession.search( Book.class )
					.predicate( f -> f.matchAll() )
					.aggregation( countsByPriceKey, f -> f.range()
							.field( "price", Double.class )
							.range( Range.canonical( 0.0, 10.0 ) ) // <1>
							.range( Range.between( 10.0, RangeBoundInclusion.INCLUDED,
									20.0, RangeBoundInclusion.EXCLUDED ) ) // <2>
							.range( Range.atLeast( 20.0 ) ) // <3>
					)
					.fetch( 20 );
			Map<Range<Double>, Long> countsByPrice = result.getAggregation( countsByPriceKey );
			// end::range-objects[]
			assertThat( countsByPrice )
					.containsExactly(
							entry( Range.canonical( 0.0, 10.0 ), 1L ),
							entry( Range.canonical( 10.0, 20.0 ), 2L ),
							entry( Range.canonical( 20.0, null ), 1L )
					);
		} );

		withinSearchSession( searchSession -> {
			// tag::range-noConverter[]
			AggregationKey<Map<Range<Instant>, Long>> countsByPriceKey = AggregationKey.of( "countsByPrice" );
			SearchResult<Book> result = searchSession.search( Book.class )
					.predicate( f -> f.matchAll() )
					.aggregation( countsByPriceKey, f -> f.range()
							// Assuming "releaseDate" is of type "java.util.Date" or "java.sql.Date"
							.field( "releaseDate", Instant.class, ValueConvert.NO )
							.range( null,
									LocalDate.of( 1970, 1, 1 )
											.atStartOfDay().toInstant( ZoneOffset.UTC ) )
							.range( LocalDate.of( 1970, 1, 1 )
											.atStartOfDay().toInstant( ZoneOffset.UTC ),
									LocalDate.of( 2000, 1, 1 )
											.atStartOfDay().toInstant( ZoneOffset.UTC ) )
							.range( LocalDate.of( 2000, 1, 1 )
											.atStartOfDay().toInstant( ZoneOffset.UTC ),
									null )
					)
					.fetch( 20 );
			Map<Range<Instant>, Long> countsByPrice = result.getAggregation( countsByPriceKey );
			// end::range-noConverter[]
			assertThat( countsByPrice )
					.containsExactly(
							entry(
									Range.canonical(
											null,
											LocalDate.of( 1970, 1, 1 )
													.atStartOfDay().toInstant( ZoneOffset.UTC )
									),
									2L
							),
							entry(
									Range.canonical(
											LocalDate.of( 1970, 1, 1 )
													.atStartOfDay().toInstant( ZoneOffset.UTC ),
											LocalDate.of( 2000, 1, 1 )
													.atStartOfDay().toInstant( ZoneOffset.UTC )
									),
									1L
							),
							entry(
									Range.canonical(
											LocalDate.of( 2000, 1, 1 )
													.atStartOfDay().toInstant( ZoneOffset.UTC ),
											null
									),
									1L
							)
					);
		} );
	}

	private void withinSearchSession(Consumer<SearchSession> action) {
		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			action.accept( searchSession );
		} );
	}

	private void initData() {
		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			Book book1 = new Book();
			book1.setId( BOOK1_ID );
			book1.setTitle( "I, Robot" );
			book1.setPrice( 24.99 );
			book1.setGenre( Genre.SCIENCE_FICTION );
			book1.setReleaseDate( Date.valueOf( "1950-12-02" ) );

			Book book2 = new Book();
			book2.setId( BOOK2_ID );
			book2.setTitle( "The Caves of Steel" );
			book2.setPrice( 19.99 );
			book2.setGenre( Genre.SCIENCE_FICTION );
			book2.setReleaseDate( Date.valueOf( "1953-10-01" ) );

			Book book3 = new Book();
			book3.setId( BOOK3_ID );
			book3.setTitle( "The Robots of Dawn" );
			book3.setPrice( 15.99 );
			book3.setGenre( Genre.SCIENCE_FICTION );
			book3.setReleaseDate( Date.valueOf( "1983-01-01" ) );

			Book book4 = new Book();
			book4.setId( BOOK4_ID );
			book4.setTitle( "The Automatic Detective" );
			book4.setPrice( 7.99 );
			book4.setGenre( Genre.CRIME_FICTION );
			book4.setReleaseDate( Date.valueOf( "2008-02-05" ) );

			entityManager.persist( book1 );
			entityManager.persist( book2 );
			entityManager.persist( book3 );
			entityManager.persist( book4 );
		} );
	}
}
