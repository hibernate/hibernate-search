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
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;
import static org.junit.Assume.assumeTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.search.engine.backend.document.DocumentElement;
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
import org.hibernate.search.integrationtest.backend.tck.testsupport.operations.TermsAggregationDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.StandardFieldMapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.ValueWrapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.test.SubTest;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;
import org.hibernate.search.util.impl.test.singleinstance.BeforeAll;
import org.hibernate.search.util.impl.test.singleinstance.InstanceRule;
import org.hibernate.search.util.impl.test.singleinstance.SingleInstanceRunnerWithParameters;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Tests behavior specific to the terms aggregation on supported field types.
 * <p>
 * Behavior common to all single-field aggregations is tested in {@link SingleFieldAggregationBaseIT}.
 */
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(SingleInstanceRunnerWithParameters.Factory.class)
public class TermsAggregationSpecificsIT<F> {

	private static final String INDEX_NAME = "IndexName";

	private static final String AGGREGATION_NAME = "aggregationName";

	@Parameterized.Parameters(name = "{0}")
	public static Object[][] supportedTypes() {
		List<Object[]> combinations = new ArrayList<>();
		AggregationDescriptor aggregationDescriptor = new TermsAggregationDescriptor();
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
	private final List<F> valuesInAscendingOrder;
	private final List<F> valuesInDescendingOrder;
	private final List<F> valuesInAscendingDocumentCountOrder;
	private final List<F> valuesInDescendingDocumentCountOrder;
	private final Map<F, List<String>> documentIdPerTerm;

	private IndexMapping indexMapping;
	private StubMappingIndexManager indexManager;

	public TermsAggregationSpecificsIT(FieldTypeDescriptor<F> typeDescriptor) {
		this.typeDescriptor = typeDescriptor;
		this.documentIdPerTerm = new LinkedHashMap<>();

		this.valuesInAscendingOrder = typeDescriptor.getAscendingUniqueTermValues();

		this.valuesInDescendingOrder = new ArrayList<>( valuesInAscendingOrder );
		Collections.reverse( valuesInDescendingOrder );

		this.valuesInDescendingDocumentCountOrder = new ArrayList<>( valuesInAscendingOrder );
		/*
		 * Mess with the value order, because some tests would be pointless
		 * if the document count order was the same as (or the opposite of) the value order
		 */
		valuesInDescendingDocumentCountOrder.add( valuesInDescendingDocumentCountOrder.get( 0 ) );
		valuesInDescendingDocumentCountOrder.remove( 0 );
		valuesInDescendingDocumentCountOrder.add( valuesInDescendingDocumentCountOrder.get( 0 ) );
		valuesInDescendingDocumentCountOrder.remove( 0 );

		this.valuesInAscendingDocumentCountOrder = new ArrayList<>( valuesInDescendingDocumentCountOrder );
		Collections.reverse( valuesInAscendingDocumentCountOrder );

		// Simple dataset: strictly decreasing number of documents for each term
		int documentIdAsInteger = 0;
		int numberOfDocuments = valuesInDescendingDocumentCountOrder.size();
		for ( F value : valuesInDescendingDocumentCountOrder ) {
			ArrayList<String> documentIdsForTerm = new ArrayList<>();
			documentIdPerTerm.put( value, documentIdsForTerm );
			for ( int i = 0; i < numberOfDocuments; i++ ) {
				String documentId = "document_" + documentIdAsInteger;
				++documentIdAsInteger;
				documentIdsForTerm.add( documentId );
			}
			--numberOfDocuments;
		}
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
	public void superClassFieldType() {
		Class<? super F> superClass = typeDescriptor.getJavaType().getSuperclass();

		doTestSuperClassFieldType( superClass );
	}

	private <S> void doTestSuperClassFieldType(Class<S> superClass) {
		String fieldPath = indexMapping.fieldModel.relativeFieldName;

		AggregationKey<Map<S, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		SearchResultAssert.assertThat(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.terms().field( fieldPath, superClass ) )
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						// All documents should be mentioned in the aggregation, even those excluded by the limit/offset
						containsInAnyOrder( c -> {
							documentIdPerTerm.forEach( (key, value) -> c.accept( key, (long) value.size() ) );
						} )
				);
	}

	/**
	 * Check that defining a predicate will affect the aggregation result.
	 */
	@Test
	public void predicate() {
		String fieldPath = indexMapping.fieldModel.relativeFieldName;

		AggregationKey<Map<F, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		Map.Entry<F, List<String>> firstTermEntry = documentIdPerTerm.entrySet().iterator().next();

		SearchResultAssert.assertThat(
				indexManager.createScope().query()
						.predicate( f -> f.id()
								.matching( firstTermEntry.getValue().get( 0 ) )
								.matching( firstTermEntry.getValue().get( 1 ) )
						)
						.aggregation( aggregationKey, f -> f.terms().field( fieldPath, typeDescriptor.getJavaType() ) )
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						// Only document 0 should be taken into account by the aggregation
						containsInAnyOrder( c -> {
							c.accept( firstTermEntry.getKey(), 2L );
						} )
				);
	}

	/**
	 * Check that defining a limit and offset will <strong>not</strong> affect the aggregation result.
	 */
	@Test
	public void limitAndOffset() {
		String fieldPath = indexMapping.fieldModel.relativeFieldName;

		AggregationKey<Map<F, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		SearchResultAssert.assertThat(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.terms().field( fieldPath, typeDescriptor.getJavaType() ) )
						.fetch( 3, 4 )
		)
				.aggregation(
						aggregationKey,
						// All documents should be mentioned in the aggregation, even those excluded by the limit/offset
						containsInAnyOrder( c -> {
							documentIdPerTerm.forEach( (key, value) -> c.accept( key, (long) value.size() ) );
						} )
				);
	}

