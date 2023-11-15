/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.aggregation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.hibernate.search.util.impl.integrationtest.common.NormalizationUtils.normalize;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatResult;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.operations.AggregationDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.operations.RangeAggregationDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.StandardFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModelsByType;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.ValueWrapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.data.Range;
import org.hibernate.search.util.common.data.RangeBoundInclusion;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests behavior specific to the range aggregation on supported field types.
 * <p>
 * Behavior common to all single-field aggregations is tested in {@link SingleFieldAggregationBaseIT}.
 */

class RangeAggregationSpecificsIT<F> {

	private static final String AGGREGATION_NAME = "aggregationName";

	private static final Set<StandardFieldTypeDescriptor<?>> supportedFieldTypes = new LinkedHashSet<>();
	private static final List<DataSet<?>> dataSets = new ArrayList<>();
	private static final List<Arguments> parameters = new ArrayList<>();

	static {
		AggregationDescriptor aggregationDescriptor = RangeAggregationDescriptor.INSTANCE;
		for ( StandardFieldTypeDescriptor<?> fieldType : FieldTypeDescriptor.getAllStandard() ) {
			if ( aggregationDescriptor.getSingleFieldAggregationExpectations( fieldType ).isSupported() ) {
				supportedFieldTypes.add( fieldType );
				DataSet<?> dataSet = new DataSet<>( fieldType );
				dataSets.add( dataSet );
				parameters.add( Arguments.of( fieldType, dataSet ) );
			}
		}
	}

	public static List<? extends Arguments> params() {
		return parameters;
	}

