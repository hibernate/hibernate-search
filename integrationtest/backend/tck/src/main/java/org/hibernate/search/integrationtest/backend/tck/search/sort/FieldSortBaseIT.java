/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.sort;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchHitsAssert.assertThatHits;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.documentProvider;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

import java.time.MonthDay;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.search.common.SortMode;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.search.sort.dsl.FieldSortOptionsStep;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.integrationtest.backend.tck.testsupport.model.singlefield.AbstractObjectBinding;
import org.hibernate.search.integrationtest.backend.tck.testsupport.model.singlefield.SingleFieldIndexBinding;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.NormalizedStringFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TestedFieldStructure;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Tests basic behavior of sorts by field value common to all supported types.
 */
@RunWith(Parameterized.class)
public class FieldSortBaseIT<F> {

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

	private static final int BEFORE_DOCUMENT_1_ORDINAL = 0;
	private static final int DOCUMENT_1_ORDINAL = 1;
	private static final int BETWEEN_DOCUMENT_1_AND_2_ORDINAL = 2;
	private static final int DOCUMENT_2_ORDINAL = 3;
	private static final int BETWEEN_DOCUMENT_2_AND_3_ORDINAL = 4;
	private static final int DOCUMENT_3_ORDINAL = 5;
	private static final int AFTER_DOCUMENT_3_ORDINAL = 6;

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

	public FieldSortBaseIT(TestedFieldStructure fieldStructure,
			FieldTypeDescriptor<F> fieldType, SortMode sortMode,
			DataSet<F> dataSetForAsc, DataSet<F> dataSetForDesc) {
		this.fieldStructure = fieldStructure;
		this.fieldType = fieldType;
		this.sortMode = sortMode;
		this.dataSetForAsc = dataSetForAsc;
		this.dataSetForDesc = dataSetForDesc;
	}

	@Test
	@TestForIssue(jiraKey = { "HSEARCH-3798", "HSEARCH-2252", "HSEARCH-2254", "HSEARCH-3103" })
	public void simple() {
		assumeTestParametersWork();

		DataSet<F> dataSet;
		SearchQuery<DocumentReference> query;
		String fieldPath = getFieldPath();

		// Default order
		dataSet = dataSetForAsc;
		query = matchNonEmptyQuery( dataSet, b -> b.field( fieldPath ) );
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.doc1Id, dataSet.doc2Id, dataSet.doc3Id );