	@Test
	@PortedFromSearch5(original = "org.hibernate.search.test.query.facet.SimpleFacetingTest.testDefaultSortOrderIsCount")
	public void order_default() {
		String fieldPath = indexMapping.fieldModel.relativeFieldName;

		AggregationKey<Map<F, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		SearchResultAssert.assertThat(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.terms().field( fieldPath, typeDescriptor.getJavaType() ) )
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						// The result should present buckets with decreasing term count
						containsExactly( c -> {
							for ( F value : valuesInDescendingDocumentCountOrder ) {
								c.accept( value, (long) documentIdPerTerm.get( value ).size() );
							}
						} )
				);
	}

	@Test
	@PortedFromSearch5(original = "org.hibernate.search.test.query.facet.SimpleFacetingTest.testCountSortOrderDesc")
	public void orderByCountDescending() {
		String fieldPath = indexMapping.fieldModel.relativeFieldName;

		AggregationKey<Map<F, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		SearchResultAssert.assertThat(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.terms().field( fieldPath, typeDescriptor.getJavaType() )
								.orderByCountDescending()
						)
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						// The result should present buckets with decreasing term count
						containsExactly( c -> {
							for ( F value : valuesInDescendingDocumentCountOrder ) {
								c.accept( value, (long) documentIdPerTerm.get( value ).size() );
							}
						} )
				);
	}

	@Test
	@PortedFromSearch5(original = "org.hibernate.search.test.query.facet.SimpleFacetingTest.testCountSortOrderAsc")
	public void orderByCountAscending() {
		assumeNonDefaultOrdersSupported();

		String fieldPath = indexMapping.fieldModel.relativeFieldName;

		AggregationKey<Map<F, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		SearchResultAssert.assertThat(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.terms().field( fieldPath, typeDescriptor.getJavaType() )
								.orderByCountAscending()
						)
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						// The result should present buckets with increasing term count
						containsExactly( c -> {
							for ( F value : valuesInAscendingDocumentCountOrder ) {
								c.accept( value, (long) documentIdPerTerm.get( value ).size() );
							}
						} )
				);
	}

	@Test
	public void orderByTermDescending() {
		assumeNonDefaultOrdersSupported();

		String fieldPath = indexMapping.fieldModel.relativeFieldName;

		AggregationKey<Map<F, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		SearchResultAssert.assertThat(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.terms().field( fieldPath, typeDescriptor.getJavaType() )
								.orderByTermDescending()
						)
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						// The result should present buckets with decreasing term values
						containsExactly( c -> {
							for ( F value : valuesInDescendingOrder ) {
								c.accept( value, (long) documentIdPerTerm.get( value ).size() );
							}
						} )
				);
	}

	@Test
	@PortedFromSearch5(original = "org.hibernate.search.test.query.facet.SimpleFacetingTest.testAlphabeticalSortOrder")
	public void orderByTermAscending() {
		assumeNonDefaultOrdersSupported();

		String fieldPath = indexMapping.fieldModel.relativeFieldName;

		AggregationKey<Map<F, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		SearchResultAssert.assertThat(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.terms().field( fieldPath, typeDescriptor.getJavaType() )
								.orderByTermAscending()
						)
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						// The result should present buckets with increasing term values
						containsExactly( c -> {
							for ( F value : valuesInAscendingOrder ) {
								c.accept( value, (long) documentIdPerTerm.get( value ).size() );
							}
						} )
				);
	}

	@Test
	@PortedFromSearch5(original = "org.hibernate.search.test.query.facet.SimpleFacetingTest.testZeroCountsExcluded")
	public void minDocumentCount_positive() {
		String fieldPath = indexMapping.fieldModel.relativeFieldName;

		AggregationKey<Map<F, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		SearchResultAssert.assertThat(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.terms().field( fieldPath, typeDescriptor.getJavaType() )
								.minDocumentCount( 2 )
						)
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						// Only buckets with the minimum required document count should appear in the result
						containsInAnyOrder( c -> {
							documentIdPerTerm.forEach( (key, value) -> {
								int documentCount = value.size();
								if ( documentCount >= 2 ) {
									c.accept( key, (long) documentCount );
								}
							} );
						} )
				);
	}

	@Test
	@PortedFromSearch5(original = "org.hibernate.search.test.query.facet.SimpleFacetingTest.testZeroCountsIncluded")
	public void minDocumentCount_zero() {
		String fieldPath = indexMapping.fieldModel.relativeFieldName;

		AggregationKey<Map<F, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		Map.Entry<F, List<String>> firstTermEntry = documentIdPerTerm.entrySet().iterator().next();

		SearchResultAssert.assertThat(
				indexManager.createScope().query()
						// Exclude documents containing the first term from matches
						.predicate( f -> f.matchAll().except(
								f.id().matchingAny( firstTermEntry.getValue() )
						) )
						.aggregation( aggregationKey, f -> f.terms().field( fieldPath, typeDescriptor.getJavaType() )
								.minDocumentCount( 0 )
						)
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						/*
						 * Buckets with a count of 0 should appear for values that are in the index,
						 * but are not encountered in any matching document.
						 */
						containsInAnyOrder( c -> {
							documentIdPerTerm.entrySet().stream().skip( 1 ).forEach( e -> {
								c.accept( e.getKey(), (long) e.getValue().size() );
							} );
							c.accept( firstTermEntry.getKey(), 0L );
						} )
				);
	}

	@Test
	public void minDocumentCount_zero_noMatch() {
		String fieldPath = indexMapping.fieldModel.relativeFieldName;

		AggregationKey<Map<F, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		SearchResultAssert.assertThat(
				indexManager.createScope().query()
						// Exclude all documents from the matches
						.predicate( f -> f.id().matching( "none" ) )
						.aggregation( aggregationKey, f -> f.terms().field( fieldPath, typeDescriptor.getJavaType() )
								.minDocumentCount( 0 )
						)
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						/*
						 * All indexed terms should appear in a bucket, in ascending value order, with a count of zero.
						 */
						containsInAnyOrder( c -> {
							for ( F value : valuesInAscendingOrder ) {
								c.accept( value, 0L );
							}
						} )
				);
	}

	@Test
	public void minDocumentCount_zero_noMatch_orderByTermDescending() {
		assumeNonDefaultOrdersSupported();

		String fieldPath = indexMapping.fieldModel.relativeFieldName;

		AggregationKey<Map<F, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		SearchResultAssert.assertThat(
				indexManager.createScope().query()
						// Exclude all documents from the matches
						.predicate( f -> f.id().matching( "none" ) )
						.aggregation( aggregationKey, f -> f.terms().field( fieldPath, typeDescriptor.getJavaType() )
								.minDocumentCount( 0 )
								.orderByTermDescending()
						)
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						/*
						 * All indexed terms should appear in a bucket, in descending value order, with a count of zero.
						 */
						containsInAnyOrder( c -> {
							for ( F value : valuesInDescendingOrder ) {
								c.accept( value, 0L );
							}
						} )
				);
	}

	@Test
	public void minDocumentCount_negative() {
		String fieldPath = indexMapping.fieldModel.relativeFieldName;

		SubTest.expectException( () ->
				indexManager.createScope().aggregation().terms().field( fieldPath, typeDescriptor.getJavaType() )
						.minDocumentCount( -1 ) )
				.assertThrown()
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "'minDocumentCount'" )
				.hasMessageContaining( "must be positive or zero" );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-776")
	@PortedFromSearch5(original = "org.hibernate.search.test.query.facet.SimpleFacetingTest.testMaxFacetCounts")
	public void maxTermCount_positive() {
		String fieldPath = indexMapping.fieldModel.relativeFieldName;

		AggregationKey<Map<F, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		SearchResultAssert.assertThat(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.terms().field( fieldPath, typeDescriptor.getJavaType() )
								.maxTermCount( 1 )
						)
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						/*
						 * Only the bucket with the most documents should be returned.
						 */
						containsInAnyOrder( c -> {
							F valueWithMostDocuments = valuesInDescendingDocumentCountOrder.get( 0 );
							c.accept( valueWithMostDocuments, (long) documentIdPerTerm.get( valueWithMostDocuments ).size() );
						} )
				);
	}

	/**
	 * Test maxTermCount with a non-default sort by ascending term value.
	 * The returned terms should be the "lowest" values.
	 */
	@Test
	public void maxTermCount_positive_orderByTermAscending() {
		assumeNonDefaultOrdersSupported();

		String fieldPath = indexMapping.fieldModel.relativeFieldName;

		AggregationKey<Map<F, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		SearchResultAssert.assertThat(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.terms().field( fieldPath, typeDescriptor.getJavaType() )
								.maxTermCount( 1 )
								.orderByTermAscending()
						)
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						/*
						 * Only the bucket with the "lowest" value should be returned.
						 */
						containsInAnyOrder( c -> {
							F lowestValue = valuesInAscendingOrder.get( 0 );
							c.accept( lowestValue, (long) documentIdPerTerm.get( lowestValue ).size() );
						} )
				);
	}

	@Test
	public void maxTermCount_positive_orderByCountAscending() {
		assumeNonDefaultOrdersSupported();

		String fieldPath = indexMapping.fieldModel.relativeFieldName;

		AggregationKey<Map<F, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		SearchResultAssert.assertThat(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.terms().field( fieldPath, typeDescriptor.getJavaType() )
								.maxTermCount( 1 )
								.orderByCountAscending()
						)
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						/*
						 * Only the bucket with the least documents should be returned.
						 */
						containsInAnyOrder( c -> {
							F valueWithLeastDocuments = valuesInAscendingDocumentCountOrder.get( 0 );
							c.accept( valueWithLeastDocuments, (long) documentIdPerTerm.get( valueWithLeastDocuments ).size() );
						} )
				);
	}

	@Test
	public void maxTermCount_zero() {
		String fieldPath = indexMapping.fieldModel.relativeFieldName;

		SubTest.expectException( () ->
				indexManager.createScope().aggregation().terms().field( fieldPath, typeDescriptor.getJavaType() )
						.maxTermCount( 0 ) )
				.assertThrown()
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "'maxTermCount'" )
				.hasMessageContaining( "must be strictly positive" );
	}

	@Test
	public void maxTermCount_negative() {
		String fieldPath = indexMapping.fieldModel.relativeFieldName;

		SubTest.expectException( () ->
				indexManager.createScope().aggregation().terms().field( fieldPath, typeDescriptor.getJavaType() )
						.maxTermCount( -1 ) )
				.assertThrown()
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "'maxTermCount'" )
				.hasMessageContaining( "must be strictly positive" );
	}

	private SearchQueryOptionsStep<?, DocumentReference, ?, ?> matchAllQuery() {
		return indexManager.createScope().query().predicate( f -> f.matchAll() );
	}

	private void assumeNonDefaultOrdersSupported() {
		assumeTrue(
				"Non-default orders are not supported for terms aggregations with this backend",
				TckConfiguration.get().getBackendFeatures().nonDefaultOrderInTermsAggregations()
		);
	}

	private void initData() {
		IndexIndexingPlan<? extends DocumentElement> plan = indexManager.createIndexingPlan();
		int documentCount = 0;
		for ( Map.Entry<F, List<String>> entry : documentIdPerTerm.entrySet() ) {
			F value = entry.getKey();
			for ( String documentId : entry.getValue() ) {
				plan.add( referenceProvider( documentId ), document -> {
					document.addValue( indexMapping.fieldModel.reference, value );
					document.addValue( indexMapping.fieldWithConverterModel.reference, value );
				} );
				++documentCount;
			}
		}
		plan.add( referenceProvider( "document_empty" ), document -> { } );
		++documentCount;
		plan.execute().join();

		// Check that all documents are searchable
		SearchResultAssert.assertThat(
				indexManager.createScope().query()
						.predicate( f -> f.matchAll() )
						.toQuery()
		)
				.hasTotalHitCount( documentCount );
	}

	private FieldModel<F> mapField(IndexSchemaElement parent, String prefix,
			Consumer<StandardIndexFieldTypeOptionsStep<?, F>> additionalConfiguration) {
		return FieldModel.mapper( typeDescriptor )
				.map( parent, prefix + typeDescriptor.getUniqueName(), additionalConfiguration );
	}

	@SuppressWarnings("unchecked")
	private <K, V> Consumer<Map<F, V>> containsExactly(Consumer<BiConsumer<F, V>> expectationBuilder) {
		List<Map.Entry<F, V>> expected = new ArrayList<>();
		expectationBuilder.accept( (k, v) -> expected.add( entry( typeDescriptor.toExpectedDocValue( k ), v ) ) );
		return actual -> assertThat( normalize( actual ) )
				.containsExactly( normalize( expected ).toArray( new Map.Entry[0] ) );
	}

	@SuppressWarnings("unchecked")
	private <K, V> Consumer<Map<K, V>> containsInAnyOrder(Consumer<BiConsumer<F, V>> expectationBuilder) {
		List<Map.Entry<F, V>> expected = new ArrayList<>();
		expectationBuilder.accept( (k, v) -> expected.add( entry( typeDescriptor.toExpectedDocValue( k ), v ) ) );
		return actual -> assertThat( normalize( actual ).entrySet() )
				.containsExactlyInAnyOrder( normalize( expected ).toArray( new Map.Entry[0] ) );
	}

	private class IndexMapping {
		final FieldModel<F> fieldModel;
		final FieldModel<F> fieldWithConverterModel;
		final FieldModel<F> fieldWithAggregationDisabledModel;

		IndexMapping(IndexSchemaElement root) {
			fieldModel = mapField(
					root, "",
					c -> c.aggregable( Aggregable.YES )
							.searchable( Searchable.NO ) // Terms aggregations should not need this
			);
			fieldWithConverterModel = mapField(
					root, "converted_",
					c -> c.aggregable( Aggregable.YES )
							.dslConverter( ValueWrapper.toIndexFieldConverter() )
							.projectionConverter( ValueWrapper.fromIndexFieldConverter() )
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