	@RegisterExtension
	public static final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@BeforeAll
	static void setup() {
		setupHelper.start().withIndex( index ).setup();

		for ( DataSet<?> dataSet : dataSets ) {
			dataSet.init();
		}
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	@PortedFromSearch5(original = "org.hibernate.search.test.query.facet.RangeFacetingTest.testRangeBelow")
	void rangeAtMost(FieldTypeDescriptor<F, ?> fieldType, DataSet<F> dataSet) {
		assumeNonCanonicalRangesSupported();

		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationKey<Map<Range<F>, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		assertThatQuery(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.range().field( fieldPath, fieldType.getJavaType() )
								.range( Range.atMost( dataSet.ascendingValues.get( 2 ) ) )
						)
						.routing( dataSet.name )
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						containsExactly( c -> {
							c.accept( Range.atMost( dataSet.ascendingValues.get( 2 ) ), 3L );
						} )
				);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	@PortedFromSearch5(original = "org.hibernate.search.test.query.facet.RangeFacetingTest.testRangeBelowExcludeLimit")
	void rangeLessThan(FieldTypeDescriptor<F, ?> fieldType, DataSet<F> dataSet) {
		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationKey<Map<Range<F>, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		assertThatQuery(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.range().field( fieldPath, fieldType.getJavaType() )
								.range( Range.lessThan( dataSet.ascendingValues.get( 2 ) ) )
						)
						.routing( dataSet.name )
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						containsExactly( c -> {
							c.accept( Range.canonical( null, dataSet.ascendingValues.get( 2 ) ), 2L );
						} )
				);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	@PortedFromSearch5(original = "org.hibernate.search.test.query.facet.RangeFacetingTest.testRangeAbove")
	void rangeAtLeast(FieldTypeDescriptor<F, ?> fieldType, DataSet<F> dataSet) {
		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationKey<Map<Range<F>, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		assertThatQuery(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.range().field( fieldPath, fieldType.getJavaType() )
								.range( Range.atLeast( dataSet.ascendingValues.get( 3 ) ) )
						)
						.routing( dataSet.name )
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						containsExactly( c -> {
							c.accept( Range.atLeast( dataSet.ascendingValues.get( 3 ) ), 4L );
						} )
				);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	@PortedFromSearch5(original = "org.hibernate.search.test.query.facet.RangeFacetingTest.testRangeAboveExcludeLimit")
	void rangeGreaterThan(FieldTypeDescriptor<F, ?> fieldType, DataSet<F> dataSet) {
		assumeNonCanonicalRangesSupported();

		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationKey<Map<Range<F>, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		assertThatQuery(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.range().field( fieldPath, fieldType.getJavaType() )
								.range( Range.greaterThan( dataSet.ascendingValues.get( 3 ) ) )
						)
						.routing( dataSet.name )
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						containsExactly( c -> {
							c.accept( Range.greaterThan( dataSet.ascendingValues.get( 3 ) ), 3L );
						} )
				);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	@PortedFromSearch5(
			original = "org.hibernate.search.test.query.facet.RangeFacetingTest.testRangeWithExcludeLimitsAtEachLevel")
	void rangesCanonical(FieldTypeDescriptor<F, ?> fieldType, DataSet<F> dataSet) {
		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationKey<Map<Range<F>, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		assertThatQuery(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.range().field( fieldPath, fieldType.getJavaType() )
								.ranges( Arrays.asList(
										Range.canonical( null, dataSet.ascendingValues.get( 3 ) ),
										Range.canonical( dataSet.ascendingValues.get( 3 ), dataSet.ascendingValues.get( 5 ) ),
										Range.canonical( dataSet.ascendingValues.get( 5 ), null )
								) )
						)
						.routing( dataSet.name )
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						containsExactly( c -> {
							c.accept( Range.canonical( null, dataSet.ascendingValues.get( 3 ) ), 3L );
							c.accept( Range.canonical( dataSet.ascendingValues.get( 3 ), dataSet.ascendingValues.get( 5 ) ),
									2L );
							c.accept( Range.canonical( dataSet.ascendingValues.get( 5 ), null ), 2L );
						} )
				);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	@PortedFromSearch5(original = "org.hibernate.search.test.query.facet.RangeFacetingTest.testRangeBelowMiddleAbove")
	void rangesBetweenIncludingAllBounds(FieldTypeDescriptor<F, ?> fieldType, DataSet<F> dataSet) {
		assumeNonCanonicalRangesSupported();

		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationKey<Map<Range<F>, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		assertThatQuery(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.range().field( fieldPath, fieldType.getJavaType() )
								.ranges( Arrays.asList(
										Range.between( null, RangeBoundInclusion.INCLUDED,
												dataSet.ascendingValues.get( 2 ), RangeBoundInclusion.INCLUDED ),
										Range.between( dataSet.ascendingValues.get( 3 ), RangeBoundInclusion.INCLUDED,
												dataSet.ascendingValues.get( 4 ), RangeBoundInclusion.INCLUDED ),
										Range.between( dataSet.ascendingValues.get( 5 ), RangeBoundInclusion.INCLUDED,
												null, RangeBoundInclusion.INCLUDED )
								) )
						)
						.routing( dataSet.name )
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						containsExactly( c -> {
							c.accept(
									Range.between( null, RangeBoundInclusion.INCLUDED,
											dataSet.ascendingValues.get( 2 ), RangeBoundInclusion.INCLUDED ),
									3L
							);
							c.accept(
									Range.between( dataSet.ascendingValues.get( 3 ), RangeBoundInclusion.INCLUDED,
											dataSet.ascendingValues.get( 4 ), RangeBoundInclusion.INCLUDED ),
									2L
							);
							c.accept(
									Range.between( dataSet.ascendingValues.get( 5 ), RangeBoundInclusion.INCLUDED,
											null, RangeBoundInclusion.INCLUDED ),
									2L
							);
						} )
				);


	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void rangesOverlap(FieldTypeDescriptor<F, ?> fieldType, DataSet<F> dataSet) {
		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationKey<Map<Range<F>, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		assertThatQuery(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.range().field( fieldPath, fieldType.getJavaType() )
								.ranges( Arrays.asList(
										Range.canonical( null, dataSet.ascendingValues.get( 3 ) ),
										Range.canonical( dataSet.ascendingValues.get( 1 ), dataSet.ascendingValues.get( 5 ) ),
										Range.canonical( dataSet.ascendingValues.get( 2 ), null )
								) )
						)
						.routing( dataSet.name )
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						containsExactly( c -> {
							c.accept( Range.canonical( null, dataSet.ascendingValues.get( 3 ) ), 3L );
							c.accept( Range.canonical( dataSet.ascendingValues.get( 1 ), dataSet.ascendingValues.get( 5 ) ),
									4L );
							c.accept( Range.canonical( dataSet.ascendingValues.get( 2 ), null ), 5L );
						} )
				);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	@PortedFromSearch5(original = "org.hibernate.search.test.query.facet.RangeFacetingTest.testRangeQueryWithNullToAndFrom")
	void rangeNull(FieldTypeDescriptor<F, ?> fieldType, DataSet<F> dataSet) {
		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		assertThatThrownBy( () -> index.createScope().aggregation().range()
				.field( fieldPath, fieldType.getJavaType() )
				.range( null )
		)
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "'range'" )
				.hasMessageContaining( "must not be null" );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void rangesNull(FieldTypeDescriptor<F, ?> fieldType, DataSet<F> dataSet) {
		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		assertThatThrownBy( () -> index.createScope().aggregation().range()
				.field( fieldPath, fieldType.getJavaType() )
				.ranges( null )
		)
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "'ranges'" )
				.hasMessageContaining( "must not be null" );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void rangesContainingNull(FieldTypeDescriptor<F, ?> fieldType, DataSet<F> dataSet) {
		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		assertThatThrownBy( () -> index.createScope().aggregation().range()
				.field( fieldPath, fieldType.getJavaType() )
				.ranges( Arrays.asList(
						Range.canonical( dataSet.ascendingValues.get( 0 ), dataSet.ascendingValues.get( 1 ) ),
						null
				) )
		)
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "'range'" )
				.hasMessageContaining( "must not be null" );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	@PortedFromSearch5(
			original = "org.hibernate.search.test.query.facet.RangeFacetingTest.testUnsupportedRangeParameterTypeThrowsException")
	void fieldTypeSuperClass(FieldTypeDescriptor<F, ?> fieldType, DataSet<F> dataSet) {
		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		Class<? super F> fieldTypeSuperClass = fieldType.getJavaType().getSuperclass();

		assertThatThrownBy( () -> index.createScope().aggregation().range()
				.field( fieldPath, fieldTypeSuperClass ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Invalid type for DSL arguments: '" + fieldTypeSuperClass.getName() + "'",
						"Expected '" + fieldType.getJavaType().getName() + "' or a subtype",
						"field '" + fieldPath + "'"
				);
	}

	/**
	 * Check that defining a predicate will affect the aggregation result.
	 */
	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	@PortedFromSearch5(
			original = "org.hibernate.search.test.query.facet.RangeFacetingTest.testRangeQueryForDoubleWithZeroCount")
	void predicate(FieldTypeDescriptor<F, ?> fieldType, DataSet<F> dataSet) {
		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationKey<Map<Range<F>, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		assertThatQuery(
				index.createScope().query()
						.where( f -> f.id()
								.matchingAny( Arrays.asList( dataSet.name + "_document_1", dataSet.name + "_document_5" ) ) )
						.aggregation( aggregationKey, f -> f.range().field( fieldPath, fieldType.getJavaType() )
								.range( null, dataSet.ascendingValues.get( 2 ) )
								.range( dataSet.ascendingValues.get( 2 ), dataSet.ascendingValues.get( 5 ) )
								.range( dataSet.ascendingValues.get( 5 ), null )
						)
						.routing( dataSet.name )
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						// Only document 1 and 5 should be taken into account by the aggregation
						containsExactly( c -> {
							c.accept( Range.canonical( null, dataSet.ascendingValues.get( 2 ) ), 1L );
							// Ranges with 0 matching documents should still be returned
							c.accept( Range.canonical( dataSet.ascendingValues.get( 2 ), dataSet.ascendingValues.get( 5 ) ),
									0L );
							c.accept( Range.canonical( dataSet.ascendingValues.get( 5 ), null ), 1L );
						} )
				);
	}

	/**
	 * Check that defining a limit and offset will <strong>not</strong> affect the aggregation result.
	 */
	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void limitAndOffset(FieldTypeDescriptor<F, ?> fieldType, DataSet<F> dataSet) {
		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationKey<Map<Range<F>, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		assertThatResult(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.range().field( fieldPath, fieldType.getJavaType() )
								.range( null, dataSet.ascendingValues.get( 2 ) )
								.range( dataSet.ascendingValues.get( 2 ), dataSet.ascendingValues.get( 5 ) )
								.range( dataSet.ascendingValues.get( 5 ), null )
						)
						.fetch( 3, 4 )
		)
				.aggregation(
						aggregationKey,
						// All documents should be taken into account by the aggregation, even those excluded by the limit/offset
						containsExactly( c -> {
							c.accept( Range.canonical( null, dataSet.ascendingValues.get( 2 ) ), 2L );
							c.accept( Range.canonical( dataSet.ascendingValues.get( 2 ), dataSet.ascendingValues.get( 5 ) ),
									3L );
							c.accept( Range.canonical( dataSet.ascendingValues.get( 5 ), null ), 2L );
						} )
				);
	}

	/**
	 * Check that defining overlapping ranges will work as expected.
	 */
	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void rangeOverlap(FieldTypeDescriptor<F, ?> fieldType, DataSet<F> dataSet) {
		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationKey<Map<Range<F>, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		assertThatQuery(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.range().field( fieldPath, fieldType.getJavaType() )
								.range( dataSet.ascendingValues.get( 0 ), null )
								.range( null, dataSet.ascendingValues.get( 2 ) )
								.range( dataSet.ascendingValues.get( 2 ), dataSet.ascendingValues.get( 5 ) )
								.range( null, null )
								.range( dataSet.ascendingValues.get( 0 ), dataSet.ascendingValues.get( 7 ) )
								.range( dataSet.ascendingValues.get( 5 ), null )
								.range( null, dataSet.ascendingValues.get( 6 ) )
						)
						.routing( dataSet.name )
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						containsExactly( c -> {
							c.accept( Range.canonical( dataSet.ascendingValues.get( 0 ), null ), 7L );
							c.accept( Range.canonical( null, dataSet.ascendingValues.get( 2 ) ), 2L );
							c.accept( Range.canonical( dataSet.ascendingValues.get( 2 ), dataSet.ascendingValues.get( 5 ) ),
									3L );
							c.accept( Range.canonical( null, null ), 7L );
							c.accept( Range.canonical( dataSet.ascendingValues.get( 0 ), dataSet.ascendingValues.get( 7 ) ),
									7L );
							c.accept( Range.canonical( dataSet.ascendingValues.get( 5 ), null ), 2L );
							c.accept( Range.canonical( null, dataSet.ascendingValues.get( 6 ) ), 6L );
						} )
				);
	}