		// Explicit order
		dataSet = dataSetForAsc;
		query = matchNonEmptyQuery( dataSet, b -> b.field( fieldPath ).asc() );
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.doc1Id, dataSet.doc2Id, dataSet.doc3Id );
		dataSet = dataSetForDesc;
		query = matchNonEmptyQuery( dataSet, b -> b.field( fieldPath ).desc() );
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.doc3Id, dataSet.doc2Id, dataSet.doc1Id );
	}

	@Test
	@TestForIssue(jiraKey = { "HSEARCH-3103" })
	public void medianWithNestedField() {
		assumeTrue(
				"This test is only relevant when using SortMode.MEDIAN in nested fields",
				isMedianWithNestedField() && !isSumOrAvgOrMedianWithStringField()
		);

		String fieldPath = getFieldPath();

		assertThatThrownBy( () -> matchNonEmptyQuery( dataSetForAsc, b -> b.field( fieldPath ) ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Invalid sort mode: MEDIAN",
						"This sort mode is not supported for fields in nested documents",
						fieldPath
				);
	}

	@Test
	@TestForIssue(jiraKey = { "HSEARCH-3103" })
	public void sumOrAvgOrMedianWithStringField() {
		assumeTrue(
				"This test is only relevant when using SortMode.SUM/AVG/MEDIAN on String fields",
				isSumOrAvgOrMedianWithStringField()
		);

		String fieldPath = getFieldPath();

		assertThatThrownBy( () -> matchNonEmptyQuery( dataSetForAsc, b -> b.field( fieldPath ) ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Invalid sort mode: " + sortMode.name() + ". This sort mode is not supported for String fields",
						"Only MIN and MAX are supported",
						fieldPath
				);
	}

	@Test
	@TestForIssue(jiraKey = { "HSEARCH-3103" })
	public void sumWithTemporalField() {
		assumeTrue(
				"This test is only relevant when using SortMode.SUM on Temporal fields",
				isSumWithTemporalField()
		);

		String fieldPath = getFieldPath();

		assertThatThrownBy( () -> matchNonEmptyQuery( dataSetForAsc, b -> b.field( fieldPath ) ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Invalid sort mode: SUM. This sort mode is not supported for temporal fields",
						"Only MIN, MAX, AVG and MEDIAN are supported",
						fieldPath
				);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3886")
	public void missingValue_default() {
		assumeTestParametersWork();

		DataSet<F> dataSet;
		SearchQuery<DocumentReference> query;

		String fieldPath = getFieldPath();

		// Default for missing values is last, regardless of the order

		dataSet = dataSetForAsc;
		query = matchNonEmptyAndEmpty1Query( dataSet, f -> f.field( fieldPath ) );
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.doc1Id, dataSet.doc2Id, dataSet.doc3Id,
						dataSet.emptyDoc1Id );

		dataSet = dataSetForAsc;
		query = matchNonEmptyAndEmpty1Query( dataSet, f -> f.field( fieldPath ).asc() );
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.doc1Id, dataSet.doc2Id, dataSet.doc3Id,
						dataSet.emptyDoc1Id );

		dataSet = dataSetForDesc;
		query = matchNonEmptyAndEmpty1Query( dataSet, f -> f.field( fieldPath ).desc() );
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.doc3Id, dataSet.doc2Id, dataSet.doc1Id,
						dataSet.emptyDoc1Id );
	}

	@Test
	public void missingValue_explicit() {
		assumeTestParametersWork();

		DataSet<F> dataSet;
		SearchQuery<DocumentReference> query;

		String fieldPath = getFieldPath();

		// Explicit order with missing().last()
		dataSet = dataSetForAsc;
		query = matchNonEmptyAndEmpty1Query( dataSet, f -> f.field( fieldPath ).asc().missing().last() );
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.doc1Id, dataSet.doc2Id, dataSet.doc3Id,
						dataSet.emptyDoc1Id );
		dataSet = dataSetForDesc;
		query = matchNonEmptyAndEmpty1Query( dataSet, f -> f.field( fieldPath ).desc().missing().last() );
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.doc3Id, dataSet.doc2Id, dataSet.doc1Id,
						dataSet.emptyDoc1Id );

		// Explicit order with missing().first()
		dataSet = dataSetForAsc;
		query = matchNonEmptyAndEmpty1Query( dataSet, f -> f.field( fieldPath ).asc().missing().first() );
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.emptyDoc1Id, dataSet.doc1Id, dataSet.doc2Id,
						dataSet.doc3Id );
		dataSet = dataSetForDesc;
		query = matchNonEmptyAndEmpty1Query( dataSet, f -> f.field( fieldPath ).desc().missing().first() );
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.emptyDoc1Id, dataSet.doc3Id, dataSet.doc2Id,
						dataSet.doc1Id );

		// Explicit order with missing().lowest()
		dataSet = dataSetForAsc;
		query = matchNonEmptyAndEmpty1Query( dataSet, f -> f.field( fieldPath ).asc().missing().lowest() );
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.emptyDoc1Id, dataSet.doc1Id, dataSet.doc2Id,
						dataSet.doc3Id );
		dataSet = dataSetForDesc;
		query = matchNonEmptyAndEmpty1Query( dataSet, f -> f.field( fieldPath ).desc().missing().lowest() );
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.doc3Id, dataSet.doc2Id, dataSet.doc1Id,
						dataSet.emptyDoc1Id );

		// Explicit order with missing().highest()
		dataSet = dataSetForAsc;
		query = matchNonEmptyAndEmpty1Query( dataSet, f -> f.field( fieldPath ).asc().missing().highest() );
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.doc1Id, dataSet.doc2Id, dataSet.doc3Id,
						dataSet.emptyDoc1Id );
		dataSet = dataSetForDesc;
		query = matchNonEmptyAndEmpty1Query( dataSet, f -> f.field( fieldPath ).desc().missing().highest() );
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.emptyDoc1Id, dataSet.doc3Id, dataSet.doc2Id,
						dataSet.doc1Id );

		// Explicit order with missing().use( ... )
		dataSet = dataSetForAsc;
		query = matchNonEmptyAndEmpty1Query( dataSet, f -> f.field( fieldPath ).asc()
				.missing().use( getSingleValueForMissingUse( BEFORE_DOCUMENT_1_ORDINAL ) ) );
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.emptyDoc1Id, dataSet.doc1Id, dataSet.doc2Id,
						dataSet.doc3Id );
		dataSet = dataSetForAsc;
		query = matchNonEmptyAndEmpty1Query( dataSet, f -> f.field( fieldPath ).asc()
				.missing().use( getSingleValueForMissingUse( BETWEEN_DOCUMENT_1_AND_2_ORDINAL ) ) );
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.doc1Id, dataSet.emptyDoc1Id, dataSet.doc2Id,
						dataSet.doc3Id );
		dataSet = dataSetForAsc;
		query = matchNonEmptyAndEmpty1Query( dataSet, f -> f.field( fieldPath ).asc()
				.missing().use( getSingleValueForMissingUse( BETWEEN_DOCUMENT_2_AND_3_ORDINAL ) ) );
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.doc1Id, dataSet.doc2Id, dataSet.emptyDoc1Id,
						dataSet.doc3Id );
		dataSet = dataSetForAsc;
		query = matchNonEmptyAndEmpty1Query( dataSet, f -> f.field( fieldPath ).asc()
				.missing().use( getSingleValueForMissingUse( AFTER_DOCUMENT_3_ORDINAL ) ) );
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.doc1Id, dataSet.doc2Id, dataSet.doc3Id,
						dataSet.emptyDoc1Id );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3254")
	public void missingValue_explicit_multipleEmpty() {
		assumeTestParametersWork();

		DataSet<F> dataSet = dataSetForAsc; // We're only using ascending order
		List<DocumentReference> docRefHits;
		String fieldPath = getFieldPath();

		// using before 1 value
		docRefHits = matchAllQuery( dataSet, f -> f.field( fieldPath ).asc()
				.missing().use( getSingleValueForMissingUse( BEFORE_DOCUMENT_1_ORDINAL ) ) )
				.fetchAllHits();
		assertThatHits( docRefHits ).ordinals( 0, 1, 2, 3 )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.emptyDoc1Id, dataSet.emptyDoc2Id,
						dataSet.emptyDoc3Id, dataSet.emptyDoc4Id );
		assertThatHits( docRefHits ).ordinal( 4 ).isDocRefHit( index.typeName(), dataSet.doc1Id );
		assertThatHits( docRefHits ).ordinal( 5 ).isDocRefHit( index.typeName(), dataSet.doc2Id );
		assertThatHits( docRefHits ).ordinal( 6 ).isDocRefHit( index.typeName(), dataSet.doc3Id );

		// using between 1 and 2 value
		docRefHits = matchAllQuery( dataSet, f -> f.field( fieldPath ).asc()
				.missing().use( getSingleValueForMissingUse( BETWEEN_DOCUMENT_1_AND_2_ORDINAL ) ) )
				.fetchAllHits();
		assertThatHits( docRefHits ).ordinal( 0 ).isDocRefHit( index.typeName(), dataSet.doc1Id );
		assertThatHits( docRefHits ).ordinals( 1, 2, 3, 4 )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.emptyDoc1Id, dataSet.emptyDoc2Id,
						dataSet.emptyDoc3Id, dataSet.emptyDoc4Id );
		assertThatHits( docRefHits ).ordinal( 5 ).isDocRefHit( index.typeName(), dataSet.doc2Id );
		assertThatHits( docRefHits ).ordinal( 6 ).isDocRefHit( index.typeName(), dataSet.doc3Id );

		// using between 2 and 3 value
		docRefHits = matchAllQuery( dataSet, f -> f.field( fieldPath ).asc()
				.missing().use( getSingleValueForMissingUse( BETWEEN_DOCUMENT_2_AND_3_ORDINAL ) ) )
				.fetchAllHits();
		assertThatHits( docRefHits ).ordinal( 0 ).isDocRefHit( index.typeName(), dataSet.doc1Id );
		assertThatHits( docRefHits ).ordinal( 1 ).isDocRefHit( index.typeName(), dataSet.doc2Id );
		assertThatHits( docRefHits ).ordinals( 2, 3, 4, 5 )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.emptyDoc1Id, dataSet.emptyDoc2Id,
						dataSet.emptyDoc3Id, dataSet.emptyDoc4Id );
		assertThatHits( docRefHits ).ordinal( 6 ).isDocRefHit( index.typeName(), dataSet.doc3Id );

		// using after 3 value
		docRefHits = matchAllQuery( dataSet, f -> f.field( fieldPath ).asc()
				.missing().use( getSingleValueForMissingUse( AFTER_DOCUMENT_3_ORDINAL ) ) ).fetchAllHits();
		assertThatHits( docRefHits ).ordinal( 0 ).isDocRefHit( index.typeName(), dataSet.doc1Id );
		assertThatHits( docRefHits ).ordinal( 1 ).isDocRefHit( index.typeName(), dataSet.doc2Id );
		assertThatHits( docRefHits ).ordinal( 2 ).isDocRefHit( index.typeName(), dataSet.doc3Id );
		assertThatHits( docRefHits ).ordinals( 3, 4, 5, 6 )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.emptyDoc1Id, dataSet.emptyDoc2Id,
						dataSet.emptyDoc3Id, dataSet.emptyDoc4Id );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3254")
	public void missingValue_multipleEmpty_useExistingDocumentValue() {
		assumeTestParametersWork();

		DataSet<F> dataSet = dataSetForAsc; // We're only using ascending order
		List<DocumentReference> docRefHits;
		String fieldPath = getFieldPath();

		Object docValue1 = getSingleValueForMissingUse( DOCUMENT_1_ORDINAL );
		Object docValue2 = getSingleValueForMissingUse( DOCUMENT_2_ORDINAL );
		Object docValue3 = getSingleValueForMissingUse( DOCUMENT_3_ORDINAL );

		// using doc 1 value
		docRefHits = matchAllQuery( dataSet, f -> f.field( fieldPath ).asc()
				.missing().use( docValue1 ) )
				.fetchAllHits();
		assertThatHits( docRefHits ).ordinals( 0, 1, 2, 3, 4 )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.doc1Id, dataSet.emptyDoc1Id,
						dataSet.emptyDoc2Id, dataSet.emptyDoc3Id, dataSet.emptyDoc4Id );
		assertThatHits( docRefHits ).ordinal( 5 ).isDocRefHit( index.typeName(), dataSet.doc2Id );
		assertThatHits( docRefHits ).ordinal( 6 ).isDocRefHit( index.typeName(), dataSet.doc3Id );

		// using doc 2 value
		docRefHits = matchAllQuery( dataSet, f -> f.field( fieldPath ).asc()
				.missing().use( docValue2 ) )
				.fetchAllHits();
		assertThatHits( docRefHits ).ordinal( 0 ).isDocRefHit( index.typeName(), dataSet.doc1Id );
		assertThatHits( docRefHits ).ordinals( 1, 2, 3, 4, 5 )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.doc2Id, dataSet.emptyDoc1Id,
						dataSet.emptyDoc2Id, dataSet.emptyDoc3Id, dataSet.emptyDoc4Id );
		assertThatHits( docRefHits ).ordinal( 6 ).isDocRefHit( index.typeName(), dataSet.doc3Id );

		// using doc 3 value
		docRefHits = matchAllQuery( dataSet, f -> f.field( fieldPath ).asc()
				.missing().use( docValue3 ) )
				.fetchAllHits();
		assertThatHits( docRefHits ).ordinal( 0 ).isDocRefHit( index.typeName(), dataSet.doc1Id );
		assertThatHits( docRefHits ).ordinal( 1 ).isDocRefHit( index.typeName(), dataSet.doc2Id );
		assertThatHits( docRefHits ).ordinals( 2, 3, 4, 5, 6 )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.doc3Id, dataSet.emptyDoc1Id,
						dataSet.emptyDoc2Id, dataSet.emptyDoc3Id, dataSet.emptyDoc4Id );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4162")
	public void factoryWithRoot() {
		assumeTestParametersWork();

		AbstractObjectBinding parentObjectBinding = index.binding().getParentObject( fieldStructure );

		assumeTrue( "This test is only relevant when the field is located on an object field",
				parentObjectBinding.absolutePath != null );

		DataSet<F> dataSet = dataSetForAsc;
		assertThatQuery( index.query()
				.where( f -> f.matchAll().except( f.id().matchingAny( Arrays.asList(
						dataSet.emptyDoc1Id, dataSet.emptyDoc2Id, dataSet.emptyDoc3Id, dataSet.emptyDoc4Id
				) ) ) )
				.routing( dataSet.routingKey )
				.sort( ( (Function<SearchSortFactory,
						FieldSortOptionsStep<?, ?>>) f -> f.withRoot( parentObjectBinding.absolutePath )
								.field( parentObjectBinding.getRelativeFieldName( fieldStructure, fieldType ) ) )
						.andThen( this::applySortMode )
						// Don't call this.applyFilter: we need to use the relative name of the discriminator field.
						.andThen( optionsStep -> {
							if ( fieldStructure.isInNested() ) {
								return optionsStep.filter( f -> f.match()
										.field( AbstractObjectBinding.DISCRIMINATOR_FIELD_NAME )
										.matching( "included" ) );
							}
							else {
								return optionsStep;
							}
						} ) ) )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.doc1Id, dataSet.doc2Id, dataSet.doc3Id );
	}

	@Test
	@TestForIssue(jiraKey = { "HSEARCH-4513" })
	public void concurrentQueriesUsingSameSort() {
		assumeTestParametersWork();

		DataSet<F> dataSet;
		String fieldPath = getFieldPath();

		StubMappingScope scope = index.createScope();

		SearchSort sort = applyFilter( applySortMode( scope.sort().field( fieldPath ) ) ).toSort();

		dataSet = dataSetForAsc;
		SearchQuery<DocumentReference> query1 = scope.query()
				.where( f -> f.id().matchingAny( Arrays.asList( dataSet.doc1Id, dataSet.doc2Id ) ) )
				.routing( dataSet.routingKey )
				// Reuse the same sort in multiple queries
				.sort( sort )
				.toQuery();
		SearchQuery<DocumentReference> query2 = scope.query()
				.where( f -> f.id().matching( "NOT_MATCHING_ANYTHING" ) )
				.routing( dataSet.routingKey )
				// Reuse the same sort in multiple queries
				.sort( sort )
				.toQuery();
		assertThatQuery( query1 )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.doc1Id, dataSet.doc2Id );
		assertThatQuery( query2 )
				.hasNoHits();
	}

	@Test
	public void missingValue_multipleOptionsSameTime() {
		assumeTestParametersWork();

		DataSet<F> dataSet;
		SearchQuery<DocumentReference> query;

		String fieldPath = getFieldPath();

		// Explicit order with missing().last()
		dataSet = dataSetForAsc;
		query = matchNonEmptyAndEmpty1Query( dataSet,
				f -> f.field( fieldPath ).asc().missing().last().missing().lowest().missing().first() );
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.emptyDoc1Id, dataSet.doc1Id, dataSet.doc2Id,
						dataSet.doc3Id );
		dataSet = dataSetForDesc;
		query = matchNonEmptyAndEmpty1Query( dataSet, f -> f.field( fieldPath ).desc().missing().first().missing().highest() );
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.emptyDoc1Id, dataSet.doc3Id, dataSet.doc2Id,
						dataSet.doc1Id );
		dataSet = dataSetForDesc;
		query = matchNonEmptyAndEmpty1Query( dataSet, f -> f.field( fieldPath ).desc()
				.missing().first()
				.missing().highest()
				.missing().last()
				.missing().lowest()
		);
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.doc3Id, dataSet.doc2Id, dataSet.doc1Id,
						dataSet.emptyDoc1Id );
	}

	private SearchQuery<DocumentReference> matchNonEmptyQuery(DataSet<F> dataSet,
			Function<? super SearchSortFactory, ? extends FieldSortOptionsStep<?, ?>> sortContributor) {
		return matchNonEmptyQuery( dataSet, sortContributor, index.createScope() );
	}

	private SearchQuery<DocumentReference> matchNonEmptyQuery(DataSet<F> dataSet,
			Function<? super SearchSortFactory, ? extends FieldSortOptionsStep<?, ?>> sortContributor, StubMappingScope scope) {
		return query(
				dataSet,
				f -> f.matchAll()
						.except( f.id()
								.matchingAny( Arrays.asList( dataSet.emptyDoc1Id, dataSet.emptyDoc2Id, dataSet.emptyDoc3Id,
										dataSet.emptyDoc4Id ) ) ),
				sortContributor,
				scope
		);
	}

	private SearchQuery<DocumentReference> matchNonEmptyAndEmpty1Query(DataSet<F> dataSet,
			Function<? super SearchSortFactory, ? extends FieldSortOptionsStep<?, ?>> sortContributor) {
		return matchNonEmptyAndEmpty1Query( dataSet, sortContributor, index.createScope() );
	}

	private SearchQuery<DocumentReference> matchNonEmptyAndEmpty1Query(DataSet<F> dataSet,
			Function<? super SearchSortFactory, ? extends FieldSortOptionsStep<?, ?>> sortContributor, StubMappingScope scope) {
		return query(
				dataSet,
				f -> f.matchAll().except(
						f.id().matchingAny( Arrays.asList( dataSet.emptyDoc2Id, dataSet.emptyDoc3Id, dataSet.emptyDoc4Id ) ) ),
				sortContributor,
				scope
		);
	}

	private SearchQuery<DocumentReference> matchAllQuery(DataSet<F> dataSet,
			Function<? super SearchSortFactory, ? extends FieldSortOptionsStep<?, ?>> sortContributor) {
		return matchAllQuery( dataSet, sortContributor, index.createScope() );
	}

	private SearchQuery<DocumentReference> matchAllQuery(DataSet<F> dataSet,
			Function<? super SearchSortFactory, ? extends FieldSortOptionsStep<?, ?>> sortContributor, StubMappingScope scope) {
		return query( dataSet, f -> f.matchAll(), sortContributor, scope );
	}

	private SearchQuery<DocumentReference> query(DataSet<F> dataSet,
			Function<? super SearchPredicateFactory, ? extends PredicateFinalStep> predicateContributor,
			Function<? super SearchSortFactory, ? extends FieldSortOptionsStep<?, ?>> sortContributor,
			StubMappingScope scope) {
		return scope.query()
				.where( predicateContributor )
				.routing( dataSet.routingKey )
				.sort( sortContributor.andThen( this::applySortMode ).andThen( this::applyFilter ) )
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

	private void assumeTestParametersWork() {
		assumeFalse(
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
				&& ( Temporal.class.isAssignableFrom( fieldType.getJavaType() )
						|| MonthDay.class.equals( fieldType.getJavaType() ) );
	}

	private boolean isMedianWithNestedField() {
		return SortMode.MEDIAN.equals( sortMode )
				&& fieldStructure.isInNested();
	}

	private String getFieldPath() {
		return index.binding().getFieldPath( fieldStructure, fieldType );
	}

	@SuppressWarnings("unchecked")
	private F getSingleValueForMissingUse(int ordinal) {
		F value = fieldType.getAscendingUniqueTermValues().getSingle().get( ordinal );

		if ( fieldType instanceof NormalizedStringFieldTypeDescriptor
				&& !TckConfiguration.get().getBackendFeatures().normalizesStringMissingValues() ) {
			// The backend doesn't normalize missing value replacements automatically, we have to do it ourselves
			// TODO HSEARCH-3387 Remove this once all backends correctly normalize missing value replacements
			value = (F) ( (String) value ).toLowerCase( Locale.ROOT );
		}

		return value;
	}

	private static class DataSet<F> {
		private final TestedFieldStructure fieldStructure;
		private final FieldTypeDescriptor<F> fieldType;
		private final SortMode expectedSortMode;
		private final String routingKey;

		private final String doc1Id;
		private final String doc2Id;
		private final String doc3Id;

		private final String emptyDoc1Id;
		private final String emptyDoc2Id;
		private final String emptyDoc3Id;
		private final String emptyDoc4Id;

		private DataSet(TestedFieldStructure fieldStructure, FieldTypeDescriptor<F> fieldType, SortMode sortModeOrNull,
				SortMode expectedSortMode) {
			this.fieldStructure = fieldStructure;
			this.fieldType = fieldType;
			this.expectedSortMode = expectedSortMode;
			this.routingKey = fieldType.getUniqueName() + "_" + fieldStructure.getUniqueName()
					+ "_" + sortModeOrNull + "_" + expectedSortMode;
			this.doc1Id = docId( 1 );
			this.doc2Id = docId( 2 );
			this.doc3Id = docId( 3 );
			this.emptyDoc1Id = emptyDocId( 1 );
			this.emptyDoc2Id = emptyDocId( 2 );
			this.emptyDoc3Id = emptyDocId( 3 );
			this.emptyDoc4Id = emptyDocId( 4 );
		}

		private String docId(int docNumber) {
			return routingKey + "_doc_" + docNumber;
		}

		private String emptyDocId(int docNumber) {
			return routingKey + "_emptyDoc_" + docNumber;
		}

		private void contribute(BulkIndexer indexer) {
			if ( fieldStructure.isSingleValued() ) {
				List<F> values = fieldType.getAscendingUniqueTermValues().getSingle();
				// Important: do not index the documents in the expected order after sorts (1, 2, 3)
				indexer.add( documentProvider( emptyDoc4Id, routingKey,
						document -> index.binding().initSingleValued( fieldType, fieldStructure.location,
								document, null, null ) ) );
				indexer.add( documentProvider( doc2Id, routingKey,
						document -> index.binding().initSingleValued( fieldType, fieldStructure.location,
								document, values.get( DOCUMENT_2_ORDINAL ), values.get( DOCUMENT_3_ORDINAL ) ) ) );
				indexer.add( documentProvider( emptyDoc1Id, routingKey,
						document -> index.binding().initSingleValued( fieldType, fieldStructure.location,
								document, null, null ) ) );
				indexer.add( documentProvider( doc1Id, routingKey,
						document -> index.binding().initSingleValued( fieldType, fieldStructure.location,
								document, values.get( DOCUMENT_1_ORDINAL ), values.get( DOCUMENT_2_ORDINAL ) ) ) );
				indexer.add( documentProvider( emptyDoc2Id, routingKey,
						document -> index.binding().initSingleValued( fieldType, fieldStructure.location,
								document, null, null ) ) );
				indexer.add( documentProvider( doc3Id, routingKey,
						document -> index.binding().initSingleValued( fieldType, fieldStructure.location,
								document, values.get( DOCUMENT_3_ORDINAL ), values.get( DOCUMENT_1_ORDINAL ) ) ) );
				indexer.add( documentProvider( emptyDoc3Id, routingKey,
						document -> index.binding().initSingleValued( fieldType, fieldStructure.location,
								document, null, null ) ) );
			}
			else {
				List<List<F>> values = fieldType.getAscendingUniqueTermValues()
						.getMultiResultingInSingle( expectedSortMode );
				// Important: do not index the documents in the expected order after sorts (1, 2, 3)
				indexer.add( documentProvider( emptyDoc4Id, routingKey,
						document -> index.binding().initSingleValued( fieldType, fieldStructure.location,
								document, null, null ) ) );
				indexer.add( documentProvider( doc2Id, routingKey,
						document -> index.binding().initMultiValued( fieldType, fieldStructure.location,
								document, values.get( DOCUMENT_2_ORDINAL ), values.get( DOCUMENT_3_ORDINAL ) ) ) );
				indexer.add( documentProvider( emptyDoc1Id, routingKey,
						document -> index.binding().initSingleValued( fieldType, fieldStructure.location,
								document, null, null ) ) );
				indexer.add( documentProvider( doc1Id, routingKey,
						document -> index.binding().initMultiValued( fieldType, fieldStructure.location,
								document, values.get( DOCUMENT_1_ORDINAL ), values.get( DOCUMENT_2_ORDINAL ) ) ) );
				indexer.add( documentProvider( emptyDoc2Id, routingKey,
						document -> index.binding().initSingleValued( fieldType, fieldStructure.location,
								document, null, null ) ) );
				indexer.add( documentProvider( doc3Id, routingKey,
						document -> index.binding().initMultiValued( fieldType, fieldStructure.location,
								document, values.get( DOCUMENT_3_ORDINAL ), values.get( DOCUMENT_1_ORDINAL ) ) ) );
				indexer.add( documentProvider( emptyDoc3Id, routingKey,
						document -> index.binding().initSingleValued( fieldType, fieldStructure.location,
								document, null, null ) ) );
			}
		}
	}

}
