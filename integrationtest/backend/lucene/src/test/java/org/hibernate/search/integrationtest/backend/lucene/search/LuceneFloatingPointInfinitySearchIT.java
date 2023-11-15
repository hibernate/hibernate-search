/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import java.util.Map;
import java.util.function.Function;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldMapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.data.Range;
import org.hibernate.search.util.common.data.RangeBoundInclusion;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class LuceneFloatingPointInfinitySearchIT<F> {

	private static final String AGGREGATION_NAME = "aggregationName";

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@BeforeEach
	void setup() {
		setupHelper.start().withIndex( index ).setup();
		initData();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3685")
	void float_infinityIncluded() {
		String fieldPath = index.binding().floatFieldModel.relativeFieldName;

		AggregationKey<Map<Range<Float>, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		assertThatQuery(
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
	void float_infinityExcluded() {
		String fieldPath = index.binding().floatFieldModel.relativeFieldName;

		AggregationKey<Map<Range<Float>, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		assertThatQuery(
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
	void double_infinityIncluded() {
		String fieldPath = index.binding().doubleFieldModel.relativeFieldName;

		AggregationKey<Map<Range<Double>, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		assertThatQuery(
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
	void double_infinityExcluded() {
		String fieldPath = index.binding().doubleFieldModel.relativeFieldName;

		AggregationKey<Map<Range<Double>, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		assertThatQuery(
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
		return index.createScope().query().where( f -> f.matchAll() );
	}

	private void initData() {
		index.bulkIndexer()
				.add( "negative-infinity", document -> {
					document.addValue( index.binding().floatFieldModel.reference, Float.NEGATIVE_INFINITY );
					document.addValue( index.binding().doubleFieldModel.reference, Double.NEGATIVE_INFINITY );
				} )
				.add( "zero", document -> {
					document.addValue( index.binding().floatFieldModel.reference, 0f );
					document.addValue( index.binding().doubleFieldModel.reference, 0d );
				} )
				.add( "positive-infinity", document -> {
					document.addValue( index.binding().floatFieldModel.reference, Float.POSITIVE_INFINITY );
					document.addValue( index.binding().doubleFieldModel.reference, Double.POSITIVE_INFINITY );
				} )
				.join();
	}

	private static class IndexBinding {
		final FieldModel<Float> floatFieldModel;
		final FieldModel<Double> doubleFieldModel;

		IndexBinding(IndexSchemaElement root) {
			floatFieldModel = FieldModel.mapper( f -> f.asFloat().aggregable( Aggregable.YES ) )
					.map( root, "float" );
			doubleFieldModel = FieldModel.mapper( f -> f.asDouble().aggregable( Aggregable.YES ) )
					.map( root, "double" );
		}
	}

	private static class FieldModel<F> {
		static <F> SimpleFieldMapper<F, ?, FieldModel<F>> mapper(
				Function<IndexFieldTypeFactory, StandardIndexFieldTypeOptionsStep<?, F>> initialConfiguration) {
			return SimpleFieldMapper.of(
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
