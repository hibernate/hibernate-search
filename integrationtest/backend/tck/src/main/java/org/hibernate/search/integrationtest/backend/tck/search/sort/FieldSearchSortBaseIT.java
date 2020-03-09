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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.SortFinalStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.NormalizedStringFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.ExpectationsAlternative;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.InvalidType;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.StandardFieldMapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.ValueWrapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.SubTest;
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

	@Parameterized.Parameters(name = "{0}")
	public static Object[][] parameters() {
		List<Object[]> parameters = new ArrayList<>();
		for ( FieldTypeDescriptor<?> fieldTypeDescriptor : FieldTypeDescriptor.getAll() ) {
			ExpectationsAlternative<?, ?> expectations = fieldTypeDescriptor.getFieldSortExpectations();
			if ( expectations.isSupported() ) {
				parameters.add( new Object[] { fieldTypeDescriptor } );
			}
		}
		return parameters.toArray( new Object[0][] );
	}

	private static final String INDEX_NAME = "IndexName";
	private static final String COMPATIBLE_INDEX_NAME = "IndexWithCompatibleFields";
	private static final String RAW_FIELD_COMPATIBLE_INDEX_NAME = "IndexWithCompatibleRawFields";
	private static final String INCOMPATIBLE_INDEX_NAME = "IndexWithIncompatibleFields";

	private static final String DOCUMENT_1 = "1";
	private static final String DOCUMENT_2 = "2";
	private static final String DOCUMENT_3 = "3";

	private static final String EMPTY_1 = "empty1";
	private static final String EMPTY_2 = "empty2";
	private static final String EMPTY_3 = "empty3";
	private static final String EMPTY_4 = "empty4";

	private static final String COMPATIBLE_INDEX_DOCUMENT_1 = "compatible_1";
	private static final String RAW_FIELD_COMPATIBLE_INDEX_DOCUMENT_1 = "raw_field_compatible_1";

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

	private static IndexMapping compatibleIndexMapping;
	private static StubMappingIndexManager compatibleIndexManager;

	private static RawFieldCompatibleIndexMapping rawFieldCompatibleIndexMapping;
	private static StubMappingIndexManager rawFieldCompatibleIndexManager;

	private static StubMappingIndexManager incompatibleIndexManager;

	@BeforeClass
	public static void setup() {
		setupHelper.start()
				.withIndex(
						INDEX_NAME,
						ctx -> indexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> FieldSearchSortBaseIT.indexManager = indexManager
				)
				.withIndex(
						COMPATIBLE_INDEX_NAME,
						ctx -> compatibleIndexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> compatibleIndexManager = indexManager
				)
				.withIndex(
						RAW_FIELD_COMPATIBLE_INDEX_NAME,
						ctx -> rawFieldCompatibleIndexMapping = new RawFieldCompatibleIndexMapping( ctx.getSchemaElement() ),
						indexManager -> rawFieldCompatibleIndexManager = indexManager
				)
				.withIndex(
						INCOMPATIBLE_INDEX_NAME,
						ctx -> new IncompatibleIndexMapping( ctx.getSchemaElement() ),
						indexManager -> incompatibleIndexManager = indexManager
				)
				.setup();

		initData();
	}

	private final FieldTypeDescriptor<F> fieldTypeDescriptor;

	public FieldSearchSortBaseIT(FieldTypeDescriptor<F> fieldTypeDescriptor) {
		this.fieldTypeDescriptor = fieldTypeDescriptor;
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3798")
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

	@Test
	public void withDslConverters_dslConverterEnabled() {
		SearchQuery<DocumentReference> query;
		String fieldPath = getFieldWithDslConverterPath();

		query = matchNonEmptyAndEmpty1Query( f -> f.field( fieldPath ).asc().missing()
				.use( new ValueWrapper<>( getSingleValueForMissingUse( BEFORE_DOCUMENT_1_ORDINAL ) ) ) );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, EMPTY_1, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );
		query = matchNonEmptyAndEmpty1Query( f -> f.field( fieldPath ).asc().missing()
				.use( new ValueWrapper<>( getSingleValueForMissingUse( BETWEEN_DOCUMENT_1_AND_2_ORDINAL ) ) ) );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, EMPTY_1, DOCUMENT_2, DOCUMENT_3 );
		query = matchNonEmptyAndEmpty1Query( f -> f.field( fieldPath ).asc().missing()
				.use( new ValueWrapper<>( getSingleValueForMissingUse( BETWEEN_DOCUMENT_2_AND_3_ORDINAL ) ) ) );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, EMPTY_1, DOCUMENT_3 );
		query = matchNonEmptyAndEmpty1Query( f -> f.field( fieldPath ).asc().missing()
				.use( new ValueWrapper<>( getSingleValueForMissingUse( AFTER_DOCUMENT_3_ORDINAL ) ) ) );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, EMPTY_1 );
	}

	@Test
	public void withDslConverters_dslConverterDisabled() {
		SearchQuery<DocumentReference> query;
		String fieldPath = getFieldWithDslConverterPath();

		query = matchNonEmptyAndEmpty1Query( f -> f.field( fieldPath ).asc().missing()
				.use( getSingleValueForMissingUse( BEFORE_DOCUMENT_1_ORDINAL ), ValueConvert.NO ) );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, EMPTY_1, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );
		query = matchNonEmptyAndEmpty1Query( f -> f.field( fieldPath ).asc().missing()
				.use( getSingleValueForMissingUse( BETWEEN_DOCUMENT_1_AND_2_ORDINAL ), ValueConvert.NO ) );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, EMPTY_1, DOCUMENT_2, DOCUMENT_3 );
		query = matchNonEmptyAndEmpty1Query( f -> f.field( fieldPath ).asc().missing()
				.use( getSingleValueForMissingUse( BETWEEN_DOCUMENT_2_AND_3_ORDINAL ), ValueConvert.NO ) );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, EMPTY_1, DOCUMENT_3 );
		query = matchNonEmptyAndEmpty1Query( f -> f.field( fieldPath ).asc().missing()
				.use( getSingleValueForMissingUse( AFTER_DOCUMENT_3_ORDINAL ), ValueConvert.NO ) );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, EMPTY_1 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3798")
	public void inFlattenedObject() {
		SearchQuery<DocumentReference> query;
		String fieldPath = getFieldPathInObject( indexMapping.flattenedObject );

		query = matchNonEmptyQuery( f -> f.field( fieldPath ) );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );

		query = matchNonEmptyQuery( f -> f.field( fieldPath ).asc() );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );

		query = matchNonEmptyQuery( f -> f.field( fieldPath ).desc() );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_3, DOCUMENT_2, DOCUMENT_1 );
	}

	@Test
	public void inFlattenedObject_missingValue() {
		SearchQuery<DocumentReference> query;
		String fieldPath = getFieldPathInObject( indexMapping.flattenedObject );

		query = matchNonEmptyAndEmpty1Query( f -> f.field( fieldPath ).missing().last() );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, EMPTY_1 );

		query = matchNonEmptyAndEmpty1Query( f -> f.field( fieldPath ).asc().missing().last() );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, EMPTY_1 );

		query = matchNonEmptyAndEmpty1Query( f -> f.field( fieldPath ).desc().missing().last() );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_3, DOCUMENT_2, DOCUMENT_1, EMPTY_1 );

		query = matchNonEmptyAndEmpty1Query( f -> f.field( fieldPath ).asc().missing().first() );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, EMPTY_1, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );

		query = matchNonEmptyAndEmpty1Query( f -> f.field( fieldPath ).desc().missing().first() );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, EMPTY_1, DOCUMENT_3, DOCUMENT_2, DOCUMENT_1 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3798")
	public void inNestedObject() {
		SearchQuery<DocumentReference> query;
		String fieldPath = getFieldPathInObject( indexMapping.nestedObject );

		query = matchNonEmptyQuery( f -> f.field( fieldPath ) );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );

		query = matchNonEmptyQuery( f -> f.field( fieldPath ).asc() );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );

		query = matchNonEmptyQuery( f -> f.field( fieldPath ).desc() );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_3, DOCUMENT_2, DOCUMENT_1 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2254")
	public void inNestedObject_missingValue() {
		SearchQuery<DocumentReference> query;
		String fieldPath = getFieldPathInObject( indexMapping.nestedObject );

		query = matchNonEmptyAndEmpty1Query( f -> f.field( fieldPath ).missing().last() );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, EMPTY_1 );

		query = matchNonEmptyAndEmpty1Query( f -> f.field( fieldPath ).asc().missing().last() );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, EMPTY_1 );

		query = matchNonEmptyAndEmpty1Query( f -> f.field( fieldPath ).desc().missing().last() );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_3, DOCUMENT_2, DOCUMENT_1, EMPTY_1 );

		query = matchNonEmptyAndEmpty1Query( f -> f.field( fieldPath ).asc().missing().first() );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, EMPTY_1, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );

		query = matchNonEmptyAndEmpty1Query( f -> f.field( fieldPath ).desc().missing().first() );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, EMPTY_1, DOCUMENT_3, DOCUMENT_2, DOCUMENT_1 );

		// Explicit order with onMissingValue().use( ... )
		query = matchNonEmptyAndEmpty1Query( f -> f.field( fieldPath ).asc().missing()
				.use( getSingleValueForMissingUse( BEFORE_DOCUMENT_1_ORDINAL ) ) );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, EMPTY_1, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );
		query = matchNonEmptyAndEmpty1Query( f -> f.field( fieldPath ).asc().missing()
				.use( getSingleValueForMissingUse( BETWEEN_DOCUMENT_1_AND_2_ORDINAL ) ) );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, EMPTY_1, DOCUMENT_2, DOCUMENT_3 );
		query = matchNonEmptyAndEmpty1Query( f -> f.field( fieldPath ).asc().missing()
				.use( getSingleValueForMissingUse( BETWEEN_DOCUMENT_2_AND_3_ORDINAL ) ) );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, EMPTY_1, DOCUMENT_3 );
		query = matchNonEmptyAndEmpty1Query( f -> f.field( fieldPath ).asc().missing()
				.use( getSingleValueForMissingUse( AFTER_DOCUMENT_3_ORDINAL ) ) );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, EMPTY_1 );
	}

	@Test
	public void error_unsortable() {
		StubMappingScope scope = indexManager.createScope();
		String fieldPath = getNonSortableFieldPath();

		SubTest.expectException( () -> {
				scope.sort().field( fieldPath );
		} ).assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Sorting is not enabled for field" )
				.hasMessageContaining( fieldPath );
	}

	@Test
	public void error_invalidType_noDslConverter() {
		StubMappingScope scope = indexManager.createScope();

		String absoluteFieldPath = getFieldPath();
		Object invalidValueToMatch = new InvalidType();

		SubTest.expectException(
				"field() sort with invalid parameter type for missing().use() on field " + absoluteFieldPath,
				() -> scope.sort().field( absoluteFieldPath ).missing()
						.use( invalidValueToMatch )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unable to convert DSL parameter: " )
				.hasMessageContaining( InvalidType.class.getName() )
				.hasCauseInstanceOf( ClassCastException.class )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
				) );
	}

	@Test
	public void error_invalidType_withDslConverter() {
		StubMappingScope scope = indexManager.createScope();

		String absoluteFieldPath = getFieldWithDslConverterPath();
		Object invalidValueToMatch = new InvalidType();

		SubTest.expectException(
				"field() sort with invalid parameter type for missing().use() on field " + absoluteFieldPath,
				() -> scope.sort().field( absoluteFieldPath ).missing()
						.use( invalidValueToMatch )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unable to convert DSL parameter: " )
				.hasMessageContaining( InvalidType.class.getName() )
				.hasCauseInstanceOf( ClassCastException.class )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
				) );
	}

	@Test
	public void multiIndex_withCompatibleIndexManager_usingField() {
		StubMappingScope scope = indexManager.createScope( compatibleIndexManager );

		SearchQuery<DocumentReference> query;
		String fieldPath = getFieldPath();

		query = matchNonEmptyAndEmpty1Query( f -> f.field( fieldPath ).asc().missing()
				.use( getSingleValueForMissingUse( BEFORE_DOCUMENT_1_ORDINAL ) ), scope );

		/*
		 * Not testing the ordering of results here because some documents have the same value.
		 * It's not what we want tot test anyway: we just want to check that fields are correctly
		 * detected as compatible and that no exception is thrown.
		 */
		assertThat( query ).hasDocRefHitsAnyOrder( b -> {
			b.doc( INDEX_NAME, EMPTY_1 );
			b.doc( INDEX_NAME, DOCUMENT_1 );
			b.doc( INDEX_NAME, DOCUMENT_2 );
			b.doc( INDEX_NAME, DOCUMENT_3 );
			b.doc( COMPATIBLE_INDEX_NAME, COMPATIBLE_INDEX_DOCUMENT_1 );
		} );
	}

	@Test
	public void multiIndex_withRawFieldCompatibleIndexManager_dslConverterEnabled() {
		StubMappingScope scope = indexManager.createScope( rawFieldCompatibleIndexManager );

		String fieldPath = getFieldPath();

		SubTest.expectException(
				() -> {
					matchNonEmptyAndEmpty1Query( f -> f.field( fieldPath ).asc().missing()
							.use( new ValueWrapper<>( getSingleValueForMissingUse( BEFORE_DOCUMENT_1_ORDINAL ) ) ), scope );
				}
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Multiple conflicting types to build a sort" )
				.hasMessageContaining( "'" + fieldPath + "'" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( INDEX_NAME, RAW_FIELD_COMPATIBLE_INDEX_NAME )
				) );
	}

	@Test
	public void multiIndex_withRawFieldCompatibleIndexManager_dslConverterDisabled() {
		StubMappingScope scope = indexManager.createScope( rawFieldCompatibleIndexManager );

		SearchQuery<DocumentReference> query;
		String fieldPath = getFieldPath();

		query = matchNonEmptyAndEmpty1Query( f -> f.field( fieldPath ).asc().missing()
				.use( getSingleValueForMissingUse( BEFORE_DOCUMENT_1_ORDINAL ), ValueConvert.NO ), scope );

		/*
		 * Not testing the ordering of results here because some documents have the same value.
		 * It's not what we want tot test anyway: we just want to check that fields are correctly
		 * detected as compatible and that no exception is thrown.
		 */
		assertThat( query ).hasDocRefHitsAnyOrder( b -> {
			b.doc( INDEX_NAME, EMPTY_1 );
			b.doc( INDEX_NAME, DOCUMENT_1 );
			b.doc( INDEX_NAME, DOCUMENT_2 );
			b.doc( INDEX_NAME, DOCUMENT_3 );
			b.doc( RAW_FIELD_COMPATIBLE_INDEX_NAME, RAW_FIELD_COMPATIBLE_INDEX_DOCUMENT_1 );
		} );
	}

	@Test
	public void multiIndex_withNoCompatibleIndexManager_dslConverterEnabled() {
		StubMappingScope scope = indexManager.createScope( incompatibleIndexManager );

		String fieldPath = getFieldPath();

		SubTest.expectException(
				() -> {
					matchNonEmptyAndEmpty1Query( f -> f.field( fieldPath ), scope );
				}
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Multiple conflicting types to build a sort" )
				.hasMessageContaining( "'" + fieldPath + "'" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( INDEX_NAME, INCOMPATIBLE_INDEX_NAME )
				) );
	}

	@Test
	public void multiIndex_withNoCompatibleIndexManager_dslConverterDisabled() {
		StubMappingScope scope = indexManager.createScope( incompatibleIndexManager );

		String fieldPath = getFieldPath();

		SubTest.expectException(
				() -> {
					matchNonEmptyAndEmpty1Query( f -> f.field( fieldPath ), scope );
				}
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Multiple conflicting types to build a sort" )
				.hasMessageContaining( "'" + fieldPath + "'" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( INDEX_NAME, INCOMPATIBLE_INDEX_NAME )
				) );
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
		return indexMapping.fieldModels.get( fieldTypeDescriptor ).relativeFieldName;
	}

	private String getFieldWithDslConverterPath() {
		return indexMapping.fieldWithDslConverterModels.get( fieldTypeDescriptor ).relativeFieldName;
	}

	private String getNonSortableFieldPath() {
		return indexMapping.nonSortableFieldModels.get( fieldTypeDescriptor ).relativeFieldName;
	}

	private String getFieldPathInObject(ObjectMapping objectMapping) {
		return objectMapping.relativeFieldName + "." + objectMapping.fieldModels.get( fieldTypeDescriptor ).relativeFieldName;
	}

	private static void initDocument(DocumentElement document, int ordinal) {
		forEachSupportedTypeDescriptor( typeDescriptor -> {
			addValue( document, indexMapping.fieldModels, typeDescriptor, ordinal );
			addValue( document, indexMapping.fieldWithDslConverterModels, typeDescriptor, ordinal );
		} );

		// Note: these objects must be single-valued for these tests
		DocumentElement flattenedObject = document.addObject( indexMapping.flattenedObject.self );
		DocumentElement nestedObject = document.addObject( indexMapping.nestedObject.self );

		forEachSupportedTypeDescriptor( typeDescriptor -> {
			addValue( flattenedObject, indexMapping.flattenedObject.fieldModels, typeDescriptor, ordinal );
			addValue( nestedObject, indexMapping.nestedObject.fieldModels, typeDescriptor, ordinal );
		} );
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

	private static <F> void addValue(DocumentElement documentElement,
			FieldModelsByType fieldModels, FieldTypeDescriptor<F> typeDescriptor, int ordinal) {
		documentElement.addValue(
				fieldModels.get( typeDescriptor ).reference,
				typeDescriptor.getAscendingUniqueTermValues().get( ordinal )
		);
	}

	private static void initData() {
		IndexIndexingPlan<?> plan = indexManager.createIndexingPlan();
		// Important: do not index the documents in the expected order after sorts (1, 2, 3)
		plan.add( referenceProvider( EMPTY_4 ), document -> { } );
		plan.add( referenceProvider( DOCUMENT_2 ), document -> initDocument( document, DOCUMENT_2_ORDINAL ) );
		plan.add( referenceProvider( EMPTY_1 ), document -> { } );
		plan.add( referenceProvider( DOCUMENT_1 ), document -> initDocument( document, DOCUMENT_1_ORDINAL ) );
		plan.add( referenceProvider( EMPTY_2 ), document -> { } );
		plan.add( referenceProvider( DOCUMENT_3 ), document -> initDocument( document, DOCUMENT_3_ORDINAL ) );
		plan.add( referenceProvider( EMPTY_3 ), document -> { } );
		plan.execute().join();

		plan = compatibleIndexManager.createIndexingPlan();
		plan.add( referenceProvider( COMPATIBLE_INDEX_DOCUMENT_1 ), document -> {
			forEachSupportedTypeDescriptor( typeDescriptor -> {
				addValue( document, compatibleIndexMapping.fieldModels, typeDescriptor, DOCUMENT_1_ORDINAL );
				addValue( document, compatibleIndexMapping.fieldWithDslConverterModels, typeDescriptor, DOCUMENT_1_ORDINAL );
			} );
		} );
		plan.execute().join();

		plan = rawFieldCompatibleIndexManager.createIndexingPlan();
		plan.add( referenceProvider( RAW_FIELD_COMPATIBLE_INDEX_DOCUMENT_1 ), document -> {
			forEachSupportedTypeDescriptor( typeDescriptor -> {
				addValue( document, rawFieldCompatibleIndexMapping.fieldModels, typeDescriptor, DOCUMENT_1_ORDINAL );
				addValue( document, compatibleIndexMapping.fieldWithDslConverterModels, typeDescriptor, DOCUMENT_1_ORDINAL );
			} );
		} );
		plan.execute().join();

		// Check that all documents are searchable
		SearchQuery<DocumentReference> query = indexManager.createScope().query()
				.where( f -> f.matchAll() )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, EMPTY_1, EMPTY_2, EMPTY_3, EMPTY_4 );
		query = compatibleIndexManager.createScope().query()
				.where( f -> f.matchAll() )
				.toQuery();
		assertThat( query ).hasDocRefHitsAnyOrder( COMPATIBLE_INDEX_NAME, COMPATIBLE_INDEX_DOCUMENT_1 );
		query = rawFieldCompatibleIndexManager.createScope().query()
				.where( f -> f.matchAll() )
				.toQuery();
		assertThat( query ).hasDocRefHitsAnyOrder( RAW_FIELD_COMPATIBLE_INDEX_NAME, RAW_FIELD_COMPATIBLE_INDEX_DOCUMENT_1 );
	}

	private static void forEachSupportedTypeDescriptor(Consumer<FieldTypeDescriptor<?>> action) {
		FieldTypeDescriptor.getAll().stream()
				.filter( typeDescriptor -> typeDescriptor.getFieldSortExpectations().isSupported() )
				.forEach( action );
	}

	private static class IndexMapping {
		final FieldModelsByType fieldModels;
		final FieldModelsByType fieldWithDslConverterModels;
		final FieldModelsByType nonSortableFieldModels;

		final ObjectMapping flattenedObject;
		final ObjectMapping nestedObject;

		IndexMapping(IndexSchemaElement root) {
			fieldModels = FieldModelsByType.mapSupported( root, "", ignored -> { } );
			fieldWithDslConverterModels = FieldModelsByType.mapSupported(
					root, "converted_", c -> c.dslConverter( ValueWrapper.class, ValueWrapper.toIndexFieldConverter() )
			);
			nonSortableFieldModels = FieldModelsByType.mapSupported(
					root, "nonSortable_", c -> c.sortable( Sortable.NO )
			);

			flattenedObject = new ObjectMapping( root, "flattenedObject", ObjectFieldStorage.FLATTENED );
			nestedObject = new ObjectMapping( root, "nestedObject", ObjectFieldStorage.NESTED );
		}
	}

	private static class ObjectMapping {
		final String relativeFieldName;
		final IndexObjectFieldReference self;
		final FieldModelsByType fieldModels;

		ObjectMapping(IndexSchemaElement parent, String relativeFieldName, ObjectFieldStorage storage) {
			this.relativeFieldName = relativeFieldName;
			IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, storage );
			self = objectField.toReference();
			fieldModels = FieldModelsByType.mapSupported(
					objectField, "", ignored -> { }
			);
		}
	}

	private static class RawFieldCompatibleIndexMapping {
		FieldModelsByType fieldModels;

		RawFieldCompatibleIndexMapping(IndexSchemaElement root) {
			/*
			 * Add fields with the same name as the fieldModels from IndexMapping,
			 * but with an incompatible DSL converter.
			 */
			fieldModels = FieldModelsByType.mapSupported(
					root, "", c -> c.dslConverter( ValueWrapper.class, ValueWrapper.toIndexFieldConverter() )
			);
		}
	}

	private static class IncompatibleIndexMapping {
		IncompatibleIndexMapping(IndexSchemaElement root) {
			/*
			 * Add fields with the same name as the supportedFieldModels from IndexMapping,
			 * but with an incompatible type.
			 */
			forEachSupportedTypeDescriptor( typeDescriptor -> {
				StandardFieldMapper<?, IncompatibleFieldModel> mapper;
				if ( Integer.class.equals( typeDescriptor.getJavaType() ) ) {
					mapper = IncompatibleFieldModel.mapper( context -> context.asLong() );
				}
				else {
					mapper = IncompatibleFieldModel.mapper( context -> context.asInteger() );
				}
				mapper.map( root, "" + typeDescriptor.getUniqueName() );
			} );
		}
	}

	private static class FieldModelsByType {
		public static FieldModelsByType mapSupported(IndexSchemaElement parent, String prefix,
				Consumer<StandardIndexFieldTypeOptionsStep<?, ?>> additionalConfiguration) {
			FieldModelsByType result = new FieldModelsByType();
			forEachSupportedTypeDescriptor( typeDescriptor -> {
				result.content.put(
						typeDescriptor,
						FieldModel.mapper( typeDescriptor )
								.map( parent, prefix + typeDescriptor.getUniqueName(), additionalConfiguration )
				);
			} );
			return result;
		}

		private final Map<FieldTypeDescriptor<?>, FieldModel<?>> content = new LinkedHashMap<>();

		@SuppressWarnings("unchecked")
		private <F> FieldModel<F> get(FieldTypeDescriptor<F> typeDescriptor) {
			return (FieldModel<F>) content.get( typeDescriptor );
		}
	}

	private static class FieldModel<F> {
		static <F> StandardFieldMapper<F, FieldModel<F>> mapper(FieldTypeDescriptor<F> typeDescriptor) {
			return StandardFieldMapper.of(
					typeDescriptor::configure,
					c -> c.sortable( Sortable.YES ),
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

	private static class IncompatibleFieldModel {
		static <F> StandardFieldMapper<F, IncompatibleFieldModel> mapper(
				Function<IndexFieldTypeFactory, StandardIndexFieldTypeOptionsStep<?, F>> configuration) {
			return StandardFieldMapper.of(
					configuration,
					(reference, name) -> new IncompatibleFieldModel( name )
			);
		}

		final String relativeFieldName;

		private IncompatibleFieldModel(String relativeFieldName) {
			this.relativeFieldName = relativeFieldName;
		}
	}
}
