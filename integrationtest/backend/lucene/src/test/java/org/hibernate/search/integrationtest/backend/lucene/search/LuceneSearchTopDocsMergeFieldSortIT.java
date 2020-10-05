/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatResult;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.documentProvider;

import java.time.MonthDay;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.hibernate.search.backend.lucene.LuceneExtension;
import org.hibernate.search.backend.lucene.search.query.LuceneSearchQuery;
import org.hibernate.search.backend.lucene.search.query.LuceneSearchResult;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.search.common.SortMode;
import org.hibernate.search.engine.search.sort.dsl.FieldSortOptionsStep;
import org.hibernate.search.engine.search.sort.dsl.SortOrder;
import org.hibernate.search.integrationtest.backend.tck.testsupport.model.singlefield.SingleFieldIndexBinding;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TestedFieldStructure;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopFieldDocs;

/**
 * Test that one can use {@link org.apache.lucene.search.TopDocs#merge(Sort, int, TopFieldDocs[])}
 * to merge top docs coming from different Lucene search queries
 * (which could run on different server nodes),
 * when relying on field value sort.
 * <p>
 * This is a use case in Infinispan, in particular.
 */
@RunWith(Parameterized.class)
public class LuceneSearchTopDocsMergeFieldSortIT<F> {

	private static Set<FieldTypeDescriptor<?>> supportedFieldTypes;
	private static List<DataSet<?>> dataSets;

	@Parameterized.Parameters(name = "{0} - {2} - {1}")
	public static Object[][] parameters() {
		supportedFieldTypes = new LinkedHashSet<>();
		dataSets = new ArrayList<>();
		List<Object[]> parameters = new ArrayList<>();
		for ( FieldTypeDescriptor<?> fieldType : FieldTypeDescriptor.getAll() ) {
			if ( fieldType.isFieldSortSupported() ) {
				supportedFieldTypes.add( fieldType );
				for ( TestedFieldStructure fieldStructure : TestedFieldStructure.all() ) {
					// We need two separate datasets when the sort mode is not defined,
					// because then the sort mode will be inferred automatically to
					// MIN for desc order, or MAX for asc order.
					DataSet<?> dataSetForAsc = new DataSet<>( fieldStructure, fieldType, null, SortMode.MIN );
					dataSets.add( dataSetForAsc );
					DataSet<?> dataSetForDesc = new DataSet<>( fieldStructure, fieldType, null, SortMode.MAX );
					dataSets.add( dataSetForDesc );
					parameters.add( new Object[] { fieldStructure, fieldType, null, dataSetForAsc, dataSetForDesc } );
					for ( SortMode sortMode : SortMode.values() ) {
						// When the sort mode is defined, we only need one dataset.
						dataSetForAsc = new DataSet<>( fieldStructure, fieldType, sortMode, sortMode );
						dataSets.add( dataSetForAsc );
						dataSetForDesc = dataSetForAsc;
						parameters.add( new Object[] { fieldStructure, fieldType, sortMode, dataSetForAsc, dataSetForDesc } );
					}
				}
			}
		}
		return parameters.toArray( new Object[0][] );
	}

	private static final int SEGMENT_0 = 0;
	private static final int SEGMENT_1 = 1;

	private static final int DOCUMENT_1_ORDINAL = 1;
	private static final int DOCUMENT_2_ORDINAL = 3;
	private static final int DOCUMENT_3_ORDINAL = 5;

	@ClassRule
	public static SearchSetupHelper setupHelper = new SearchSetupHelper();

	private static final Function<IndexSchemaElement, SingleFieldIndexBinding> bindingFactory =
			root -> SingleFieldIndexBinding.create( root, supportedFieldTypes, c -> c.sortable( Sortable.YES ) );

	private static final SimpleMappedIndex<SingleFieldIndexBinding> index = SimpleMappedIndex.of( bindingFactory );

