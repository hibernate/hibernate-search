/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.sort;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.referenceProvider;

import java.time.MonthDay;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.search.common.SortMode;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.sort.dsl.FieldSortOptionsStep;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.SortOrder;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.NormalizedStringFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.ExpectationsAlternative;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.IndexFieldStructure;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.IndexFieldValueCardinality;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModel;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModelsByType;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.SubTest;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Tests basic behavior of sorts by field value common to all supported types.
 */
@RunWith(Parameterized.class)
public class FieldSearchSortBaseIT<F> {

	private static Stream<FieldTypeDescriptor<?>> supportedTypeDescriptors() {
		return FieldTypeDescriptor.getAll().stream()
				.filter( typeDescriptor -> typeDescriptor.getFieldSortExpectations().isSupported() );
	}

	@Parameterized.Parameters(name = "{0} - {1} - {3} - {2}")
	public static Object[][] parameters() {
		List<Object[]> parameters = new ArrayList<>();
		supportedTypeDescriptors().forEach( fieldTypeDescriptor -> {
			ExpectationsAlternative<?, ?> expectations = fieldTypeDescriptor.getFieldSortExpectations();
			if ( expectations.isSupported() ) {
				for ( IndexFieldStructure indexFieldStructure : IndexFieldStructure.values() ) {
					for ( IndexFieldValueCardinality indexFieldValueCardinality : IndexFieldValueCardinality.values() ) {
						parameters.add( new Object[] { indexFieldStructure, indexFieldValueCardinality, fieldTypeDescriptor, null } );
						for ( SortMode sortMode : SortMode.values() ) {
							parameters.add( new Object[] { indexFieldStructure, indexFieldValueCardinality, fieldTypeDescriptor, sortMode } );
						}
					}
				}
			}
		} );
		return parameters.toArray( new Object[0][] );
	}

	private static final String INDEX_NAME = "IndexName";

	private static final String DOCUMENT_1 = "1";
	private static final String DOCUMENT_2 = "2";
	private static final String DOCUMENT_3 = "3";

	private static final String EMPTY_1 = "empty1";
	private static final String EMPTY_2 = "empty2";
	private static final String EMPTY_3 = "empty3";
	private static final String EMPTY_4 = "empty4";

	private static final int BEFORE_DOCUMENT_1_ORDINAL = 0;
	private static final int DOCUMENT_1_ORDINAL = 1;
	private static final int BETWEEN_DOCUMENT_1_AND_2_ORDINAL = 2;
	private static final int DOCUMENT_2_ORDINAL = 3;
	private static final int BETWEEN_DOCUMENT_2_AND_3_ORDINAL = 4;
	private static final int DOCUMENT_3_ORDINAL = 5;
	private static final int AFTER_DOCUMENT_3_ORDINAL = 6;

	@ClassRule
	public static SearchSetupHelper setupHelper = new SearchSetupHelper();

	private static IndexMapping indexMapping;
	private static StubMappingIndexManager indexManager;

	@BeforeClass
	public static void setup() {
		setupHelper.start()
				.withIndex(
						INDEX_NAME,
						ctx -> indexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> FieldSearchSortBaseIT.indexManager = indexManager
				)
				.setup();

		initData();
	}

	private final IndexFieldStructure indexFieldStructure;
	private final IndexFieldValueCardinality indexFieldValueCardinality;
	private final FieldTypeDescriptor<F> fieldTypeDescriptor;
	private final SortMode sortMode;

	public FieldSearchSortBaseIT(IndexFieldStructure indexFieldStructure, IndexFieldValueCardinality indexFieldValueCardinality,
			FieldTypeDescriptor<F> fieldTypeDescriptor, SortMode sortMode) {
		this.indexFieldStructure = indexFieldStructure;
		this.indexFieldValueCardinality = indexFieldValueCardinality;
		this.fieldTypeDescriptor = fieldTypeDescriptor;
		this.sortMode = sortMode;
	}

