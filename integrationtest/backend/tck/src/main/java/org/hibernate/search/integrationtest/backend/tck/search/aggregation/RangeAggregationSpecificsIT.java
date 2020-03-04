/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.aggregation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.hibernate.search.util.impl.integrationtest.common.NormalizationUtils.normalize;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.referenceProvider;
import static org.junit.Assume.assumeTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.operations.AggregationDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.operations.RangeAggregationDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.StandardFieldMapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.ValueWrapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.data.Range;
import org.hibernate.search.util.common.data.RangeBoundInclusion;
import org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingIndexManager;
import org.hibernate.search.util.impl.test.SubTest;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;
import org.hibernate.search.util.impl.test.singleinstance.BeforeAll;
import org.hibernate.search.util.impl.test.singleinstance.InstanceRule;
import org.hibernate.search.util.impl.test.singleinstance.SingleInstanceRunnerWithParameters;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Tests behavior specific to the range aggregation on supported field types.
 * <p>
 * Behavior common to all single-field aggregations is tested in {@link SingleFieldAggregationBaseIT}.
 */
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(SingleInstanceRunnerWithParameters.Factory.class)
public class RangeAggregationSpecificsIT<F> {

	private static final String INDEX_NAME = "IndexName";

	private static final String AGGREGATION_NAME = "aggregationName";

	@Parameterized.Parameters(name = "{0}")
	public static Object[][] supportedTypes() {
		List<Object[]> combinations = new ArrayList<>();
		AggregationDescriptor aggregationDescriptor = new RangeAggregationDescriptor();
		for ( FieldTypeDescriptor<?> fieldTypeDescriptor : FieldTypeDescriptor.getAll() ) {
			if ( aggregationDescriptor.getSingleFieldAggregationExpectations( fieldTypeDescriptor ).isSupported() ) {
				combinations.add( new Object[] {
						fieldTypeDescriptor
				} );
			}
		}
		return combinations.toArray( new Object[0][] );
	}

	@InstanceRule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	private final FieldTypeDescriptor<F> typeDescriptor;
	private final List<F> ascendingValues;

	private IndexMapping indexMapping;
	private StubMappingIndexManager indexManager;

	public RangeAggregationSpecificsIT(FieldTypeDescriptor<F> typeDescriptor) {
		this.typeDescriptor = typeDescriptor;
		this.ascendingValues = typeDescriptor.getAscendingUniqueTermValues();
	}

