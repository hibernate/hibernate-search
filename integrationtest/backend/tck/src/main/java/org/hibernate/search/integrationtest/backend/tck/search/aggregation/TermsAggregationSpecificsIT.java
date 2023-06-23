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
import java.util.Collections;
import java.util.LinkedHashMap;
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
import org.hibernate.search.integrationtest.backend.tck.testsupport.operations.TermsAggregationDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModelsByType;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.ValueWrapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Tests behavior specific to the terms aggregation on supported field types.
 * <p>
 * Behavior common to all single-field aggregations is tested in {@link SingleFieldAggregationBaseIT}.
 */
@RunWith(Parameterized.class)
public class TermsAggregationSpecificsIT<F> {

	private static final String AGGREGATION_NAME = "aggregationName";

	private static Set<FieldTypeDescriptor<?>> supportedFieldTypes;
	private static List<DataSet<?>> dataSets;

	@Parameterized.Parameters(name = "{0}")
	public static Object[][] parameters() {
		supportedFieldTypes = new LinkedHashSet<>();
		dataSets = new ArrayList<>();
		List<Object[]> parameters = new ArrayList<>();
		AggregationDescriptor aggregationDescriptor = TermsAggregationDescriptor.INSTANCE;
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

	public TermsAggregationSpecificsIT(FieldTypeDescriptor<F> fieldType, DataSet<F> dataSet) {
		this.fieldType = fieldType;
		this.dataSet = dataSet;
	}

	@Test
	public void superClassFieldType() {
		Class<? super F> superClass = fieldType.getJavaType().getSuperclass();

		doTestSuperClassFieldType( superClass );
	}

	private <S> void doTestSuperClassFieldType(Class<S> superClass) {
		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationKey<Map<S, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		assertThatQuery(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.terms().field( fieldPath, superClass ) )
						.routing( dataSet.name )
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						// All documents should be mentioned in the aggregation, even those excluded by the limit/offset
						containsInAnyOrder( c -> {
							dataSet.documentIdPerTerm.forEach( (key, value) -> c.accept( key, (long) value.size() ) );
						} )
				);
	}

	/**
	 * Check that defining a predicate will affect the aggregation result.
	 */
	@Test
	public void predicate() {
		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationKey<Map<F, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		Map.Entry<F, List<String>> firstTermEntry = dataSet.documentIdPerTerm.entrySet().iterator().next();

		assertThatQuery(
				index.createScope().query()
						.where( f -> f.id()
								.matching( firstTermEntry.getValue().get( 0 ) )
								.matching( firstTermEntry.getValue().get( 1 ) )
						)
						.aggregation( aggregationKey, f -> f.terms().field( fieldPath, fieldType.getJavaType() ) )
						.routing( dataSet.name )
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
		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationKey<Map<F, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		assertThatResult(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.terms().field( fieldPath, fieldType.getJavaType() ) )
						.fetch( 3, 4 )
		)
				.aggregation(
						aggregationKey,
						// All documents should be mentioned in the aggregation, even those excluded by the limit/offset
						containsInAnyOrder( c -> {
							dataSet.documentIdPerTerm.forEach( (key, value) -> c.accept( key, (long) value.size() ) );
						} )
				);
	}

