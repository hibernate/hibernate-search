/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.operations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.hibernate.search.engine.search.aggregation.dsl.AggregationFinalStep;
import org.hibernate.search.engine.search.aggregation.dsl.SearchAggregationFactory;
import org.hibernate.search.engine.search.aggregation.dsl.TermsAggregationOptionsStep;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.backend.tck.testsupport.operations.expectations.AggregationScenario;
import org.hibernate.search.integrationtest.backend.tck.testsupport.operations.expectations.SupportedSingleFieldAggregationExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.operations.expectations.UnsupportedSingleFieldAggregationExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.AnalyzedStringFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.ExpectationsAlternative;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TypeAssertionHelper;
import org.hibernate.search.util.common.data.Range;
import org.hibernate.search.util.impl.integrationtest.common.NormalizationUtils;

public class TermsAggregationDescriptor extends AggregationDescriptor {

	public static TermsAggregationDescriptor INSTANCE = new TermsAggregationDescriptor();

	private TermsAggregationDescriptor() {
	}

	@Override
	public <F> ExpectationsAlternative<
			SupportedSingleFieldAggregationExpectations<F>,
			UnsupportedSingleFieldAggregationExpectations> getSingleFieldAggregationExpectations(
					FieldTypeDescriptor<F, ?> typeDescriptor) {
		if ( AnalyzedStringFieldTypeDescriptor.class.equals( typeDescriptor.getClass() )
				|| GeoPoint.class.equals( typeDescriptor.getJavaType() )
				|| byte[].class.equals( typeDescriptor.getJavaType() )
				|| float[].class.equals( typeDescriptor.getJavaType() ) ) {
			// Terms aggregations are not supported on analyzed or GeoPoint fields
			return ExpectationsAlternative.unsupported( unsupportedExpectations( typeDescriptor ) );
		}

		List<F> uniqueTermValues = new ArrayList<>( typeDescriptor.getAscendingUniqueTermValues().getSingle() );
		// Mess with the value order, because it should not matter
		uniqueTermValues.add( uniqueTermValues.get( 0 ) );
		uniqueTermValues.remove( 0 );
		uniqueTermValues.add( uniqueTermValues.get( 0 ) );
		uniqueTermValues.remove( 0 );

		List<F> mainIndexDocumentFieldValues = new ArrayList<>();
		List<F> otherIndexDocumentFieldValues = new ArrayList<>();
		List<List<F>> multiValuedIndexDocumentFieldValues = new ArrayList<>();
		Map<F, Long> mainIndexExpected = new LinkedHashMap<>();
		Map<F, Long> mainAndOtherIndexExpected = new LinkedHashMap<>();
		Map<F, Long> multiValuedIndexExpected = new LinkedHashMap<>();

		// Simple dataset for the main index: strictly decreasing number of documents for each term
		long numberOfDocuments = uniqueTermValues.size();
		for ( F uniqueTermValue : uniqueTermValues ) {
			for ( int i = 0; i < numberOfDocuments; i++ ) {
				mainIndexDocumentFieldValues.add( uniqueTermValue );
			}
			mainIndexExpected.put( typeDescriptor.toExpectedDocValue( uniqueTermValue ), numberOfDocuments );
			--numberOfDocuments;
		}

		// For the other index, make sure not to break the "strictly decreasing" property of the map.
		// Just add one term for the terms with the highest and second-highest counts.
		F termWithHighestCount = uniqueTermValues.get( 0 );
		F termWithSecondHighestCount = uniqueTermValues.get( 1 );
		otherIndexDocumentFieldValues.add( termWithHighestCount );
		otherIndexDocumentFieldValues.add( termWithSecondHighestCount );
		mainAndOtherIndexExpected.putAll( mainIndexExpected );
		mainAndOtherIndexExpected.compute(
				typeDescriptor.toExpectedDocValue( termWithHighestCount ),
				(key, count) -> count + 1
		);
		mainAndOtherIndexExpected.compute(
				typeDescriptor.toExpectedDocValue( termWithSecondHighestCount ),
				(key, count) -> count + 1
		);

		// Dataset and expectations for the multi-valued index
		// Single-valued documents
		multiValuedIndexDocumentFieldValues.add( Arrays.asList( uniqueTermValues.get( 0 ) ) );
		multiValuedIndexDocumentFieldValues.add( Arrays.asList( uniqueTermValues.get( 1 ) ) );
		multiValuedIndexDocumentFieldValues.add(
				// Document matching two different buckets
				Arrays.asList( uniqueTermValues.get( 0 ), uniqueTermValues.get( 1 ) )
		);
		multiValuedIndexDocumentFieldValues.add(
				// Document matching the same bucket twice
				Arrays.asList( uniqueTermValues.get( 0 ), uniqueTermValues.get( 0 ) )
		);

		multiValuedIndexExpected.put( typeDescriptor.toExpectedDocValue( uniqueTermValues.get( 0 ) ), 3L );
		multiValuedIndexExpected.put( typeDescriptor.toExpectedDocValue( uniqueTermValues.get( 1 ) ), 2L );

		return ExpectationsAlternative.supported( new SupportedSingleFieldAggregationExpectations<F>(
				typeDescriptor, "terms",
				mainIndexDocumentFieldValues,
				otherIndexDocumentFieldValues,
				multiValuedIndexDocumentFieldValues
		) {
			@Override
			public <T> AggregationScenario<Map<T, Long>> withFieldType(TypeAssertionHelper<F, T> helper) {
				return doCreate( mainIndexExpected, helper );
			}

			@Override
			public <T> AggregationScenario<Map<T, Long>> withFieldTypeOnMainAndOtherIndex(TypeAssertionHelper<F, T> helper) {
				return doCreate( mainAndOtherIndexExpected, helper );
			}

			@Override
			public AggregationScenario<?> withoutMatch() {
				return doCreate( Collections.emptyMap(), TypeAssertionHelper.identity( fieldType() ) );
			}

			@Override
			public AggregationScenario<?> onMultiValuedIndex() {
				return doCreate( multiValuedIndexExpected, TypeAssertionHelper.identity( fieldType() ) );
			}

			private <T> AggregationScenario<Map<T, Long>> doCreate(Map<F, Long> expectedResult,
					TypeAssertionHelper<F, T> helper) {
				return new AggregationScenario<Map<T, Long>>() {
					@Override
					public AggregationFinalStep<Map<T, Long>> setup(SearchAggregationFactory factory, String fieldPath,
							Function<? super SearchPredicateFactory, ? extends PredicateFinalStep> filterOrNull) {
						TermsAggregationOptionsStep<?, ?, ?, Map<T, Long>> optionsStep =
								factory.terms().field( fieldPath, helper.getJavaClass() );
						if ( filterOrNull == null ) {
							return optionsStep;
						}
						else {
							return optionsStep.filter( filterOrNull );
						}
					}

					@Override
					public AggregationFinalStep<Map<T, Long>> setupWithConverterSetting(SearchAggregationFactory factory,
							String fieldPath, ValueConvert convert) {
						return factory.terms().field( fieldPath, helper.getJavaClass(), convert );
					}

					@Override
					public void check(Map<T, Long> aggregationResult) {
						@SuppressWarnings("unchecked")
						Map.Entry<Range<T>, Long>[] expectedEntries = NormalizationUtils.normalize( expectedResult )
								.entrySet().stream()
								.map( e -> entry(
										helper.create( e.getKey() ),
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
				return "terms";
			}

			@Override
			public void trySetup(SearchAggregationFactory factory, String fieldPath) {
				factory.terms().field( fieldPath, typeDescriptor.getJavaType() );
			}

			@Override
			public String toString() {
				return "terms on " + typeDescriptor;
			}
		};
	}

}