	@BeforeAll
	public void setup() {
		setupHelper.start()
				.withIndex(
						INDEX_NAME,
						ctx -> this.indexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();

		initData();
	}

	@Test
	@PortedFromSearch5(original = "org.hibernate.search.test.query.facet.RangeFacetingTest.testRangeBelow")
	public void rangeAtMost() {
		assumeNonCanonicalRangesSupported();

		String fieldPath = indexMapping.fieldModel.relativeFieldName;

		AggregationKey<Map<Range<F>, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		SearchResultAssert.assertThat(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.range().field( fieldPath, typeDescriptor.getJavaType() )
								.range( Range.atMost( ascendingValues.get( 2 ) ) )
						)
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
		String fieldPath = indexMapping.fieldModel.relativeFieldName;

		AggregationKey<Map<Range<F>, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		SearchResultAssert.assertThat(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.range().field( fieldPath, typeDescriptor.getJavaType() )
								.range( Range.lessThan( ascendingValues.get( 2 ) ) )
						)
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
		String fieldPath = indexMapping.fieldModel.relativeFieldName;

		AggregationKey<Map<Range<F>, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		SearchResultAssert.assertThat(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.range().field( fieldPath, typeDescriptor.getJavaType() )
								.range( Range.atLeast( ascendingValues.get( 3 ) ) )
						)
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

		String fieldPath = indexMapping.fieldModel.relativeFieldName;

		AggregationKey<Map<Range<F>, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		SearchResultAssert.assertThat(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.range().field( fieldPath, typeDescriptor.getJavaType() )
								.range( Range.greaterThan( ascendingValues.get( 3 ) ) )
						)
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
	@PortedFromSearch5(original = "org.hibernate.search.test.query.facet.RangeFacetingTest.testRangeWithExcludeLimitsAtEachLevel")
	public void rangesCanonical() {
		String fieldPath = indexMapping.fieldModel.relativeFieldName;

		AggregationKey<Map<Range<F>, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		SearchResultAssert.assertThat(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.range().field( fieldPath, typeDescriptor.getJavaType() )
								.ranges( Arrays.asList(
										Range.canonical( null, ascendingValues.get( 3 ) ),
										Range.canonical( ascendingValues.get( 3 ), ascendingValues.get( 5 ) ),
										Range.canonical( ascendingValues.get( 5 ), null )
								) )
						)
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

		String fieldPath = indexMapping.fieldModel.relativeFieldName;

		AggregationKey<Map<Range<F>, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		SearchResultAssert.assertThat(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.range().field( fieldPath, typeDescriptor.getJavaType() )
								.ranges( Arrays.asList(
										Range.between( null, RangeBoundInclusion.INCLUDED,
												ascendingValues.get( 2 ), RangeBoundInclusion.INCLUDED ),
										Range.between( ascendingValues.get( 3 ), RangeBoundInclusion.INCLUDED,
												ascendingValues.get( 4 ), RangeBoundInclusion.INCLUDED ),
										Range.between( ascendingValues.get( 5 ), RangeBoundInclusion.INCLUDED,
												null, RangeBoundInclusion.INCLUDED )
								) )
						)
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
		String fieldPath = indexMapping.fieldModel.relativeFieldName;

		AggregationKey<Map<Range<F>, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		SearchResultAssert.assertThat(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.range().field( fieldPath, typeDescriptor.getJavaType() )
								.ranges( Arrays.asList(
										Range.canonical( null, ascendingValues.get( 3 ) ),
										Range.canonical( ascendingValues.get( 1 ), ascendingValues.get( 5 ) ),
										Range.canonical( ascendingValues.get( 2 ), null )
								) )
						)
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
		String fieldPath = indexMapping.fieldModel.relativeFieldName;

		SubTest.expectException( () ->
				indexManager.createScope().aggregation().range()
						.field( fieldPath, typeDescriptor.getJavaType() )
						.range( null )
		)
				.assertThrown()
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "'range'" )
				.hasMessageContaining( "must not be null" );
	}

	@Test
	public void rangesNull() {
		String fieldPath = indexMapping.fieldModel.relativeFieldName;

		SubTest.expectException( () ->
				indexManager.createScope().aggregation().range()
						.field( fieldPath, typeDescriptor.getJavaType() )
						.ranges( null )
		)
				.assertThrown()
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "'ranges'" )
				.hasMessageContaining( "must not be null" );
	}

