/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.search;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.referenceProvider;

import java.time.MonthDay;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.hibernate.search.backend.lucene.LuceneExtension;
import org.hibernate.search.backend.lucene.search.query.LuceneSearchQuery;
import org.hibernate.search.backend.lucene.search.query.LuceneSearchResult;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.search.common.SortMode;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.sort.dsl.FieldSortOptionsStep;
import org.hibernate.search.engine.search.sort.dsl.SortOrder;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.ExpectationsAlternative;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModel;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModelsByType;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TestedFieldStructure;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
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
import org.assertj.core.api.Assertions;

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

	private static Stream<FieldTypeDescriptor<?>> supportedTypeDescriptors() {
		return FieldTypeDescriptor.getAll().stream()
				.filter( typeDescriptor -> typeDescriptor.getFieldSortExpectations().isSupported() );
	}

	@Parameterized.Parameters(name = "{0} - {2} - {1}")
	public static Object[][] parameters() {
		List<Object[]> parameters = new ArrayList<>();
		supportedTypeDescriptors().forEach( fieldTypeDescriptor -> {
			ExpectationsAlternative<?, ?> expectations = fieldTypeDescriptor.getFieldSortExpectations();
			if ( expectations.isSupported() ) {
				for ( TestedFieldStructure fieldStructure : TestedFieldStructure.all() ) {
					parameters.add( new Object[] { fieldStructure, fieldTypeDescriptor, null } );
					for ( SortMode sortMode : SortMode.values() ) {
						parameters.add( new Object[] { fieldStructure, fieldTypeDescriptor, sortMode } );
					}
				}
			}
		} );
		return parameters.toArray( new Object[0][] );
	}

	private static final String SEGMENT_0 = "seg0";
	private static final String SEGMENT_1 = "seg1";

	private static final String SEGMENT_0_DOC_0 = "0_0";
	private static final String SEGMENT_0_DOC_1 = "0_1";
	private static final String SEGMENT_0_DOC_EMPTY = "0_nonMatching";
	private static final String SEGMENT_1_DOC_0 = "1_0";
	private static final String SEGMENT_1_DOC_EMPTY = "1_nonMatching";

	private static final int DOCUMENT_1_ORDINAL = 1;
	private static final int DOCUMENT_2_ORDINAL = 3;
	private static final int DOCUMENT_3_ORDINAL = 5;

	@ClassRule
	public static final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private static final SimpleMappedIndex<IndexBinding> index =
			SimpleMappedIndex.of( "MainIndex", IndexBinding::new );

	@BeforeClass
	public static void setup() {
		setupHelper.start()
				.withIndex( index )
				.setup();

		initData();
	}

	private final TestedFieldStructure fieldStructure;
	private final FieldTypeDescriptor<F> fieldTypeDescriptor;
	private final SortMode sortMode;

	public LuceneSearchTopDocsMergeFieldSortIT(TestedFieldStructure fieldStructure,
			FieldTypeDescriptor<F> fieldTypeDescriptor, SortMode sortMode) {
		this.fieldStructure = fieldStructure;
		this.fieldTypeDescriptor = fieldTypeDescriptor;
		this.sortMode = sortMode;
	}

	@Test
	public void asc() {
		assumeTestParametersWork();

		LuceneSearchQuery<DocumentReference> segment0Query = matchNonEmptySortedByFieldQuery( SortOrder.ASC, SEGMENT_0 );
		LuceneSearchQuery<DocumentReference> segment1Query = matchNonEmptySortedByFieldQuery( SortOrder.ASC, SEGMENT_1 );
		LuceneSearchResult segment0Result = segment0Query.fetch( 10 );
		LuceneSearchResult segment1Result = segment1Query.fetch( 10 );
		assertThat( segment0Result )
				.hasDocRefHitsExactOrder( index.name(), SEGMENT_0_DOC_1, SEGMENT_0_DOC_0 );
		assertThat( segment1Result )
				.hasDocRefHitsExactOrder( index.name(), SEGMENT_1_DOC_0 );

		TopFieldDocs[] allTopDocs = retrieveTopDocs( segment0Query, segment0Result, segment1Result );
		Assertions.assertThat( TopDocs.merge( segment0Query.getLuceneSort(), 10, allTopDocs ).scoreDocs )
				.containsExactly(
						allTopDocs[0].scoreDocs[0], // SEGMENT_0_DOC_1
						allTopDocs[1].scoreDocs[0], // SEGMENT_1_DOC_0
						allTopDocs[0].scoreDocs[1] // SEGMENT_0_DOC_0
				);
	}

	// Also check descending order, to be sure the above didn't just pass by chance
	@Test
	public void desc() {
		assumeTestParametersWork();

		LuceneSearchQuery<DocumentReference> segment0Query = matchNonEmptySortedByFieldQuery( SortOrder.DESC, SEGMENT_0 );
		LuceneSearchQuery<DocumentReference> segment1Query = matchNonEmptySortedByFieldQuery( SortOrder.DESC, SEGMENT_1 );
		LuceneSearchResult segment0Result = segment0Query.fetch( 10 );
		LuceneSearchResult segment1Result = segment1Query.fetch( 10 );
		assertThat( segment0Result )
				.hasDocRefHitsExactOrder( index.name(), SEGMENT_0_DOC_0, SEGMENT_0_DOC_1 );
		assertThat( segment1Result )
				.hasDocRefHitsExactOrder( index.name(), SEGMENT_1_DOC_0 );

		TopFieldDocs[] allTopDocs = retrieveTopDocs( segment0Query, segment0Result, segment1Result );
		Assertions.assertThat( TopDocs.merge( segment0Query.getLuceneSort(), 10, allTopDocs ).scoreDocs )
				.containsExactly(
						allTopDocs[0].scoreDocs[0], // SEGMENT_0_DOC_0
						allTopDocs[1].scoreDocs[0], // SEGMENT_1_DOC_0
						allTopDocs[0].scoreDocs[1] // SEGMENT_0_DOC_1
				);
	}

	private LuceneSearchQuery<DocumentReference> matchNonEmptySortedByFieldQuery(SortOrder sortOrder, String routingKey) {
		StubMappingScope scope = index.createScope();
		return scope.query().extension( LuceneExtension.get() )
				.where( f -> f.matchAll().except( f.id().matchingAny( Arrays.asList( SEGMENT_0_DOC_EMPTY, SEGMENT_1_DOC_EMPTY ) ) ) )
				.sort( f -> applyFilter( applySortMode(
						scope.sort().field( getFieldPath( sortOrder ) ).order( sortOrder )
				) ) )
				.routing( routingKey )
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
					.field( getFieldPath( parent -> "discriminator" ) )
					.matching( "included" ) );
		}
		else {
			return optionsStep;
		}
	}

	private TopFieldDocs[] retrieveTopDocs(LuceneSearchQuery<?> query, LuceneSearchResult ... results) {
		Sort sort = query.getLuceneSort();
		TopFieldDocs[] allTopDocs = new TopFieldDocs[results.length];
		for ( int i = 0; i < results.length; i++ ) {
			TopDocs topDocs = results[i].getTopDocs();
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
				&& fieldStructure.isInNested();
	}

	private String getFieldPath(SortOrder expectedOrder) {
		return getFieldPath( parentMapping -> getRelativeFieldName( parentMapping, expectedOrder ) );
	}

	private String getFieldPath(Function<AbstractObjectMapping, String> relativeFieldNameFunction) {
		switch ( fieldStructure.location ) {
			case ROOT:
				return relativeFieldNameFunction.apply( index.binding() );
			case IN_FLATTENED:
				return index.binding().flattenedObject.relativeFieldName
						+ "." + relativeFieldNameFunction.apply( index.binding().flattenedObject );
			case IN_NESTED:
				return index.binding().nestedObject.relativeFieldName
						+ "." + relativeFieldNameFunction.apply( index.binding().nestedObject );
			case IN_NESTED_TWICE:
				return index.binding().nestedObject.relativeFieldName
						+ "." + index.binding().nestedObject.nestedObject.relativeFieldName
						+ "." + relativeFieldNameFunction.apply( index.binding().nestedObject.nestedObject );
			default:
				throw new IllegalStateException( "Unexpected value: " + fieldStructure.location );
		}
	}

	private String getRelativeFieldName(AbstractObjectMapping mapping, SortOrder expectedOrder) {
		return getFieldModelsByType( mapping, expectedOrder ).get( fieldTypeDescriptor ).relativeFieldName;
	}

	private SimpleFieldModelsByType getFieldModelsByType(AbstractObjectMapping mapping, SortOrder expectedOrder) {
		if ( fieldStructure.isSingleValued() ) {
			return mapping.fieldWithSingleValueModels;
		}
		else {
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
		}
	}

	private static void initDocument(IndexBinding indexBinding, DocumentElement document, Integer ordinal) {
		initAllFields( indexBinding, document, ordinal );

		DocumentElement flattenedObject = document.addObject( indexBinding.flattenedObject.self );
		initAllFields( indexBinding.flattenedObject, flattenedObject, ordinal );

		// The nested object is split into four objects:
		// the first two are included by the filter and each hold part of the values that will be sorted on,
		// and the last two are excluded by the filter and hold garbage values that, if they were taken into account,
		// would mess with the sort order and eventually fail at least *some* tests.

		DocumentElement nestedObject0 = document.addObject( indexBinding.nestedObject.self );
		nestedObject0.addValue( indexBinding.nestedObject.discriminator, "included" );
		initAllFields( indexBinding.nestedObject, nestedObject0, ordinal, ValueSelection.FIRST_PARTITION );

		DocumentElement nestedObject1 = document.addObject( indexBinding.nestedObject.self );
		nestedObject1.addValue( indexBinding.nestedObject.discriminator, "included" );
		initAllFields( indexBinding.nestedObject, nestedObject1, ordinal, ValueSelection.SECOND_PARTITION );

		DocumentElement nestedObject2 = document.addObject( indexBinding.nestedObject.self );
		nestedObject2.addValue( indexBinding.nestedObject.discriminator, "excluded" );
		initAllFields( indexBinding.nestedObject, nestedObject2, ordinal == null ? null : ordinal - 1 );

		DocumentElement nestedObject3 = document.addObject( indexBinding.nestedObject.self );
		nestedObject3.addValue( indexBinding.nestedObject.discriminator, "excluded" );
		initAllFields( indexBinding.nestedObject, nestedObject3, ordinal == null ? null : ordinal + 1 );

		// Same for the second level of nesting

		DocumentElement nestedNestedObject0 = nestedObject0.addObject( indexBinding.nestedObject.nestedObject.self );
		nestedNestedObject0.addValue( indexBinding.nestedObject.nestedObject.discriminator, "included" );
		initAllFields( indexBinding.nestedObject.nestedObject, nestedNestedObject0, ordinal, ValueSelection.FIRST_PARTITION );

		DocumentElement nestedNestedObject1 = nestedObject1.addObject( indexBinding.nestedObject.nestedObject.self );
		nestedNestedObject1.addValue( indexBinding.nestedObject.nestedObject.discriminator, "included" );
		initAllFields( indexBinding.nestedObject.nestedObject, nestedNestedObject1, ordinal, ValueSelection.SECOND_PARTITION );

		DocumentElement nestedNestedObject2 = nestedObject0.addObject( indexBinding.nestedObject.nestedObject.self );
		nestedNestedObject2.addValue( indexBinding.nestedObject.nestedObject.discriminator, "excluded" );
		initAllFields( indexBinding.nestedObject.nestedObject, nestedNestedObject2, ordinal == null ? null : ordinal - 1 );

		DocumentElement nestedNestedObject3 = nestedObject1.addObject( indexBinding.nestedObject.nestedObject.self );
		nestedNestedObject3.addValue( indexBinding.nestedObject.nestedObject.discriminator, "excluded" );
		initAllFields( indexBinding.nestedObject.nestedObject, nestedNestedObject3, ordinal == null ? null : ordinal + 1 );
	}

	private static void initAllFields(AbstractObjectMapping mapping, DocumentElement document, Integer ordinal) {
		initAllFields( mapping, document, ordinal, ValueSelection.ALL );
	}

	private static void initAllFields(AbstractObjectMapping mapping, DocumentElement document, Integer ordinal,
			ValueSelection valueSelection) {
		if ( EnumSet.of( ValueSelection.ALL, ValueSelection.FIRST_PARTITION ).contains( valueSelection ) ) {
			mapping.fieldWithSingleValueModels.forEach(
					fieldModel -> addSingleValue( fieldModel, document, ordinal )
			);
		}

		Integer startIndexForMultiValued;
		Integer endIndexForMultiValued;

		switch ( valueSelection ) {
			case FIRST_PARTITION:
				startIndexForMultiValued = 0;
				endIndexForMultiValued = 1;
				break;
			case SECOND_PARTITION:
				startIndexForMultiValued = 1;
				endIndexForMultiValued = null;
				break;
			case ALL:
			default:
				startIndexForMultiValued = null;
				endIndexForMultiValued = null;
				break;
		}

		mapping.fieldWithAscendingMinModels.forEach(
				fieldModel -> addMultipleValues( fieldModel, document, SortMode.MIN, ordinal,
						startIndexForMultiValued, endIndexForMultiValued )
		);
		mapping.fieldWithAscendingMaxModels.forEach(
				fieldModel -> addMultipleValues( fieldModel, document, SortMode.MAX, ordinal,
						startIndexForMultiValued, endIndexForMultiValued )
		);
		mapping.fieldWithAscendingSumModels.forEach(
				fieldModel -> addMultipleValues( fieldModel, document, SortMode.SUM, ordinal,
						startIndexForMultiValued, endIndexForMultiValued )
		);
		mapping.fieldWithAscendingAvgModels.forEach(
				fieldModel -> addMultipleValues( fieldModel, document, SortMode.AVG, ordinal,
						startIndexForMultiValued, endIndexForMultiValued )
		);
		mapping.fieldWithAscendingMedianModels.forEach(
				fieldModel -> addMultipleValues( fieldModel, document, SortMode.MEDIAN, ordinal,
						startIndexForMultiValued, endIndexForMultiValued )
		);
	}

	private F getSingleValueForMissingUse(int ordinal) {
		return fieldTypeDescriptor.getAscendingUniqueTermValues().getSingle().get( ordinal );
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
			SortMode sortMode, Integer ordinal,
			Integer startIndex, Integer endIndex) {
		if ( ordinal == null ) {
			return;
		}
		List<F> values = fieldModel.typeDescriptor.getAscendingUniqueTermValues().getMultiResultingInSingle( sortMode )
				.get( ordinal );
		if ( values.isEmpty() ) {
			return;
		}
		if ( startIndex == null ) {
			startIndex = 0;
		}
		if ( endIndex == null ) {
			endIndex = values.size();
		}
		for ( int i = startIndex; i < endIndex; i++ ) {
			documentElement.addValue( fieldModel.reference, values.get( i ) );
		}
	}

	private static void initData() {
		IndexIndexingPlan<?> plan = index.createIndexingPlan();
		// Important: do not index the documents in the expected order after sorts (1, 2, 3)
		plan.add( referenceProvider( SEGMENT_1_DOC_0, SEGMENT_1 ),
				document -> initDocument( index.binding(), document, DOCUMENT_2_ORDINAL ) );
		plan.add( referenceProvider( SEGMENT_1_DOC_EMPTY, SEGMENT_1 ),
				document -> initDocument( index.binding(), document, null ) );
		plan.add( referenceProvider( SEGMENT_0_DOC_1, SEGMENT_0 ),
				document -> initDocument( index.binding(), document, DOCUMENT_1_ORDINAL ) );
		plan.add( referenceProvider( SEGMENT_0_DOC_0, SEGMENT_0 ),
				document -> initDocument( index.binding(), document, DOCUMENT_3_ORDINAL ) );
		plan.add( referenceProvider( SEGMENT_0_DOC_EMPTY, SEGMENT_0 ),
				document -> initDocument( index.binding(), document, null ) );
		plan.execute().join();

		// Check that all documents are searchable
		SearchQuery<DocumentReference> query = index.createScope().query()
				.where( f -> f.matchAll() )
				.toQuery();
		assertThat( query ).hasDocRefHitsAnyOrder(
				index.name(),
				SEGMENT_1_DOC_0, SEGMENT_0_DOC_1, SEGMENT_0_DOC_0,
				SEGMENT_0_DOC_EMPTY, SEGMENT_1_DOC_EMPTY
		);
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

	private static class IndexBinding extends AbstractObjectMapping {
		final FirstLevelObjectMapping flattenedObject;
		final FirstLevelObjectMapping nestedObject;

		IndexBinding(IndexSchemaElement root) {
			this( root, ignored -> { } );
		}

		IndexBinding(IndexSchemaElement root,
				Consumer<StandardIndexFieldTypeOptionsStep<?, ?>> additionalConfiguration) {
			super( root, additionalConfiguration );

			flattenedObject = FirstLevelObjectMapping.create( root, "flattenedObject",
					ObjectFieldStorage.FLATTENED, false, additionalConfiguration );
			nestedObject = FirstLevelObjectMapping.create( root, "nestedObject",
					ObjectFieldStorage.NESTED, true, additionalConfiguration );
		}
	}

	private static class FirstLevelObjectMapping extends AbstractObjectMapping {
		final String relativeFieldName;
		final IndexObjectFieldReference self;

		final IndexFieldReference<String> discriminator;

		final SecondLevelObjectMapping nestedObject;

		public static FirstLevelObjectMapping create(IndexSchemaElement parent, String relativeFieldName,
				ObjectFieldStorage storage,
				boolean multiValued,
				Consumer<StandardIndexFieldTypeOptionsStep<?, ?>> additionalConfiguration) {
			IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, storage );
			if ( multiValued ) {
				objectField.multiValued();
			}
			return new FirstLevelObjectMapping( relativeFieldName, objectField, additionalConfiguration );
		}

		private FirstLevelObjectMapping(String relativeFieldName, IndexSchemaObjectField objectField,
				Consumer<StandardIndexFieldTypeOptionsStep<?, ?>> additionalConfiguration) {
			super( objectField, additionalConfiguration );
			this.relativeFieldName = relativeFieldName;
			self = objectField.toReference();

			discriminator = objectField.field( "discriminator", f -> f.asString() ).toReference();

			nestedObject = SecondLevelObjectMapping.create( objectField, "nestedObject",
					ObjectFieldStorage.NESTED, additionalConfiguration );
		}
	}

	private static class SecondLevelObjectMapping extends AbstractObjectMapping {
		final String relativeFieldName;
		final IndexObjectFieldReference self;

		final IndexFieldReference<String> discriminator;

		public static SecondLevelObjectMapping create(IndexSchemaElement parent, String relativeFieldName,
				ObjectFieldStorage storage,
				Consumer<StandardIndexFieldTypeOptionsStep<?, ?>> additionalConfiguration) {
			IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, storage );
			objectField.multiValued();
			return new SecondLevelObjectMapping( relativeFieldName, objectField, additionalConfiguration );
		}

		private SecondLevelObjectMapping(String relativeFieldName, IndexSchemaObjectField objectField,
				Consumer<StandardIndexFieldTypeOptionsStep<?, ?>> additionalConfiguration) {
			super( objectField, additionalConfiguration );
			this.relativeFieldName = relativeFieldName;
			self = objectField.toReference();

			discriminator = objectField.field( "discriminator", f -> f.asString() ).toReference();
		}
	}

	private enum ValueSelection {
		FIRST_PARTITION,
		SECOND_PARTITION,
		ALL
	}
}