	/**
	 * Check that, by default, ranges are returned in the order they are defined.
	 */
	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void order_asDefined(FieldTypeDescriptor<F, ?> fieldType, DataSet<F> dataSet) {
		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationKey<Map<Range<F>, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		assertThatQuery(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.range().field( fieldPath, fieldType.getJavaType() )
								.range( null, dataSet.ascendingValues.get( 2 ) )
								.range( dataSet.ascendingValues.get( 2 ), dataSet.ascendingValues.get( 5 ) )
								.range( dataSet.ascendingValues.get( 5 ), null )
						)
						.routing( dataSet.name )
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						containsExactly( c -> {
							c.accept( Range.canonical( null, dataSet.ascendingValues.get( 2 ) ), 2L );
							c.accept( Range.canonical( dataSet.ascendingValues.get( 2 ), dataSet.ascendingValues.get( 5 ) ),
									3L );
							c.accept( Range.canonical( dataSet.ascendingValues.get( 5 ), null ), 2L );
						} )
				);
	}

	private void assumeNonCanonicalRangesSupported() {
		assumeTrue(
				TckConfiguration.get().getBackendFeatures().nonCanonicalRangeInAggregations(),
				"Non-canonical ranges are not supported for aggregations with this backend"
		);
	}

	private SearchQueryOptionsStep<?, DocumentReference, ?, ?, ?> matchAllQuery() {
		return index.createScope().query().where( f -> f.matchAll() );
	}

	@SuppressWarnings("unchecked")
	private <K, V> Consumer<Map<K, V>> containsExactly(Consumer<BiConsumer<K, V>> expectationBuilder) {
		List<Map.Entry<K, V>> expected = new ArrayList<>();
		expectationBuilder.accept( (k, v) -> expected.add( entry( k, v ) ) );
		return actual -> assertThat( normalize( actual ) )
				.containsExactly( normalize( expected ).toArray( new Map.Entry[0] ) );
	}

	private static class DataSet<F> {
		final FieldTypeDescriptor<F, ?> fieldType;
		final String name;
		final List<F> ascendingValues;
		final List<F> documentFieldValues;

		private DataSet(FieldTypeDescriptor<F, ?> fieldType) {
			this.fieldType = fieldType;
			this.name = fieldType.getUniqueName();
			this.ascendingValues = fieldType.getAscendingUniqueTermValues().getSingle();
			this.documentFieldValues = ascendingValues.subList( 0, 7 );
		}

		private void init() {
			BulkIndexer indexer = index.bulkIndexer();
			for ( int i = 0; i < documentFieldValues.size(); i++ ) {
				F value = documentFieldValues.get( i );
				indexer.add( name + "_document_" + i, name, document -> {
					document.addValue( index.binding().fieldModels.get( fieldType ).reference, value );
					document.addValue( index.binding().fieldWithConverterModels.get( fieldType ).reference, value );
				} );
			}
			indexer.add( name + "_document_empty", name, document -> {} );
			indexer.join();
		}
	}

	private static class IndexBinding {
		final SimpleFieldModelsByType fieldModels;
		final SimpleFieldModelsByType fieldWithConverterModels;
		final SimpleFieldModelsByType fieldWithAggregationDisabledModels;

		IndexBinding(IndexSchemaElement root) {
			fieldModels = SimpleFieldModelsByType.mapAll( supportedFieldTypes, root,
					"", c -> c.aggregable( Aggregable.YES )
							.searchable( Searchable.NO ) // Range aggregations should not need this
			);
			fieldWithConverterModels = SimpleFieldModelsByType.mapAll( supportedFieldTypes, root,
					"converted_", c -> c.aggregable( Aggregable.YES )
							.dslConverter( ValueWrapper.class, ValueWrapper.toDocumentValueConverter() )
							.projectionConverter( ValueWrapper.class, ValueWrapper.fromDocumentValueConverter() )
			);
			fieldWithAggregationDisabledModels = SimpleFieldModelsByType.mapAll( supportedFieldTypes, root,
					"nonAggregable_", c -> c.aggregable( Aggregable.NO )
			);
		}
	}

}