	@BeforeClass
	public static void setup() {
		setupHelper.start().withIndex( index ).setup();

		BulkIndexer indexer = index.bulkIndexer();
		for ( DataSet<?> dataSet : dataSets ) {
			dataSet.contribute( indexer );
		}
		indexer.join();
	}

	private final TestedFieldStructure fieldStructure;
	private final FieldTypeDescriptor<F> fieldType;
	private final SortMode sortMode;
	private final DataSet<F> dataSetForAsc;
	private final DataSet<F> dataSetForDesc;

	public LuceneSearchTopDocsMergeFieldSortIT(TestedFieldStructure fieldStructure,
			FieldTypeDescriptor<F> fieldType, SortMode sortMode,
			DataSet<F> dataSetForAsc, DataSet<F> dataSetForDesc) {
		this.fieldStructure = fieldStructure;
		this.fieldType = fieldType;
		this.sortMode = sortMode;
		this.dataSetForAsc = dataSetForAsc;
		this.dataSetForDesc = dataSetForDesc;
	}

	@Test
	public void asc() {
		assumeTestParametersWork();

		DataSet<F> dataSet = dataSetForAsc;
		LuceneSearchQuery<DocumentReference> segment0Query = matchNonEmptySortedByFieldQuery( dataSet, SEGMENT_0, SortOrder.ASC );
		LuceneSearchQuery<DocumentReference> segment1Query = matchNonEmptySortedByFieldQuery( dataSet, SEGMENT_1, SortOrder.ASC );
		LuceneSearchResult<?> segment0Result = segment0Query.fetch( 10 );
		LuceneSearchResult<?> segment1Result = segment1Query.fetch( 10 );
		assertThatResult( segment0Result ).fromQuery( segment0Query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.seg0Doc1Id, dataSet.seg0Doc0Id );
		assertThatResult( segment1Result ).fromQuery( segment1Query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.seg1Doc0Id );

		TopFieldDocs[] allTopDocs = retrieveTopDocs( segment0Query, segment0Result, segment1Result );
		assertThat( TopDocs.merge( segment0Query.luceneSort(), 10, allTopDocs ).scoreDocs )
				.containsExactly(
						allTopDocs[0].scoreDocs[0], // dataSet.seg0Doc1Id
						allTopDocs[1].scoreDocs[0], // dataSet.seg1Doc0Id
						allTopDocs[0].scoreDocs[1] // dataSet.seg0Doc0Id
				);
	}

	// Also check descending order, to be sure the above didn't just pass by chance
	@Test
	public void desc() {
		assumeTestParametersWork();

		DataSet<F> dataSet = dataSetForDesc;
		LuceneSearchQuery<DocumentReference> segment0Query = matchNonEmptySortedByFieldQuery( dataSet, SEGMENT_0, SortOrder.DESC );
		LuceneSearchQuery<DocumentReference> segment1Query = matchNonEmptySortedByFieldQuery( dataSet, SEGMENT_1, SortOrder.DESC );
		LuceneSearchResult<?> segment0Result = segment0Query.fetch( 10 );
		LuceneSearchResult<?> segment1Result = segment1Query.fetch( 10 );
		assertThatResult( segment0Result ).fromQuery( segment0Query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.seg0Doc0Id, dataSet.seg0Doc1Id );
		assertThatResult( segment1Result ).fromQuery( segment1Query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.seg1Doc0Id );

		TopFieldDocs[] allTopDocs = retrieveTopDocs( segment0Query, segment0Result, segment1Result );
		assertThat( TopDocs.merge( segment0Query.luceneSort(), 10, allTopDocs ).scoreDocs )
				.containsExactly(
						allTopDocs[0].scoreDocs[0], // dataSet.seg0Doc0Id
						allTopDocs[1].scoreDocs[0], // dataSet.seg1Doc0Id
						allTopDocs[0].scoreDocs[1] // dataSet.seg0Doc1Id
				);
	}

