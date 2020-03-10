/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.sort;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.referenceProvider;

import java.util.ArrayList;
import java.util.Arrays;
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
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.SortFinalStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.NormalizedStringFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.ExpectationsAlternative;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.IndexFieldStructure;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModel;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModelsByType;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingIndexManager;
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
public class FieldSearchSortBaseIT<F> {

	private static Stream<FieldTypeDescriptor<?>> supportedTypeDescriptors() {
		return FieldTypeDescriptor.getAll().stream()
				.filter( typeDescriptor -> typeDescriptor.getFieldSortExpectations().isSupported() );
	}

	@Parameterized.Parameters(name = "{0} - {1}")
	public static Object[][] parameters() {
		List<Object[]> parameters = new ArrayList<>();
		supportedTypeDescriptors().forEach( fieldTypeDescriptor -> {
			ExpectationsAlternative<?, ?> expectations = fieldTypeDescriptor.getFieldSortExpectations();
			if ( expectations.isSupported() ) {
				for ( IndexFieldStructure indexFieldStructure : IndexFieldStructure.values() ) {
					parameters.add( new Object[] { indexFieldStructure, fieldTypeDescriptor } );
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
	private final FieldTypeDescriptor<F> fieldTypeDescriptor;

	public FieldSearchSortBaseIT(IndexFieldStructure indexFieldStructure, FieldTypeDescriptor<F> fieldTypeDescriptor) {
		this.indexFieldStructure = indexFieldStructure;
		this.fieldTypeDescriptor = fieldTypeDescriptor;
	}

	@Test
	@TestForIssue(jiraKey = { "HSEARCH-3798", "HSEARCH-2252", "HSEARCH-2254" })
	public void simple() {
		SearchQuery<DocumentReference> query;
		String absoluteFieldPath = getFieldPath();

		// Default order
		query = matchNonEmptyQuery( b -> b.field( absoluteFieldPath ) );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );

		// Explicit order
		query = matchNonEmptyQuery( b -> b.field( absoluteFieldPath ).asc() );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );
		query = matchNonEmptyQuery( b -> b.field( absoluteFieldPath ).desc() );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_3, DOCUMENT_2, DOCUMENT_1 );
	}

	@Test
	public void missingValue() {
		SearchQuery<DocumentReference> query;

		String fieldPath = getFieldPath();

		// Default order
		query = matchNonEmptyAndEmpty1Query( f -> f.field( fieldPath ).missing().last() );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, EMPTY_1 );

		// Explicit order with missing().last()
		query = matchNonEmptyAndEmpty1Query( f -> f.field( fieldPath ).asc().missing().last() );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, EMPTY_1 );
		query = matchNonEmptyAndEmpty1Query( f -> f.field( fieldPath ).desc().missing().last() );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_3, DOCUMENT_2, DOCUMENT_1, EMPTY_1 );

