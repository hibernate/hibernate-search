/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.sort;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactoryContext;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeContext;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkPlan;
import org.hibernate.search.engine.search.predicate.DslConverter;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldModelConsumer;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.FieldSortExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingScope;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.InvalidType;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.StandardFieldMapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.ValueWrapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.test.SubTest;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class FieldSearchSortIT {

	private static final String INDEX_NAME = "IndexName";
	private static final String COMPATIBLE_INDEX_NAME = "IndexWithCompatibleFields";
	private static final String RAW_FIELD_COMPATIBLE_INDEX_NAME = "IndexWithCompatibleRawFields";
	private static final String INCOMPATIBLE_INDEX_NAME = "IndexWithIncompatibleFields";
	private static final String INCOMPATIBLE_DECIMAL_SCALE_INDEX_NAME = "IndexWithIncompatibleDecimalScale";
	private static final String MULTIPLE_EMPTY_INDEX_NAME = "MultipleEmptyIndexName";

	private static final String DOCUMENT_1 = "1";
	private static final String DOCUMENT_2 = "2";
	private static final String DOCUMENT_3 = "3";
	private static final String EMPTY = "empty";

	private static final String EMPTY_1 = "empty1";
	private static final String EMPTY_2 = "empty2";
	private static final String EMPTY_3 = "empty3";
	private static final String EMPTY_4 = "empty4";

	private static final String COMPATIBLE_INDEX_DOCUMENT_1 = "compatible_1";
	private static final String RAW_FIELD_COMPATIBLE_INDEX_DOCUMENT_1 = "raw_field_compatible_1";
	private static final String INCOMPATIBLE_DECIMAL_SCALE_INDEX_DOCUMENT_1 = "incompatible_decimal_scale_1";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private IndexMapping indexMapping;
	private StubMappingIndexManager indexManager;

	private IndexMapping compatibleIndexMapping;
	private StubMappingIndexManager compatibleIndexManager;

	private RawFieldCompatibleIndexMapping rawFieldCompatibleIndexMapping;
	private StubMappingIndexManager rawFieldCompatibleIndexManager;

	private StubMappingIndexManager incompatibleIndexManager;

	private IncompatibleDecimalScaleIndexMapping incompatibleDecimalScaleIndexMapping;
	private StubMappingIndexManager incompatibleDecimalScaleIndexManager;

	private IndexMapping multipleEmptyIndexMapping;
	private StubMappingIndexManager multipleEmptyIndexManager;

	@Before
	public void setup() {
		setupHelper.withDefaultConfiguration()
				.withIndex(
						INDEX_NAME,
						ctx -> this.indexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.withIndex(
						COMPATIBLE_INDEX_NAME,
						ctx -> this.compatibleIndexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.compatibleIndexManager = indexManager
				)
				.withIndex(
						RAW_FIELD_COMPATIBLE_INDEX_NAME,
						ctx -> this.rawFieldCompatibleIndexMapping = new RawFieldCompatibleIndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.rawFieldCompatibleIndexManager = indexManager
				)
				.withIndex(
						INCOMPATIBLE_INDEX_NAME,
						ctx -> new IncompatibleIndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.incompatibleIndexManager = indexManager
				)
				.withIndex(
						INCOMPATIBLE_DECIMAL_SCALE_INDEX_NAME,
						ctx -> this.incompatibleDecimalScaleIndexMapping = new IncompatibleDecimalScaleIndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.incompatibleDecimalScaleIndexManager = indexManager
				)
				.withIndex(
						MULTIPLE_EMPTY_INDEX_NAME,
						ctx -> this.multipleEmptyIndexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.multipleEmptyIndexManager = indexManager
				)
				.setup();

		initData();
	}

	private SearchQuery<DocumentReference> simpleQuery(Consumer<? super SearchSortContainerContext> sortContributor) {
		return simpleQuery( sortContributor, indexManager.createScope() );
	}

	private SearchQuery<DocumentReference> simpleQuery(Consumer<? super SearchSortContainerContext> sortContributor, StubMappingScope scope) {
		return scope.query()
				.predicate( f -> f.matchAll() )
				.sort( sortContributor )
				.toQuery();
	}

	@Test
	public void byField() {
		for ( ByTypeFieldModel<?> fieldModel : indexMapping.supportedFieldModels ) {
			SearchQuery<DocumentReference> query;
			String fieldPath = fieldModel.relativeFieldName;

			// Default order
			query = simpleQuery( b -> b.byField( fieldPath ).onMissingValue().sortLast() );
			assertThat( query )
					.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, EMPTY );

			// Explicit order with onMissingValue().sortLast()
			query = simpleQuery( b -> b.byField( fieldPath ).asc().onMissingValue().sortLast() );
			assertThat( query )
					.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, EMPTY );
			query = simpleQuery( b -> b.byField( fieldPath ).desc().onMissingValue().sortLast() );
			assertThat( query )
					.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_3, DOCUMENT_2, DOCUMENT_1, EMPTY );

			// Explicit order with onMissingValue().sortFirst()
			query = simpleQuery( b -> b.byField( fieldPath ).asc().onMissingValue().sortFirst() );
			assertThat( query )
					.hasDocRefHitsExactOrder( INDEX_NAME, EMPTY, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );
			query = simpleQuery( b -> b.byField( fieldPath ).desc().onMissingValue().sortFirst() );
			assertThat( query )
					.hasDocRefHitsExactOrder( INDEX_NAME, EMPTY, DOCUMENT_3, DOCUMENT_2, DOCUMENT_1 );

			// Explicit order with onMissingValue().use( ... )
			query = simpleQuery( b -> b.byField( fieldPath ).asc().onMissingValue().use( fieldModel.before1Value ) );
			assertThat( query )
					.hasDocRefHitsExactOrder( INDEX_NAME, EMPTY, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );
			query = simpleQuery( b -> b.byField( fieldPath ).asc().onMissingValue().use( fieldModel.between1And2Value ) );
			assertThat( query )
					.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, EMPTY, DOCUMENT_2, DOCUMENT_3 );
			query = simpleQuery( b -> b.byField( fieldPath ).asc().onMissingValue().use( fieldModel.between2And3Value ) );
			assertThat( query )
					.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, EMPTY, DOCUMENT_3 );
			query = simpleQuery( b -> b.byField( fieldPath ).asc().onMissingValue().use( fieldModel.after3Value ) );
			assertThat( query )
					.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, EMPTY );
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3254")
	public void byField_multipleEmpty_missingValue() {
		StubMappingScope scope = multipleEmptyIndexManager.createScope();

		for ( ByTypeFieldModel<?> fieldModel : multipleEmptyIndexMapping.supportedFieldModels ) {
			List<DocumentReference> docRefHits;
			String fieldPath = fieldModel.relativeFieldName;

			// using before 1 value
			docRefHits = simpleQuery( b -> b.byField( fieldPath ).asc().onMissingValue().use( fieldModel.before1Value ), scope ).fetchHits();
			assertThat( docRefHits ).ordinals( 0, 1, 2, 3 ).hasDocRefHitsAnyOrder( MULTIPLE_EMPTY_INDEX_NAME, EMPTY_1, EMPTY_2, EMPTY_3, EMPTY_4 );
			assertThat( docRefHits ).ordinal( 4 ).isDocRefHit( MULTIPLE_EMPTY_INDEX_NAME, DOCUMENT_1 );
			assertThat( docRefHits ).ordinal( 5 ).isDocRefHit( MULTIPLE_EMPTY_INDEX_NAME, DOCUMENT_2 );
			assertThat( docRefHits ).ordinal( 6 ).isDocRefHit( MULTIPLE_EMPTY_INDEX_NAME, DOCUMENT_3 );

			// using between 1 and 2 value
			docRefHits = simpleQuery( b -> b.byField( fieldPath ).asc().onMissingValue().use( fieldModel.between1And2Value ), scope ).fetchHits();
			assertThat( docRefHits ).ordinal( 0 ).isDocRefHit( MULTIPLE_EMPTY_INDEX_NAME, DOCUMENT_1 );
			assertThat( docRefHits ).ordinals( 1, 2, 3, 4 ).hasDocRefHitsAnyOrder( MULTIPLE_EMPTY_INDEX_NAME, EMPTY_1, EMPTY_2, EMPTY_3, EMPTY_4 );
			assertThat( docRefHits ).ordinal( 5 ).isDocRefHit( MULTIPLE_EMPTY_INDEX_NAME, DOCUMENT_2 );
			assertThat( docRefHits ).ordinal( 6 ).isDocRefHit( MULTIPLE_EMPTY_INDEX_NAME, DOCUMENT_3 );

			// using between 2 and 3 value
			docRefHits = simpleQuery( b -> b.byField( fieldPath ).asc().onMissingValue().use( fieldModel.between2And3Value ), scope ).fetchHits();
			assertThat( docRefHits ).ordinal( 0 ).isDocRefHit( MULTIPLE_EMPTY_INDEX_NAME, DOCUMENT_1 );
			assertThat( docRefHits ).ordinal( 1 ).isDocRefHit( MULTIPLE_EMPTY_INDEX_NAME, DOCUMENT_2 );
			assertThat( docRefHits ).ordinals( 2, 3, 4, 5 ).hasDocRefHitsAnyOrder( MULTIPLE_EMPTY_INDEX_NAME, EMPTY_1, EMPTY_2, EMPTY_3, EMPTY_4 );
			assertThat( docRefHits ).ordinal( 6 ).isDocRefHit( MULTIPLE_EMPTY_INDEX_NAME, DOCUMENT_3 );

			// using after 3 value
			docRefHits = simpleQuery( b -> b.byField( fieldPath ).asc().onMissingValue().use( fieldModel.after3Value ), scope ).fetchHits();
			assertThat( docRefHits ).ordinal( 0 ).isDocRefHit( MULTIPLE_EMPTY_INDEX_NAME, DOCUMENT_1 );
			assertThat( docRefHits ).ordinal( 1 ).isDocRefHit( MULTIPLE_EMPTY_INDEX_NAME, DOCUMENT_2 );
			assertThat( docRefHits ).ordinal( 2 ).isDocRefHit( MULTIPLE_EMPTY_INDEX_NAME, DOCUMENT_3 );
			assertThat( docRefHits ).ordinals( 3, 4, 5, 6 ).hasDocRefHitsAnyOrder( MULTIPLE_EMPTY_INDEX_NAME, EMPTY_1, EMPTY_2, EMPTY_3, EMPTY_4 );
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3254")
	public void byField_multipleEmpty_alreadyExists_missingValue() {
		StubMappingScope scope = multipleEmptyIndexManager.createScope();

		for ( ByTypeFieldModel<?> fieldModel : multipleEmptyIndexMapping.supportedFieldModels ) {
			List<DocumentReference> docRefHits;
			String fieldPath = fieldModel.relativeFieldName;

			Object docValue1 = normalizeIfNecessary( fieldModel.document1Value.indexedValue );
			Object docValue2 = normalizeIfNecessary( fieldModel.document2Value.indexedValue );
			Object docValue3 = normalizeIfNecessary( fieldModel.document3Value.indexedValue );

			// using doc 1 value
			docRefHits = simpleQuery( b -> b.byField( fieldPath ).asc().onMissingValue().use( docValue1 ), scope ).fetchHits();
			assertThat( docRefHits ).ordinals( 0, 1, 2, 3, 4 ).hasDocRefHitsAnyOrder( MULTIPLE_EMPTY_INDEX_NAME, DOCUMENT_1, EMPTY_1, EMPTY_2, EMPTY_3, EMPTY_4 );
			assertThat( docRefHits ).ordinal( 5 ).isDocRefHit( MULTIPLE_EMPTY_INDEX_NAME, DOCUMENT_2 );
			assertThat( docRefHits ).ordinal( 6 ).isDocRefHit( MULTIPLE_EMPTY_INDEX_NAME, DOCUMENT_3 );

			// using doc 2 value
			docRefHits = simpleQuery( b -> b.byField( fieldPath ).asc().onMissingValue().use( docValue2 ), scope ).fetchHits();
			assertThat( docRefHits ).ordinal( 0 ).isDocRefHit( MULTIPLE_EMPTY_INDEX_NAME, DOCUMENT_1 );
			assertThat( docRefHits ).ordinals( 1, 2, 3, 4, 5 ).hasDocRefHitsAnyOrder( MULTIPLE_EMPTY_INDEX_NAME, DOCUMENT_2, EMPTY_1, EMPTY_2, EMPTY_3, EMPTY_4 );
			assertThat( docRefHits ).ordinal( 6 ).isDocRefHit( MULTIPLE_EMPTY_INDEX_NAME, DOCUMENT_3 );

			// using doc 3 value
			docRefHits = simpleQuery( b -> b.byField( fieldPath ).asc().onMissingValue().use( docValue3 ), scope ).fetchHits();
			assertThat( docRefHits ).ordinal( 0 ).isDocRefHit( MULTIPLE_EMPTY_INDEX_NAME, DOCUMENT_1 );
			assertThat( docRefHits ).ordinal( 1 ).isDocRefHit( MULTIPLE_EMPTY_INDEX_NAME, DOCUMENT_2 );
			assertThat( docRefHits ).ordinals( 2, 3, 4, 5, 6 ).hasDocRefHitsAnyOrder( MULTIPLE_EMPTY_INDEX_NAME, DOCUMENT_3, EMPTY_1, EMPTY_2, EMPTY_3, EMPTY_4 );
		}
	}

	@Test
	public void byField_withDslConverters_dslConverterEnabled() {
		for ( ByTypeFieldModel<?> fieldModel : indexMapping.supportedFieldWithDslConverterModels ) {
			SearchQuery<DocumentReference> query;
			String fieldPath = fieldModel.relativeFieldName;

			query = simpleQuery( b -> b.byField( fieldPath ).asc().onMissingValue()
					.use( new ValueWrapper<>( fieldModel.before1Value ) ) );
			assertThat( query )
					.hasDocRefHitsExactOrder( INDEX_NAME, EMPTY, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );
			query = simpleQuery( b -> b.byField( fieldPath ).asc().onMissingValue()
					.use( new ValueWrapper<>( fieldModel.between1And2Value ) ) );
			assertThat( query )
					.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, EMPTY, DOCUMENT_2, DOCUMENT_3 );
			query = simpleQuery( b -> b.byField( fieldPath ).asc().onMissingValue()
					.use( new ValueWrapper<>( fieldModel.between2And3Value ) ) );
			assertThat( query )
					.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, EMPTY, DOCUMENT_3 );
			query = simpleQuery( b -> b.byField( fieldPath ).asc().onMissingValue()
					.use( new ValueWrapper<>( fieldModel.after3Value ) ) );
			assertThat( query )
					.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, EMPTY );
		}
	}

	@Test
	public void byField_withDslConverters_dslConverterDisabled() {
		for ( ByTypeFieldModel<?> fieldModel : indexMapping.supportedFieldWithDslConverterModels ) {
			SearchQuery<DocumentReference> query;
			String fieldPath = fieldModel.relativeFieldName;

			query = simpleQuery( b -> b.byField( fieldPath ).asc().onMissingValue()
					.use( fieldModel.before1Value, DslConverter.DISABLED ) );
			assertThat( query )
					.hasDocRefHitsExactOrder( INDEX_NAME, EMPTY, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );
			query = simpleQuery( b -> b.byField( fieldPath ).asc().onMissingValue()
					.use( fieldModel.between1And2Value, DslConverter.DISABLED ) );
			assertThat( query )
					.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, EMPTY, DOCUMENT_2, DOCUMENT_3 );
			query = simpleQuery( b -> b.byField( fieldPath ).asc().onMissingValue()
					.use( fieldModel.between2And3Value, DslConverter.DISABLED ) );
			assertThat( query )
					.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, EMPTY, DOCUMENT_3 );
			query = simpleQuery( b -> b.byField( fieldPath ).asc().onMissingValue()
					.use( fieldModel.after3Value, DslConverter.DISABLED ) );
			assertThat( query )
					.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, EMPTY );
		}
	}

	@Test
	public void byField_inFlattenedObject() {
		for ( ByTypeFieldModel<?> fieldModel : indexMapping.flattenedObject.supportedFieldModels ) {
			SearchQuery<DocumentReference> query;
			String fieldPath = indexMapping.flattenedObject.relativeFieldName + "." + fieldModel.relativeFieldName;

			query = simpleQuery( b -> b.byField( fieldPath ).onMissingValue().sortLast() );
			assertThat( query )
					.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, EMPTY );

			query = simpleQuery( b -> b.byField( fieldPath ).asc().onMissingValue().sortLast() );
			assertThat( query )
					.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, EMPTY );

			query = simpleQuery( b -> b.byField( fieldPath ).desc().onMissingValue().sortLast() );
			assertThat( query )
					.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_3, DOCUMENT_2, DOCUMENT_1, EMPTY );

			query = simpleQuery( b -> b.byField( fieldPath ).asc().onMissingValue().sortFirst() );
			assertThat( query )
					.hasDocRefHitsExactOrder( INDEX_NAME, EMPTY, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );

			query = simpleQuery( b -> b.byField( fieldPath ).desc().onMissingValue().sortFirst() );
			assertThat( query )
					.hasDocRefHitsExactOrder( INDEX_NAME, EMPTY, DOCUMENT_3, DOCUMENT_2, DOCUMENT_1 );
		}
	}

	@Test
	public void multipleFields() {
		SearchQuery<DocumentReference> query;

		query = simpleQuery( b -> b
				.byField( indexMapping.identicalForFirstTwo.relativeFieldName ).asc().onMissingValue().sortFirst()
				.then().byField( indexMapping.identicalForLastTwo.relativeFieldName ).asc()
		);
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, EMPTY, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );

		query = simpleQuery( b -> b
				.byField( indexMapping.identicalForFirstTwo.relativeFieldName ).desc().onMissingValue().sortFirst()
				.then().byField( indexMapping.identicalForLastTwo.relativeFieldName ).desc()
		);
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, EMPTY, DOCUMENT_3, DOCUMENT_2, DOCUMENT_1 );

		query = simpleQuery( b -> b
				.byField( indexMapping.identicalForFirstTwo.relativeFieldName ).asc().onMissingValue().sortFirst()
				.then().byField( indexMapping.identicalForLastTwo.relativeFieldName ).desc()
		);
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, EMPTY, DOCUMENT_2, DOCUMENT_1, DOCUMENT_3 );

		query = simpleQuery( b -> b
				.byField( indexMapping.identicalForFirstTwo.relativeFieldName ).desc().onMissingValue().sortFirst()
				.then().byField( indexMapping.identicalForLastTwo.relativeFieldName ).asc()
		);
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, EMPTY, DOCUMENT_3, DOCUMENT_1, DOCUMENT_2 );
	}

	@Test
	public void error_unsortable() {
		StubMappingScope scope = indexManager.createScope();

		for ( ByTypeFieldModel<?> fieldModel : indexMapping.supportedNonSortableFieldModels ) {
			String fieldPath = fieldModel.relativeFieldName;

			SubTest.expectException( () -> {
					scope.sort().byField( fieldPath );
			} ).assertThrown()
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Sorting is not enabled for field" )
					.hasMessageContaining( fieldPath );
		}
	}

	@Test
	public void error_unknownField() {
		StubMappingScope scope = indexManager.createScope();

		String absoluteFieldPath = "unknownField";

		thrown.expect( SearchException.class );
		thrown.expectMessage( "Unknown field" );
		thrown.expectMessage( absoluteFieldPath );
		thrown.expectMessage( INDEX_NAME );

		scope.query()
				.predicate( f -> f.matchAll() )
				.sort( c -> c.byField( absoluteFieldPath ) )
				.toQuery();
	}

	@Test
	public void error_objectField_nested() {
		StubMappingScope scope = indexManager.createScope();

		String absoluteFieldPath = indexMapping.nestedObject.relativeFieldName;

		thrown.expect( SearchException.class );
		thrown.expectMessage( "Unknown field" );
		thrown.expectMessage( absoluteFieldPath );
		thrown.expectMessage( INDEX_NAME );

		scope.query()
				.predicate( f -> f.matchAll() )
				.sort( c -> c.byField( absoluteFieldPath ) )
				.toQuery();
	}

	@Test
	public void error_objectField_flattened() {
		StubMappingScope scope = indexManager.createScope();

		String absoluteFieldPath = indexMapping.flattenedObject.relativeFieldName;

		thrown.expect( SearchException.class );
		thrown.expectMessage( "Unknown field" );
		thrown.expectMessage( absoluteFieldPath );
		thrown.expectMessage( INDEX_NAME );

		scope.query()
				.predicate( f -> f.matchAll() )
				.sort( c -> c.byField( absoluteFieldPath ) )
				.toQuery();
	}

	@Test
	public void error_invalidType() {
		StubMappingScope scope = indexManager.createScope();

		List<ByTypeFieldModel<?>> fieldModels = new ArrayList<>();
		fieldModels.addAll( indexMapping.supportedFieldModels );
		fieldModels.addAll( indexMapping.supportedFieldWithDslConverterModels );

		for ( ByTypeFieldModel<?> fieldModel : fieldModels ) {
			String absoluteFieldPath = fieldModel.relativeFieldName;
			Object invalidValueToMatch = new InvalidType();

			SubTest.expectException(
					"byField() sort with invalid parameter type for onMissingValue().use() on field " + absoluteFieldPath,
					() -> scope.sort().byField( absoluteFieldPath ).onMissingValue()
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
	}

	@Test
	public void multiIndex_withCompatibleIndexManager_usingField() {
		StubMappingScope scope = indexManager.createScope( compatibleIndexManager );

		for ( ByTypeFieldModel<?> fieldModel : indexMapping.supportedFieldModels ) {
			SearchQuery<DocumentReference> query;
			String fieldPath = fieldModel.relativeFieldName;

			query = simpleQuery( b -> b.byField( fieldPath ).asc().onMissingValue()
					.use( fieldModel.before1Value ), scope );

			/*
			 * Not testing the ordering of results here because some documents have the same value.
			 * It's not what we want tot test anyway: we just want to check that fields are correctly
			 * detected as compatible and that no exception is thrown.
			 */
			assertThat( query ).hasDocRefHitsAnyOrder( b -> {
				b.doc( INDEX_NAME, EMPTY );
				b.doc( INDEX_NAME, DOCUMENT_1 );
				b.doc( INDEX_NAME, DOCUMENT_2 );
				b.doc( INDEX_NAME, DOCUMENT_3 );
				b.doc( COMPATIBLE_INDEX_NAME, COMPATIBLE_INDEX_DOCUMENT_1 );
			} );
		}
	}

	@Test
	public void multiIndex_withRawFieldCompatibleIndexManager_dslConverterEnabled() {
		StubMappingScope scope = indexManager.createScope( rawFieldCompatibleIndexManager );

		for ( ByTypeFieldModel<?> fieldModel : indexMapping.supportedFieldModels ) {
			String fieldPath = fieldModel.relativeFieldName;

			SubTest.expectException(
					() -> {
						simpleQuery( b -> b.byField( fieldPath ).asc().onMissingValue()
								.use( new ValueWrapper<>( fieldModel.before1Value ) ), scope );
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
	}

	@Test
	public void multiIndex_withRawFieldCompatibleIndexManager_dslConverterDisabled() {
		StubMappingScope scope = indexManager.createScope( rawFieldCompatibleIndexManager );

		for ( ByTypeFieldModel<?> fieldModel : indexMapping.supportedFieldModels ) {
			SearchQuery<DocumentReference> query;
			String fieldPath = fieldModel.relativeFieldName;

			query = simpleQuery( b -> b.byField( fieldPath ).asc().onMissingValue()
					.use( fieldModel.before1Value, DslConverter.DISABLED ), scope );

			/*
			 * Not testing the ordering of results here because some documents have the same value.
			 * It's not what we want tot test anyway: we just want to check that fields are correctly
			 * detected as compatible and that no exception is thrown.
			 */
			assertThat( query ).hasDocRefHitsAnyOrder( b -> {
				b.doc( INDEX_NAME, EMPTY );
				b.doc( INDEX_NAME, DOCUMENT_1 );
				b.doc( INDEX_NAME, DOCUMENT_2 );
				b.doc( INDEX_NAME, DOCUMENT_3 );
				b.doc( RAW_FIELD_COMPATIBLE_INDEX_NAME, RAW_FIELD_COMPATIBLE_INDEX_DOCUMENT_1 );
			} );
		}
	}

	@Test
	public void multiIndex_withNoCompatibleIndexManager_dslConverterEnabled() {
		StubMappingScope scope = indexManager.createScope( incompatibleIndexManager );

		for ( ByTypeFieldModel<?> fieldModel : indexMapping.supportedFieldModels ) {
			String fieldPath = fieldModel.relativeFieldName;

			SubTest.expectException(
					() -> {
						simpleQuery( b -> b.byField( fieldPath ), scope );
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
	}

	@Test
	public void multiIndex_withNoCompatibleIndexManager_dslConverterDisabled() {
		StubMappingScope scope = indexManager.createScope( incompatibleIndexManager );

		for ( ByTypeFieldModel<?> fieldModel : indexMapping.supportedFieldModels ) {
			String fieldPath = fieldModel.relativeFieldName;

			SubTest.expectException(
					() -> {
						simpleQuery( b -> b.byField( fieldPath ), scope );
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
	}

	@Test
	public void multiIndex_incompatibleDecimalScale() {
		StubMappingScope scope = indexManager.createScope( incompatibleDecimalScaleIndexManager );
		String fieldPath = indexMapping.scaledBigDecimal.relativeFieldName;

		SubTest.expectException(
				() -> {
					simpleQuery( b -> b.byField( fieldPath ), scope );
				}
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Multiple conflicting types to build a sort" )
				.hasMessageContaining( "'scaledBigDecimal'" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( INDEX_NAME, INCOMPATIBLE_DECIMAL_SCALE_INDEX_NAME )
				) );
	}

	private void initData() {
		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		// Important: do not index the documents in the expected order after sorts (1, 2, 3)
		workPlan.add( referenceProvider( DOCUMENT_2 ), document -> {
			indexMapping.supportedFieldModels.forEach( f -> f.document2Value.write( document ) );
			indexMapping.supportedFieldWithDslConverterModels.forEach( f -> f.document2Value.write( document ) );
			indexMapping.unsupportedFieldModels.forEach( f -> f.document2Value.write( document ) );

			indexMapping.identicalForFirstTwo.document2Value.write( document );
			indexMapping.identicalForLastTwo.document2Value.write( document );

			// Note: this object must be single-valued for these tests
			DocumentElement flattenedObject = document.addObject( indexMapping.flattenedObject.self );
			indexMapping.flattenedObject.supportedFieldModels.forEach( f -> f.document2Value.write( flattenedObject ) );
			indexMapping.flattenedObject.unsupportedFieldModels.forEach( f -> f.document2Value.write( flattenedObject ) );

			// Note: this object must be single-valued for these tests
			DocumentElement nestedObject = document.addObject( indexMapping.nestedObject.self );
			indexMapping.nestedObject.supportedFieldModels.forEach( f -> f.document2Value.write( nestedObject ) );
			indexMapping.nestedObject.unsupportedFieldModels.forEach( f -> f.document2Value.write( nestedObject ) );

			indexMapping.scaledBigDecimal.document2Value.write( document );
		} );
		workPlan.add( referenceProvider( DOCUMENT_1 ), document -> {
			indexMapping.supportedFieldModels.forEach( f -> f.document1Value.write( document ) );
			indexMapping.supportedFieldWithDslConverterModels.forEach( f -> f.document1Value.write( document ) );
			indexMapping.unsupportedFieldModels.forEach( f -> f.document1Value.write( document ) );

			indexMapping.identicalForFirstTwo.document1Value.write( document );
			indexMapping.identicalForLastTwo.document1Value.write( document );

			// Note: this object must be single-valued for these tests
			DocumentElement flattenedObject = document.addObject( indexMapping.flattenedObject.self );
			indexMapping.flattenedObject.supportedFieldModels.forEach( f -> f.document1Value.write( flattenedObject ) );
			indexMapping.flattenedObject.unsupportedFieldModels.forEach( f -> f.document1Value.write( flattenedObject ) );

			// Note: this object must be single-valued for these tests
			DocumentElement nestedObject = document.addObject( indexMapping.nestedObject.self );
			indexMapping.nestedObject.supportedFieldModels.forEach( f -> f.document1Value.write( nestedObject ) );
			indexMapping.nestedObject.unsupportedFieldModels.forEach( f -> f.document1Value.write( nestedObject ) );

			indexMapping.scaledBigDecimal.document1Value.write( document );
		} );
		workPlan.add( referenceProvider( DOCUMENT_3 ), document -> {
			indexMapping.supportedFieldModels.forEach( f -> f.document3Value.write( document ) );
			indexMapping.supportedFieldWithDslConverterModels.forEach( f -> f.document3Value.write( document ) );
			indexMapping.unsupportedFieldModels.forEach( f -> f.document3Value.write( document ) );

			indexMapping.identicalForFirstTwo.document3Value.write( document );
			indexMapping.identicalForLastTwo.document3Value.write( document );

			// Note: this object must be single-valued for these tests
			DocumentElement flattenedObject = document.addObject( indexMapping.flattenedObject.self );
			indexMapping.flattenedObject.supportedFieldModels.forEach( f -> f.document3Value.write( flattenedObject ) );
			indexMapping.flattenedObject.unsupportedFieldModels.forEach( f -> f.document3Value.write( flattenedObject ) );

			// Note: this object must be single-valued for these tests
			DocumentElement nestedObject = document.addObject( indexMapping.nestedObject.self );
			indexMapping.nestedObject.supportedFieldModels.forEach( f -> f.document3Value.write( nestedObject ) );
			indexMapping.nestedObject.unsupportedFieldModels.forEach( f -> f.document3Value.write( nestedObject ) );

			indexMapping.scaledBigDecimal.document3Value.write( document );
		} );
		workPlan.add( referenceProvider( EMPTY ), document -> { } );
		workPlan.execute().join();

		workPlan = compatibleIndexManager.createWorkPlan();
		workPlan.add( referenceProvider( COMPATIBLE_INDEX_DOCUMENT_1 ), document -> {
			compatibleIndexMapping.supportedFieldModels.forEach( f -> f.document1Value.write( document ) );
			compatibleIndexMapping.supportedFieldWithDslConverterModels.forEach( f -> f.document1Value.write( document ) );
		} );
		workPlan.execute().join();

		workPlan = rawFieldCompatibleIndexManager.createWorkPlan();
		workPlan.add( referenceProvider( RAW_FIELD_COMPATIBLE_INDEX_DOCUMENT_1 ), document -> {
			rawFieldCompatibleIndexMapping.supportedFieldModels.forEach( f -> f.document1Value.write( document ) );
		} );
		workPlan.execute().join();

		workPlan = incompatibleDecimalScaleIndexManager.createWorkPlan();
		workPlan.add( referenceProvider( INCOMPATIBLE_DECIMAL_SCALE_INDEX_DOCUMENT_1 ), document -> {
			incompatibleDecimalScaleIndexMapping.scaledBigDecimal.document1Value.write( document );
		} );
		workPlan.execute().join();

		workPlan = multipleEmptyIndexManager.createWorkPlan();
		workPlan.add( referenceProvider( EMPTY_1 ), document -> { } );
		workPlan.add( referenceProvider( DOCUMENT_2 ), document ->
			multipleEmptyIndexMapping.supportedFieldModels.forEach( f -> f.document2Value.write( document ) )
		);
		workPlan.add( referenceProvider( EMPTY_2 ), document -> { } );
		workPlan.add( referenceProvider( DOCUMENT_3 ), document ->
			multipleEmptyIndexMapping.supportedFieldModels.forEach( f -> f.document3Value.write( document ) )
		);
		workPlan.add( referenceProvider( EMPTY_3 ), document -> { } );
		workPlan.add( referenceProvider( DOCUMENT_1 ), document ->
			multipleEmptyIndexMapping.supportedFieldModels.forEach( f -> f.document1Value.write( document ) )
		);
		workPlan.add( referenceProvider( EMPTY_4 ), document -> { } );
		workPlan.execute().join();

		// Check that all documents are searchable
		SearchQuery<DocumentReference> query = indexManager.createScope().query()
				.predicate( f -> f.matchAll() )
				.toQuery();
		assertThat( query ).hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, EMPTY );
		query = compatibleIndexManager.createScope().query()
				.predicate( f -> f.matchAll() )
				.toQuery();
		assertThat( query ).hasDocRefHitsAnyOrder( COMPATIBLE_INDEX_NAME, COMPATIBLE_INDEX_DOCUMENT_1 );
		query = rawFieldCompatibleIndexManager.createScope().query()
				.predicate( f -> f.matchAll() )
				.toQuery();
		assertThat( query ).hasDocRefHitsAnyOrder( RAW_FIELD_COMPATIBLE_INDEX_NAME, RAW_FIELD_COMPATIBLE_INDEX_DOCUMENT_1 );
		query = incompatibleDecimalScaleIndexManager.createScope().query()
				.asReference()
				.predicate( f -> f.matchAll() )
				.toQuery();
		assertThat( query ).hasDocRefHitsAnyOrder( INCOMPATIBLE_DECIMAL_SCALE_INDEX_NAME, INCOMPATIBLE_DECIMAL_SCALE_INDEX_DOCUMENT_1 );
	}

	@SuppressWarnings("unchecked")
	public <T> T normalizeIfNecessary(T missingValue) {
		if ( !String.class.isInstance( missingValue ) || TckConfiguration.get().getBackendFeatures().normalizeStringMissingValues() ) {
			return missingValue;
		}

		// The backend doesn't normalize missing value replacements automatically, we have to do it ourselves
		// TODO HSEARCH-3387 Remove this once all backends correctly normalize missing value replacements
		return (T) ( (String)missingValue ).toLowerCase( Locale.ROOT );
	}

	private static void forEachTypeDescriptor(Consumer<FieldTypeDescriptor<?>> action) {
		FieldTypeDescriptor.getAll().stream()
				.filter( typeDescriptor -> typeDescriptor.getFieldSortExpectations().isPresent() )
				.forEach( action );
	}

	private static void mapByTypeFields(IndexSchemaElement parent, String prefix,
			Consumer<StandardIndexFieldTypeContext<?, ?>> additionalConfiguration,
			FieldModelConsumer<FieldSortExpectations<?>, ByTypeFieldModel<?>> consumer) {
		forEachTypeDescriptor( typeDescriptor -> {
			// Safe, see forEachTypeDescriptor
			FieldSortExpectations<?> expectations = typeDescriptor.getFieldSortExpectations().get();
			ByTypeFieldModel<?> fieldModel = ByTypeFieldModel.mapper( typeDescriptor )
					.map( parent, prefix + typeDescriptor.getUniqueName(), additionalConfiguration );
			consumer.accept( typeDescriptor, expectations, fieldModel );
		} );
	}

	private static class IndexMapping {
		final List<ByTypeFieldModel<?>> supportedFieldModels = new ArrayList<>();
		final List<ByTypeFieldModel<?>> supportedFieldWithDslConverterModels = new ArrayList<>();
		final List<ByTypeFieldModel<?>> unsupportedFieldModels = new ArrayList<>();
		final List<ByTypeFieldModel<?>> supportedNonSortableFieldModels = new ArrayList<>();

		final MainFieldModel<String> identicalForFirstTwo;
		final MainFieldModel<String> identicalForLastTwo;

		final ObjectMapping flattenedObject;
		final ObjectMapping nestedObject;

		final MainFieldModel<BigDecimal> scaledBigDecimal;

		IndexMapping(IndexSchemaElement root) {
			mapByTypeFields(
					root, "byType_", ignored -> { },
					(typeDescriptor, expectations, model) -> {
						if ( expectations.isFieldSortSupported() ) {
							supportedFieldModels.add( model );
						}
						else {
							unsupportedFieldModels.add( model );
						}
					}
			);
			mapByTypeFields(
					root, "byType_converted_", c -> c.dslConverter( ValueWrapper.toIndexFieldConverter() ),
					(typeDescriptor, expectations, model) -> {
						if ( expectations.isFieldSortSupported() ) {
							supportedFieldWithDslConverterModels.add( model );
						}
					}
			);
			mapByTypeFields(
					root, "byType_nonSortable_", c -> c.sortable( Sortable.NO ),
					(typeDescriptor, expectations, model) -> {
						if ( expectations.isFieldSortSupported() ) {
							supportedNonSortableFieldModels.add( model );
						}
					}
			);

			identicalForFirstTwo = MainFieldModel.mapper(
					"aaron", "aaron", "zach"
			)
					.map( root, "identicalForFirstTwo", c -> c.sortable( Sortable.YES ) );
			identicalForLastTwo = MainFieldModel.mapper(
					"aaron", "zach", "zach"
			)
					.map( root, "identicalForLastTwo", c -> c.sortable( Sortable.YES ) );

			flattenedObject = new ObjectMapping( root, "flattenedObject", ObjectFieldStorage.FLATTENED );
			nestedObject = new ObjectMapping( root, "nestedObject", ObjectFieldStorage.NESTED );
			scaledBigDecimal = MainFieldModel.mapper(
					c -> c.asBigDecimal().decimalScale( 3 ),
					new BigDecimal( "739.739" ), BigDecimal.ONE, BigDecimal.TEN
			)
					.map( root, "scaledBigDecimal" );
		}
	}

	private static class ObjectMapping {
		final String relativeFieldName;
		final IndexObjectFieldReference self;
		final List<ByTypeFieldModel<?>> supportedFieldModels = new ArrayList<>();
		final List<ByTypeFieldModel<?>> unsupportedFieldModels = new ArrayList<>();

		ObjectMapping(IndexSchemaElement parent, String relativeFieldName, ObjectFieldStorage storage) {
			this.relativeFieldName = relativeFieldName;
			IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, storage );
			self = objectField.toReference();
			mapByTypeFields(
					objectField, "byType_", ignored -> { },
					(typeDescriptor, expectations, model) -> {
						if ( expectations.isFieldSortSupported() ) {
							supportedFieldModels.add( model );
						}
						else {
							unsupportedFieldModels.add( model );
						}
					}
			);
		}
	}

	private static class RawFieldCompatibleIndexMapping {
		final List<ByTypeFieldModel<?>> supportedFieldModels = new ArrayList<>();

		RawFieldCompatibleIndexMapping(IndexSchemaElement root) {
			/*
			 * Add fields with the same name as the supportedFieldModels from IndexMapping,
			 * but with an incompatible DSL converter.
			 */
			mapByTypeFields(
					root, "byType_", c -> c.dslConverter( ValueWrapper.toIndexFieldConverter() ),
					(typeDescriptor, expectations, model) -> {
						if ( expectations.isFieldSortSupported() ) {
							supportedFieldModels.add( model );
						}
					}
			);
		}
	}

	private static class IncompatibleIndexMapping {
		IncompatibleIndexMapping(IndexSchemaElement root) {
			/*
			 * Add fields with the same name as the supportedFieldModels from IndexMapping,
			 * but with an incompatible type.
			 */
			forEachTypeDescriptor( typeDescriptor -> {
				StandardFieldMapper<?, IncompatibleFieldModel> mapper;
				if ( Integer.class.equals( typeDescriptor.getJavaType() ) ) {
					mapper = IncompatibleFieldModel.mapper( context -> context.asLong() );
				}
				else {
					mapper = IncompatibleFieldModel.mapper( context -> context.asInteger() );
				}
				mapper.map( root, "byType_" + typeDescriptor.getUniqueName() );
			} );
		}
	}

	private static class IncompatibleDecimalScaleIndexMapping {
		final MainFieldModel<BigDecimal> scaledBigDecimal;

		/*
		 * Unlike IndexMapping#scaledBigDecimal,
		 * we're using here a different decimal scale for the field.
		 */
		IncompatibleDecimalScaleIndexMapping(IndexSchemaElement root) {
			scaledBigDecimal = MainFieldModel.mapper(
					c -> c.asBigDecimal().decimalScale( 7 ),
					new BigDecimal( "739.739" ), BigDecimal.ONE, BigDecimal.TEN
			)
					.map( root, "scaledBigDecimal" );
		}
	}

	private static class ValueModel<F> {
		private final IndexFieldReference<F> reference;
		final F indexedValue;

		private ValueModel(IndexFieldReference<F> reference, F indexedValue) {
			this.reference = reference;
			this.indexedValue = indexedValue;
		}

		public void write(DocumentElement target) {
			target.addValue( reference, indexedValue );
		}
	}

	private static class MainFieldModel<T> {
		static StandardFieldMapper<String, MainFieldModel<String>> mapper(
				String document1Value, String document2Value, String document3Value) {
			return mapper( c -> c.asString(), document1Value, document2Value, document3Value );
		}

		static <LT> StandardFieldMapper<LT, MainFieldModel<LT>> mapper(
				Function<IndexFieldTypeFactoryContext, StandardIndexFieldTypeContext<?, LT>> configuration,
				LT document1Value, LT document2Value, LT document3Value) {
			return StandardFieldMapper.of(
					configuration,
					(reference, name) -> new MainFieldModel<>( reference, name, document1Value, document2Value, document3Value )
			);
		}

		final String relativeFieldName;
		final ValueModel<T> document1Value;
		final ValueModel<T> document2Value;
		final ValueModel<T> document3Value;

		private MainFieldModel(IndexFieldReference<T> reference, String relativeFieldName,
				T document1Value, T document2Value, T document3Value) {
			this.relativeFieldName = relativeFieldName;
			this.document1Value = new ValueModel<>( reference, document1Value );
			this.document3Value = new ValueModel<>( reference, document3Value );
			this.document2Value = new ValueModel<>( reference, document2Value );
		}
	}

	private static class ByTypeFieldModel<F> {
		static <F> StandardFieldMapper<F, ByTypeFieldModel<F>> mapper(FieldTypeDescriptor<F> typeDescriptor) {
			// Safe, see caller
			FieldSortExpectations<F> expectations = typeDescriptor.getFieldSortExpectations().get();
			return StandardFieldMapper.of(
					typeDescriptor::configure,
					c -> c.sortable( Sortable.YES ),
					(reference, name) -> new ByTypeFieldModel<>( reference, name, typeDescriptor.getJavaType(), expectations )
			);
		}

		final String relativeFieldName;
		final Class<F> type;

		final ValueModel<F> document1Value;
		final ValueModel<F> document2Value;
		final ValueModel<F> document3Value;

		final F before1Value;
		final F between1And2Value;
		final F between2And3Value;
		final F after3Value;

		private ByTypeFieldModel(IndexFieldReference<F> reference, String relativeFieldName,
				Class<F> type, FieldSortExpectations<F> expectations) {
			this.relativeFieldName = relativeFieldName;
			this.type = type;
			this.document1Value = new ValueModel<>( reference, expectations.getDocument1Value() );
			this.document2Value = new ValueModel<>( reference, expectations.getDocument2Value() );
			this.document3Value = new ValueModel<>( reference, expectations.getDocument3Value() );
			this.before1Value = expectations.getBeforeDocument1Value();
			this.between1And2Value = expectations.getBetweenDocument1And2Value();
			this.between2And3Value = expectations.getBetweenDocument2And3Value();
			this.after3Value = expectations.getAfterDocument3Value();
		}
	}

	private static class IncompatibleFieldModel {
		static <F> StandardFieldMapper<F, IncompatibleFieldModel> mapper(
				Function<IndexFieldTypeFactoryContext, StandardIndexFieldTypeContext<?, F>> configuration) {
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