	private LuceneSearchQuery<DocumentReference> matchNonEmptySortedByFieldQuery(DataSet<F> dataSet, int segment,
			SortOrder sortOrder) {
		StubMappingScope scope = index.createScope();
		return scope.query().extension( LuceneExtension.get() )
				.where( f -> f.matchAll().except( f.id().matchingAny( Arrays.asList( dataSet.seg0EmptyDocId, dataSet.seg1EmptyDocId ) ) ) )
				.sort( f -> applyFilter( applySortMode(
						scope.sort().field( getFieldPath() ).order( sortOrder )
				) ) )
				.routing( dataSet.routingKey( segment ) )
				.toQuery();
	}

	private FieldSortOptionsStep<?, ?> applySortMode(FieldSortOptionsStep<?, ?> optionsStep) {
		if ( sortMode != null ) {
			return optionsStep.mode( sortMode );
		}
		else {
			return optionsStep;
		}
	}

	private FieldSortOptionsStep<?, ?> applyFilter(FieldSortOptionsStep<?, ?> optionsStep) {
		if ( fieldStructure.isInNested() ) {
			return optionsStep.filter( f -> f.match()
					.field( index.binding().getDiscriminatorFieldPath( fieldStructure ) )
					.matching( "included" ) );
		}
		else {
			return optionsStep;
		}
	}

	private TopFieldDocs[] retrieveTopDocs(LuceneSearchQuery<?> query, LuceneSearchResult ... results) {
		Sort sort = query.luceneSort();
		TopFieldDocs[] allTopDocs = new TopFieldDocs[results.length];
		for ( int i = 0; i < results.length; i++ ) {
			TopDocs topDocs = results[i].topDocs();
			allTopDocs[i] = new TopFieldDocs( topDocs.totalHits, topDocs.scoreDocs, sort.getSort() );
		}
		return allTopDocs;
	}

	private void assumeTestParametersWork() {
		Assume.assumeFalse(
				"This combination is not expected to work",
				isMedianWithNestedField() || isSumOrAvgOrMedianWithStringField() || isSumWithTemporalField()
		);
	}

	private boolean isSumOrAvgOrMedianWithStringField() {
		return EnumSet.of( SortMode.SUM, SortMode.AVG, SortMode.MEDIAN ).contains( sortMode )
				&& String.class.equals( fieldType.getJavaType() );
	}

	private boolean isSumWithTemporalField() {
		return SortMode.SUM.equals( sortMode )
				&& (
						Temporal.class.isAssignableFrom( fieldType.getJavaType() )
						|| MonthDay.class.equals( fieldType.getJavaType() )
				);
	}

	private boolean isMedianWithNestedField() {
		return SortMode.MEDIAN.equals( sortMode )
				&& fieldStructure.isInNested();
	}

	private String getFieldPath() {
		return index.binding().getFieldPath( fieldStructure, fieldType );
	}

	private static class DataSet<F> {
		private final TestedFieldStructure fieldStructure;
		private final FieldTypeDescriptor<F> fieldType;
		private final SortMode expectedSortMode;
		private final String routingKeyForSegment0;
		private final String routingKeyForSegment1;

		private final String seg0Doc0Id;
		private final String seg0Doc1Id;
		private final String seg1Doc0Id;

		private final String seg0EmptyDocId;
		private final String seg1EmptyDocId;

		private DataSet(TestedFieldStructure fieldStructure, FieldTypeDescriptor<F> fieldType, SortMode sortModeOrNull,
				SortMode expectedSortMode) {
			this.fieldStructure = fieldStructure;
			this.fieldType = fieldType;
			this.expectedSortMode = expectedSortMode;
			String routingKeyBase = fieldType.getUniqueName() + "_" + fieldStructure.getUniqueName()
					+ "_" + sortModeOrNull + "_" + expectedSortMode;
			this.routingKeyForSegment0 = "segment_0_" + routingKeyBase;
			this.routingKeyForSegment1 = "segment_1_" + routingKeyBase;
			this.seg0Doc0Id = docId( SEGMENT_0, 0 );
			this.seg0Doc1Id = docId( SEGMENT_0, 1 );
			this.seg1Doc0Id = docId( SEGMENT_1, 0 );
			this.seg0EmptyDocId = emptyDocId( SEGMENT_0, 0 );
			this.seg1EmptyDocId = emptyDocId( SEGMENT_1, 0 );
		}