	@Test
	@PortedFromSearch5(original = "org.hibernate.search.test.query.facet.SimpleFacetingTest.testDefaultSortOrderIsCount")
	public void order_default() {
		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationKey<Map<F, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		assertThatQuery(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.terms().field( fieldPath, fieldType.getJavaType() ) )
						.routing( dataSet.name )
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						// The result should present buckets with decreasing term count
						containsExactly( c -> {
							for ( F value : dataSet.valuesInDescendingDocumentCountOrder ) {
								c.accept( value, (long) dataSet.documentIdPerTerm.get( value ).size() );
							}
						} )
				);
	}

	@Test
	@PortedFromSearch5(original = "org.hibernate.search.test.query.facet.SimpleFacetingTest.testCountSortOrderDesc")
	public void orderByCountDescending() {
		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationKey<Map<F, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		assertThatQuery(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.terms().field( fieldPath, fieldType.getJavaType() )
								.orderByCountDescending()
						)
						.routing( dataSet.name )
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						// The result should present buckets with decreasing term count
						containsExactly( c -> {
							for ( F value : dataSet.valuesInDescendingDocumentCountOrder ) {
								c.accept( value, (long) dataSet.documentIdPerTerm.get( value ).size() );
							}
						} )
				);
	}

	@Test
	@PortedFromSearch5(original = "org.hibernate.search.test.query.facet.SimpleFacetingTest.testCountSortOrderAsc")
	public void orderByCountAscending() {
		assumeNonDefaultOrdersSupported();

		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationKey<Map<F, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		assertThatQuery(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.terms().field( fieldPath, fieldType.getJavaType() )
								.orderByCountAscending()
						)
						.routing( dataSet.name )
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						// The result should present buckets with increasing term count
						containsExactly( c -> {
							for ( F value : dataSet.valuesInAscendingDocumentCountOrder ) {
								c.accept( value, (long) dataSet.documentIdPerTerm.get( value ).size() );
							}
						} )
				);
	}

	@Test
	public void orderByTermDescending() {
		assumeNonDefaultOrdersSupported();

		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationKey<Map<F, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		assertThatQuery(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.terms().field( fieldPath, fieldType.getJavaType() )
								.orderByTermDescending()
						)
						.routing( dataSet.name )
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						// The result should present buckets with decreasing term dataSet.values
						containsExactly( c -> {
							for ( F value : dataSet.valuesInDescendingOrder ) {
								c.accept( value, (long) dataSet.documentIdPerTerm.get( value ).size() );
							}
						} )
				);
	}

	@Test
	@PortedFromSearch5(original = "org.hibernate.search.test.query.facet.SimpleFacetingTest.testAlphabeticalSortOrder")
	public void orderByTermAscending() {
		assumeNonDefaultOrdersSupported();

		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationKey<Map<F, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		assertThatQuery(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.terms().field( fieldPath, fieldType.getJavaType() )
								.orderByTermAscending()
						)
						.routing( dataSet.name )
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						// The result should present buckets with increasing term dataSet.values
						containsExactly( c -> {
							for ( F value : dataSet.valuesInAscendingOrder ) {
								c.accept( value, (long) dataSet.documentIdPerTerm.get( value ).size() );
							}
						} )
				);
	}

	@Test
	@PortedFromSearch5(original = "org.hibernate.search.test.query.facet.SimpleFacetingTest.testZeroCountsExcluded")
	public void minDocumentCount_positive() {
		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationKey<Map<F, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		assertThatQuery(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.terms().field( fieldPath, fieldType.getJavaType() )
								.minDocumentCount( 2 )
						)
						.routing( dataSet.name )
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						// Only buckets with the minimum required document count should appear in the result
						containsInAnyOrder( c -> {
							dataSet.documentIdPerTerm.forEach( (key, value) -> {
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
		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationKey<Map<F, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		Map.Entry<F, List<String>> firstTermEntry = dataSet.documentIdPerTerm.entrySet().iterator().next();

		assertThatQuery(
				index.createScope().query()
						// Exclude documents containing the first term from matches
						.where( f -> f.matchAll().except(
								f.id().matchingAny( firstTermEntry.getValue() )
						) )
						.aggregation( aggregationKey, f -> f.terms().field( fieldPath, fieldType.getJavaType() )
								.minDocumentCount( 0 )
						)
						.routing( dataSet.name )
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						/*
						 * Buckets with a count of 0 should appear for dataSet.values that are in the index,
						 * but are not encountered in any matching document.
						 */
						containsInAnyOrder( c -> {
							dataSet.documentIdPerTerm.entrySet().stream().skip( 1 ).forEach( e -> {
								c.accept( e.getKey(), (long) e.getValue().size() );
							} );
							c.accept( firstTermEntry.getKey(), 0L );
						} )
				);
	}

	@Test
	public void minDocumentCount_zero_noMatch() {
		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationKey<Map<F, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		assertThatQuery(
				index.createScope().query()
						// Exclude all documents from the matches
						.where( f -> f.id().matching( "none" ) )
						.aggregation( aggregationKey, f -> f.terms().field( fieldPath, fieldType.getJavaType() )
								.minDocumentCount( 0 )
						)
						.routing( dataSet.name )
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						/*
						 * All indexed terms should appear in a bucket, in ascending value order, with a count of zero.
						 */
						containsInAnyOrder( c -> {
							for ( F value : dataSet.valuesInAscendingOrder ) {
								c.accept( value, 0L );
							}
						} )
				);
	}

	@Test
	public void minDocumentCount_zero_noMatch_orderByTermDescending() {
		assumeNonDefaultOrdersSupported();

		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationKey<Map<F, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		assertThatQuery(
				index.createScope().query()
						// Exclude all documents from the matches
						.where( f -> f.id().matching( "none" ) )
						.aggregation( aggregationKey, f -> f.terms().field( fieldPath, fieldType.getJavaType() )
								.minDocumentCount( 0 )
								.orderByTermDescending()
						)
						.routing( dataSet.name )
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						/*
						 * All indexed terms should appear in a bucket, in descending value order, with a count of zero.
						 */
						containsInAnyOrder( c -> {
							for ( F value : dataSet.valuesInDescendingOrder ) {
								c.accept( value, 0L );
							}
						} )
				);
	}

	@Test
	public void minDocumentCount_negative() {
		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		assertThatThrownBy( () -> index.createScope().aggregation().terms().field( fieldPath, fieldType.getJavaType() )
				.minDocumentCount( -1 ) )
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "'minDocumentCount'" )
				.hasMessageContaining( "must be positive or zero" );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-776")
	@PortedFromSearch5(original = "org.hibernate.search.test.query.facet.SimpleFacetingTest.testMaxFacetCounts")
	public void maxTermCount_positive() {
		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationKey<Map<F, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		assertThatQuery(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.terms().field( fieldPath, fieldType.getJavaType() )
								.maxTermCount( 1 )
						)
						.routing( dataSet.name )
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						/*
						 * Only the bucket with the most documents should be returned.
						 */
						containsInAnyOrder( c -> {
							F valueWithMostDocuments = dataSet.valuesInDescendingDocumentCountOrder.get( 0 );
							c.accept( valueWithMostDocuments,
									(long) dataSet.documentIdPerTerm.get( valueWithMostDocuments ).size() );
						} )
				);
	}

	/**
	 * Test maxTermCount with a non-default sort by ascending term value.
	 * The returned terms should be the "lowest" dataSet.values.
	 */
	@Test
	public void maxTermCount_positive_orderByTermAscending() {
		assumeNonDefaultOrdersSupported();

		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationKey<Map<F, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		assertThatQuery(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.terms().field( fieldPath, fieldType.getJavaType() )
								.maxTermCount( 1 )
								.orderByTermAscending()
						)
						.routing( dataSet.name )
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						/*
						 * Only the bucket with the "lowest" value should be returned.
						 */
						containsInAnyOrder( c -> {
							F lowestValue = dataSet.valuesInAscendingOrder.get( 0 );
							c.accept( lowestValue, (long) dataSet.documentIdPerTerm.get( lowestValue ).size() );
						} )
				);
	}

	@Test
	public void maxTermCount_positive_orderByCountAscending() {
		assumeNonDefaultOrdersSupported();

		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationKey<Map<F, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		assertThatQuery(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.terms().field( fieldPath, fieldType.getJavaType() )
								.maxTermCount( 1 )
								.orderByCountAscending()
						)
						.routing( dataSet.name )
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						/*
						 * Only the bucket with the least documents should be returned.
						 */
						containsInAnyOrder( c -> {
							F valueWithLeastDocuments = dataSet.valuesInAscendingDocumentCountOrder.get( 0 );
							c.accept( valueWithLeastDocuments,
									(long) dataSet.documentIdPerTerm.get( valueWithLeastDocuments ).size() );
						} )
				);
	}

	@Test
	public void maxTermCount_zero() {
		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		assertThatThrownBy( () -> index.createScope().aggregation().terms().field( fieldPath, fieldType.getJavaType() )
				.maxTermCount( 0 ) )
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "'maxTermCount'" )
				.hasMessageContaining( "must be strictly positive" );
	}

	@Test
	public void maxTermCount_negative() {
		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		assertThatThrownBy( () -> index.createScope().aggregation().terms().field( fieldPath, fieldType.getJavaType() )
				.maxTermCount( -1 ) )
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "'maxTermCount'" )
				.hasMessageContaining( "must be strictly positive" );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4544")
	public void maxTermCount_integerMaxValue() {
		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationKey<Map<F, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		assertThatQuery( matchAllQuery()
				.aggregation( aggregationKey, f -> f.terms().field( fieldPath, fieldType.getJavaType() )
						.maxTermCount( Integer.MAX_VALUE ) )
				.routing( dataSet.name ) )
				.aggregation(
						aggregationKey,
						// All buckets should be returned.
						containsInAnyOrder( c -> {
							for ( F value : dataSet.valuesInDescendingOrder ) {
								c.accept( value, (long) dataSet.documentIdPerTerm.get( value ).size() );
							}
						} )
				);
	}

	// This is interesting even if we already test Integer.MAX_VALUE (see above),
	// because Lucene has some hardcoded limits for PriorityQueue sizes,
	// somewhere around 2147483631, which is lower than Integer.MAX_VALUE.
	@Test
	@TestForIssue(jiraKey = "HSEARCH-4544")
	public void maxTermCount_veryLarge() {
		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationKey<Map<F, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		assertThatQuery( matchAllQuery()
				.aggregation( aggregationKey, f -> f.terms().field( fieldPath, fieldType.getJavaType() )
						.maxTermCount( 2_000_000_000 ) )
				.routing( dataSet.name ) )
				.aggregation(
						aggregationKey,
						// All buckets should be returned.
						containsInAnyOrder( c -> {
							for ( F value : dataSet.valuesInDescendingOrder ) {
								c.accept( value, (long) dataSet.documentIdPerTerm.get( value ).size() );
							}
						} )
				);
	}

	private SearchQueryOptionsStep<?, DocumentReference, ?, ?, ?> matchAllQuery() {
		return index.createScope().query().where( f -> f.matchAll() );
	}

	private void assumeNonDefaultOrdersSupported() {
		assumeTrue(
				"Non-default orders are not supported for terms aggregations with this backend",
				TckConfiguration.get().getBackendFeatures().nonDefaultOrderInTermsAggregations()
		);
	}

	@SuppressWarnings("unchecked")
	private <K, V> Consumer<Map<F, V>> containsExactly(Consumer<BiConsumer<F, V>> expectationBuilder) {
		List<Map.Entry<F, V>> expected = new ArrayList<>();
		expectationBuilder.accept( (k, v) -> expected.add( entry( fieldType.toExpectedDocValue( k ), v ) ) );
		return actual -> assertThat( normalize( actual ) )
				.containsExactly( normalize( expected ).toArray( new Map.Entry[0] ) );
	}

	@SuppressWarnings("unchecked")
	private <K, V> Consumer<Map<K, V>> containsInAnyOrder(Consumer<BiConsumer<F, V>> expectationBuilder) {
		List<Map.Entry<F, V>> expected = new ArrayList<>();
		expectationBuilder.accept( (k, v) -> expected.add( entry( fieldType.toExpectedDocValue( k ), v ) ) );
		return actual -> assertThat( normalize( actual ).entrySet() )
				.containsExactlyInAnyOrder( normalize( expected ).toArray( new Map.Entry[0] ) );
	}

	private static class DataSet<F> {
		final FieldTypeDescriptor<F> fieldType;
		final String name;
		final Map<F, List<String>> documentIdPerTerm;
		final List<F> valuesInAscendingOrder;
		final List<F> valuesInDescendingOrder;
		final List<F> valuesInAscendingDocumentCountOrder;
		final List<F> valuesInDescendingDocumentCountOrder;

		private DataSet(FieldTypeDescriptor<F> fieldType) {
			this.fieldType = fieldType;
			this.name = fieldType.getUniqueName();
			this.documentIdPerTerm = new LinkedHashMap<>();

			this.valuesInAscendingOrder = fieldType.getAscendingUniqueTermValues().getSingle();

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
					String documentId = name + "_document_" + documentIdAsInteger;
					++documentIdAsInteger;
					documentIdsForTerm.add( documentId );
				}
				--numberOfDocuments;
			}
		}

		private void init() {
			BulkIndexer indexer = index.bulkIndexer();
			for ( Map.Entry<F, List<String>> entry : documentIdPerTerm.entrySet() ) {
				F value = entry.getKey();
				for ( String documentId : entry.getValue() ) {
					indexer.add( documentId, name, document -> {
						document.addValue( index.binding().fieldModels.get( fieldType ).reference, value );
						document.addValue( index.binding().fieldWithConverterModels.get( fieldType ).reference, value );
					} );
				}
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
							.searchable( Searchable.NO ) // Terms aggregations should not need this
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