	@Test
	@TestForIssue(jiraKey = { "HSEARCH-3798", "HSEARCH-2252", "HSEARCH-2254", "HSEARCH-3103" })
	public void simple() {
		assumeTestParametersWork();

		SearchQuery<DocumentReference> query;
		String fieldPathForAscendingOrderTests = getFieldPath( SortOrder.ASC );
		String fieldPathForDescendingOrderTests = getFieldPath( SortOrder.DESC );

		// Default order
		query = matchNonEmptyQuery( b -> b.field( fieldPathForAscendingOrderTests ) );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );

		// Explicit order
		query = matchNonEmptyQuery( b -> b.field( fieldPathForAscendingOrderTests ).asc() );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );
		query = matchNonEmptyQuery( b -> b.field( fieldPathForDescendingOrderTests ).desc() );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_3, DOCUMENT_2, DOCUMENT_1 );
	}

	@Test
	@TestForIssue(jiraKey = { "HSEARCH-3103" })
	public void medianWithNestedField() {
		Assume.assumeTrue(
				"This test is only relevant when using SortMode.MEDIAN in nested fields",
				isMedianWithNestedField() && !isSumOrAvgOrMedianWithStringField()
		);

		String fieldPath = getFieldPath( SortOrder.ASC );

		SubTest.expectException( () -> matchNonEmptyQuery( b -> b.field( fieldPath ) ) )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Cannot compute the median across nested documents",
						fieldPath
				);
	}

	@Test
	@TestForIssue(jiraKey = { "HSEARCH-3103" })
	public void sumOrAvgOrMedianWithStringField() {
		Assume.assumeTrue(
				"This test is only relevant when using SortMode.SUM/AVG/MEDIAN on String fields",
				isSumOrAvgOrMedianWithStringField()
		);

		String fieldPath = getFieldPath( SortOrder.ASC );

		SubTest.expectException( () -> matchNonEmptyQuery( b -> b.field( fieldPath ) ) )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Cannot compute the sum, average or median of a text field",
						"Only min and max are supported",
						fieldPath
				);
	}

	@Test
	@TestForIssue(jiraKey = { "HSEARCH-3103" })
	public void sumWithTemporalField() {
		Assume.assumeTrue(
				"This test is only relevant when using SortMode.SUM on Temporal fields",
				isSumWithTemporalField()
		);

		String fieldPath = getFieldPath( SortOrder.ASC );

		SubTest.expectException( () -> matchNonEmptyQuery( b -> b.field( fieldPath ) ) )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Cannot compute the sum of a temporal field",
						"Only min, max, avg and median are supported",
						fieldPath
				);
	}

	@Test
	public void missingValue() {
		assumeTestParametersWork();

		SearchQuery<DocumentReference> query;

		String fieldPathForAscendingOrderTests = getFieldPath( SortOrder.ASC );
		String fieldPathForDescendingOrderTests = getFieldPath( SortOrder.DESC );

		// Default order
		query = matchNonEmptyAndEmpty1Query( f -> f.field( fieldPathForAscendingOrderTests ).missing().last() );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, EMPTY_1 );

		// Explicit order with missing().last()
		query = matchNonEmptyAndEmpty1Query( f -> f.field( fieldPathForAscendingOrderTests ).asc().missing().last() );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, EMPTY_1 );
		query = matchNonEmptyAndEmpty1Query( f -> f.field( fieldPathForDescendingOrderTests ).desc().missing().last() );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_3, DOCUMENT_2, DOCUMENT_1, EMPTY_1 );

		// Explicit order with missing().first()
		query = matchNonEmptyAndEmpty1Query( f -> f.field( fieldPathForAscendingOrderTests ).asc().missing().first() );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, EMPTY_1, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );
		query = matchNonEmptyAndEmpty1Query( f -> f.field( fieldPathForDescendingOrderTests ).desc().missing().first() );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, EMPTY_1, DOCUMENT_3, DOCUMENT_2, DOCUMENT_1 );

		// Explicit order with missing().use( ... )
		query = matchNonEmptyAndEmpty1Query( f -> f.field( fieldPathForAscendingOrderTests ).asc()
				.missing().use( getSingleValueForMissingUse( BEFORE_DOCUMENT_1_ORDINAL ) ) );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, EMPTY_1, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );
		query = matchNonEmptyAndEmpty1Query( f -> f.field( fieldPathForAscendingOrderTests ).asc()
				.missing().use( getSingleValueForMissingUse( BETWEEN_DOCUMENT_1_AND_2_ORDINAL ) ) );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, EMPTY_1, DOCUMENT_2, DOCUMENT_3 );
		query = matchNonEmptyAndEmpty1Query( f -> f.field( fieldPathForAscendingOrderTests ).asc()
				.missing().use( getSingleValueForMissingUse( BETWEEN_DOCUMENT_2_AND_3_ORDINAL ) ) );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, EMPTY_1, DOCUMENT_3 );
		query = matchNonEmptyAndEmpty1Query( f -> f.field( fieldPathForAscendingOrderTests ).asc()
				.missing().use( getSingleValueForMissingUse( AFTER_DOCUMENT_3_ORDINAL ) ) );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, EMPTY_1 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3254")
	public void missingValue_multipleEmpty() {
		assumeTestParametersWork();

		List<DocumentReference> docRefHits;
		String fieldPathForAscendingOrderTests = getFieldPath( SortOrder.ASC );

		// using before 1 value
		docRefHits = matchAllQuery( f -> f.field( fieldPathForAscendingOrderTests ).asc()
				.missing().use( getSingleValueForMissingUse( BEFORE_DOCUMENT_1_ORDINAL ) ) )
				.fetchAllHits();
		assertThat( docRefHits ).ordinals( 0, 1, 2, 3 )
				.hasDocRefHitsAnyOrder( INDEX_NAME, EMPTY_1, EMPTY_2, EMPTY_3, EMPTY_4 );
		assertThat( docRefHits ).ordinal( 4 ).isDocRefHit( INDEX_NAME, DOCUMENT_1 );
		assertThat( docRefHits ).ordinal( 5 ).isDocRefHit( INDEX_NAME, DOCUMENT_2 );
		assertThat( docRefHits ).ordinal( 6 ).isDocRefHit( INDEX_NAME, DOCUMENT_3 );

		// using between 1 and 2 value
		docRefHits = matchAllQuery( f -> f.field( fieldPathForAscendingOrderTests ).asc()
				.missing().use( getSingleValueForMissingUse( BETWEEN_DOCUMENT_1_AND_2_ORDINAL ) ) )
				.fetchAllHits();
		assertThat( docRefHits ).ordinal( 0 ).isDocRefHit( INDEX_NAME, DOCUMENT_1 );
		assertThat( docRefHits ).ordinals( 1, 2, 3, 4 )
				.hasDocRefHitsAnyOrder( INDEX_NAME, EMPTY_1, EMPTY_2, EMPTY_3, EMPTY_4 );
		assertThat( docRefHits ).ordinal( 5 ).isDocRefHit( INDEX_NAME, DOCUMENT_2 );
		assertThat( docRefHits ).ordinal( 6 ).isDocRefHit( INDEX_NAME, DOCUMENT_3 );

		// using between 2 and 3 value
		docRefHits = matchAllQuery( f -> f.field( fieldPathForAscendingOrderTests ).asc().missing()
				.use( getSingleValueForMissingUse( BETWEEN_DOCUMENT_2_AND_3_ORDINAL ) ) )
				.fetchAllHits();
		assertThat( docRefHits ).ordinal( 0 ).isDocRefHit( INDEX_NAME, DOCUMENT_1 );
		assertThat( docRefHits ).ordinal( 1 ).isDocRefHit( INDEX_NAME, DOCUMENT_2 );
		assertThat( docRefHits ).ordinals( 2, 3, 4, 5 )
				.hasDocRefHitsAnyOrder( INDEX_NAME, EMPTY_1, EMPTY_2, EMPTY_3, EMPTY_4 );
		assertThat( docRefHits ).ordinal( 6 ).isDocRefHit( INDEX_NAME, DOCUMENT_3 );

		// using after 3 value
		docRefHits = matchAllQuery( f -> f.field( fieldPathForAscendingOrderTests ).asc()
				.missing().use( getSingleValueForMissingUse( AFTER_DOCUMENT_3_ORDINAL ) ) ).fetchAllHits();
		assertThat( docRefHits ).ordinal( 0 ).isDocRefHit( INDEX_NAME, DOCUMENT_1 );
		assertThat( docRefHits ).ordinal( 1 ).isDocRefHit( INDEX_NAME, DOCUMENT_2 );
		assertThat( docRefHits ).ordinal( 2 ).isDocRefHit( INDEX_NAME, DOCUMENT_3 );
		assertThat( docRefHits ).ordinals( 3, 4, 5, 6 )
				.hasDocRefHitsAnyOrder( INDEX_NAME, EMPTY_1, EMPTY_2, EMPTY_3, EMPTY_4 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3254")
	public void missingValue_multipleEmpty_useExistingDocumentValue() {
		assumeTestParametersWork();

		List<DocumentReference> docRefHits;
		String fieldPathForAscendingOrderTests = getFieldPath( SortOrder.ASC );

		Object docValue1 = getSingleValueForMissingUse( DOCUMENT_1_ORDINAL );
		Object docValue2 = getSingleValueForMissingUse( DOCUMENT_2_ORDINAL );
		Object docValue3 = getSingleValueForMissingUse( DOCUMENT_3_ORDINAL );

		// using doc 1 value
		docRefHits = matchAllQuery( f -> f.field( fieldPathForAscendingOrderTests ).asc()
				.missing().use( docValue1 ) ).fetchAllHits();
		assertThat( docRefHits ).ordinals( 0, 1, 2, 3, 4 )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, EMPTY_1, EMPTY_2, EMPTY_3, EMPTY_4 );
		assertThat( docRefHits ).ordinal( 5 ).isDocRefHit( INDEX_NAME, DOCUMENT_2 );
		assertThat( docRefHits ).ordinal( 6 ).isDocRefHit( INDEX_NAME, DOCUMENT_3 );

		// using doc 2 value
		docRefHits = matchAllQuery( f -> f.field( fieldPathForAscendingOrderTests ).asc()
				.missing().use( docValue2 ) ).fetchAllHits();
		assertThat( docRefHits ).ordinal( 0 ).isDocRefHit( INDEX_NAME, DOCUMENT_1 );
		assertThat( docRefHits ).ordinals( 1, 2, 3, 4, 5 )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_2, EMPTY_1, EMPTY_2, EMPTY_3, EMPTY_4 );
		assertThat( docRefHits ).ordinal( 6 ).isDocRefHit( INDEX_NAME, DOCUMENT_3 );

		// using doc 3 value
		docRefHits = matchAllQuery( f -> f.field( fieldPathForAscendingOrderTests ).asc()
				.missing().use( docValue3 ) ).fetchAllHits();
		assertThat( docRefHits ).ordinal( 0 ).isDocRefHit( INDEX_NAME, DOCUMENT_1 );
		assertThat( docRefHits ).ordinal( 1 ).isDocRefHit( INDEX_NAME, DOCUMENT_2 );
		assertThat( docRefHits ).ordinals( 2, 3, 4, 5, 6 )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_3, EMPTY_1, EMPTY_2, EMPTY_3, EMPTY_4 );
	}

	private SearchQuery<DocumentReference> matchNonEmptyQuery(
			Function<? super SearchSortFactory, ? extends FieldSortOptionsStep<?>> sortContributor) {
		return matchNonEmptyQuery( sortContributor, indexManager.createScope() );
	}

	private SearchQuery<DocumentReference> matchNonEmptyQuery(
			Function<? super SearchSortFactory, ? extends FieldSortOptionsStep<?>> sortContributor, StubMappingScope scope) {
		return query(
				f -> f.matchAll().except( f.id().matchingAny( Arrays.asList( EMPTY_1, EMPTY_2, EMPTY_3, EMPTY_4 ) ) ),
				sortContributor,
				scope
		);
	}

	private SearchQuery<DocumentReference> matchNonEmptyAndEmpty1Query(
			Function<? super SearchSortFactory, ? extends FieldSortOptionsStep<?>> sortContributor) {
		return matchNonEmptyAndEmpty1Query( sortContributor, indexManager.createScope() );
	}

	private SearchQuery<DocumentReference> matchNonEmptyAndEmpty1Query(
			Function<? super SearchSortFactory, ? extends FieldSortOptionsStep<?>> sortContributor, StubMappingScope scope) {
		return query(
				f -> f.matchAll().except( f.id().matchingAny( Arrays.asList( EMPTY_2, EMPTY_3, EMPTY_4 ) ) ),
				sortContributor,
				scope
		);
	}

	private SearchQuery<DocumentReference> matchAllQuery(
			Function<? super SearchSortFactory, ? extends FieldSortOptionsStep<?>> sortContributor) {
		return matchAllQuery( sortContributor, indexManager.createScope() );
	}

	private SearchQuery<DocumentReference> matchAllQuery(
			Function<? super SearchSortFactory, ? extends FieldSortOptionsStep<?>> sortContributor, StubMappingScope scope) {
		return query( f -> f.matchAll(), sortContributor, scope );
	}

	private SearchQuery<DocumentReference> query(
			Function<? super SearchPredicateFactory, ? extends PredicateFinalStep> predicateContributor,
			Function<? super SearchSortFactory, ? extends FieldSortOptionsStep<?>> sortContributor,
			StubMappingScope scope) {
		return scope.query()
				.where( predicateContributor )
				.sort( sortContributor.andThen( this::applySortMode ) )
				.toQuery();
	}

	private FieldSortOptionsStep<?> applySortMode(FieldSortOptionsStep<?> optionsStep) {
		if ( sortMode != null ) {
			return optionsStep.mode( sortMode );
		}
		else {
			return optionsStep;
		}
	}

	private void assumeTestParametersWork() {
		Assume.assumeFalse(
				"This combination is not expected to work",
				isMedianWithNestedField() || isSumOrAvgOrMedianWithStringField() || isSumWithTemporalField()
		);
		Assume.assumeTrue(
				"This combination is buggy with this backend",
				TckConfiguration.get().getBackendFeatures()
						.sortByFieldValue( indexFieldStructure, fieldTypeDescriptor.getJavaType(), sortMode )
		);
	}

	private boolean isSumOrAvgOrMedianWithStringField() {
		return EnumSet.of( SortMode.SUM, SortMode.AVG, SortMode.MEDIAN ).contains( sortMode )
				&& String.class.equals( fieldTypeDescriptor.getJavaType() );
	}

	private boolean isSumWithTemporalField() {
		return SortMode.SUM.equals( sortMode )
				&& (
						Temporal.class.isAssignableFrom( fieldTypeDescriptor.getJavaType() )
						|| MonthDay.class.equals( fieldTypeDescriptor.getJavaType() )
				);
	}

	private boolean isMedianWithNestedField() {
		return SortMode.MEDIAN.equals( sortMode )
				&& EnumSet.of( IndexFieldStructure.IN_NESTED, IndexFieldStructure.IN_NESTED_TWICE )
				.contains( indexFieldStructure );
	}

	private String getFieldPath(SortOrder expectedOrder) {
		switch ( indexFieldStructure ) {
			case ROOT:
				return getRelativeFieldName( indexMapping, expectedOrder );
			case IN_FLATTENED:
				return indexMapping.flattenedObject.relativeFieldName
						+ "." + getRelativeFieldName( indexMapping.flattenedObject, expectedOrder );
			case IN_NESTED:
				return indexMapping.nestedObject.relativeFieldName
						+ "." + getRelativeFieldName( indexMapping.nestedObject, expectedOrder );
			case IN_NESTED_TWICE:
				return indexMapping.nestedObject.relativeFieldName
						+ "." + indexMapping.nestedObject.nestedObject.relativeFieldName
						+ "." + getRelativeFieldName( indexMapping.nestedObject.nestedObject, expectedOrder );
			default:
				throw new IllegalStateException( "Unexpected value: " + indexFieldStructure );
		}
	}

	private String getRelativeFieldName(AbstractObjectMapping mapping, SortOrder expectedOrder) {
		return getFieldModelsByType( mapping, expectedOrder ).get( fieldTypeDescriptor ).relativeFieldName;
	}

	private SimpleFieldModelsByType getFieldModelsByType(AbstractObjectMapping mapping, SortOrder expectedOrder) {
		switch ( indexFieldValueCardinality ) {
			case SINGLE_VALUED:
				// Sort on a single-valued field.
				return mapping.fieldWithSingleValueModels;
			case MULTI_VALUED:
				// Sort on a multi-valued field.
				// We must chose the field carefuly so that documents are in the expected order for the configured sort mode.
				if ( sortMode == null ) {
					// Default sort mode: min in ascending order, max in descending order
					switch ( expectedOrder ) {
						case ASC:
							return mapping.fieldWithAscendingMinModels;
						case DESC:
							return mapping.fieldWithAscendingMaxModels;
						default:
							throw new IllegalStateException( "Unexpected sort order: " + expectedOrder );
					}
				}
				else {
					switch ( sortMode ) {
						case SUM:
							return mapping.fieldWithAscendingSumModels;
						case MIN:
							return mapping.fieldWithAscendingMinModels;
						case MAX:
							return mapping.fieldWithAscendingMaxModels;
						case AVG:
							return mapping.fieldWithAscendingAvgModels;
						case MEDIAN:
							return mapping.fieldWithAscendingMedianModels;
						default:
							throw new IllegalStateException( "Unexpected sort mode: " + sortMode );
					}
				}
			default:
				throw new IllegalStateException( "Unexpected field value cardinality: " + indexFieldValueCardinality );
		}
	}

	private static void initDocument(IndexMapping indexMapping, DocumentElement document, Integer ordinal) {
		initAllFields( indexMapping, document, ordinal );

		// Note: these objects must be single-valued for these tests
		DocumentElement flattenedObject = document.addObject( indexMapping.flattenedObject.self );
		DocumentElement nestedObject = document.addObject( indexMapping.nestedObject.self );
		DocumentElement nestedObjectInNestedObject = nestedObject.addObject( indexMapping.nestedObject.nestedObject.self );

		initAllFields( indexMapping.flattenedObject, flattenedObject, ordinal );
		initAllFields( indexMapping.nestedObject, nestedObject, ordinal );
		initAllFields( indexMapping.nestedObject.nestedObject, nestedObjectInNestedObject, ordinal );
	}

	private static void initAllFields(AbstractObjectMapping mapping, DocumentElement document, Integer ordinal) {
		mapping.fieldWithSingleValueModels.forEach(
				fieldModel -> addSingleValue( fieldModel, document, ordinal )
		);
		mapping.fieldWithAscendingMinModels.forEach(
				fieldModel -> addMultipleValues( fieldModel, document, SortMode.MIN, ordinal )
		);
		mapping.fieldWithAscendingMaxModels.forEach(
				fieldModel -> addMultipleValues( fieldModel, document, SortMode.MAX, ordinal )
		);
		mapping.fieldWithAscendingSumModels.forEach(
				fieldModel -> addMultipleValues( fieldModel, document, SortMode.SUM, ordinal )
		);
		mapping.fieldWithAscendingAvgModels.forEach(
				fieldModel -> addMultipleValues( fieldModel, document, SortMode.AVG, ordinal )
		);
		mapping.fieldWithAscendingMedianModels.forEach(
				fieldModel -> addMultipleValues( fieldModel, document, SortMode.MEDIAN, ordinal )
		);
	}

	@SuppressWarnings("unchecked")
	private F getSingleValueForMissingUse(int ordinal) {
		F value = fieldTypeDescriptor.getAscendingUniqueTermValues().getSingle().get( ordinal );

		if ( fieldTypeDescriptor instanceof NormalizedStringFieldTypeDescriptor
				&& !TckConfiguration.get().getBackendFeatures().normalizesStringMissingValues() ) {
			// The backend doesn't normalize missing value replacements automatically, we have to do it ourselves
			// TODO HSEARCH-3387 Remove this once all backends correctly normalize missing value replacements
			value = (F) ( (String) value ).toLowerCase( Locale.ROOT );
		}

		return value;
	}

	private static <F> void addSingleValue(SimpleFieldModel<F> fieldModel, DocumentElement documentElement, Integer ordinal) {
		if ( ordinal == null ) {
			return;
		}
		documentElement.addValue(
				fieldModel.reference,
				fieldModel.typeDescriptor.getAscendingUniqueTermValues().getSingle().get( ordinal )
		);
	}

	private static <F> void addMultipleValues(SimpleFieldModel<F> fieldModel, DocumentElement documentElement,
			SortMode sortMode, Integer ordinal) {
		if ( ordinal == null ) {
			return;
		}
		fieldModel.typeDescriptor.getAscendingUniqueTermValues().getMultiResultingInSingle( sortMode ).get( ordinal )
				.forEach( value -> documentElement.addValue( fieldModel.reference, value ) );
	}

	private static void initData() {
		IndexIndexingPlan<?> plan = indexManager.createIndexingPlan();
		// Important: do not index the documents in the expected order after sorts (1, 2, 3)
		plan.add( referenceProvider( EMPTY_4 ),
				document -> initDocument( indexMapping, document, null ) );
		plan.add( referenceProvider( DOCUMENT_2 ),
				document -> initDocument( indexMapping, document, DOCUMENT_2_ORDINAL ) );
		plan.add( referenceProvider( EMPTY_1 ),
				document -> initDocument( indexMapping, document, null ) );
		plan.add( referenceProvider( DOCUMENT_1 ),
				document -> initDocument( indexMapping, document, DOCUMENT_1_ORDINAL ) );
		plan.add( referenceProvider( EMPTY_2 ),
				document -> initDocument( indexMapping, document, null ) );
		plan.add( referenceProvider( DOCUMENT_3 ),
				document -> initDocument( indexMapping, document, DOCUMENT_3_ORDINAL ) );
		plan.add( referenceProvider( EMPTY_3 ),
				document -> initDocument( indexMapping, document, null ) );
		plan.execute().join();

		// Check that all documents are searchable
		SearchQuery<DocumentReference> query = indexManager.createScope().query()
				.where( f -> f.matchAll() )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, EMPTY_1, EMPTY_2, EMPTY_3, EMPTY_4 );
	}

	private static class AbstractObjectMapping {
		final SimpleFieldModelsByType fieldWithSingleValueModels;
		final SimpleFieldModelsByType fieldWithAscendingSumModels;
		final SimpleFieldModelsByType fieldWithAscendingMinModels;
		final SimpleFieldModelsByType fieldWithAscendingMaxModels;
		final SimpleFieldModelsByType fieldWithAscendingAvgModels;
		final SimpleFieldModelsByType fieldWithAscendingMedianModels;

		AbstractObjectMapping(IndexSchemaElement self,
				Consumer<StandardIndexFieldTypeOptionsStep<?, ?>> additionalConfiguration) {
			fieldWithSingleValueModels = SimpleFieldModelsByType.mapAll( supportedTypeDescriptors(), self,
					"single_", c -> c.sortable( Sortable.YES ), additionalConfiguration );
			fieldWithAscendingSumModels = SimpleFieldModelsByType.mapAllMultiValued( supportedTypeDescriptors(), self,
					"multi_ascendingsum_", c -> c.sortable( Sortable.YES ), additionalConfiguration );
			fieldWithAscendingMinModels = SimpleFieldModelsByType.mapAllMultiValued( supportedTypeDescriptors(), self,
					"multi_ascendingmin_", c -> c.sortable( Sortable.YES ), additionalConfiguration );
			fieldWithAscendingMaxModels = SimpleFieldModelsByType.mapAllMultiValued( supportedTypeDescriptors(), self,
					"multi_ascendingmax_", c -> c.sortable( Sortable.YES ), additionalConfiguration );
			fieldWithAscendingAvgModels = SimpleFieldModelsByType.mapAllMultiValued( supportedTypeDescriptors(), self,
					"multi_ascendingavg_", c -> c.sortable( Sortable.YES ), additionalConfiguration );
			fieldWithAscendingMedianModels = SimpleFieldModelsByType.mapAllMultiValued( supportedTypeDescriptors(), self,
					"multi_ascendingmedian_", c -> c.sortable( Sortable.YES ), additionalConfiguration );
		}
	}

	private static class IndexMapping extends AbstractObjectMapping {
		final FirstLevelObjectMapping flattenedObject;
		final FirstLevelObjectMapping nestedObject;

		IndexMapping(IndexSchemaElement root) {
			this( root, ignored -> { } );
		}

		IndexMapping(IndexSchemaElement root,
				Consumer<StandardIndexFieldTypeOptionsStep<?, ?>> additionalConfiguration) {
			super( root, additionalConfiguration );

			flattenedObject = FirstLevelObjectMapping.create( root, "flattenedObject",
					ObjectFieldStorage.FLATTENED, additionalConfiguration );
			nestedObject = FirstLevelObjectMapping.create( root, "nestedObject",
					ObjectFieldStorage.NESTED, additionalConfiguration );
		}
	}

	private static class FirstLevelObjectMapping extends AbstractObjectMapping {
		final String relativeFieldName;
		final IndexObjectFieldReference self;

		final SecondLevelObjectMapping nestedObject;

		public static FirstLevelObjectMapping create(IndexSchemaElement parent, String relativeFieldName,
				ObjectFieldStorage storage,
				Consumer<StandardIndexFieldTypeOptionsStep<?, ?>> additionalConfiguration) {
			IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, storage );
			return new FirstLevelObjectMapping( relativeFieldName, objectField, additionalConfiguration );
		}

		private FirstLevelObjectMapping(String relativeFieldName, IndexSchemaObjectField objectField,
				Consumer<StandardIndexFieldTypeOptionsStep<?, ?>> additionalConfiguration) {
			super( objectField, additionalConfiguration );
			this.relativeFieldName = relativeFieldName;
			self = objectField.toReference();

			nestedObject = SecondLevelObjectMapping.create( objectField, "nestedObject",
					ObjectFieldStorage.NESTED, additionalConfiguration );
		}
	}

	private static class SecondLevelObjectMapping extends AbstractObjectMapping {
		final String relativeFieldName;
		final IndexObjectFieldReference self;

		public static SecondLevelObjectMapping create(IndexSchemaElement parent, String relativeFieldName,
				ObjectFieldStorage storage,
				Consumer<StandardIndexFieldTypeOptionsStep<?, ?>> additionalConfiguration) {
			IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, storage );
			return new SecondLevelObjectMapping( relativeFieldName, objectField, additionalConfiguration );
		}

		private SecondLevelObjectMapping(String relativeFieldName, IndexSchemaObjectField objectField,
				Consumer<StandardIndexFieldTypeOptionsStep<?, ?>> additionalConfiguration) {
			super( objectField, additionalConfiguration );
			this.relativeFieldName = relativeFieldName;
			self = objectField.toReference();
		}
	}

}
