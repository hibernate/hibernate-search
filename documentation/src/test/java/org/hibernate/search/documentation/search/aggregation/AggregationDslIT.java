/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.search.aggregation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.data.Offset.offset;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.scope.SearchScope;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.util.common.data.Range;
import org.hibernate.search.util.common.data.RangeBoundInclusion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class AggregationDslIT {

	private static final int BOOK1_ID = 1;
	private static final int BOOK2_ID = 2;
	private static final int BOOK3_ID = 3;
	private static final int BOOK4_ID = 4;

	@RegisterExtension
	public DocumentationSetupHelper setupHelper = DocumentationSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	private EntityManagerFactory entityManagerFactory;

	@BeforeEach
	void setup() {
		entityManagerFactory = setupHelper.start().setup( Book.class, BookEdition.class );
		initData();
	}

	@Test
	void entryPoint() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			// tag::entryPoint-lambdas[]
			SearchSession searchSession = /* ... */ // <1>
					// end::entryPoint-lambdas[]
					Search.session( entityManager );
			// tag::entryPoint-lambdas[]

			AggregationKey<Map<Genre, Long>> countsByGenreKey = AggregationKey.of( "countsByGenre" ); // <2>

			SearchResult<Book> result = searchSession.search( Book.class ) // <3>
					.where( f -> f.match().field( "title" ) // <4>
							.matching( "robot" ) )
					.aggregation( countsByGenreKey, f -> f.terms() // <5>
							.field( "genre", Genre.class ) )
					.fetch( 20 ); // <6>

			Map<Genre, Long> countsByGenre = result.aggregation( countsByGenreKey ); // <7>
			// end::entryPoint-lambdas[]
			assertThat( countsByGenre )
					.containsExactly(
							entry( Genre.SCIENCE_FICTION, 2L )
					);
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			// tag::entryPoint-objects[]
			SearchSession searchSession = /* ... */
					// end::entryPoint-objects[]
					Search.session( entityManager );
			// tag::entryPoint-objects[]

			SearchScope<Book> scope = searchSession.scope( Book.class );

			AggregationKey<Map<Genre, Long>> countsByGenreKey = AggregationKey.of( "countsByGenre" );

			SearchResult<Book> result = searchSession.search( scope )
					.where( scope.predicate().match().field( "title" )
							.matching( "robot" )
							.toPredicate() )
					.aggregation( countsByGenreKey, scope.aggregation().terms()
							.field( "genre", Genre.class )
							.toAggregation() )
					.fetch( 20 );

			Map<Genre, Long> countsByGenre = result.aggregation( countsByGenreKey );
			// end::entryPoint-objects[]
			assertThat( countsByGenre )
					.containsExactly(
							entry( Genre.SCIENCE_FICTION, 2L )
					);
		} );
	}

	@Test
	void terms() {
		withinSearchSession( searchSession -> {
			// tag::terms[]
			AggregationKey<Map<Genre, Long>> countsByGenreKey = AggregationKey.of( "countsByGenre" );
			SearchResult<Book> result = searchSession.search( Book.class )
					.where( f -> f.matchAll() )
					.aggregation( countsByGenreKey, f -> f.terms()
							.field( "genre", Genre.class ) ) // <1>
					.fetch( 20 );
			Map<Genre, Long> countsByGenre = result.aggregation( countsByGenreKey ); // <2>
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
					.where( f -> f.matchAll() )
					.aggregation( countsByGenreKey, f -> f.terms()
							.field( "genre", String.class, ValueModel.INDEX ) )
					.fetch( 20 );
			Map<String, Long> countsByGenre = result.aggregation( countsByGenreKey );
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
					.where( f -> f.matchAll() )
					.aggregation( countsByGenreKey, f -> f.terms()
							.field( "genre", Genre.class )
							.maxTermCount( 1 ) )
					.fetch( 20 );
			Map<Genre, Long> countsByGenre = result.aggregation( countsByGenreKey );
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
					.where( f -> f.matchAll() )
					.aggregation( countsByGenreKey, f -> f.terms()
							.field( "genre", Genre.class )
							.minDocumentCount( 0 ) )
					.fetch( 20 );
			Map<Genre, Long> countsByGenre = result.aggregation( countsByGenreKey );
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
					.where( f -> f.matchAll() )
					.aggregation( countsByGenreKey, f -> f.terms()
							.field( "genre", Genre.class )
							.minDocumentCount( 2 ) )
					.fetch( 20 );
			Map<Genre, Long> countsByGenre = result.aggregation( countsByGenreKey );
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
					.where( f -> f.matchAll() )
					.aggregation( countsByGenreKey, f -> f.terms()
							.field( "genre", Genre.class )
							.orderByTermAscending() )
					.fetch( 20 );
			Map<Genre, Long> countsByGenre = result.aggregation( countsByGenreKey );
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
					.where( f -> f.matchAll() )
					.aggregation( countsByGenreKey, f -> f.terms()
							.field( "genre", Genre.class )
							.orderByTermDescending() )
					.fetch( 20 );
			Map<Genre, Long> countsByGenre = result.aggregation( countsByGenreKey );
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
					.where( f -> f.matchAll() )
					.aggregation( countsByGenreKey, f -> f.terms()
							.field( "genre", Genre.class )
							.orderByCountAscending() )
					.fetch( 20 );
			Map<Genre, Long> countsByGenre = result.aggregation( countsByGenreKey );
			// end::terms-order-count-ascending[]
			assertThat( countsByGenre )
					.containsExactly(
							entry( Genre.CRIME_FICTION, 1L ),
							entry( Genre.SCIENCE_FICTION, 3L )
					);
		} );

		withinSearchSession( searchSession -> {
			AggregationKey<Map<Double, Long>> countsByPriceKey = AggregationKey.of( "countsByPrice" );
			SearchResult<Book> result = searchSession.search( Book.class )
					.where( f -> f.matchAll() )
					.aggregation( countsByPriceKey, f -> f.terms()
							.field( "price", Double.class )
							.orderByCountAscending() )
					.fetch( 20 );
			Map<Double, Long> countsByPrice = result.aggregation( countsByPriceKey );
			assertThat( countsByPrice )
					.containsExactly(
							entry( 7.99, 1L ),
							entry( 15.99, 1L ),
							entry( 19.99, 1L ),
							entry( 24.99, 1L )
					);
		} );
	}

	@Test
	void terms_value() {
		withinSearchSession( searchSession -> {
			// tag::terms-sum[]
			AggregationKey<Map<Genre, Double>> sumByCategoryKey = AggregationKey.of( "sumByCategory" );
			SearchResult<Book> result = searchSession.search( Book.class )
					.where( f -> f.matchAll() )
					.aggregation(
							sumByCategoryKey, f -> f.terms()
									.field( "genre", Genre.class ) // <1>
									.value( f.sum().field( "price", Double.class ) ) // <2>
					)
					.fetch( 20 );
			Map<Genre, Double> sumByPrice = result.aggregation( sumByCategoryKey );
			// end::terms-sum[]
			assertThat( sumByPrice )
					.hasSize( 2 )
					.containsOnlyKeys(
							Genre.SCIENCE_FICTION,
							Genre.CRIME_FICTION
					)
					.satisfies(
							map -> assertThat( map.get( Genre.SCIENCE_FICTION ) ).isCloseTo( 60.97, offset( 0.0001 ) ),
							map -> assertThat( map.get( Genre.CRIME_FICTION ) ).isCloseTo( 7.99, offset( 0.0001 ) )
					);
		} );

		withinSearchSession( searchSession -> {
			// tag::terms-count[]
			AggregationKey<Map<Double, Long>> countsByPriceKey = AggregationKey.of( "countsByPrice" );
			SearchResult<Book> result = searchSession.search( Book.class )
					.where( f -> f.matchAll() )
					.aggregation(
							countsByPriceKey, f -> f.terms()
									.field( "price", Double.class ) // <1>
									.value( f.count().documents() ) // <4>
					)
					.fetch( 20 );
			Map<Double, Long> countsByPrice = result.aggregation( countsByPriceKey );
			// end::terms-count[]
			assertThat( countsByPrice )
					.containsExactly(
							entry( 7.99, 1L ),
							entry( 15.99, 1L ),
							entry( 19.99, 1L ),
							entry( 24.99, 1L )
					);
		} );

		withinSearchSession( searchSession -> {
			// tag::terms-count-implicit[]
			AggregationKey<Map<Double, Long>> countsByPriceKey = AggregationKey.of( "countsByPrice" );
			SearchResult<Book> result = searchSession.search( Book.class )
					.where( f -> f.matchAll() )
					.aggregation(
							countsByPriceKey, f -> f.terms()
									.field( "price", Double.class ) // <1>
					)
					.fetch( 20 );
			Map<Double, Long> countsByPrice = result.aggregation( countsByPriceKey );
			// end::terms-count-implicit[]
			assertThat( countsByPrice )
					.containsExactly(
							entry( 7.99, 1L ),
							entry( 15.99, 1L ),
							entry( 19.99, 1L ),
							entry( 24.99, 1L )
					);
		} );

		withinSearchSession( searchSession -> {
			// tag::terms-count-composite[]
			record PriceAggregation(Double avg, Double min, Double max) {
			}
			AggregationKey<Map<Double, PriceAggregation>> countsByPriceKey = AggregationKey.of( "countsByPrice" );
			SearchResult<Book> result = searchSession.search( Book.class )
					.where( f -> f.matchAll() )
					.aggregation(
							countsByPriceKey, f -> f.terms()
									.field( "price", Double.class ) // <1>
									.value( f.composite()
											.from(
													f.avg().field( "price", Double.class ),
													f.min().field( "price", Double.class ),
													f.max().field( "price", Double.class )
											).as( PriceAggregation::new ) )
					)
					.fetch( 20 );
			Map<Double, PriceAggregation> countsByPrice = result.aggregation( countsByPriceKey );
			// end::terms-count-composite[]
			assertThat( countsByPrice )
					.hasSize( 4 )
					.containsOnlyKeys( 7.99, 15.99, 19.99, 24.99 )
					.satisfies(
							map -> {
								var agg = map.get( 7.99 );
								assertThat( agg ).satisfies(
										a -> assertThat( a.avg() ).isCloseTo( 7.99, offset( 0.0001 ) ),
										a -> assertThat( a.min() ).isCloseTo( 7.99, offset( 0.0001 ) ),
										a -> assertThat( a.max() ).isCloseTo( 7.99, offset( 0.0001 ) )
								);
							},
							map -> {
								var agg = map.get( 15.99 );
								assertThat( agg ).satisfies(
										a -> assertThat( a.avg() ).isCloseTo( 15.99, offset( 0.0001 ) ),
										a -> assertThat( a.min() ).isCloseTo( 15.99, offset( 0.0001 ) ),
										a -> assertThat( a.max() ).isCloseTo( 15.99, offset( 0.0001 ) )
								);
							},
							map -> {
								var agg = map.get( 19.99 );
								assertThat( agg ).satisfies(
										a -> assertThat( a.avg() ).isCloseTo( 19.99, offset( 0.0001 ) ),
										a -> assertThat( a.min() ).isCloseTo( 19.99, offset( 0.0001 ) ),
										a -> assertThat( a.max() ).isCloseTo( 19.99, offset( 0.0001 ) )
								);
							},
							map -> {
								var agg = map.get( 24.99 );
								assertThat( agg ).satisfies(
										a -> assertThat( a.avg() ).isCloseTo( 24.99, offset( 0.0001 ) ),
										a -> assertThat( a.min() ).isCloseTo( 24.99, offset( 0.0001 ) ),
										a -> assertThat( a.max() ).isCloseTo( 24.99, offset( 0.0001 ) )
								);
							}
					);
		} );
	}

	@Test
	void range_value() {
		withinSearchSession( searchSession -> {
			// tag::range-avg[]
			AggregationKey<Map<Range<Double>, Double>> avgRatingByPriceKey = AggregationKey.of( "avgRatingByPrice" );
			SearchResult<Book> result = searchSession.search( Book.class )
					.where( f -> f.matchAll() )
					.aggregation(
							avgRatingByPriceKey, f -> f.range()
									.field( "price", Double.class ) // <1>
									.range( 0.0, 10.0 )
									.range( 10.0, 20.0 )
									.range( 20.0, null )
									.value( f.avg().field( "ratings", Double.class, ValueModel.RAW ) ) // <2>
					)
					.fetch( 20 );
			Map<Range<Double>, Double> countsByPrice = result.aggregation( avgRatingByPriceKey );
			// end::range-avg[]
			assertThat( countsByPrice )
					.hasSize( 3 )
					.containsOnlyKeys(
							Range.canonical( 0.0, 10.0 ),
							Range.canonical( 10.0, 20.0 ),
							Range.canonical( 20.0, null )
					)
					.satisfies(
							map -> assertThat( map.get( Range.canonical( 0.0, 10.0 ) ) ).isCloseTo( 4.0, offset( 0.0001 ) ),
							map -> assertThat( map.get( Range.canonical( 10.0, 20.0 ) ) ).isCloseTo( 3.6, offset( 0.0001 ) ),
							map -> assertThat( map.get( Range.canonical( 20.0, null ) ) ).isCloseTo( 3.2, offset( 0.0001 ) )
					);
		} );

		withinSearchSession( searchSession -> {
			// tag::range-count[]
			AggregationKey<Map<Range<Double>, Long>> countsByPriceKey = AggregationKey.of( "countsByPrice" );
			SearchResult<Book> result = searchSession.search( Book.class )
					.where( f -> f.matchAll() )
					.aggregation(
							countsByPriceKey, f -> f.range()
									.field( "price", Double.class ) // <1>
									.range( 0.0, 10.0 ) // <2>
									.range( 10.0, 20.0 )
									.range( 20.0, null ) // <3>
									.value( f.count().documents() ) // <4>
					)
					.fetch( 20 );
			Map<Range<Double>, Long> countsByPrice = result.aggregation( countsByPriceKey );
			// end::range-count[]
			assertThat( countsByPrice )
					.containsExactly(
							entry( Range.canonical( 0.0, 10.0 ), 1L ),
							entry( Range.canonical( 10.0, 20.0 ), 2L ),
							entry( Range.canonical( 20.0, null ), 1L )
					);
		} );

		withinSearchSession( searchSession -> {
			// tag::range-composite[]
			record PriceAggregation(Double avg, Double min, Double max) {
			}
			AggregationKey<Map<Range<Double>, PriceAggregation>> countsByPriceKey = AggregationKey.of( "countsByPrice" );
			SearchResult<Book> result = searchSession.search( Book.class )
					.where( f -> f.matchAll() )
					.aggregation(
							countsByPriceKey, f -> f.range()
									.field( "price", Double.class ) // <1>
									.range( 0.0, 10.0 ) // <2>
									.range( 10.0, 20.0 )
									.range( 20.0, null )
									.value( f.composite() // <3>
											.from(
													f.avg().field( "price", Double.class ),
													f.min().field( "price", Double.class ),
													f.max().field( "price", Double.class )
											).as( PriceAggregation::new ) )
					)
					.fetch( 20 );
			Map<Range<Double>, PriceAggregation> countsByPrice = result.aggregation( countsByPriceKey );
			// end::range-composite[]
			assertThat( countsByPrice )
					.hasSize( 3 )
					.containsOnlyKeys(
							Range.canonical( 0.0, 10.0 ),
							Range.canonical( 10.0, 20.0 ),
							Range.canonical( 20.0, null )
					)
					.satisfies(
							map -> {
								var agg = map.get( Range.canonical( 0.0, 10.0 ) );
								assertThat( agg ).satisfies(
										a -> assertThat( a.avg() ).isCloseTo( 7.99, offset( 0.0001 ) ),
										a -> assertThat( a.min() ).isCloseTo( 7.99, offset( 0.0001 ) ),
										a -> assertThat( a.max() ).isCloseTo( 7.99, offset( 0.0001 ) )
								);
							},
							map -> {
								var agg = map.get( Range.canonical( 10.0, 20.0 ) );
								assertThat( agg ).satisfies(
										a -> assertThat( a.avg() ).isCloseTo( 17.99, offset( 0.0001 ) ),
										a -> assertThat( a.min() ).isCloseTo( 15.99, offset( 0.0001 ) ),
										a -> assertThat( a.max() ).isCloseTo( 19.99, offset( 0.0001 ) )
								);
							},
							map -> {
								var agg = map.get( Range.canonical( 20.0, null ) );
								assertThat( agg ).satisfies(
										a -> assertThat( a.avg() ).isCloseTo( 24.99, offset( 0.0001 ) ),
										a -> assertThat( a.min() ).isCloseTo( 24.99, offset( 0.0001 ) ),
										a -> assertThat( a.max() ).isCloseTo( 24.99, offset( 0.0001 ) )
								);
							}
					);
		} );
	}

	@Test
	void range() {
		withinSearchSession( searchSession -> {
			// tag::range[]
			AggregationKey<Map<Range<Double>, Long>> countsByPriceKey = AggregationKey.of( "countsByPrice" );
			SearchResult<Book> result = searchSession.search( Book.class )
					.where( f -> f.matchAll() )
					.aggregation( countsByPriceKey, f -> f.range()
							.field( "price", Double.class ) // <1>
							.range( 0.0, 10.0 ) // <2>
							.range( 10.0, 20.0 )
							.range( 20.0, null ) // <3>
					)
					.fetch( 20 );
			Map<Range<Double>, Long> countsByPrice = result.aggregation( countsByPriceKey );
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
					.where( f -> f.matchAll() )
					.aggregation( countsByPriceKey, f -> f.range()
							.field( "price", Double.class )
							.range( Range.canonical( 0.0, 10.0 ) ) // <1>
							.range( Range.between( 10.0, RangeBoundInclusion.INCLUDED,
									20.0, RangeBoundInclusion.EXCLUDED ) ) // <2>
							.range( Range.atLeast( 20.0 ) ) // <3>
					)
					.fetch( 20 );
			Map<Range<Double>, Long> countsByPrice = result.aggregation( countsByPriceKey );
			// end::range-objects[]
			assertThat( countsByPrice )
					.containsExactly(
							entry( Range.canonical( 0.0, 10.0 ), 1L ),
							entry( Range.canonical( 10.0, 20.0 ), 2L ),
							entry( Range.canonical( 20.0, null ), 1L )
					);
		} );

		withinSearchSession( searchSession -> {
			// tag::range-objects-collection[]
			List<Range<Double>> ranges =
					// end::range-objects-collection[]
					Arrays.asList(
							Range.canonical( 0.0, 10.0 ),
							Range.canonical( 10.0, 20.0 ),
							Range.atLeast( 20.0 )
					)
			// tag::range-objects-collection[]
			/* ... */;

			AggregationKey<Map<Range<Double>, Long>> countsByPriceKey = AggregationKey.of( "countsByPrice" );
			SearchResult<Book> result = searchSession.search( Book.class )
					.where( f -> f.matchAll() )
					.aggregation( countsByPriceKey, f -> f.range()
							.field( "price", Double.class )
							.ranges( ranges )
					)
					.fetch( 20 );
			Map<Range<Double>, Long> countsByPrice = result.aggregation( countsByPriceKey );
			// end::range-objects-collection[]
			assertThat( countsByPrice )
					.containsExactly(
							entry( Range.canonical( 0.0, 10.0 ), 1L ),
							entry( Range.canonical( 10.0, 20.0 ), 2L ),
							entry( Range.canonical( 20.0, null ), 1L )
					);
		} );

		withinSearchSession( searchSession -> {
			// @formatter:off
			// tag::range-noConverter[]
			AggregationKey<Map<Range<Instant>, Long>> countsByPriceKey = AggregationKey.of( "countsByPrice" );
			SearchResult<Book> result = searchSession.search( Book.class )
					.where( f -> f.matchAll() )
					.aggregation( countsByPriceKey, f -> f.range()
							// Assuming "releaseDate" is of type "java.util.Date" or "java.sql.Date"
							.field( "releaseDate", Instant.class, ValueModel.INDEX )
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
			Map<Range<Instant>, Long> countsByPrice = result.aggregation( countsByPriceKey );
			// end::range-noConverter[]
			// @formatter:on
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

	@Test
	void filter() {
		withinSearchSession( searchSession -> {
			// tag::filter[]
			AggregationKey<Map<Range<Double>, Long>> countsByPriceKey = AggregationKey.of( "countsByPrice" );
			SearchResult<Book> result = searchSession.search( Book.class )
					.where( f -> f.matchAll() )
					.aggregation( countsByPriceKey, f -> f.range()
							.field( "editions.price", Double.class )
							.range( 0.0, 10.0 )
							.range( 10.0, 20.0 )
							.range( 20.0, null )
							.filter( pf -> pf.match().field( "editions.label" ).matching( "paperback" ) )
					)
					.fetch( 20 );
			Map<Range<Double>, Long> countsByPrice = result.aggregation( countsByPriceKey );
			// end::filter[]
			assertThat( countsByPrice )
					.containsExactly(
							entry( Range.canonical( 0.0, 10.0 ), 3L ),
							entry( Range.canonical( 10.0, 20.0 ), 1L ),
							entry( Range.canonical( 20.0, null ), 0L )
					);
		} );
	}

	@Test
	void withParameters() {
		withinSearchSession( searchSession -> {
			// tag::with-parameters[]
			AggregationKey<Map<Range<Double>, Long>> countsByPriceKey = AggregationKey.of( "countsByPrice" );
			SearchResult<Book> result = searchSession.search( Book.class )
					.where( f -> f.matchAll() )
					.aggregation( countsByPriceKey, f -> f.withParameters( params -> f.range() // <1>
							.field( "price", Double.class )
							.range( params.get( "bound0", Double.class ), params.get( "bound1", Double.class ) ) // <2>
							.range( params.get( "bound1", Double.class ), params.get( "bound2", Double.class ) )
							.range( params.get( "bound2", Double.class ), params.get( "bound3", Double.class ) )
					) )
					.param( "bound0", 0.0 ) // <3>
					.param( "bound1", 10.0 )
					.param( "bound2", 20.0 )
					.param( "bound3", null )
					.fetch( 20 );
			Map<Range<Double>, Long> countsByPrice = result.aggregation( countsByPriceKey );
			// end::with-parameters[]
			assertThat( countsByPrice )
					.containsExactly(
							entry( Range.canonical( 0.0, 10.0 ), 1L ),
							entry( Range.canonical( 10.0, 20.0 ), 2L ),
							entry( Range.canonical( 20.0, null ), 1L )
					);
		} );
	}

	@Test
	void sum() {
		withinSearchSession( searchSession -> {
			// tag::sums[]
			AggregationKey<Double> sumPricesKey = AggregationKey.of( "sumPricesScienceFictionBooks" );
			SearchResult<Book> result = searchSession.search( Book.class )
					.where( f -> f.match().field( "genre" ).matching( Genre.SCIENCE_FICTION ) )
					.aggregation( sumPricesKey, f -> f.sum().field( "price", Double.class ) ) // <1>
					.fetch( 20 );
			Double sumPrices = result.aggregation( sumPricesKey );
			// end::sums[]
			assertThat( sumPrices ).isCloseTo( 60.97, offset( 0.0001 ) );
		} );
	}

	@Test
	void min() {
		withinSearchSession( searchSession -> {
			// tag::min[]
			AggregationKey<Date> oldestReleaseKey = AggregationKey.of( "oldestRelease" );
			SearchResult<Book> result = searchSession.search( Book.class )
					.where( f -> f.match().field( "genre" ).matching( Genre.SCIENCE_FICTION ) )
					.aggregation( oldestReleaseKey, f -> f.min().field( "releaseDate", Date.class ) ) // <1>
					.fetch( 20 );
			Date oldestRelease = result.aggregation( oldestReleaseKey );
			// end::min[]
			assertThat( oldestRelease ).isEqualTo( Date.valueOf( "1950-12-02" ) );
		} );
	}

	@Test
	void max() {
		withinSearchSession( searchSession -> {
			// tag::max[]
			AggregationKey<Date> mostRecentReleaseKey = AggregationKey.of( "mostRecentRelease" );
			SearchResult<Book> result = searchSession.search( Book.class )
					.where( f -> f.match().field( "genre" ).matching( Genre.SCIENCE_FICTION ) )
					.aggregation( mostRecentReleaseKey, f -> f.max().field( "releaseDate", Date.class ) ) // <1>
					.fetch( 20 );
			Date mostRecentRelease = result.aggregation( mostRecentReleaseKey );
			// end::max[]
			assertThat( mostRecentRelease ).isEqualTo( Date.valueOf( "1983-01-01" ) );
		} );
	}

	@Test
	void countDocuments() {
		withinSearchSession( searchSession -> {
			// tag::count-documents[]
			AggregationKey<Long> countBooksKey = AggregationKey.of( "countBooks" );
			SearchResult<Book> result = searchSession.search( Book.class )
					.where( f -> f.match().field( "genre" ).matching( Genre.SCIENCE_FICTION ) )
					.aggregation( countBooksKey, f -> f.count() // <1>
							.documents() ) // <2>
					.fetch( 20 );
			Long countPrices = result.aggregation( countBooksKey );
			// end::count-documents[]
			assertThat( countPrices ).isEqualTo( 3L );
		} );
	}

	@Test
	void countValues() {
		withinSearchSession( searchSession -> {
			// tag::count[]
			AggregationKey<Long> countRatingsKey = AggregationKey.of( "countRatings" );
			SearchResult<Book> result = searchSession.search( Book.class )
					.where( f -> f.match().field( "genre" ).matching( Genre.SCIENCE_FICTION ) )
					.aggregation( countRatingsKey, f -> f.count() // <1>
							.field( "ratings" ) ) // <2>
					.fetch( 20 );
			Long countPrices = result.aggregation( countRatingsKey );
			// end::count[]
			assertThat( countPrices ).isEqualTo( 15L );
		} );
	}

	@Test
	void countDistinctValues() {
		withinSearchSession( searchSession -> {
			// tag::count-distinct[]
			AggregationKey<Long> countDistinctPricesKey = AggregationKey.of( "countDistinctPrices" );
			SearchResult<Book> result = searchSession.search( Book.class )
					.where( f -> f.match().field( "genre" ).matching( Genre.SCIENCE_FICTION ) )
					.aggregation( countDistinctPricesKey, f -> f.count() // <1>
							.field( "price" ) // <2>
							.distinct() ) // <3>
					.fetch( 20 );
			Long countDistinctPrices = result.aggregation( countDistinctPricesKey );
			// end::count-distinct[]
			assertThat( countDistinctPrices ).isEqualTo( 3L );
		} );
	}

	@Test
	void avg() {
		withinSearchSession( searchSession -> {
			// tag::avg[]
			AggregationKey<Double> avgPricesKey = AggregationKey.of( "avgPrices" );
			SearchResult<Book> result = searchSession.search( Book.class )
					.where( f -> f.match().field( "genre" ).matching( Genre.SCIENCE_FICTION ) )
					.aggregation( avgPricesKey, f -> f.avg().field( "price", Double.class ) ) // <1>
					.fetch( 20 );
			Double avgPrices = result.aggregation( avgPricesKey );
			// end::avg[]
			assertThat( avgPrices ).isCloseTo( 20.323333333333334, offset( 0.0001 ) );
		} );
	}

	@SuppressWarnings("raw")
	@Test
	void composite() {
		withinSearchSession( searchSession -> {
			// tag::composite-customObject[]
			record PriceAggregation(Double avg, Double min, Double max) {
			}

			AggregationKey<PriceAggregation> avgPricesKey = AggregationKey.of( "aggregations" );
			SearchResult<Book> result = searchSession.search( Book.class )
					.where( f -> f.matchAll() )
					.aggregation( avgPricesKey, f -> f.composite() // <1>
							.from(
									f.avg().field( "price", Double.class ), // <2>
									f.min().field( "price", Double.class ),
									f.max().field( "price", Double.class )
							).as( PriceAggregation::new ) ) // <3>
					.fetch( 20 );
			PriceAggregation aggregations = result.aggregation( avgPricesKey ); // <4>
			// end::composite-customObject[]
			assertThat( aggregations ).satisfies(
					a -> assertThat( a.avg() ).isCloseTo( 17.24, offset( 0.0001 ) ),
					a -> assertThat( a.min() ).isCloseTo( 7.99, offset( 0.0001 ) ),
					a -> assertThat( a.max() ).isCloseTo( 24.99, offset( 0.0001 ) )
			);
		} );

		withinSearchSession( searchSession -> {
			// tag::composite-customObject-asList[]
			record BookAggregation(Double avg, Double min, Double max, Long ratingCount) {
				BookAggregation(List<?> list) {
					this( (Double) list.get( 0 ),
							(Double) list.get( 1 ),
							(Double) list.get( 2 ),
							(Long) list.get( 3 ) );
				}
			}

			AggregationKey<BookAggregation> aggKey = AggregationKey.of( "aggregations" );
			SearchResult<Book> result = searchSession.search( Book.class )
					.where( f -> f.matchAll() )
					.aggregation( aggKey, f -> f.composite() // <1>
							.from(
									f.avg().field( "price", Double.class ), // <2>
									f.min().field( "price", Double.class ),
									f.max().field( "price", Double.class ),
									f.count().field( "ratings" )
							).asList( BookAggregation::new ) ) // <3>
					.fetch( 20 );
			BookAggregation aggregations = result.aggregation( aggKey ); // <4>
			// end::composite-customObject-asList[]
			assertThat( aggregations ).satisfies(
					a -> assertThat( a.avg() ).isCloseTo( 17.24, offset( 0.0001 ) ),
					a -> assertThat( a.min() ).isCloseTo( 7.99, offset( 0.0001 ) ),
					a -> assertThat( a.max() ).isCloseTo( 24.99, offset( 0.0001 ) )
			);
		} );

		withinSearchSession( searchSession -> {
			// tag::composite-list[]
			AggregationKey<List<?>> aggKey = AggregationKey.of( "aggregations" );
			SearchResult<Book> result = searchSession.search( Book.class )
					.where( f -> f.matchAll() )
					.aggregation( aggKey, f -> f.composite() // <1>
							.from(
									f.avg().field( "price", Double.class ), // <2>
									f.min().field( "price", Double.class ),
									f.max().field( "price", Double.class ),
									f.count().field( "ratings" )
							).asList() ) // <3>
					.fetch( 20 );
			List<?> aggregations = result.aggregation( aggKey ); // <4>
			// end::composite-list[]
			assertThat( (List) aggregations )
					.hasSize( 4 );
			assertThat( aggregations ).satisfies(
					a -> assertThat( (Double) a.get( 0 ) ).isCloseTo( 17.24, offset( 0.0001 ) ),
					a -> assertThat( (Double) a.get( 1 ) ).isCloseTo( 7.99, offset( 0.0001 ) ),
					a -> assertThat( (Double) a.get( 2 ) ).isCloseTo( 24.99, offset( 0.0001 ) ),
					a -> assertThat( (Long) a.get( 3 ) ).isEqualTo( 20L )
			);
		} );

		withinSearchSession( searchSession -> {
			// tag::composite-array[]
			AggregationKey<Object[]> aggKey = AggregationKey.of( "aggregations" );
			SearchResult<Book> result = searchSession.search( Book.class )
					.where( f -> f.matchAll() )
					.aggregation( aggKey, f -> f.composite() // <1>
							.from(
									f.avg().field( "price", Double.class ), // <2>
									f.min().field( "price", Double.class ),
									f.max().field( "price", Double.class ),
									f.count().field( "ratings" )
							).asArray() ) // <3>
					.fetch( 20 );
			Object[] aggregations = result.aggregation( aggKey ); // <4>
			// end::composite-array[]
			assertThat( aggregations )
					.hasSize( 4 );
			assertThat( aggregations ).satisfies(
					a -> assertThat( (Double) a[0] ).isCloseTo( 17.24, offset( 0.0001 ) ),
					a -> assertThat( (Double) a[1] ).isCloseTo( 7.99, offset( 0.0001 ) ),
					a -> assertThat( (Double) a[2] ).isCloseTo( 24.99, offset( 0.0001 ) ),
					a -> assertThat( (Long) a[3] ).isEqualTo( 20L )
			);
		} );

		withinSearchSession( searchSession -> {
			// tag::composite-list-singlestep[]
			AggregationKey<List<?>> aggKey = AggregationKey.of( "aggregations" );
			SearchResult<Book> result = searchSession.search( Book.class )
					.where( f -> f.matchAll() )
					.aggregation( aggKey, f -> f.composite( // <1>
							f.avg().field( "price", Double.class ), // <2>
							f.min().field( "price", Double.class ),
							f.max().field( "price", Double.class ),
							f.count().field( "ratings" )
					) ) // <3>
					.fetch( 20 );
			List<?> aggregations = result.aggregation( aggKey ); // <4>
			// end::composite-list-singlestep[]
			assertThat( (List) aggregations )
					.hasSize( 4 );
			assertThat( aggregations ).satisfies(
					a -> assertThat( (Double) a.get( 0 ) ).isCloseTo( 17.24, offset( 0.0001 ) ),
					a -> assertThat( (Double) a.get( 1 ) ).isCloseTo( 7.99, offset( 0.0001 ) ),
					a -> assertThat( (Double) a.get( 2 ) ).isCloseTo( 24.99, offset( 0.0001 ) ),
					a -> assertThat( (Long) a.get( 3 ) ).isEqualTo( 20L )
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
			book1.setRatings( List.of( 5, 5, 4, 2, 0 ) );
			addEdition( book1, "Mass Market Paperback, 1st Edition", 9.99 );
			addEdition( book1, "Kindle", 9.99 );

			Book book2 = new Book();
			book2.setId( BOOK2_ID );
			book2.setTitle( "The Caves of Steel" );
			book2.setPrice( 19.99 );
			book2.setGenre( Genre.SCIENCE_FICTION );
			book2.setReleaseDate( Date.valueOf( "1953-10-01" ) );
			book2.setRatings( List.of( 5, 5, 3, 3, 5 ) );
			addEdition( book2, "Mass Market Paperback, 12th Edition", 4.99 );
			addEdition( book2, "Kindle", 19.99 );

			Book book3 = new Book();
			book3.setId( BOOK3_ID );
			book3.setTitle( "The Robots of Dawn" );
			book3.setPrice( 15.99 );
			book3.setGenre( Genre.SCIENCE_FICTION );
			book3.setReleaseDate( Date.valueOf( "1983-01-01" ) );
			book3.setRatings( List.of( 3, 3, 3, 3, 3 ) );
			addEdition( book3, "Mass Market Paperback, 59th Edition", 3.99 );
			addEdition( book3, "Kindle", 5.99 );

			Book book4 = new Book();
			book4.setId( BOOK4_ID );
			book4.setTitle( "The Automatic Detective" );
			book4.setPrice( 7.99 );
			book4.setGenre( Genre.CRIME_FICTION );
			book4.setReleaseDate( Date.valueOf( "2008-02-05" ) );
			book4.setRatings( List.of( 4, 4, 4, 4, 4 ) );
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