		// Explicit order with missing().first()
		query = matchNonEmptyAndEmpty1Query( f -> f.field( fieldPath ).asc().missing().first() );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, EMPTY_1, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );
		query = matchNonEmptyAndEmpty1Query( f -> f.field( fieldPath ).desc().missing().first() );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, EMPTY_1, DOCUMENT_3, DOCUMENT_2, DOCUMENT_1 );

		// Explicit order with missing().use( ... )
		query = matchNonEmptyAndEmpty1Query( f -> f.field( fieldPath ).asc()
				.missing().use( getSingleValueForMissingUse( BEFORE_DOCUMENT_1_ORDINAL ) ) );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, EMPTY_1, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );
		query = matchNonEmptyAndEmpty1Query( f -> f.field( fieldPath ).asc()
				.missing().use( getSingleValueForMissingUse( BETWEEN_DOCUMENT_1_AND_2_ORDINAL ) ) );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, EMPTY_1, DOCUMENT_2, DOCUMENT_3 );
		query = matchNonEmptyAndEmpty1Query( f -> f.field( fieldPath ).asc()
				.missing().use( getSingleValueForMissingUse( BETWEEN_DOCUMENT_2_AND_3_ORDINAL ) ) );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, EMPTY_1, DOCUMENT_3 );
		query = matchNonEmptyAndEmpty1Query( f -> f.field( fieldPath ).asc()
				.missing().use( getSingleValueForMissingUse( AFTER_DOCUMENT_3_ORDINAL ) ) );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, EMPTY_1 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3254")
	public void missingValue_multipleEmpty() {
		List<DocumentReference> docRefHits;
		String fieldPath = getFieldPath();

		// using before 1 value
		docRefHits = matchAllQuery( f -> f.field( fieldPath ).asc()
				.missing().use( getSingleValueForMissingUse( BEFORE_DOCUMENT_1_ORDINAL ) ) )
				.fetchAllHits();
		assertThat( docRefHits ).ordinals( 0, 1, 2, 3 )
				.hasDocRefHitsAnyOrder( INDEX_NAME, EMPTY_1, EMPTY_2, EMPTY_3, EMPTY_4 );
		assertThat( docRefHits ).ordinal( 4 ).isDocRefHit( INDEX_NAME, DOCUMENT_1 );
		assertThat( docRefHits ).ordinal( 5 ).isDocRefHit( INDEX_NAME, DOCUMENT_2 );
		assertThat( docRefHits ).ordinal( 6 ).isDocRefHit( INDEX_NAME, DOCUMENT_3 );

		// using between 1 and 2 value
		docRefHits = matchAllQuery( f -> f.field( fieldPath ).asc()
				.missing().use( getSingleValueForMissingUse( BETWEEN_DOCUMENT_1_AND_2_ORDINAL ) ) )
				.fetchAllHits();
		assertThat( docRefHits ).ordinal( 0 ).isDocRefHit( INDEX_NAME, DOCUMENT_1 );
		assertThat( docRefHits ).ordinals( 1, 2, 3, 4 )
				.hasDocRefHitsAnyOrder( INDEX_NAME, EMPTY_1, EMPTY_2, EMPTY_3, EMPTY_4 );
		assertThat( docRefHits ).ordinal( 5 ).isDocRefHit( INDEX_NAME, DOCUMENT_2 );
		assertThat( docRefHits ).ordinal( 6 ).isDocRefHit( INDEX_NAME, DOCUMENT_3 );

		// using between 2 and 3 value
		docRefHits = matchAllQuery( f -> f.field( fieldPath ).asc().missing()
				.use( getSingleValueForMissingUse( BETWEEN_DOCUMENT_2_AND_3_ORDINAL ) ) )
				.fetchAllHits();
		assertThat( docRefHits ).ordinal( 0 ).isDocRefHit( INDEX_NAME, DOCUMENT_1 );
		assertThat( docRefHits ).ordinal( 1 ).isDocRefHit( INDEX_NAME, DOCUMENT_2 );
		assertThat( docRefHits ).ordinals( 2, 3, 4, 5 )
				.hasDocRefHitsAnyOrder( INDEX_NAME, EMPTY_1, EMPTY_2, EMPTY_3, EMPTY_4 );
		assertThat( docRefHits ).ordinal( 6 ).isDocRefHit( INDEX_NAME, DOCUMENT_3 );

		// using after 3 value
		docRefHits = matchAllQuery( f -> f.field( fieldPath ).asc()
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
		List<DocumentReference> docRefHits;
		String fieldPath = getFieldPath();

		Object docValue1 = getSingleValueForMissingUse( DOCUMENT_1_ORDINAL );
		Object docValue2 = getSingleValueForMissingUse( DOCUMENT_2_ORDINAL );
		Object docValue3 = getSingleValueForMissingUse( DOCUMENT_3_ORDINAL );

		// using doc 1 value
		docRefHits = matchAllQuery( f -> f.field( fieldPath ).asc()
				.missing().use( docValue1 ) ).fetchAllHits();
		assertThat( docRefHits ).ordinals( 0, 1, 2, 3, 4 )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, EMPTY_1, EMPTY_2, EMPTY_3, EMPTY_4 );
		assertThat( docRefHits ).ordinal( 5 ).isDocRefHit( INDEX_NAME, DOCUMENT_2 );
		assertThat( docRefHits ).ordinal( 6 ).isDocRefHit( INDEX_NAME, DOCUMENT_3 );

		// using doc 2 value
		docRefHits = matchAllQuery( f -> f.field( fieldPath ).asc()
				.missing().use( docValue2 ) ).fetchAllHits();
		assertThat( docRefHits ).ordinal( 0 ).isDocRefHit( INDEX_NAME, DOCUMENT_1 );
		assertThat( docRefHits ).ordinals( 1, 2, 3, 4, 5 )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_2, EMPTY_1, EMPTY_2, EMPTY_3, EMPTY_4 );
		assertThat( docRefHits ).ordinal( 6 ).isDocRefHit( INDEX_NAME, DOCUMENT_3 );

		// using doc 3 value
		docRefHits = matchAllQuery( f -> f.field( fieldPath ).asc()
				.missing().use( docValue3 ) ).fetchAllHits();
		assertThat( docRefHits ).ordinal( 0 ).isDocRefHit( INDEX_NAME, DOCUMENT_1 );
		assertThat( docRefHits ).ordinal( 1 ).isDocRefHit( INDEX_NAME, DOCUMENT_2 );
		assertThat( docRefHits ).ordinals( 2, 3, 4, 5, 6 )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_3, EMPTY_1, EMPTY_2, EMPTY_3, EMPTY_4 );
	}

	private SearchQuery<DocumentReference> matchNonEmptyQuery(
			Function<? super SearchSortFactory, ? extends SortFinalStep> sortContributor) {
		return matchNonEmptyQuery( sortContributor, indexManager.createScope() );
	}

	private SearchQuery<DocumentReference> matchNonEmptyQuery(
			Function<? super SearchSortFactory, ? extends SortFinalStep> sortContributor, StubMappingScope scope) {
		return scope.query()
				.where( f -> f.matchAll()
						.except( f.id().matchingAny( Arrays.asList( EMPTY_1, EMPTY_2, EMPTY_3, EMPTY_4 ) ) ) )
				.sort( sortContributor )
				.toQuery();
	}

	private SearchQuery<DocumentReference> matchNonEmptyAndEmpty1Query(
			Function<? super SearchSortFactory, ? extends SortFinalStep> sortContributor) {
		return matchNonEmptyAndEmpty1Query( sortContributor, indexManager.createScope() );
	}

	private SearchQuery<DocumentReference> matchNonEmptyAndEmpty1Query(
			Function<? super SearchSortFactory, ? extends SortFinalStep> sortContributor, StubMappingScope scope) {
		return scope.query()
				.where( f -> f.matchAll().except( f.id().matchingAny( Arrays.asList( EMPTY_2, EMPTY_3, EMPTY_4 ) ) ) )
				.sort( sortContributor )
				.toQuery();
	}

	private SearchQuery<DocumentReference> matchAllQuery(
			Function<? super SearchSortFactory, ? extends SortFinalStep> sortContributor) {
		return matchAllQuery( sortContributor, indexManager.createScope() );
	}

	private SearchQuery<DocumentReference> matchAllQuery(
			Function<? super SearchSortFactory, ? extends SortFinalStep> sortContributor, StubMappingScope scope) {
		return scope.query()
				.where( f -> f.matchAll() )
				.sort( sortContributor )
				.toQuery();
	}

	private String getFieldPath() {
		switch ( indexFieldStructure ) {
			case ROOT:
				return indexMapping.fieldModels.get( fieldTypeDescriptor ).relativeFieldName;
			case IN_FLATTENED:
				return indexMapping.flattenedObject.relativeFieldName
						+ "." + indexMapping.flattenedObject.fieldModels.get( fieldTypeDescriptor ).relativeFieldName;
			case IN_NESTED:
				return indexMapping.nestedObject.relativeFieldName
						+ "." + indexMapping.nestedObject.fieldModels.get( fieldTypeDescriptor ).relativeFieldName;
			case IN_NESTED_TWICE:
				return indexMapping.nestedObject.relativeFieldName
						+ "." + indexMapping.nestedObject.nestedObject.relativeFieldName
						+ "." + indexMapping.nestedObject.nestedObject.fieldModels.get( fieldTypeDescriptor ).relativeFieldName;
			default:
				throw new IllegalStateException( "Unexpected value: " + indexFieldStructure );
		}
	}

	private static void initDocument(IndexMapping indexMapping, DocumentElement document, Integer ordinal) {
		indexMapping.fieldModels.forEach(
				fieldModel -> addValue( fieldModel, document, ordinal )
		);

		// Note: these objects must be single-valued for these tests
		DocumentElement flattenedObject = document.addObject( indexMapping.flattenedObject.self );
		DocumentElement nestedObject = document.addObject( indexMapping.nestedObject.self );
		DocumentElement nestedObjectInNestedObject = nestedObject.addObject( indexMapping.nestedObject.nestedObject.self );

		indexMapping.flattenedObject.fieldModels.forEach(
				fieldModel -> addValue( fieldModel, flattenedObject, ordinal )
		);
		indexMapping.nestedObject.fieldModels.forEach(
				fieldModel -> addValue( fieldModel, nestedObject, ordinal )
		);
		indexMapping.nestedObject.nestedObject.fieldModels.forEach(
				fieldModel -> addValue( fieldModel, nestedObjectInNestedObject, ordinal )
		);
	}

	@SuppressWarnings("unchecked")
	private F getSingleValueForMissingUse(int ordinal) {
		F value = fieldTypeDescriptor.getAscendingUniqueTermValues().get( ordinal );

		if ( fieldTypeDescriptor instanceof NormalizedStringFieldTypeDescriptor
				&& !TckConfiguration.get().getBackendFeatures().normalizesStringMissingValues() ) {
			// The backend doesn't normalize missing value replacements automatically, we have to do it ourselves
			// TODO HSEARCH-3387 Remove this once all backends correctly normalize missing value replacements
			value = (F) ( (String) value ).toLowerCase( Locale.ROOT );
		}

		return value;
	}

	private static <F> void addValue(SimpleFieldModel<F> fieldModel, DocumentElement documentElement, Integer ordinal) {
		if ( ordinal == null ) {
			return;
		}
		documentElement.addValue(
				fieldModel.reference,
				fieldModel.typeDescriptor.getAscendingUniqueTermValues().get( ordinal )
		);
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
		final SimpleFieldModelsByType fieldModels;

		AbstractObjectMapping(IndexSchemaElement self,
				Consumer<StandardIndexFieldTypeOptionsStep<?, ?>> additionalConfiguration) {
			fieldModels = SimpleFieldModelsByType.mapAll( supportedTypeDescriptors(), self, "",
					c -> c.sortable( Sortable.YES ), additionalConfiguration );
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
