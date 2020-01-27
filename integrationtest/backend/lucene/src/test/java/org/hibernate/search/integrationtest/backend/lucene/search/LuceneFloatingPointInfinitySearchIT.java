/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import java.util.Map;
import java.util.function.Function;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.StandardFieldMapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.data.Range;
import org.hibernate.search.util.common.data.RangeBoundInclusion;
import org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class LuceneFloatingPointInfinitySearchIT<F> {

	private static final String INDEX_NAME = "IndexName";

	private static final String AGGREGATION_NAME = "aggregationName";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	private IndexMapping indexMapping;
	private StubMappingIndexManager indexManager;

	@Before
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
	@TestForIssue(jiraKey = "HSEARCH-3685")
	public void float_infinityIncluded() {
		String fieldPath = indexMapping.floatFieldModel.relativeFieldName;

		AggregationKey<Map<Range<Float>, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		SearchResultAssert.assertThat(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.range().field( fieldPath, Float.class )
								.range( Range.canonical( null, 0f ) )
								.range( Range.canonical( 0f, null ) )
								.range( Range.all() )
						)
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						a -> assertThat( a ).containsExactly(
								entry( Range.canonical( null, 0f ), 1L ),
								entry( Range.canonical( 0f, null ), 2L ),
								entry( Range.<Float>all(), 3L )
						)
				);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3685")
	public void float_infinityExcluded() {
		String fieldPath = indexMapping.floatFieldModel.relativeFieldName;

		AggregationKey<Map<Range<Float>, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		SearchResultAssert.assertThat(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.range().field( fieldPath, Float.class )
								.range( Range.between( null, RangeBoundInclusion.EXCLUDED,
										0f, RangeBoundInclusion.EXCLUDED ) )
								.range( Range.between( 0f, RangeBoundInclusion.INCLUDED,
										null, RangeBoundInclusion.EXCLUDED ) )
						)
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						a -> assertThat( a ).containsExactly(
								entry(
										Range.between( null, RangeBoundInclusion.EXCLUDED,
												0f, RangeBoundInclusion.EXCLUDED ),
										0L
								),
								entry(
										Range.between( 0f, RangeBoundInclusion.INCLUDED,
												null, RangeBoundInclusion.EXCLUDED ),
										1L
								)
						)
				);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3685")
	public void double_infinityIncluded() {
		String fieldPath = indexMapping.doubleFieldModel.relativeFieldName;

		AggregationKey<Map<Range<Double>, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		SearchResultAssert.assertThat(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.range().field( fieldPath, Double.class )
								.range( Range.canonical( null, 0d ) )
								.range( Range.canonical( 0d, null ) )
								.range( Range.all() )
						)
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						a -> assertThat( a ).containsExactly(
								entry( Range.canonical( null, 0d ), 1L ),
								entry( Range.canonical( 0d, null ), 2L ),
								entry( Range.<Double>all(), 3L )
						)
				);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3685")
	public void double_infinityExcluded() {
		String fieldPath = indexMapping.doubleFieldModel.relativeFieldName;

		AggregationKey<Map<Range<Double>, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		SearchResultAssert.assertThat(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.range().field( fieldPath, Double.class )
								.range( Range.between( null, RangeBoundInclusion.EXCLUDED,
										0d, RangeBoundInclusion.EXCLUDED ) )
								.range( Range.between( 0d, RangeBoundInclusion.INCLUDED,
										null, RangeBoundInclusion.EXCLUDED ) )
						)
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						a -> assertThat( a ).containsExactly(
								entry(
										Range.between( null, RangeBoundInclusion.EXCLUDED,
												0d, RangeBoundInclusion.EXCLUDED ),
										0L
								),
								entry(
										Range.between( 0d, RangeBoundInclusion.INCLUDED,
												null, RangeBoundInclusion.EXCLUDED ),
										1L
								)
						)
				);
	}

	private SearchQueryOptionsStep<?, DocumentReference, ?, ?, ?> matchAllQuery() {
		return indexManager.createScope().query().where( f -> f.matchAll() );
	}

	private void initData() {
		IndexIndexingPlan<? extends DocumentElement> plan = indexManager.createIndexingPlan();
		plan.add( referenceProvider( "negative-infinity" ), document -> {
			document.addValue( indexMapping.floatFieldModel.reference, Float.NEGATIVE_INFINITY );
			document.addValue( indexMapping.doubleFieldModel.reference, Double.NEGATIVE_INFINITY );
		} );
		plan.add( referenceProvider( "zero" ), document -> {
			document.addValue( indexMapping.floatFieldModel.reference, 0f );
			document.addValue( indexMapping.doubleFieldModel.reference, 0d );
		} );
		plan.add( referenceProvider( "positive-infinity" ), document -> {
			document.addValue( indexMapping.floatFieldModel.reference, Float.POSITIVE_INFINITY );
			document.addValue( indexMapping.doubleFieldModel.reference, Double.POSITIVE_INFINITY );
		} );
		plan.execute().join();

		// Check that all documents are searchable
		SearchResultAssert.assertThat(
				indexManager.createScope().query()
						.where( f -> f.matchAll() )
						.toQuery()
		)
				.hasTotalHitCount( 3 );
	}

	private static class IndexMapping {
		final FieldModel<Float> floatFieldModel;
		final FieldModel<Double> doubleFieldModel;

		IndexMapping(IndexSchemaElement root) {
			floatFieldModel = FieldModel.mapper( f -> f.asFloat().aggregable( Aggregable.YES ) )
					.map( root, "float" );
			doubleFieldModel = FieldModel.mapper( f -> f.asDouble().aggregable( Aggregable.YES ) )
					.map( root, "double" );
		}
	}

	private static class FieldModel<F> {
		static <F> StandardFieldMapper<F, FieldModel<F>> mapper(
				Function<IndexFieldTypeFactory, StandardIndexFieldTypeOptionsStep<?, F>> initialConfiguration) {
			return StandardFieldMapper.of(
					initialConfiguration,
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
