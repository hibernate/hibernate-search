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
import static org.junit.Assume.assumeTrue;

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
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModelsByType;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.ValueWrapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.data.Range;
import org.hibernate.search.util.common.data.RangeBoundInclusion;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Tests behavior specific to the range aggregation on supported field types.
 * <p>
 * Behavior common to all single-field aggregations is tested in {@link SingleFieldAggregationBaseIT}.
 */
@RunWith(Parameterized.class)
public class RangeAggregationSpecificsIT<F> {

	private static final String AGGREGATION_NAME = "aggregationName";

	private static Set<FieldTypeDescriptor<?>> supportedFieldTypes;
	private static List<DataSet<?>> dataSets;

	@Parameterized.Parameters(name = "{0}")
	public static Object[][] parameters() {
		supportedFieldTypes = new LinkedHashSet<>();
		dataSets = new ArrayList<>();
		List<Object[]> parameters = new ArrayList<>();
		AggregationDescriptor aggregationDescriptor = RangeAggregationDescriptor.INSTANCE;
		for ( FieldTypeDescriptor<?> fieldType : FieldTypeDescriptor.getAll() ) {
			if ( aggregationDescriptor.getSingleFieldAggregationExpectations( fieldType ).isSupported() ) {
				supportedFieldTypes.add( fieldType );
				DataSet<?> dataSet = new DataSet<>( fieldType );
				dataSets.add( dataSet );
				parameters.add( new Object[] { fieldType, dataSet } );
			}
		}
		return parameters.toArray( new Object[0][] );
	}

	@ClassRule
	public static final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@BeforeClass
	public static void setup() {
		setupHelper.start().withIndex( index ).setup();

		for ( DataSet<?> dataSet : dataSets ) {
			dataSet.init();
		}
	}

	private final FieldTypeDescriptor<F> fieldType;
	private final DataSet<F> dataSet;
	private final List<F> ascendingValues;

	public RangeAggregationSpecificsIT(FieldTypeDescriptor<F> fieldType, DataSet<F> dataSet) {
		this.fieldType = fieldType;
		this.dataSet = dataSet;
		this.ascendingValues = dataSet.ascendingValues;
	}