	@Test
	public void rangesContainingNull() {
		String fieldPath = indexMapping.fieldModel.relativeFieldName;

		SubTest.expectException( () ->
				indexManager.createScope().aggregation().range()
						.field( fieldPath, typeDescriptor.getJavaType() )
						.ranges( Arrays.asList(
								Range.canonical( ascendingValues.get( 0 ), ascendingValues.get( 1 ) ),
								null
						) )
		)
				.assertThrown()
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "'range'" )
				.hasMessageContaining( "must not be null" );
	}

	@Test
	@PortedFromSearch5(original = "org.hibernate.search.test.query.facet.RangeFacetingTest.testUnsupportedRangeParameterTypeThrowsException")
	public void superClassFieldType() {
		String fieldPath = indexMapping.fieldModel.relativeFieldName;

		SubTest.expectException( () ->
				indexManager.createScope().aggregation().range()
						.field( fieldPath, typeDescriptor.getJavaType().getSuperclass() )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Invalid type" )
				.hasMessageContaining( "for aggregation on field" )
				.hasMessageContaining( "'" + fieldPath + "'" );
	}

	/**
	 * Check that defining a predicate will affect the aggregation result.
	 */
	@Test
	@PortedFromSearch5(original = "org.hibernate.search.test.query.facet.RangeFacetingTest.testRangeQueryForDoubleWithZeroCount")
	public void predicate() {
		String fieldPath = indexMapping.fieldModel.relativeFieldName;

		AggregationKey<Map<Range<F>, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		SearchResultAssert.assertThat(
				indexManager.createScope().query()
						.where( f -> f.id().matchingAny( Arrays.asList( "document_1", "document_5" ) ) )
						.aggregation( aggregationKey, f -> f.range().field( fieldPath, typeDescriptor.getJavaType() )
								.range( null, ascendingValues.get( 2 ) )
								.range( ascendingValues.get( 2 ), ascendingValues.get( 5 ) )
								.range( ascendingValues.get( 5 ), null )
						)
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
		String fieldPath = indexMapping.fieldModel.relativeFieldName;

		AggregationKey<Map<Range<F>, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		SearchResultAssert.assertThat(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.range().field( fieldPath, typeDescriptor.getJavaType() )
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
		String fieldPath = indexMapping.fieldModel.relativeFieldName;

		AggregationKey<Map<Range<F>, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		SearchResultAssert.assertThat(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.range().field( fieldPath, typeDescriptor.getJavaType() )
								.range( ascendingValues.get( 0 ), null )
								.range( null, ascendingValues.get( 2 ) )
								.range( ascendingValues.get( 2 ), ascendingValues.get( 5 ) )
								.range( null, null )
								.range( ascendingValues.get( 0 ), ascendingValues.get( 7 ) )
								.range( ascendingValues.get( 5 ), null )
								.range( null, ascendingValues.get( 6 ) )
						)
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
		String fieldPath = indexMapping.fieldModel.relativeFieldName;

		AggregationKey<Map<Range<F>, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		SearchResultAssert.assertThat(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.range().field( fieldPath, typeDescriptor.getJavaType() )
								.range( null, ascendingValues.get( 2 ) )
								.range( ascendingValues.get( 2 ), ascendingValues.get( 5 ) )
								.range( ascendingValues.get( 5 ), null )
						)
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
		return indexManager.createScope().query().where( f -> f.matchAll() );
	}

	private void initData() {
		List<F> documentFieldValues = ascendingValues.subList( 0, 7 );

		IndexIndexingPlan plan = indexManager.createIndexingPlan();
		for ( int i = 0; i < documentFieldValues.size(); i++ ) {
			F value = documentFieldValues.get( i );
			plan.add( referenceProvider( "document_" + i ), document -> {
				document.addValue( indexMapping.fieldModel.reference, value );
				document.addValue( indexMapping.fieldWithConverterModel.reference, value );
			} );
		}
		plan.add( referenceProvider( "document_empty" ), document -> { } );
		plan.execute().join();

		// Check that all documents are searchable
		SearchResultAssert.assertThat(
				indexManager.createScope().query()
						.where( f -> f.matchAll() )
						.toQuery()
		)
				.hasTotalHitCount( documentFieldValues.size() + 1 /* +1 for the empty document */ );
	}

	private FieldModel<F> mapField(IndexSchemaElement parent, String prefix,
			Consumer<StandardIndexFieldTypeOptionsStep<?, F>> additionalConfiguration) {
		return FieldModel.mapper( typeDescriptor )
				.map( parent, prefix + typeDescriptor.getUniqueName(), additionalConfiguration );
	}

	@SuppressWarnings("unchecked")
	private <K, V> Consumer<Map<K, V>> containsExactly(Consumer<BiConsumer<K, V>> expectationBuilder) {
		List<Map.Entry<K, V>> expected = new ArrayList<>();
		expectationBuilder.accept( (k, v) -> expected.add( entry( k, v ) ) );
		return actual -> assertThat( normalize( actual ) )
				.containsExactly( normalize( expected ).toArray( new Map.Entry[0] ) );
	}

	private class IndexMapping {
		final FieldModel<F> fieldModel;
		final FieldModel<F> fieldWithConverterModel;
		final FieldModel<F> fieldWithAggregationDisabledModel;

		IndexMapping(IndexSchemaElement root) {
			fieldModel = mapField(
					root, "",
					c -> c.aggregable( Aggregable.YES )
							.searchable( Searchable.NO ) // Range aggregations should not need this
			);
			fieldWithConverterModel = mapField(
					root, "converted_",
					c -> c.aggregable( Aggregable.YES )
							.dslConverter( ValueWrapper.class, ValueWrapper.toIndexFieldConverter() )
							.projectionConverter( ValueWrapper.class, ValueWrapper.fromIndexFieldConverter() )
			);
			fieldWithAggregationDisabledModel = mapField(
					root, "nonAggregable_",
					c -> c.aggregable( Aggregable.NO )
			);
		}
	}

	private static class FieldModel<F> {
		static <F> StandardFieldMapper<F, FieldModel<F>> mapper(FieldTypeDescriptor<F> typeDescriptor) {
			return StandardFieldMapper.of(
					typeDescriptor::configure,
					FieldModel::new
			);
		}

		final IndexFieldReference<F> reference;
		final String relativeFieldName;

		private FieldModel(IndexFieldReference<F> reference, String relativeFieldName) {
			this.reference = reference;
			this.relativeFieldName = relativeFieldName;
		}
	}

}
