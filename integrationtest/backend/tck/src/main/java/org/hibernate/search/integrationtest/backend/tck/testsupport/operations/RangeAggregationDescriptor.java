/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.operations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.hibernate.search.engine.search.aggregation.dsl.AggregationFinalStep;
import org.hibernate.search.engine.search.aggregation.dsl.RangeAggregationOptionsStep;
import org.hibernate.search.engine.search.aggregation.dsl.SearchAggregationFactory;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.backend.tck.testsupport.operations.expectations.AggregationScenario;
import org.hibernate.search.integrationtest.backend.tck.testsupport.operations.expectations.SupportedSingleFieldAggregationExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.operations.expectations.UnsupportedSingleFieldAggregationExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.ExpectationsAlternative;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TypeAssertionHelper;
import org.hibernate.search.util.common.data.Range;
import org.hibernate.search.util.impl.integrationtest.common.NormalizationUtils;

public class RangeAggregationDescriptor extends AggregationDescriptor {

	public static RangeAggregationDescriptor INSTANCE = new RangeAggregationDescriptor();

	private RangeAggregationDescriptor() {
	}

	@Override
	public <F> ExpectationsAlternative<
			SupportedSingleFieldAggregationExpectations<F>,
			UnsupportedSingleFieldAggregationExpectations> getSingleFieldAggregationExpectations(
					FieldTypeDescriptor<F, ?> typeDescriptor) {
		if ( String.class.equals( typeDescriptor.getJavaType() )
				|| GeoPoint.class.equals( typeDescriptor.getJavaType() )
				|| Boolean.class.equals( typeDescriptor.getJavaType() )
				|| byte[].class.equals( typeDescriptor.getJavaType() )
				|| float[].class.equals( typeDescriptor.getJavaType() ) ) {
			// Range aggregations are not supported on text, GeoPoint or boolean fields.
			return ExpectationsAlternative.unsupported( unsupportedExpectations( typeDescriptor ) );
		}

		List<F> ascendingValues = typeDescriptor.getAscendingUniqueTermValues().getSingle();
		List<F> mainIndexDocumentFieldValues = new ArrayList<>();
		List<F> otherIndexDocumentFieldValues = new ArrayList<>();
		List<List<F>> multiValuedIndexDocumentFieldValues = new ArrayList<>();
		Map<Range<F>, Long> mainIndexExpected = new LinkedHashMap<>();
		Map<Range<F>, Long> mainAndOtherIndexExpected = new LinkedHashMap<>();
		Map<Range<F>, Long> noIndexedValueExpected = new LinkedHashMap<>();
		Map<Range<F>, Long> multiValuedIndexExpected = new LinkedHashMap<>();

		mainIndexDocumentFieldValues.addAll( ascendingValues.subList( 0, 6 ) );
		// Make sure some documents have the same value; this allows us to check HSEARCH-1929 in particular.
		mainIndexDocumentFieldValues.add( ascendingValues.get( 1 ) );
		mainIndexDocumentFieldValues.add( ascendingValues.get( 2 ) );
		mainIndexDocumentFieldValues.add( ascendingValues.get( 4 ) );
		mainIndexDocumentFieldValues.add( ascendingValues.get( 5 ) );

		otherIndexDocumentFieldValues.add( ascendingValues.get( 1 ) );
		otherIndexDocumentFieldValues.add( ascendingValues.get( 6 ) );

		mainIndexExpected.put( Range.canonical( ascendingValues.get( 0 ), ascendingValues.get( 2 ) ), 3L );
		mainIndexExpected.put( Range.canonical( ascendingValues.get( 2 ), ascendingValues.get( 5 ) ), 5L );
		mainIndexExpected.put( Range.atLeast( ascendingValues.get( 5 ) ), 2L );

		mainAndOtherIndexExpected.put( Range.canonical( ascendingValues.get( 0 ), ascendingValues.get( 2 ) ), 4L );
		mainAndOtherIndexExpected.put( Range.canonical( ascendingValues.get( 2 ), ascendingValues.get( 5 ) ), 5L );
		mainAndOtherIndexExpected.put( Range.atLeast( ascendingValues.get( 5 ) ), 3L );

		noIndexedValueExpected.put( Range.canonical( ascendingValues.get( 0 ), ascendingValues.get( 2 ) ), 0L );
		noIndexedValueExpected.put( Range.canonical( ascendingValues.get( 2 ), ascendingValues.get( 5 ) ), 0L );
		noIndexedValueExpected.put( Range.atLeast( ascendingValues.get( 5 ) ), 0L );

		// Dataset and expectations for the multi-valued index
		// Single-valued documents
		multiValuedIndexDocumentFieldValues.add( Arrays.asList( ascendingValues.get( 0 ) ) );
		multiValuedIndexDocumentFieldValues.add( Arrays.asList( ascendingValues.get( 2 ) ) );
		multiValuedIndexDocumentFieldValues.add( Arrays.asList( ascendingValues.get( 5 ) ) );
		multiValuedIndexDocumentFieldValues.add(
				// Document matching two different buckets
				Arrays.asList( ascendingValues.get( 1 ), ascendingValues.get( 6 ) )
		);
		multiValuedIndexDocumentFieldValues.add(
				// Document matching the same bucket twice
				Arrays.asList( ascendingValues.get( 3 ), ascendingValues.get( 4 ) )
		);

		multiValuedIndexExpected.put( Range.canonical( ascendingValues.get( 0 ), ascendingValues.get( 2 ) ), 2L );
		multiValuedIndexExpected.put( Range.canonical( ascendingValues.get( 2 ), ascendingValues.get( 5 ) ), 2L );
		multiValuedIndexExpected.put( Range.atLeast( ascendingValues.get( 5 ) ), 2L );

		return ExpectationsAlternative.supported( new SupportedSingleFieldAggregationExpectations<F>(
				typeDescriptor, "range",
				mainIndexDocumentFieldValues,
				otherIndexDocumentFieldValues,
				multiValuedIndexDocumentFieldValues
		) {
			@Override
			public <T> AggregationScenario<Map<Range<T>, Long>> withFieldType(TypeAssertionHelper<F, T> helper) {
				return doCreate( mainIndexExpected, helper );
			}

			@Override
			public <T> AggregationScenario<Map<Range<T>, Long>> withFieldTypeOnMainAndOtherIndex(
					TypeAssertionHelper<F, T> helper) {
				return doCreate( mainAndOtherIndexExpected, helper );
			}

			@Override
			public AggregationScenario<?> withoutMatch() {
				return doCreate( noIndexedValueExpected, TypeAssertionHelper.identity( fieldType() ) );
			}

			@Override
			public AggregationScenario<?> onMultiValuedIndex() {
				return doCreate( multiValuedIndexExpected, TypeAssertionHelper.identity( fieldType() ) );
			}

			private <T> AggregationScenario<Map<Range<T>, Long>> doCreate(Map<Range<F>, Long> expectedResult,
					TypeAssertionHelper<F, T> helper) {
				return new AggregationScenario<Map<Range<T>, Long>>() {
					@Override
					public AggregationFinalStep<Map<Range<T>, Long>> setup(SearchAggregationFactory factory,
							String fieldPath,
							Function<? super SearchPredicateFactory, ? extends PredicateFinalStep> filterOrNull) {
						RangeAggregationOptionsStep<?, ?, ?, Map<Range<T>, Long>> optionsStep =
								factory.range().field( fieldPath, helper.getJavaClass() )
										.range( helper.create( ascendingValues.get( 0 ) ),
												helper.create( ascendingValues.get( 2 ) ) )
										.range( helper.create( ascendingValues.get( 2 ) ),
												helper.create( ascendingValues.get( 5 ) ) )
										.range( helper.create( ascendingValues.get( 5 ) ),
												null );
						if ( filterOrNull == null ) {
							return optionsStep;
						}
						else {
							return optionsStep.filter( filterOrNull );
						}
					}

					@Override
					public AggregationFinalStep<Map<Range<T>, Long>> setupWithConverterSetting(SearchAggregationFactory factory,
							String fieldPath, ValueConvert convert) {
						return factory.range().field( fieldPath, helper.getJavaClass(), convert )
								.range( helper.create( ascendingValues.get( 0 ) ),
										helper.create( ascendingValues.get( 2 ) ) )
								.range( helper.create( ascendingValues.get( 2 ) ),
										helper.create( ascendingValues.get( 5 ) ) )
								.range( helper.create( ascendingValues.get( 5 ) ),
										null );
					}

					@Override
					public void check(Map<Range<T>, Long> aggregationResult) {
						@SuppressWarnings("unchecked")
						Map.Entry<Range<T>, Long>[] expectedEntries = NormalizationUtils.normalize( expectedResult )
								.entrySet().stream()
								.map( e -> entry(
										e.getKey().map( helper::create ),
										e.getValue()
								) )
								.toArray( Map.Entry[]::new );
						@SuppressWarnings("unchecked")
						Map.Entry<Range<T>, Long>[] actualEntries = NormalizationUtils.normalize( aggregationResult )
								.entrySet().toArray( new Map.Entry[0] );
						// Don't check the order, this is tested separately
						assertThat( actualEntries ).containsOnly( expectedEntries );
					}
				};
			}
		} );
	}

	private <
			F> UnsupportedSingleFieldAggregationExpectations unsupportedExpectations(FieldTypeDescriptor<F, ?> typeDescriptor) {
		return new UnsupportedSingleFieldAggregationExpectations() {
			@Override
			public String aggregationName() {
				return "range";
			}

			@Override
			public void trySetup(SearchAggregationFactory factory, String fieldPath) {
				factory.range().field( fieldPath, typeDescriptor.getJavaType() );
			}

			@Override
			public String toString() {
				return "range on " + typeDescriptor;
			}
		};
	}

}
