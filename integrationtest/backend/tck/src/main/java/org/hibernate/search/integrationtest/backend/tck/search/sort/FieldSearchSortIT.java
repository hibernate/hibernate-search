/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.sort;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.IndexObjectFieldAccessor;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactoryContext;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeContext;
import org.hibernate.search.engine.backend.index.spi.IndexWorkPlan;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldModelConsumer;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.FieldSortExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingSearchTarget;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.query.spi.SearchQuery;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.InvalidType;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.StandardFieldMapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.ValueWrapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.test.SubTest;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class FieldSearchSortIT {

	private static final String INDEX_NAME = "IndexName";
	private static final String COMPATIBLE_INDEX_NAME = "IndexWithCompatibleFields";
	private static final String RAW_FIELD_COMPATIBLE_INDEX_NAME = "IndexWithCompatibleRawFields";
	private static final String INCOMPATIBLE_INDEX_NAME = "IndexWithIncompatibleFields";

	private static final String DOCUMENT_1 = "1";
	private static final String DOCUMENT_2 = "2";
	private static final String DOCUMENT_3 = "3";
	private static final String EMPTY = "empty";

	private static final String COMPATIBLE_INDEX_DOCUMENT_1 = "compatible_1";
	private static final String RAW_FIELD_COMPATIBLE_INDEX_DOCUMENT_1 = "raw_field_compatible_1";

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
				.setup();

		initData();
	}

	private SearchQuery<DocumentReference> simpleQuery(Consumer<? super SearchSortContainerContext> sortContributor) {
		return simpleQuery( sortContributor, indexManager.createSearchTarget() );
	}

	private SearchQuery<DocumentReference> simpleQuery(Consumer<? super SearchSortContainerContext> sortContributor, StubMappingSearchTarget searchTarget) {
		return searchTarget.query()
				.asReference()
				.predicate( f -> f.matchAll() )
				.sort( sortContributor )
				.build();
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
			// Eventually we will remove any restriction here. See the issues: HSEARCH-3254 HSEARCH-3255 and HSEARCH-3387.
			if (
					( TckConfiguration.get().getBackendFeatures().stringTypeOnMissingValueUse() || !String.class.equals( fieldModel.type ) )
					&& ( TckConfiguration.get().getBackendFeatures().localDateTypeOnMissingValueUse() || !isJavaTimeType( fieldModel.type ) )
			) {
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
	}

	@Test
	public void byField_withDslConverters() {
		for ( ByTypeFieldModel<?> fieldModel : indexMapping.supportedFieldWithDslConverterModels ) {
			SearchQuery<DocumentReference> query;
			String fieldPath = fieldModel.relativeFieldName;

			// Eventually we will remove any restriction here. See the issues: HSEARCH-3254 HSEARCH-3255 and HSEARCH-3387.
			if (
					( TckConfiguration.get().getBackendFeatures().stringTypeOnMissingValueUse() || !String.class.equals( fieldModel.type ) )
					&& ( TckConfiguration.get().getBackendFeatures().localDateTypeOnMissingValueUse() || !isJavaTimeType( fieldModel.type ) )
			) {
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
	}

	@Test
	public void byField_withDslConverters_usingRawValues() {
		for ( ByTypeFieldModel<?> fieldModel : indexMapping.supportedFieldWithDslConverterModels ) {
			SearchQuery<DocumentReference> query;
			String fieldPath = fieldModel.relativeFieldName;

			// Eventually we will remove any restriction here. See the issues: HSEARCH-3254 HSEARCH-3255 and HSEARCH-3387.
			if (
					( TckConfiguration.get().getBackendFeatures().stringTypeOnMissingValueUse() || !String.class.equals( fieldModel.type ) )
							&& ( TckConfiguration.get().getBackendFeatures().localDateTypeOnMissingValueUse() || !isJavaTimeType( fieldModel.type ) )
			) {
				query = simpleQuery( b -> b.byRawField( fieldPath ).asc().onMissingValue()
						.use( fieldModel.before1Value ) );
				assertThat( query )
						.hasDocRefHitsExactOrder( INDEX_NAME, EMPTY, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );
				query = simpleQuery( b -> b.byRawField( fieldPath ).asc().onMissingValue()
						.use( fieldModel.between1And2Value ) );
				assertThat( query )
						.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, EMPTY, DOCUMENT_2, DOCUMENT_3 );
				query = simpleQuery( b -> b.byRawField( fieldPath ).asc().onMissingValue()
						.use( fieldModel.between2And3Value ) );
				assertThat( query )
						.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, EMPTY, DOCUMENT_3 );
				query = simpleQuery( b -> b.byRawField( fieldPath ).asc().onMissingValue()
						.use( fieldModel.after3Value ) );
				assertThat( query )
						.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, EMPTY );
			}
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
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		for ( ByTypeFieldModel<?> fieldModel : indexMapping.supportedNonSortableFieldModels ) {
			String fieldPath = fieldModel.relativeFieldName;

			SubTest.expectException( () -> {
					searchTarget.sort().byField( fieldPath );
			} ).assertThrown()
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Sorting is not enabled for field" )
					.hasMessageContaining( fieldPath );
		}
	}

	@Test
	public void error_unknownField() {
		Assume.assumeTrue( "Errors on attempt to sort on unknown fields are not supported yet", false );
		// TODO throw an error on attempts to sort on unknown fields

		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		String absoluteFieldPath = "unknownField";

		thrown.expect( SearchException.class );
		thrown.expectMessage( "Unknown field" );
		thrown.expectMessage( absoluteFieldPath );
		thrown.expectMessage( INDEX_NAME );

		searchTarget.query()
				.asReference()
				.predicate( f -> f.matchAll() )
				.sort( c -> c.byField( absoluteFieldPath ) )
				.build();
	}

	@Test
	public void error_objectField_nested() {
		Assume.assumeTrue( "Errors on attempt to sort on object fields are not supported yet", false );
		// TODO throw an error on attempts to sort on object fields

		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		String absoluteFieldPath = indexMapping.nestedObject.relativeFieldName;

		thrown.expect( SearchException.class );
		thrown.expectMessage( "Unknown field" );
		thrown.expectMessage( absoluteFieldPath );
		thrown.expectMessage( INDEX_NAME );

		searchTarget.query()
				.asReference()
				.predicate( f -> f.matchAll() )
				.sort( c -> c.byField( absoluteFieldPath ) )
				.build();
	}

	@Test
	public void error_objectField_flattened() {
		Assume.assumeTrue( "Errors on attempt to sort on object fields are not supported yet", false );
		// TODO throw an error on attempts to sort on object fields

		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		String absoluteFieldPath = indexMapping.flattenedObject.relativeFieldName;

		thrown.expect( SearchException.class );
		thrown.expectMessage( "Unknown field" );
		thrown.expectMessage( absoluteFieldPath );
		thrown.expectMessage( INDEX_NAME );

		searchTarget.query()
				.asReference()
				.predicate( f -> f.matchAll() )
				.sort( c -> c.byField( absoluteFieldPath ) )
				.build();
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

			if (
					( TckConfiguration.get().getBackendFeatures().stringTypeOnMissingValueUse() || !String.class.equals( fieldModel.type ) )
					&& ( TckConfiguration.get().getBackendFeatures().localDateTypeOnMissingValueUse() || !LocalDate.class.equals( fieldModel.type ) )
			) {
				SubTest.expectException(
						"byField() sort with invalid parameter type for onMissingValue().use() on field " + absoluteFieldPath,
						() -> searchTarget.sort().byField( absoluteFieldPath ).onMissingValue()
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
	}

	@Test
	public void multiIndex_withCompatibleIndexManager_usingField() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget( compatibleIndexManager );

		for ( ByTypeFieldModel<?> fieldModel : indexMapping.supportedFieldModels ) {
			SearchQuery<DocumentReference> query;
			String fieldPath = fieldModel.relativeFieldName;

			// Eventually we will remove any restriction here. See the issues: HSEARCH-3254 HSEARCH-3255 and HSEARCH-3387.
			if (
					( TckConfiguration.get().getBackendFeatures().stringTypeOnMissingValueUse() || !String.class.equals( fieldModel.type ) )
							&& ( TckConfiguration.get().getBackendFeatures().localDateTypeOnMissingValueUse() || !isJavaTimeType( fieldModel.type ) )
			) {
				query = simpleQuery( b -> b.byField( fieldPath ).asc().onMissingValue()
						.use( fieldModel.before1Value ), searchTarget );

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
	}

	@Test
	public void multiIndex_withRawFieldCompatibleIndexManager_usingField() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget( rawFieldCompatibleIndexManager );

		for ( ByTypeFieldModel<?> fieldModel : indexMapping.supportedFieldModels ) {
			String fieldPath = fieldModel.relativeFieldName;

			SubTest.expectException(
					() -> {
						simpleQuery( b -> b.byField( fieldPath ).asc().onMissingValue()
								.use( new ValueWrapper<>( fieldModel.before1Value ) ), searchTarget );
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
	public void multiIndex_withRawFieldCompatibleIndexManager_usingRawField() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget( rawFieldCompatibleIndexManager );

		for ( ByTypeFieldModel<?> fieldModel : indexMapping.supportedFieldModels ) {
			SearchQuery<DocumentReference> query;
			String fieldPath = fieldModel.relativeFieldName;

			// Eventually we will remove any restriction here. See the issues: HSEARCH-3254 HSEARCH-3255 and HSEARCH-3387.
			if (
					( TckConfiguration.get().getBackendFeatures().stringTypeOnMissingValueUse() || !String.class.equals( fieldModel.type ) )
							&& ( TckConfiguration.get().getBackendFeatures().localDateTypeOnMissingValueUse() || !isJavaTimeType( fieldModel.type ) )
			) {
				query = simpleQuery( b -> b.byRawField( fieldPath ).asc().onMissingValue()
						.use( fieldModel.before1Value ), searchTarget );

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
	}

	@Test
	public void multiIndex_withNoCompatibleIndexManager_usingField() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget( incompatibleIndexManager );

		for ( ByTypeFieldModel<?> fieldModel : indexMapping.supportedFieldModels ) {
			String fieldPath = fieldModel.relativeFieldName;

			SubTest.expectException(
					() -> {
						simpleQuery( b -> b.byField( fieldPath ).asc().onMissingValue()
								.use( new ValueWrapper<>( fieldModel.before1Value ) ), searchTarget );
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
	public void multiIndex_withNoCompatibleIndexManager_usingRawField() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget( incompatibleIndexManager );

		for ( ByTypeFieldModel<?> fieldModel : indexMapping.supportedFieldModels ) {
			String fieldPath = fieldModel.relativeFieldName;

			SubTest.expectException(
					() -> {
						simpleQuery( b -> b.byRawField( fieldPath ).asc().onMissingValue()
								.use( new ValueWrapper<>( fieldModel.before1Value ) ), searchTarget );
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

	private boolean isJavaTimeType(Class<?> type) {
		final Class<?>[] javaTimeTypes = { LocalDate.class, LocalDateTime.class, LocalTime.class, ZonedDateTime.class, Year.class, YearMonth.class, MonthDay.class,
				OffsetDateTime.class, OffsetTime.class, ZoneOffset.class, ZoneId.class, Period.class, Duration.class, Instant.class
		};
		return Arrays.asList( javaTimeTypes ).contains( type );
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
			DocumentElement flattenedObject = indexMapping.flattenedObject.self.add( document );
			indexMapping.flattenedObject.supportedFieldModels.forEach( f -> f.document2Value.write( flattenedObject ) );
			indexMapping.flattenedObject.unsupportedFieldModels.forEach( f -> f.document2Value.write( flattenedObject ) );

			// Note: this object must be single-valued for these tests
			DocumentElement nestedObject = indexMapping.nestedObject.self.add( document );
			indexMapping.nestedObject.supportedFieldModels.forEach( f -> f.document2Value.write( nestedObject ) );
			indexMapping.nestedObject.unsupportedFieldModels.forEach( f -> f.document2Value.write( nestedObject ) );
		} );
		workPlan.add( referenceProvider( DOCUMENT_1 ), document -> {
			indexMapping.supportedFieldModels.forEach( f -> f.document1Value.write( document ) );
			indexMapping.supportedFieldWithDslConverterModels.forEach( f -> f.document1Value.write( document ) );
			indexMapping.unsupportedFieldModels.forEach( f -> f.document1Value.write( document ) );

			indexMapping.identicalForFirstTwo.document1Value.write( document );
			indexMapping.identicalForLastTwo.document1Value.write( document );

			// Note: this object must be single-valued for these tests
			DocumentElement flattenedObject = indexMapping.flattenedObject.self.add( document );
			indexMapping.flattenedObject.supportedFieldModels.forEach( f -> f.document1Value.write( flattenedObject ) );
			indexMapping.flattenedObject.unsupportedFieldModels.forEach( f -> f.document1Value.write( flattenedObject ) );

			// Note: this object must be single-valued for these tests
			DocumentElement nestedObject = indexMapping.nestedObject.self.add( document );
			indexMapping.nestedObject.supportedFieldModels.forEach( f -> f.document1Value.write( nestedObject ) );
			indexMapping.nestedObject.unsupportedFieldModels.forEach( f -> f.document1Value.write( nestedObject ) );
		} );
		workPlan.add( referenceProvider( DOCUMENT_3 ), document -> {
			indexMapping.supportedFieldModels.forEach( f -> f.document3Value.write( document ) );
			indexMapping.supportedFieldWithDslConverterModels.forEach( f -> f.document3Value.write( document ) );
			indexMapping.unsupportedFieldModels.forEach( f -> f.document3Value.write( document ) );

			indexMapping.identicalForFirstTwo.document3Value.write( document );
			indexMapping.identicalForLastTwo.document3Value.write( document );

			// Note: this object must be single-valued for these tests
			DocumentElement flattenedObject = indexMapping.flattenedObject.self.add( document );
			indexMapping.flattenedObject.supportedFieldModels.forEach( f -> f.document3Value.write( flattenedObject ) );
			indexMapping.flattenedObject.unsupportedFieldModels.forEach( f -> f.document3Value.write( flattenedObject ) );

			// Note: this object must be single-valued for these tests
			DocumentElement nestedObject = indexMapping.nestedObject.self.add( document );
			indexMapping.nestedObject.supportedFieldModels.forEach( f -> f.document3Value.write( nestedObject ) );
			indexMapping.nestedObject.unsupportedFieldModels.forEach( f -> f.document3Value.write( nestedObject ) );
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

		// Check that all documents are searchable
		SearchQuery<DocumentReference> query = indexManager.createSearchTarget().query()
				.asReference()
				.predicate( f -> f.matchAll() )
				.build();
		assertThat( query ).hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, EMPTY );
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

		final MainFieldModel identicalForFirstTwo;
		final MainFieldModel identicalForLastTwo;

		final ObjectMapping flattenedObject;
		final ObjectMapping nestedObject;

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
		}
	}

	private static class ObjectMapping {
		final String relativeFieldName;
		final IndexObjectFieldAccessor self;
		final List<ByTypeFieldModel<?>> supportedFieldModels = new ArrayList<>();
		final List<ByTypeFieldModel<?>> unsupportedFieldModels = new ArrayList<>();

		ObjectMapping(IndexSchemaElement parent, String relativeFieldName, ObjectFieldStorage storage) {
			this.relativeFieldName = relativeFieldName;
			IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, storage );
			self = objectField.createAccessor();
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
		static StandardFieldMapper<String, MainFieldModel> mapper(
				String document1Value, String document2Value, String document3Value) {
			return StandardFieldMapper.of(
					f -> f.asString(),
					(accessor, name) -> new MainFieldModel(
							accessor, name, document1Value, document2Value, document3Value
					)
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
			FieldSortExpectations<F> expectations = typeDescriptor.getFieldSortExpectations().get();
			return StandardFieldMapper.of(
					typeDescriptor::configure,
					c -> c.sortable( Sortable.YES ),
					(accessor, name) -> new ByTypeFieldModel<>( accessor, name, typeDescriptor.getJavaType(), expectations )
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

		private ByTypeFieldModel(IndexFieldAccessor<F> accessor, String relativeFieldName,
				Class<F> type, FieldSortExpectations<F> expectations) {
			this.relativeFieldName = relativeFieldName;
			this.type = type;
			this.document1Value = new ValueModel<>( accessor, expectations.getDocument1Value() );
			this.document2Value = new ValueModel<>( accessor, expectations.getDocument2Value() );
			this.document3Value = new ValueModel<>( accessor, expectations.getDocument3Value() );
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
					(accessor, name) -> new IncompatibleFieldModel( name )
			);
		}

		final String relativeFieldName;

		private IncompatibleFieldModel(String relativeFieldName) {
			this.relativeFieldName = relativeFieldName;
		}
	}
}