		private String routingKey(int segment) {
			return segment == SEGMENT_0 ? routingKeyForSegment0 : routingKeyForSegment1;
		}

		private String docId(int segment, int docNumber) {
			return routingKey( segment ) + "_doc_" + docNumber;
		}

		private String emptyDocId(int segment, int docNumber) {
			return routingKey( segment ) + "_emptyDoc_" + docNumber;
		}

		private void contribute(BulkIndexer indexer) {
			if ( fieldStructure.isSingleValued() ) {
				List<F> values = fieldType.getAscendingUniqueTermValues().getSingle();
				// Important: do not index the documents in the expected order after sorts (1, 2, 3)
				indexer.add( documentProvider( seg1Doc0Id, routingKeyForSegment1,
						document -> index.binding().initSingleValued( fieldType, fieldStructure.location,
								document, values.get( DOCUMENT_2_ORDINAL ), values.get( DOCUMENT_3_ORDINAL ) ) ) );
				indexer.add( documentProvider( seg1EmptyDocId, routingKeyForSegment1,
						document -> index.binding().initSingleValued( fieldType, fieldStructure.location,
								document, null, null ) ) );
				indexer.add( documentProvider( seg0Doc1Id, routingKeyForSegment0,
						document -> index.binding().initSingleValued( fieldType, fieldStructure.location,
								document, values.get( DOCUMENT_1_ORDINAL ), values.get( DOCUMENT_2_ORDINAL ) ) ) );
				indexer.add( documentProvider( seg0Doc0Id, routingKeyForSegment0,
						document -> index.binding().initSingleValued( fieldType, fieldStructure.location,
								document, values.get( DOCUMENT_3_ORDINAL ), values.get( DOCUMENT_1_ORDINAL ) ) ) );
				indexer.add( documentProvider( seg0EmptyDocId, routingKeyForSegment0,
						document -> index.binding().initSingleValued( fieldType, fieldStructure.location,
								document, null, null ) ) );
			}
			else {
				List<List<F>> values = fieldType.getAscendingUniqueTermValues()
						.getMultiResultingInSingle( expectedSortMode );
				// Important: do not index the documents in the expected order after sorts (1, 2, 3)
				indexer.add( documentProvider( seg1Doc0Id, routingKeyForSegment1,
						document -> index.binding().initMultiValued( fieldType, fieldStructure.location,
								document, values.get( DOCUMENT_2_ORDINAL ), values.get( DOCUMENT_3_ORDINAL ) ) ) );
				indexer.add( documentProvider( seg1EmptyDocId, routingKeyForSegment1,
						document -> index.binding().initSingleValued( fieldType, fieldStructure.location,
								document, null, null ) ) );
				indexer.add( documentProvider( seg0Doc1Id, routingKeyForSegment0,
						document -> index.binding().initMultiValued( fieldType, fieldStructure.location,
								document, values.get( DOCUMENT_1_ORDINAL ), values.get( DOCUMENT_2_ORDINAL ) ) ) );
				indexer.add( documentProvider( seg0Doc0Id, routingKeyForSegment0,
						document -> index.binding().initMultiValued( fieldType, fieldStructure.location,
								document, values.get( DOCUMENT_3_ORDINAL ), values.get( DOCUMENT_1_ORDINAL ) ) ) );
				indexer.add( documentProvider( seg0EmptyDocId, routingKeyForSegment0,
						document -> index.binding().initSingleValued( fieldType, fieldStructure.location,
								document, null, null ) ) );
			}
		}
	}

}