	@Test
	@PortedFromSearch5(original = "org.hibernate.search.test.query.facet.RangeFacetingTest.testRangeBelow")
	public void rangeAtMost() {
		assumeNonCanonicalRangesSupported();

		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationKey<Map<Range<F>, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		assertThatQuery(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.range().field( fieldPath, fieldType.getJavaType() )
								.range( Range.atMost( ascendingValues.get( 2 ) ) )
						)
						.routing( dataSet.name )
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						containsExactly( c -> {
							c.accept( Range.atMost( ascendingValues.get( 2 ) ), 3L );
						} )
				);
	}

	@Test
	@PortedFromSearch5(original = "org.hibernate.search.test.query.facet.RangeFacetingTest.testRangeBelowExcludeLimit")
	public void rangeLessThan() {
		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationKey<Map<Range<F>, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		assertThatQuery(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.range().field( fieldPath, fieldType.getJavaType() )
								.range( Range.lessThan( ascendingValues.get( 2 ) ) )
						)
						.routing( dataSet.name )
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						containsExactly( c -> {
							c.accept( Range.canonical( null, ascendingValues.get( 2 ) ), 2L );
						} )
				);
	}

	@Test
	@PortedFromSearch5(original = "org.hibernate.search.test.query.facet.RangeFacetingTest.testRangeAbove")
	public void rangeAtLeast() {
		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationKey<Map<Range<F>, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		assertThatQuery(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.range().field( fieldPath, fieldType.getJavaType() )
								.range( Range.atLeast( ascendingValues.get( 3 ) ) )
						)
						.routing( dataSet.name )
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						containsExactly( c -> {
							c.accept( Range.atLeast( ascendingValues.get( 3 ) ), 4L );
						} )
				);
	}

	@Test
	@PortedFromSearch5(original = "org.hibernate.search.test.query.facet.RangeFacetingTest.testRangeAboveExcludeLimit")
	public void rangeGreaterThan() {
		assumeNonCanonicalRangesSupported();

		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationKey<Map<Range<F>, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		assertThatQuery(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.range().field( fieldPath, fieldType.getJavaType() )
								.range( Range.greaterThan( ascendingValues.get( 3 ) ) )
						)
						.routing( dataSet.name )
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						containsExactly( c -> {
							c.accept( Range.greaterThan( ascendingValues.get( 3 ) ), 3L );
						} )
				);
	}

	@Test
	@PortedFromSearch5(
			original = "org.hibernate.search.test.query.facet.RangeFacetingTest.testRangeWithExcludeLimitsAtEachLevel")
	public void rangesCanonical() {
		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationKey<Map<Range<F>, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		assertThatQuery(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.range().field( fieldPath, fieldType.getJavaType() )
								.ranges( Arrays.asList(
										Range.canonical( null, ascendingValues.get( 3 ) ),
										Range.canonical( ascendingValues.get( 3 ), ascendingValues.get( 5 ) ),
										Range.canonical( ascendingValues.get( 5 ), null )
								) )
						)
						.routing( dataSet.name )
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						containsExactly( c -> {
							c.accept( Range.canonical( null, ascendingValues.get( 3 ) ), 3L );
							c.accept( Range.canonical( ascendingValues.get( 3 ), ascendingValues.get( 5 ) ), 2L );
							c.accept( Range.canonical( ascendingValues.get( 5 ), null ), 2L );
						} )
				);
	}

	@Test
	@PortedFromSearch5(original = "org.hibernate.search.test.query.facet.RangeFacetingTest.testRangeBelowMiddleAbove")
	public void rangesBetweenIncludingAllBounds() {
		assumeNonCanonicalRangesSupported();

		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationKey<Map<Range<F>, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		assertThatQuery(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.range().field( fieldPath, fieldType.getJavaType() )
								.ranges( Arrays.asList(
										Range.between( null, RangeBoundInclusion.INCLUDED,
												ascendingValues.get( 2 ), RangeBoundInclusion.INCLUDED ),
										Range.between( ascendingValues.get( 3 ), RangeBoundInclusion.INCLUDED,
												ascendingValues.get( 4 ), RangeBoundInclusion.INCLUDED ),
										Range.between( ascendingValues.get( 5 ), RangeBoundInclusion.INCLUDED,
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
											ascendingValues.get( 2 ), RangeBoundInclusion.INCLUDED ),
									3L
							);
							c.accept(
									Range.between( ascendingValues.get( 3 ), RangeBoundInclusion.INCLUDED,
											ascendingValues.get( 4 ), RangeBoundInclusion.INCLUDED ),
									2L
							);
							c.accept(
									Range.between( ascendingValues.get( 5 ), RangeBoundInclusion.INCLUDED,
											null, RangeBoundInclusion.INCLUDED ),
									2L
							);
						} )
				);


	}

	@Test
	public void rangesOverlap() {
		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationKey<Map<Range<F>, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		assertThatQuery(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.range().field( fieldPath, fieldType.getJavaType() )
								.ranges( Arrays.asList(
										Range.canonical( null, ascendingValues.get( 3 ) ),
										Range.canonical( ascendingValues.get( 1 ), ascendingValues.get( 5 ) ),
										Range.canonical( ascendingValues.get( 2 ), null )
								) )
						)
						.routing( dataSet.name )
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						containsExactly( c -> {
							c.accept( Range.canonical( null, ascendingValues.get( 3 ) ), 3L );
							c.accept( Range.canonical( ascendingValues.get( 1 ), ascendingValues.get( 5 ) ), 4L );
							c.accept( Range.canonical( ascendingValues.get( 2 ), null ), 5L );
						} )
				);
	}

	@Test
	@PortedFromSearch5(original = "org.hibernate.search.test.query.facet.RangeFacetingTest.testRangeQueryWithNullToAndFrom")
	public void rangeNull() {
		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		assertThatThrownBy( () -> index.createScope().aggregation().range()
				.field( fieldPath, fieldType.getJavaType() )
				.range( null )
		)
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "'range'" )
				.hasMessageContaining( "must not be null" );
	}

	@Test
	public void rangesNull() {
		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		assertThatThrownBy( () -> index.createScope().aggregation().range()
				.field( fieldPath, fieldType.getJavaType() )
				.ranges( null )
		)
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "'ranges'" )
				.hasMessageContaining( "must not be null" );
	}

	@Test
	public void rangesContainingNull() {
		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		assertThatThrownBy( () -> index.createScope().aggregation().range()
				.field( fieldPath, fieldType.getJavaType() )
				.ranges( Arrays.asList(
						Range.canonical( ascendingValues.get( 0 ), ascendingValues.get( 1 ) ),
						null
				) )
		)
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "'range'" )
				.hasMessageContaining( "must not be null" );
	}

	@Test
	@PortedFromSearch5(
			original = "org.hibernate.search.test.query.facet.RangeFacetingTest.testUnsupportedRangeParameterTypeThrowsException")
	public void fieldTypeSuperClass() {
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
	@Test
	@PortedFromSearch5(
			original = "org.hibernate.search.test.query.facet.RangeFacetingTest.testRangeQueryForDoubleWithZeroCount")
	public void predicate() {
		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationKey<Map<Range<F>, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		assertThatQuery(
				index.createScope().query()
						.where( f -> f.id()
								.matchingAny( Arrays.asList( dataSet.name + "_document_1", dataSet.name + "_document_5" ) ) )
						.aggregation( aggregationKey, f -> f.range().field( fieldPath, fieldType.getJavaType() )
								.range( null, ascendingValues.get( 2 ) )
								.range( ascendingValues.get( 2 ), ascendingValues.get( 5 ) )
								.range( ascendingValues.get( 5 ), null )
						)
						.routing( dataSet.name )
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						// Only document 1 and 5 should be taken into account by the aggregation
						containsExactly( c -> {
							c.accept( Range.canonical( null, ascendingValues.get( 2 ) ), 1L );
							// Ranges with 0 matching documents should still be returned
							c.accept( Range.canonical( ascendingValues.get( 2 ), ascendingValues.get( 5 ) ), 0L );
							c.accept( Range.canonical( ascendingValues.get( 5 ), null ), 1L );
						} )
				);
	}

	/**
	 * Check that defining a limit and offset will <strong>not</strong> affect the aggregation result.
	 */
	@Test
	public void limitAndOffset() {
		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationKey<Map<Range<F>, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		assertThatResult(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.range().field( fieldPath, fieldType.getJavaType() )
								.range( null, ascendingValues.get( 2 ) )
								.range( ascendingValues.get( 2 ), ascendingValues.get( 5 ) )
								.range( ascendingValues.get( 5 ), null )
						)
						.fetch( 3, 4 )
		)
				.aggregation(
						aggregationKey,
						// All documents should be taken into account by the aggregation, even those excluded by the limit/offset
						containsExactly( c -> {
							c.accept( Range.canonical( null, ascendingValues.get( 2 ) ), 2L );
							c.accept( Range.canonical( ascendingValues.get( 2 ), ascendingValues.get( 5 ) ), 3L );
							c.accept( Range.canonical( ascendingValues.get( 5 ), null ), 2L );
						} )
				);
	}

	/**
	 * Check that defining overlapping ranges will work as expected.
	 */
	@Test
	public void rangeOverlap() {
		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationKey<Map<Range<F>, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		assertThatQuery(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.range().field( fieldPath, fieldType.getJavaType() )
								.range( ascendingValues.get( 0 ), null )
								.range( null, ascendingValues.get( 2 ) )
								.range( ascendingValues.get( 2 ), ascendingValues.get( 5 ) )
								.range( null, null )
								.range( ascendingValues.get( 0 ), ascendingValues.get( 7 ) )
								.range( ascendingValues.get( 5 ), null )
								.range( null, ascendingValues.get( 6 ) )
						)
						.routing( dataSet.name )
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						containsExactly( c -> {
							c.accept( Range.canonical( ascendingValues.get( 0 ), null ), 7L );
							c.accept( Range.canonical( null, ascendingValues.get( 2 ) ), 2L );
							c.accept( Range.canonical( ascendingValues.get( 2 ), ascendingValues.get( 5 ) ), 3L );
							c.accept( Range.canonical( null, null ), 7L );
							c.accept( Range.canonical( ascendingValues.get( 0 ), ascendingValues.get( 7 ) ), 7L );
							c.accept( Range.canonical( ascendingValues.get( 5 ), null ), 2L );
							c.accept( Range.canonical( null, ascendingValues.get( 6 ) ), 6L );
						} )
				);
	}

	/**
	 * Check that, by default, ranges are returned in the order they are defined.
	 */
	@Test
	public void order_asDefined() {
		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationKey<Map<Range<F>, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		assertThatQuery(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.range().field( fieldPath, fieldType.getJavaType() )
								.range( null, ascendingValues.get( 2 ) )
								.range( ascendingValues.get( 2 ), ascendingValues.get( 5 ) )
								.range( ascendingValues.get( 5 ), null )
						)
						.routing( dataSet.name )
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						containsExactly( c -> {
							c.accept( Range.canonical( null, ascendingValues.get( 2 ) ), 2L );
							c.accept( Range.canonical( ascendingValues.get( 2 ), ascendingValues.get( 5 ) ), 3L );
							c.accept( Range.canonical( ascendingValues.get( 5 ), null ), 2L );
						} )
				);
	}

	private void assumeNonCanonicalRangesSupported() {
		assumeTrue(
				"Non-canonical ranges are not supported for aggregations with this backend",
				TckConfiguration.get().getBackendFeatures().nonCanonicalRangeInAggregations()
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
		final FieldTypeDescriptor<F> fieldType;
		final String name;
		final List<F> ascendingValues;
		final List<F> documentFieldValues;

		private DataSet(FieldTypeDescriptor<F> fieldType) {
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
