/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactoryContext;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeContext;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldModelConsumer;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.RangePredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingSearchTarget;
import org.hibernate.search.engine.backend.index.spi.IndexWorkPlan;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.InvalidType;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.StandardFieldMapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.ValueWrapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class RangeSearchPredicateIT {

	private static final String INDEX_NAME = "IndexName";
	private static final String COMPATIBLE_INDEX_NAME = "IndexWithCompatibleFields";
	private static final String RAW_FIELD_COMPATIBLE_INDEX_NAME = "IndexWithCompatibleRawFields";
	private static final String INCOMPATIBLE_INDEX_NAME = "IndexWithIncompatibleFields";

	private static final String DOCUMENT_1 = "1";
	private static final String DOCUMENT_2 = "2";
	private static final String DOCUMENT_3 = "3";
	private static final String EMPTY_ID = "empty";

	private static final String COMPATIBLE_INDEX_DOCUMENT_1 = "compatible_1";
	private static final String RAW_FIELD_COMPATIBLE_INDEX_DOCUMENT_1 = "raw_field_compatible_1";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	private IndexMapping indexMapping;
	private StubMappingIndexManager indexManager;

	private IndexMapping compatibleIndexMapping;
	private StubMappingIndexManager compatibleIndexManager;

	private RawFieldCompatibleIndexMapping rawFieldCompatibleIndexMapping;
	private StubMappingIndexManager rawFieldCompatibleIndexManager;

	private StubMappingIndexManager incompatibleIndexManager;

	@Before
	public void setup() {
		setupHelper.withDefaultConfiguration()
				.withIndex(
						"MappedType", INDEX_NAME,
						ctx -> this.indexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.withIndex(
						"CompatibleMappedType", COMPATIBLE_INDEX_NAME,
						ctx -> this.compatibleIndexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.compatibleIndexManager = indexManager
				)
				.withIndex(
						RAW_FIELD_COMPATIBLE_INDEX_NAME + "Type", RAW_FIELD_COMPATIBLE_INDEX_NAME,
						ctx -> this.rawFieldCompatibleIndexMapping = new RawFieldCompatibleIndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.rawFieldCompatibleIndexManager = indexManager
				)
				.withIndex(
						INCOMPATIBLE_INDEX_NAME + "Type", INCOMPATIBLE_INDEX_NAME,
						ctx -> new NotCompatibleIndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.incompatibleIndexManager = indexManager
				)
				.setup();

		initData();
	}

	@Test
	public void above() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		for ( ByTypeFieldModel<?> fieldModel : indexMapping.supportedFieldModels ) {
			String absoluteFieldPath = fieldModel.relativeFieldName;
			Object lowerValueToMatch = fieldModel.predicateLowerBound;

			SearchQuery<DocumentReference> query = searchTarget.query()
					.asReference()
					.predicate( f -> f.range().onField( absoluteFieldPath ).above( lowerValueToMatch ) )
					.build();

			assertThat( query )
					.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_2, DOCUMENT_3 );
		}
	}

	@Test
	public void above_withDslConverter() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		for ( ByTypeFieldModel<?> fieldModel : indexMapping.supportedFieldWithDslConverterModels ) {
			String absoluteFieldPath = fieldModel.relativeFieldName;
			Object lowerValueToMatch = new ValueWrapper<>( fieldModel.predicateLowerBound );

			SearchQuery<DocumentReference> query = searchTarget.query()
					.asReference()
					.predicate( f -> f.range().onField( absoluteFieldPath ).above( lowerValueToMatch ) )
					.build();

			assertThat( query )
					.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_2, DOCUMENT_3 );
		}
	}

	@Test
	public void above_withDslConverter_usingRawValues() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		for ( ByTypeFieldModel<?> fieldModel : indexMapping.supportedFieldWithDslConverterModels ) {
			String absoluteFieldPath = fieldModel.relativeFieldName;

			SearchQuery<DocumentReference> query = searchTarget.query()
					.asReference()
					.predicate( f -> f.range().onRawField( absoluteFieldPath ).above( fieldModel.predicateLowerBound ) )
					.build();

			assertThat( query )
					.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_2, DOCUMENT_3 );
		}
	}

	@Test
	public void above_include_exclude() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		for ( ByTypeFieldModel<?> fieldModel : indexMapping.supportedFieldModels ) {
			String absoluteFieldPath = fieldModel.relativeFieldName;
			Object lowerValueToMatch = fieldModel.document2Value.indexedValue;

			// Default is inclusion

			SearchQuery<DocumentReference> query = searchTarget.query()
					.asReference()
					.predicate( f -> f.range().onField( absoluteFieldPath ).above( lowerValueToMatch ) )
					.build();

			assertThat( query )
					.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_2, DOCUMENT_3 );

			// explicit exclusion

			query = searchTarget.query()
					.asReference()
					.predicate( f -> f.range().onField( absoluteFieldPath ).above( lowerValueToMatch ).excludeLimit() )
					.build();

			assertThat( query )
					.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_3 );
		}
	}

	@Test
	public void below() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		for ( ByTypeFieldModel<?> fieldModel : indexMapping.supportedFieldModels ) {
			String absoluteFieldPath = fieldModel.relativeFieldName;
			Object upperValueToMatch = fieldModel.predicateUpperBound;

			SearchQuery<DocumentReference> query = searchTarget.query()
					.asReference()
					.predicate( f -> f.range().onField( absoluteFieldPath ).below( upperValueToMatch ) )
					.build();

			assertThat( query )
					.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2 );
		}
	}

	@Test
	public void below_withDslConverter() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		for ( ByTypeFieldModel<?> fieldModel : indexMapping.supportedFieldWithDslConverterModels ) {
			String absoluteFieldPath = fieldModel.relativeFieldName;
			Object upperValueToMatch = new ValueWrapper<>( fieldModel.predicateUpperBound );

			SearchQuery<DocumentReference> query = searchTarget.query()
					.asReference()
					.predicate( f -> f.range().onField( absoluteFieldPath ).below( upperValueToMatch ) )
					.build();

			assertThat( query )
					.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2 );
		}
	}

	@Test
	public void below_withDslConverter_usingRawValues() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		for ( ByTypeFieldModel<?> fieldModel : indexMapping.supportedFieldWithDslConverterModels ) {
			String absoluteFieldPath = fieldModel.relativeFieldName;

			SearchQuery<DocumentReference> query = searchTarget.query()
					.asReference()
					.predicate( f -> f.range().onRawField( absoluteFieldPath ).below( fieldModel.predicateUpperBound ) )
					.build();

			assertThat( query )
					.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2 );
		}
	}

	@Test
	public void below_include_exclude() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		for ( ByTypeFieldModel<?> fieldModel : indexMapping.supportedFieldModels ) {
			String absoluteFieldPath = fieldModel.relativeFieldName;
			Object upperValueToMatch = fieldModel.document2Value.indexedValue;

			// Default is inclusion

			SearchQuery<DocumentReference> query = searchTarget.query()
					.asReference()
					.predicate( f -> f.range().onField( absoluteFieldPath ).below( upperValueToMatch ) )
					.build();

			assertThat( query )
					.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2 );

			// explicit exclusion

			query = searchTarget.query()
					.asReference()
					.predicate( f -> f.range().onField( absoluteFieldPath ).below( upperValueToMatch ).excludeLimit() )
					.build();

			assertThat( query )
					.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
		}
	}

	@Test
	public void fromTo() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		for ( ByTypeFieldModel<?> fieldModel : indexMapping.supportedFieldModels ) {
			String absoluteFieldPath = fieldModel.relativeFieldName;
			Object lowerValueToMatch = fieldModel.predicateLowerBound;
			Object upperValueToMatch = fieldModel.predicateUpperBound;

			SearchQuery<DocumentReference> query = searchTarget.query()
					.asReference()
					.predicate( f -> f.range().onField( absoluteFieldPath ).from( lowerValueToMatch ).to( upperValueToMatch ) )
					.build();

			assertThat( query )
					.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_2 );
		}
	}

	@Test
	public void fromTo_withDslConverter() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		for ( ByTypeFieldModel<?> fieldModel : indexMapping.supportedFieldWithDslConverterModels ) {
			String absoluteFieldPath = fieldModel.relativeFieldName;
			Object lowerValueToMatch = new ValueWrapper<>( fieldModel.predicateLowerBound );
			Object upperValueToMatch = new ValueWrapper<>( fieldModel.predicateUpperBound );

			SearchQuery<DocumentReference> query = searchTarget.query()
					.asReference()
					.predicate( f -> f.range().onField( absoluteFieldPath ).from( lowerValueToMatch ).to( upperValueToMatch ) )
					.build();

			assertThat( query )
					.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_2 );
		}
	}

	@Test
	public void fromTo_withDslConverter_usingRawValues() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		for ( ByTypeFieldModel<?> fieldModel : indexMapping.supportedFieldWithDslConverterModels ) {
			String absoluteFieldPath = fieldModel.relativeFieldName;

			SearchQuery<DocumentReference> query = searchTarget.query()
					.asReference()
					.predicate( f -> f.range().onRawField( absoluteFieldPath )
							.from( fieldModel.predicateLowerBound ).to( fieldModel.predicateUpperBound ) )
					.build();

			assertThat( query )
					.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_2 );
		}
	}

	@Test
	public void fromTo_include_exclude() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		for ( ByTypeFieldModel<?> fieldModel : indexMapping.supportedFieldModels ) {
			String absoluteFieldPath = fieldModel.relativeFieldName;
			Object value1ToMatch = fieldModel.document1Value.indexedValue;
			Object value2ToMatch = fieldModel.document2Value.indexedValue;
			Object value3ToMatch = fieldModel.document3Value.indexedValue;

			// Default is inclusion

			SearchQuery<DocumentReference> query = searchTarget.query()
					.asReference()
					.predicate( f -> f.range().onField( absoluteFieldPath ).from( value1ToMatch ).to( value2ToMatch ) )
					.build();

			assertThat( query )
					.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2 );

			// explicit exclusion for the from clause

			query = searchTarget.query()
					.asReference()
					.predicate( f -> f.range().onField( absoluteFieldPath )
							.from( value1ToMatch ).excludeLimit()
							.to( value2ToMatch )
					)
					.build();

			assertThat( query )
					.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_2 );

			// explicit exclusion for the to clause

			query = searchTarget.query()
					.asReference()
					.predicate( f -> f.range().onField( absoluteFieldPath )
							.from( value1ToMatch )
							.to( value2ToMatch ).excludeLimit()
					)
					.build();

			assertThat( query )
					.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

			// explicit exclusion for both clauses

			query = searchTarget.query()
					.asReference()
					.predicate( f -> f.range().onField( absoluteFieldPath )
							.from( value1ToMatch ).excludeLimit()
							.to( value3ToMatch ).excludeLimit()
					)
					.build();

			assertThat( query )
					.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_2 );
		}
	}

	@Test
	public void unsupported_field_types() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		for ( ByTypeFieldModel<?> fieldModel : indexMapping.unsupportedFieldModels ) {
			String absoluteFieldPath = fieldModel.relativeFieldName;

			SubTest.expectException(
					"range() predicate with unsupported type on field " + absoluteFieldPath,
					() -> searchTarget.predicate().range().onField( absoluteFieldPath )
			)
					.assertThrown()
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Range predicates are not supported by" )
					.satisfies( FailureReportUtils.hasContext(
							EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
					) );
		}
	}

	@Test
	public void fieldLevelBoost() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		SearchQuery<DocumentReference> query = searchTarget.query()
				.asReference()
				.predicate( f -> f.bool()
						.should( f.range().onField( indexMapping.string1Field.relativeFieldName )
								.above( indexMapping.string1Field.document3Value.indexedValue )
						)
						.should( f.range().onField( indexMapping.string1Field.relativeFieldName ).boostedTo( 42 )
								.below( indexMapping.string1Field.document1Value.indexedValue )
						)
				)
				.sort( c -> c.byScore() )
				.build();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_3 );

		query = searchTarget.query()
				.asReference()
				.predicate( f -> f.bool()
						.should( f.range().onField( indexMapping.string1Field.relativeFieldName ).boostedTo( 42 )
								.above( indexMapping.string1Field.document3Value.indexedValue )
						)
						.should( f.range().onField( indexMapping.string1Field.relativeFieldName )
								.below( indexMapping.string1Field.document1Value.indexedValue )
						)
				)
				.sort( c -> c.byScore() )
				.build();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_3, DOCUMENT_1 );
	}

	@Test
	public void predicateLevelBoost() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		SearchQuery<DocumentReference> query = searchTarget.query()
				.asReference()
				.predicate( f -> f.bool()
						.should( f.range().onField( indexMapping.string1Field.relativeFieldName )
								.above( indexMapping.string1Field.document3Value.indexedValue )
						)
						.should( f.range().boostedTo( 7 ).onField( indexMapping.string1Field.relativeFieldName )
								.below( indexMapping.string1Field.document1Value.indexedValue )
						)
				)
				.sort( c -> c.byScore() )
				.build();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_3 );

		query = searchTarget.query()
				.asReference()
				.predicate( f -> f.bool()
						.should( f.range().boostedTo( 39 ).onField( indexMapping.string1Field.relativeFieldName )
								.above( indexMapping.string1Field.document3Value.indexedValue )
						)
						.should( f.range().onField( indexMapping.string1Field.relativeFieldName )
								.below( indexMapping.string1Field.document1Value.indexedValue )
						)
				)
				.sort( c -> c.byScore() )
				.build();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_3, DOCUMENT_1 );
	}

	@Test
	public void predicateLevelBoost_andFieldLevelBoost() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		SearchQuery<DocumentReference> query = searchTarget.query()
				.asReference()
				.predicate( f -> f.bool()
						// 2 * 3 => boost x6
						.should( f.range().boostedTo( 2 ).onField( indexMapping.string1Field.relativeFieldName ).boostedTo( 3 )
								.above( indexMapping.string1Field.document3Value.indexedValue )
						)
						// 7 * 1 => boost x7
						.should( f.range().boostedTo( 7 ).onField( indexMapping.string1Field.relativeFieldName )
								.below( indexMapping.string1Field.document1Value.indexedValue )
						)
				)
				.sort( c -> c.byScore() )
				.build();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_3 );

		query = searchTarget.query()
				.asReference()
				.predicate( f -> f.bool()
						// 39 * 0.5 => boost x19.5
						.should( f.range().boostedTo( 39 ).onField( indexMapping.string1Field.relativeFieldName ).boostedTo( 0.5f )
								.above( indexMapping.string1Field.document3Value.indexedValue )
						)
						// 3 * 3 => boost x9
						.should( f.range().boostedTo( 3 ).onField( indexMapping.string1Field.relativeFieldName ).boostedTo( 3 )
								.below( indexMapping.string1Field.document1Value.indexedValue )
						)
				)
				.sort( c -> c.byScore() )
				.build();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_3, DOCUMENT_1 );
	}

	@Test
	public void predicateLevelBoost_multiFields() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		SearchQuery<DocumentReference> query = searchTarget.query()
				.asReference()
				.predicate( f -> f.bool()
						.should( f.range().boostedTo( 2 ).onField( indexMapping.string1Field.relativeFieldName )
								.orField( indexMapping.string2Field.relativeFieldName )
								.above( indexMapping.string1Field.document3Value.indexedValue )
						)
						.should( f.range().boostedTo( 7 ).onField( indexMapping.string1Field.relativeFieldName )
								.orField( indexMapping.string2Field.relativeFieldName )
								.below( indexMapping.string1Field.document1Value.indexedValue )
						)
				)
				.sort( c -> c.byScore() )
				.build();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_3 );

		query = searchTarget.query()
				.asReference()
				.predicate( f -> f.bool()
						.should( f.range().boostedTo( 39 ).onField( indexMapping.string1Field.relativeFieldName )
								.orField( indexMapping.string2Field.relativeFieldName )
								.above( indexMapping.string1Field.document3Value.indexedValue )
						)
						.should( f.range().boostedTo( 3 ).onField( indexMapping.string1Field.relativeFieldName )
								.orField( indexMapping.string2Field.relativeFieldName )
								.below( indexMapping.string1Field.document1Value.indexedValue )
						)
				)
				.sort( c -> c.byScore() )
				.build();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_3, DOCUMENT_1 );
	}

	@Test
	public void multi_fields() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		// onField(...).orField(...)

		SearchQuery<DocumentReference> query = searchTarget.query()
				.asReference()
				.predicate( f -> f.range().onField( indexMapping.string1Field.relativeFieldName )
						.orField( indexMapping.string2Field.relativeFieldName )
						.below( indexMapping.string1Field.document1Value.indexedValue )
				)
				.build();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		query = searchTarget.query()
				.asReference()
				.predicate( f -> f.range().onField( indexMapping.string1Field.relativeFieldName )
						.orField( indexMapping.string2Field.relativeFieldName )
						.above( indexMapping.string2Field.document3Value.indexedValue )
				)
				.build();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_3 );

		// onField().orFields(...)

		query = searchTarget.query()
				.asReference()
				.predicate( f -> f.range().onField( indexMapping.string1Field.relativeFieldName )
						.orFields( indexMapping.string2Field.relativeFieldName, indexMapping.string3Field.relativeFieldName )
						.below( indexMapping.string1Field.document1Value.indexedValue )
				)
				.build();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		query = searchTarget.query()
				.asReference()
				.predicate( f -> f.range().onField( indexMapping.string1Field.relativeFieldName )
						.orFields( indexMapping.string2Field.relativeFieldName, indexMapping.string3Field.relativeFieldName )
						.from( "d" ).to( "e" )
				)
				.build();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		query = searchTarget.query()
				.asReference()
				.predicate( f -> f.range().onField( indexMapping.string1Field.relativeFieldName )
						.orFields( indexMapping.string2Field.relativeFieldName, indexMapping.string3Field.relativeFieldName )
						.above( indexMapping.string3Field.document3Value.indexedValue )
				)
				.build();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_3 );

		// onFields(...)

		query = searchTarget.query()
				.asReference()
				.predicate( f -> f.range().onFields( indexMapping.string1Field.relativeFieldName, indexMapping.string2Field.relativeFieldName )
						.below( indexMapping.string1Field.document1Value.indexedValue )
				)
				.build();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		query = searchTarget.query()
				.asReference()
				.predicate( f -> f.range().onFields( indexMapping.string1Field.relativeFieldName, indexMapping.string2Field.relativeFieldName )
						.above( indexMapping.string2Field.document3Value.indexedValue )
				)
				.build();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_3 );
	}

	@Test
	public void multiField_withDslConverter() {
		SearchQuery<DocumentReference> query = indexManager.createSearchTarget().query()
				.asReference()
				.predicate( f -> f.range().onField( indexMapping.string1FieldWithDslConverter.relativeFieldName )
						.orField( indexMapping.string2FieldWithDslConverter.relativeFieldName )
						.below( new ValueWrapper<>( indexMapping.string1FieldWithDslConverter.document1Value.indexedValue ) )
				)
				.build();

		assertThat( query ).hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void multiFields_withDslConverter_usingRawValues() {
		SearchQuery<DocumentReference> query = indexManager.createSearchTarget().query()
				.asReference()
				.predicate( f -> f.range().onRawField( indexMapping.string1FieldWithDslConverter.relativeFieldName )
						.orRawField( indexMapping.string2FieldWithDslConverter.relativeFieldName )
						.below( indexMapping.string1FieldWithDslConverter.document1Value.indexedValue )
				)
				.build();

		assertThat( query ).hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void range_error_null() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		for ( ByTypeFieldModel<?> fieldModel : indexMapping.supportedFieldModels ) {
			String fieldPath = fieldModel.relativeFieldName;
			SubTest.expectException(
					"range() predicate with null bounds on field " + fieldPath,
					() -> searchTarget.predicate().range().onField( fieldPath ).from( null ).to( null )
			)
					.assertThrown()
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Invalid value" )
					.hasMessageContaining( "at least one bound" )
					.hasMessageContaining( "must be non-null" )
					.hasMessageContaining( fieldPath );

			SubTest.expectException(
					"range() predicate with null bounds on field " + fieldPath,
					() -> searchTarget.predicate().range().onField( fieldPath ).above( null )
			)
					.assertThrown()
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Invalid value" )
					.hasMessageContaining( "at least one bound" )
					.hasMessageContaining( "must be non-null" )
					.hasMessageContaining( fieldPath );


			SubTest.expectException(
					"range() predicate with null bounds on field " + fieldPath,
					() -> searchTarget.predicate().range().onField( fieldPath ).below( null )
			)
					.assertThrown()
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Invalid value" )
					.hasMessageContaining( "at least one bound" )
					.hasMessageContaining( "must be non-null" )
					.hasMessageContaining( fieldPath );
		}
	}

	@Test
	public void unknown_field() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		SubTest.expectException(
				"range() predicate with unknown field",
				() -> searchTarget.predicate().range().onField( "unknown_field" )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" )
				.hasMessageContaining( "'unknown_field'" );

		SubTest.expectException(
				"range() predicate with unknown field",
				() -> searchTarget.predicate().range().onFields( indexMapping.string1Field.relativeFieldName, "unknown_field" )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" )
				.hasMessageContaining( "'unknown_field'" );

		SubTest.expectException(
				"range() predicate with unknown field",
				() -> searchTarget.predicate().range().onField( indexMapping.string1Field.relativeFieldName ).orField( "unknown_field" )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" )
				.hasMessageContaining( "'unknown_field'" );

		SubTest.expectException(
				"range() predicate with unknown field",
				() -> searchTarget.predicate().range().onField( indexMapping.string1Field.relativeFieldName ).orFields( "unknown_field" )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" )
				.hasMessageContaining( "'unknown_field'" );
	}

	@Test
	public void error_invalidType() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		List<ByTypeFieldModel<?>> fieldModels = new ArrayList<>();
		fieldModels.addAll( indexMapping.supportedFieldModels );
		fieldModels.addAll( indexMapping.supportedFieldWithDslConverterModels );

		for ( ByTypeFieldModel<?> fieldModel : fieldModels ) {
			String absoluteFieldPath = fieldModel.relativeFieldName;
			Object invalidValueToMatch = new InvalidType();

			SubTest.expectException(
					"range().above() predicate with invalid parameter type on field " + absoluteFieldPath,
					() -> searchTarget.predicate().range().onField( absoluteFieldPath ).above( invalidValueToMatch )
			)
					.assertThrown()
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Unable to convert DSL parameter: " )
					.hasMessageContaining( InvalidType.class.getName() )
					.hasCauseInstanceOf( ClassCastException.class )
					.satisfies( FailureReportUtils.hasContext(
							EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
					) );

			SubTest.expectException(
					"range().below() predicate with invalid parameter type on field " + absoluteFieldPath,
					() -> searchTarget.predicate().range().onField( absoluteFieldPath ).below( invalidValueToMatch )
			)
					.assertThrown()
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Unable to convert DSL parameter: " )
					.hasMessageContaining( InvalidType.class.getName() )
					.hasCauseInstanceOf( ClassCastException.class )
					.satisfies( FailureReportUtils.hasContext(
							EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
					) );

			SubTest.expectException(
					"range().from() predicate with invalid parameter type on field " + absoluteFieldPath,
					() -> searchTarget.predicate().range().onField( absoluteFieldPath ).from( invalidValueToMatch )
			)
					.assertThrown()
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Unable to convert DSL parameter: " )
					.hasMessageContaining( InvalidType.class.getName() )
					.hasCauseInstanceOf( ClassCastException.class )
					.satisfies( FailureReportUtils.hasContext(
							EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
					) );

			SubTest.expectException(
					"range().from().to() predicate with invalid parameter type on field " + absoluteFieldPath,
					() -> searchTarget.predicate().range().onField( absoluteFieldPath )
							.from( null ).to( invalidValueToMatch )
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
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget( compatibleIndexManager );

		for ( ByTypeFieldModel<?> fieldModel : indexMapping.supportedFieldModels ) {
			String absoluteFieldPath = fieldModel.relativeFieldName;
			Object upperValueToMatch = fieldModel.predicateUpperBound;

			SearchQuery<DocumentReference> query = searchTarget.query()
					.asReference()
					.predicate( f -> f.range().onField( absoluteFieldPath ).below( upperValueToMatch ) )
					.build();

			assertThat( query ).hasDocRefHitsAnyOrder( b -> {
				b.doc( INDEX_NAME, DOCUMENT_1 );
				b.doc( INDEX_NAME, DOCUMENT_2 );
				b.doc( COMPATIBLE_INDEX_NAME, COMPATIBLE_INDEX_DOCUMENT_1 );
			} );
		}
	}

	@Test
	public void multiIndex_withRawFieldCompatibleIndexManager_usingField() {
		for ( ByTypeFieldModel<?> fieldModel : indexMapping.supportedFieldModels ) {
			SubTest.expectException(
					() -> {
						indexManager.createSearchTarget( rawFieldCompatibleIndexManager )
								.predicate().range().onField( fieldModel.relativeFieldName );
					}
			)
					.assertThrown()
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Multiple conflicting types to build a predicate" )
					.hasMessageContaining( "'" + fieldModel.relativeFieldName + "'" )
					.satisfies( FailureReportUtils.hasContext(
							EventContexts.fromIndexNames( INDEX_NAME, RAW_FIELD_COMPATIBLE_INDEX_NAME )
					) );
		}
	}

	@Test
	public void multiIndex_withRawFieldCompatibleIndexManager_usingRawField() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget( rawFieldCompatibleIndexManager );

		for ( ByTypeFieldModel<?> fieldModel : indexMapping.supportedFieldModels ) {
			String absoluteFieldPath = fieldModel.relativeFieldName;
			Object upperValueToMatch = fieldModel.predicateUpperBound;

			SearchQuery<DocumentReference> query = searchTarget.query()
					.asReference()
					.predicate( f -> f.range().onRawField( absoluteFieldPath ).below( upperValueToMatch ) )
					.build();

			assertThat( query ).hasDocRefHitsAnyOrder( b -> {
				b.doc( INDEX_NAME, DOCUMENT_1 );
				b.doc( INDEX_NAME, DOCUMENT_2 );
				b.doc( RAW_FIELD_COMPATIBLE_INDEX_NAME, RAW_FIELD_COMPATIBLE_INDEX_DOCUMENT_1 );
			} );
		}
	}

	@Test
	public void multiIndex_withNoCompatibleIndexManager_usingField() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget( incompatibleIndexManager );

		for ( ByTypeFieldModel<?> fieldModel : indexMapping.supportedFieldModels ) {
			String fieldPath = fieldModel.relativeFieldName;

			SubTest.expectException(
					() -> searchTarget.predicate().range().onField( fieldPath )
			)
					.assertThrown()
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Multiple conflicting types to build a predicate" )
					.hasMessageContaining( "'" + fieldPath + "'" )
					.satisfies( FailureReportUtils.hasContext(
							EventContexts.fromIndexNames( INDEX_NAME, INCOMPATIBLE_INDEX_NAME )
					) );
		}
	}

	@Test
	public void multiIndex_withNoCompatibleIndexManager_usingRawField() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget( incompatibleIndexManager );

		for ( ByTypeFieldModel<?> fieldModel : indexMapping.supportedFieldModels ) {
			String fieldPath = fieldModel.relativeFieldName;

			SubTest.expectException(
					() -> searchTarget.predicate().range().onRawField( fieldPath )
			)
					.assertThrown()
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Multiple conflicting types to build a predicate" )
					.hasMessageContaining( "'" + fieldPath + "'" )
					.satisfies( FailureReportUtils.hasContext(
							EventContexts.fromIndexNames( INDEX_NAME, INCOMPATIBLE_INDEX_NAME )
					) );
		}
	}

	private void initData() {
		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( DOCUMENT_1 ), document -> {
			indexMapping.supportedFieldModels.forEach( f -> f.document1Value.write( document ) );
			indexMapping.supportedFieldWithDslConverterModels.forEach( f -> f.document1Value.write( document ) );
			indexMapping.unsupportedFieldModels.forEach( f -> f.document1Value.write( document ) );
			indexMapping.string1Field.document1Value.write( document );
			indexMapping.string2Field.document1Value.write( document );
			indexMapping.string3Field.document1Value.write( document );
			indexMapping.string1FieldWithDslConverter.document1Value.write( document );
			indexMapping.string2FieldWithDslConverter.document1Value.write( document );
		} );
		workPlan.add( referenceProvider( DOCUMENT_2 ), document -> {
			indexMapping.supportedFieldModels.forEach( f -> f.document2Value.write( document ) );
			indexMapping.supportedFieldWithDslConverterModels.forEach( f -> f.document2Value.write( document ) );
			indexMapping.unsupportedFieldModels.forEach( f -> f.document2Value.write( document ) );
			indexMapping.string1Field.document2Value.write( document );
			indexMapping.string2Field.document2Value.write( document );
			indexMapping.string3Field.document2Value.write( document );
			indexMapping.string1FieldWithDslConverter.document2Value.write( document );
			indexMapping.string2FieldWithDslConverter.document2Value.write( document );
		} );
		workPlan.add( referenceProvider( DOCUMENT_3 ), document -> {
			indexMapping.supportedFieldModels.forEach( f -> f.document3Value.write( document ) );
			indexMapping.supportedFieldWithDslConverterModels.forEach( f -> f.document3Value.write( document ) );
			indexMapping.unsupportedFieldModels.forEach( f -> f.document3Value.write( document ) );
			indexMapping.string1Field.document3Value.write( document );
			indexMapping.string2Field.document3Value.write( document );
			indexMapping.string3Field.document3Value.write( document );
			indexMapping.string1FieldWithDslConverter.document3Value.write( document );
			indexMapping.string2FieldWithDslConverter.document3Value.write( document );
		} );
		workPlan.add( referenceProvider( EMPTY_ID ), document -> { } );
		workPlan.execute().join();

		workPlan = compatibleIndexManager.createWorkPlan();
		workPlan.add( referenceProvider( COMPATIBLE_INDEX_DOCUMENT_1 ), document -> {
			indexMapping.supportedFieldModels.forEach( f -> f.document1Value.write( document ) );
			indexMapping.supportedFieldWithDslConverterModels.forEach( f -> f.document1Value.write( document ) );
		} );
		workPlan.execute().join();

		workPlan = rawFieldCompatibleIndexManager.createWorkPlan();
		workPlan.add( referenceProvider( RAW_FIELD_COMPATIBLE_INDEX_DOCUMENT_1 ), document -> {
			rawFieldCompatibleIndexMapping.supportedFieldModels.forEach( f -> f.document1Value.write( document ) );
		} );
		workPlan.execute().join();

		// Check that all documents are searchable
		SearchQuery<DocumentReference> query = indexManager.createSearchTarget().query()
				.asReference()
				.predicate( f -> f.matchAll() )
				.build();
		assertThat( query ).hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, EMPTY_ID );
		query = compatibleIndexManager.createSearchTarget().query()
				.asReference()
				.predicate( f -> f.matchAll() )
				.build();
		assertThat( query ).hasDocRefHitsAnyOrder( COMPATIBLE_INDEX_NAME, COMPATIBLE_INDEX_DOCUMENT_1 );
		query = rawFieldCompatibleIndexManager.createSearchTarget().query()
				.asReference()
				.predicate( f -> f.matchAll() )
				.build();
		assertThat( query ).hasDocRefHitsAnyOrder( RAW_FIELD_COMPATIBLE_INDEX_NAME, RAW_FIELD_COMPATIBLE_INDEX_DOCUMENT_1 );
	}

	private static void forEachTypeDescriptor(Consumer<FieldTypeDescriptor<?>> action) {
		FieldTypeDescriptor.getAll().stream()
				.filter( typeDescriptor -> typeDescriptor.getRangePredicateExpectations().isPresent() )
				.forEach( action );
	}

	private static void mapByTypeFields(IndexSchemaElement parent, String prefix,
			Consumer<StandardIndexFieldTypeContext<?, ?>> additionalConfiguration,
			FieldModelConsumer<RangePredicateExpectations<?>, ByTypeFieldModel<?>> consumer) {
		forEachTypeDescriptor( typeDescriptor -> {
			// Safe, see forEachTypeDescriptor
			RangePredicateExpectations<?> expectations = typeDescriptor.getRangePredicateExpectations().get();
			ByTypeFieldModel<?> fieldModel = ByTypeFieldModel.mapper( typeDescriptor )
					.map( parent, prefix + typeDescriptor.getUniqueName(), additionalConfiguration );
			consumer.accept( typeDescriptor, expectations, fieldModel );
		} );
	}

	private static class IndexMapping {
		final List<ByTypeFieldModel<?>> supportedFieldModels = new ArrayList<>();
		final List<ByTypeFieldModel<?>> supportedFieldWithDslConverterModels = new ArrayList<>();
		final List<ByTypeFieldModel<?>> unsupportedFieldModels = new ArrayList<>();

		final MainFieldModel string1Field;
		final MainFieldModel string2Field;
		final MainFieldModel string3Field;

		final MainFieldModel string1FieldWithDslConverter;
		final MainFieldModel string2FieldWithDslConverter;

		IndexMapping(IndexSchemaElement root) {
			mapByTypeFields(
					root, "byType_", ignored -> { },
					(typeDescriptor, expectations, model) -> {
						if ( expectations.isRangePredicateSupported() ) {
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
						if ( expectations.isRangePredicateSupported() ) {
							supportedFieldWithDslConverterModels.add( model );
						}
					}
			);
			string1Field = MainFieldModel.mapper( "ccc", "mmm", "xxx" )
					.map( root, "string1" );
			string2Field = MainFieldModel.mapper( "ddd", "nnn", "yyy" )
					.map( root, "string2" );
			string3Field = MainFieldModel.mapper( "eee", "ooo", "zzz" )
					.map( root, "string3" );
			string1FieldWithDslConverter = MainFieldModel.mapper(
					c -> c.asString().dslConverter( ValueWrapper.toIndexFieldConverter() ),
					"ccc", "mmm", "xxx"
			)
					.map( root, "string1FieldWithDslConverter" );
			string2FieldWithDslConverter = MainFieldModel.mapper(
					c -> c.asString().dslConverter( ValueWrapper.toIndexFieldConverter() ),
					"ddd", "nnn", "yyy"
			)
					.map( root, "string2FieldWithDslConverter" );
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
						if ( expectations.isRangePredicateSupported() ) {
							supportedFieldModels.add( model );
						}
					}
			);
		}
	}

	private static class NotCompatibleIndexMapping {
		NotCompatibleIndexMapping(IndexSchemaElement root) {
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

	private static class ValueModel<F> {
		private final IndexFieldAccessor<F> accessor;
		final F indexedValue;

		private ValueModel(IndexFieldAccessor<F> accessor, F indexedValue) {
			this.accessor = accessor;
			this.indexedValue = indexedValue;
		}

		public void write(DocumentElement target) {
			accessor.write( target, indexedValue );
		}
	}

	private static class MainFieldModel {
		public static StandardFieldMapper<String, MainFieldModel> mapper(
				String document1Value, String document2Value, String document3Value) {
			return StandardFieldMapper.of(
					f -> f.asString(),
					(accessor, name) -> new MainFieldModel( accessor, name, document1Value, document2Value, document3Value )
			);
		}

		static StandardFieldMapper<String, MainFieldModel> mapper(
				Function<IndexFieldTypeFactoryContext, StandardIndexFieldTypeContext<?, String>> configuration,
				String document1Value, String document2Value, String document3Value) {
			return StandardFieldMapper.of(
					configuration,
					(accessor, name) -> new MainFieldModel( accessor, name, document1Value, document2Value, document3Value )
			);
		}

		final String relativeFieldName;
		final ValueModel<String> document1Value;
		final ValueModel<String> document2Value;
		final ValueModel<String> document3Value;

		private MainFieldModel(IndexFieldAccessor<String> accessor, String relativeFieldName,
				String document1Value, String document2Value, String document3Value) {
			this.relativeFieldName = relativeFieldName;
			this.document1Value = new ValueModel<>( accessor, document1Value );
			this.document2Value = new ValueModel<>( accessor, document2Value );
			this.document3Value = new ValueModel<>( accessor, document3Value );
		}
	}

	private static class ByTypeFieldModel<F> {
		static <F> StandardFieldMapper<F, ByTypeFieldModel<F>> mapper(FieldTypeDescriptor<F> typeDescriptor) {
			// Safe, see caller
			RangePredicateExpectations<F> expectations = typeDescriptor.getRangePredicateExpectations().get();
			return StandardFieldMapper.of(
					typeDescriptor::configure,
					(accessor, name) -> new ByTypeFieldModel<>( accessor, name, expectations )
			);
		}

		final String relativeFieldName;
		final ValueModel<F> document1Value;
		final ValueModel<F> document2Value;
		final ValueModel<F> document3Value;

		final F predicateLowerBound;
		final F predicateUpperBound;

		private ByTypeFieldModel(IndexFieldAccessor<F> accessor, String relativeFieldName,
				RangePredicateExpectations<F> expectations) {
			this.relativeFieldName = relativeFieldName;
			this.document1Value = new ValueModel<>( accessor, expectations.getDocument1Value() );
			this.document2Value = new ValueModel<>( accessor, expectations.getDocument2Value() );
			this.document3Value = new ValueModel<>( accessor, expectations.getDocument3Value() );
			this.predicateLowerBound = expectations.getBetweenDocument1And2Value();
			this.predicateUpperBound = expectations.getBetweenDocument2And3Value();
		}
	}

	private static class IncompatibleFieldModel {
		static <F> StandardFieldMapper<F, IncompatibleFieldModel> mapper(
				Function<IndexFieldTypeFactoryContext, StandardIndexFieldTypeContext<?, F>> configuration) {
			return StandardFieldMapper.of(
					configuration,
					(accessor, name) -> new IncompatibleFieldModel( name )
			);
		}

		final String relativeFieldName;

		private IncompatibleFieldModel(String relativeFieldName) {
			this.relativeFieldName = relativeFieldName;
		}
	}
}
