/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.sort;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.DocumentReferencesSearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.IndexObjectFieldAccessor;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldContext;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.document.model.dsl.Sortable;
import org.hibernate.search.engine.backend.document.model.dsl.StandardIndexSchemaFieldTypedContext;
import org.hibernate.search.engine.backend.index.spi.IndexSearchTarget;
import org.hibernate.search.engine.backend.index.spi.IndexWorkPlan;
import org.hibernate.search.engine.common.spi.SessionContext;
import org.hibernate.search.engine.logging.spi.EventContexts;
import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexManager;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.backend.tck.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.util.InvalidType;
import org.hibernate.search.integrationtest.backend.tck.util.StandardFieldMapper;
import org.hibernate.search.integrationtest.backend.tck.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.util.ValueWrapper;
import org.hibernate.search.integrationtest.backend.tck.util.rule.SearchSetupHelper;
import org.hibernate.search.util.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.assertion.DocumentReferencesSearchResultAssert;
import org.hibernate.search.util.impl.integrationtest.common.stub.StubSessionContext;
import org.hibernate.search.util.impl.test.SubTest;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class SearchSortByFieldIT {

	private static final String INDEX_NAME = "IndexName";

	private static final String DOCUMENT_1 = "1";
	private static final String DOCUMENT_2 = "2";
	private static final String DOCUMENT_3 = "3";
	private static final String EMPTY = "empty";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private IndexMapping indexMapping;
	private MappedIndexManager<?> indexManager;
	private SessionContext sessionContext = new StubSessionContext();

	@Before
	public void setup() {
		setupHelper.withDefaultConfiguration()
				.withIndex(
						"MappedType", INDEX_NAME,
						ctx -> this.indexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();

		initData();
	}

	private SearchQuery<DocumentReference> simpleQuery(Consumer<? super SearchSortContainerContext> sortContributor) {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();
		return searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.matchAll().toPredicate() )
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
			DocumentReferencesSearchResultAssert.assertThat( query )
					.hasReferencesHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, EMPTY );

			// Explicit order with onMissingValue().sortLast()
			query = simpleQuery( b -> b.byField( fieldPath ).asc().onMissingValue().sortLast() );
			DocumentReferencesSearchResultAssert.assertThat( query )
					.hasReferencesHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, EMPTY );
			query = simpleQuery( b -> b.byField( fieldPath ).desc().onMissingValue().sortLast() );
			DocumentReferencesSearchResultAssert.assertThat( query )
					.hasReferencesHitsExactOrder( INDEX_NAME, DOCUMENT_3, DOCUMENT_2, DOCUMENT_1, EMPTY );

			// Explicit order with onMissingValue().sortFirst()
			query = simpleQuery( b -> b.byField( fieldPath ).asc().onMissingValue().sortFirst() );
			DocumentReferencesSearchResultAssert.assertThat( query )
					.hasReferencesHitsExactOrder( INDEX_NAME, EMPTY, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );
			query = simpleQuery( b -> b.byField( fieldPath ).desc().onMissingValue().sortFirst() );
			DocumentReferencesSearchResultAssert.assertThat( query )
					.hasReferencesHitsExactOrder( INDEX_NAME, EMPTY, DOCUMENT_3, DOCUMENT_2, DOCUMENT_1 );

			// Explicit order with onMissingValue().use( ... )
			if (
					( TckConfiguration.get().getBackendFeatures().stringTypeOnMissingValueUse() || !String.class.equals( fieldModel.type ) )
					&& ( TckConfiguration.get().getBackendFeatures().localDateTypeOnMissingValueUse() || !LocalDate.class.equals( fieldModel.type ) )
			) {
				query = simpleQuery( b -> b.byField( fieldPath ).asc().onMissingValue().use( fieldModel.before1Value ) );
				DocumentReferencesSearchResultAssert.assertThat( query )
						.hasReferencesHitsExactOrder( INDEX_NAME, EMPTY, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );
				query = simpleQuery( b -> b.byField( fieldPath ).asc().onMissingValue().use( fieldModel.between1And2Value ) );
				DocumentReferencesSearchResultAssert.assertThat( query )
						.hasReferencesHitsExactOrder( INDEX_NAME, DOCUMENT_1, EMPTY, DOCUMENT_2, DOCUMENT_3 );
				query = simpleQuery( b -> b.byField( fieldPath ).asc().onMissingValue().use( fieldModel.between2And3Value ) );
				DocumentReferencesSearchResultAssert.assertThat( query )
						.hasReferencesHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, EMPTY, DOCUMENT_3 );
				query = simpleQuery( b -> b.byField( fieldPath ).asc().onMissingValue().use( fieldModel.after3Value ) );
				DocumentReferencesSearchResultAssert.assertThat( query )
						.hasReferencesHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, EMPTY );
			}
		}
	}

	@Test
	public void byField_withDslConverters() {
		for ( ByTypeFieldModel<?> fieldModel : indexMapping.supportedFieldWithDslConverterModels ) {
			SearchQuery<DocumentReference> query;
			String fieldPath = fieldModel.relativeFieldName;

			if (
					( TckConfiguration.get().getBackendFeatures().stringTypeOnMissingValueUse() || !String.class.equals( fieldModel.type ) )
					&& ( TckConfiguration.get().getBackendFeatures().localDateTypeOnMissingValueUse() || !LocalDate.class.equals( fieldModel.type ) )
			) {
				query = simpleQuery( b -> b.byField( fieldPath ).asc().onMissingValue()
						.use( new ValueWrapper<>( fieldModel.before1Value ) ) );
				DocumentReferencesSearchResultAssert.assertThat( query )
						.hasReferencesHitsExactOrder( INDEX_NAME, EMPTY, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );
				query = simpleQuery( b -> b.byField( fieldPath ).asc().onMissingValue()
						.use( new ValueWrapper<>( fieldModel.between1And2Value ) ) );
				DocumentReferencesSearchResultAssert.assertThat( query )
						.hasReferencesHitsExactOrder( INDEX_NAME, DOCUMENT_1, EMPTY, DOCUMENT_2, DOCUMENT_3 );
				query = simpleQuery( b -> b.byField( fieldPath ).asc().onMissingValue()
						.use( new ValueWrapper<>( fieldModel.between2And3Value ) ) );
				DocumentReferencesSearchResultAssert.assertThat( query )
						.hasReferencesHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, EMPTY, DOCUMENT_3 );
				query = simpleQuery( b -> b.byField( fieldPath ).asc().onMissingValue()
						.use( new ValueWrapper<>( fieldModel.after3Value ) ) );
				DocumentReferencesSearchResultAssert.assertThat( query )
						.hasReferencesHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, EMPTY );
			}
		}
	}

	@Test
	public void byField_inFlattenedObject() {
		for ( ByTypeFieldModel<?> fieldModel : indexMapping.flattenedObject.supportedFieldModels ) {
			SearchQuery<DocumentReference> query;
			String fieldPath = indexMapping.flattenedObject.relativeFieldName + "." + fieldModel.relativeFieldName;

			query = simpleQuery( b -> b.byField( fieldPath ).onMissingValue().sortLast() );
			DocumentReferencesSearchResultAssert.assertThat( query )
					.hasReferencesHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, EMPTY );

			query = simpleQuery( b -> b.byField( fieldPath ).asc().onMissingValue().sortLast() );
			DocumentReferencesSearchResultAssert.assertThat( query )
					.hasReferencesHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, EMPTY );

			query = simpleQuery( b -> b.byField( fieldPath ).desc().onMissingValue().sortLast() );
			DocumentReferencesSearchResultAssert.assertThat( query )
					.hasReferencesHitsExactOrder( INDEX_NAME, DOCUMENT_3, DOCUMENT_2, DOCUMENT_1, EMPTY );

			query = simpleQuery( b -> b.byField( fieldPath ).asc().onMissingValue().sortFirst() );
			DocumentReferencesSearchResultAssert.assertThat( query )
					.hasReferencesHitsExactOrder( INDEX_NAME, EMPTY, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );

			query = simpleQuery( b -> b.byField( fieldPath ).desc().onMissingValue().sortFirst() );
			DocumentReferencesSearchResultAssert.assertThat( query )
					.hasReferencesHitsExactOrder( INDEX_NAME, EMPTY, DOCUMENT_3, DOCUMENT_2, DOCUMENT_1 );
		}
	}

	@Test
	public void multipleFields() {
		SearchQuery<DocumentReference> query;

		query = simpleQuery( b -> b
				.byField( indexMapping.identicalForFirstTwo.relativeFieldName ).asc().onMissingValue().sortFirst()
				.then().byField( indexMapping.identicalForLastTwo.relativeFieldName ).asc()
		);
		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsExactOrder( INDEX_NAME, EMPTY, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );

		query = simpleQuery( b -> b
				.byField( indexMapping.identicalForFirstTwo.relativeFieldName ).desc().onMissingValue().sortFirst()
				.then().byField( indexMapping.identicalForLastTwo.relativeFieldName ).desc()
		);
		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsExactOrder( INDEX_NAME, EMPTY, DOCUMENT_3, DOCUMENT_2, DOCUMENT_1 );

		query = simpleQuery( b -> b
				.byField( indexMapping.identicalForFirstTwo.relativeFieldName ).asc().onMissingValue().sortFirst()
				.then().byField( indexMapping.identicalForLastTwo.relativeFieldName ).desc()
		);
		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsExactOrder( INDEX_NAME, EMPTY, DOCUMENT_2, DOCUMENT_1, DOCUMENT_3 );

		query = simpleQuery( b -> b
				.byField( indexMapping.identicalForFirstTwo.relativeFieldName ).desc().onMissingValue().sortFirst()
				.then().byField( indexMapping.identicalForLastTwo.relativeFieldName ).asc()
		);
		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsExactOrder( INDEX_NAME, EMPTY, DOCUMENT_3, DOCUMENT_1, DOCUMENT_2 );
	}

	@Test
	public void error_unsortable() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		for ( ByTypeFieldModel<?> fieldModel : indexMapping.unsortableSupportedFieldModels ) {
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

		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		String absoluteFieldPath = "unknownField";

		thrown.expect( SearchException.class );
		thrown.expectMessage( "Unknown field" );
		thrown.expectMessage( absoluteFieldPath );
		thrown.expectMessage( INDEX_NAME );

		searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.matchAll().toPredicate() )
				.sort( c -> c.byField( absoluteFieldPath ) )
				.build();
	}

	@Test
	public void error_objectField_nested() {
		Assume.assumeTrue( "Errors on attempt to sort on object fields are not supported yet", false );
		// TODO throw an error on attempts to sort on object fields

		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		String absoluteFieldPath = indexMapping.nestedObject.relativeFieldName;

		thrown.expect( SearchException.class );
		thrown.expectMessage( "Unknown field" );
		thrown.expectMessage( absoluteFieldPath );
		thrown.expectMessage( INDEX_NAME );

		searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.matchAll().toPredicate() )
				.sort( c -> c.byField( absoluteFieldPath ) )
				.build();
	}

	@Test
	public void error_objectField_flattened() {
		Assume.assumeTrue( "Errors on attempt to sort on object fields are not supported yet", false );
		// TODO throw an error on attempts to sort on object fields

		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		String absoluteFieldPath = indexMapping.flattenedObject.relativeFieldName;

		thrown.expect( SearchException.class );
		thrown.expectMessage( "Unknown field" );
		thrown.expectMessage( absoluteFieldPath );
		thrown.expectMessage( INDEX_NAME );

		searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.matchAll().toPredicate() )
				.sort( c -> c.byField( absoluteFieldPath ) )
				.build();
	}

	@Test
	public void error_invalidType() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

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

	private void initData() {
		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan( sessionContext );
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

		// Check that all documents are searchable
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();
		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.matchAll().toPredicate() )
				.build();
		assertThat( query ).hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, EMPTY );
	}

	private static class IndexMapping {
		final List<ByTypeFieldModel<?>> supportedFieldModels;
		final List<ByTypeFieldModel<?>> supportedFieldWithDslConverterModels;
		final List<ByTypeFieldModel<?>> unsupportedFieldModels;
		final List<ByTypeFieldModel<?>> unsortableSupportedFieldModels;

		final MainFieldModel identicalForFirstTwo;
		final MainFieldModel identicalForLastTwo;

		final ObjectMapping flattenedObject;
		final ObjectMapping nestedObject;

		IndexMapping(IndexSchemaElement root) {
			supportedFieldModels = mapSupportedFields( root, "", ignored -> { } );
			supportedFieldWithDslConverterModels = mapSupportedFields(
					root, "converted_", c -> c.dslConverter( ValueWrapper.toIndexFieldConverter() )
			);
			unsupportedFieldModels = mapUnsupportedFields( root );
			unsortableSupportedFieldModels = mapSupportedFields( root, "unsortable_", c -> c.sortable( Sortable.NO ) );

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
		final List<ByTypeFieldModel<?>> supportedFieldModels;
		final List<ByTypeFieldModel<?>> unsupportedFieldModels;

		ObjectMapping(IndexSchemaElement parent, String relativeFieldName, ObjectFieldStorage storage) {
			this.relativeFieldName = relativeFieldName;
			IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, storage );
			self = objectField.createAccessor();
			supportedFieldModels = mapSupportedFields( objectField, "", ignored -> { } );
			unsupportedFieldModels = mapUnsupportedFields( objectField );
		}
	}

	private static List<ByTypeFieldModel<?>> mapSupportedFields(IndexSchemaElement root, String prefix,
			Consumer<StandardIndexSchemaFieldTypedContext<?, ?>> additionalConfiguration) {
		return Arrays.asList(
				ByTypeFieldModel
						// Mix capitalized and non-capitalized text on purpose
						.mapper(
								String.class,
								c -> c.asString().normalizer( DefaultAnalysisDefinitions.NORMALIZER_LOWERCASE.name ),
								"Aaron", "george", "Zach",
								// TODO Fix HSEARCH-3387, then mix capitalization here
								"aaaaa", "bastian", "marco", "zzzz"
						)
						.map( root, prefix + "normalizedString", additionalConfiguration ),
				ByTypeFieldModel.mapper( String.class, "aaron", "george", "zach",
						"aaaa", "bastian", "marc", "zzzz"
				)
						.map( root, prefix + "nonAnalyzedString", additionalConfiguration ),
				ByTypeFieldModel.mapper( Integer.class, 1, 3, 5,
						Integer.MIN_VALUE, 2, 4, Integer.MAX_VALUE
				)
						.map( root, prefix + "integer", additionalConfiguration ),
				ByTypeFieldModel.mapper(
						LocalDate.class,
						LocalDate.of( 2018, 2, 1 ),
						LocalDate.of( 2018, 3, 1 ),
						LocalDate.of( 2018, 4, 1 ),
						LocalDate.of( 2018, 1, 1 ),
						LocalDate.of( 2018, 2, 15 ),
						LocalDate.of( 2018, 3, 15 ),
						LocalDate.of( 2018, 5, 1 )
				)
						.map( root, prefix + "localDate", additionalConfiguration )
		);
	}

	private static List<ByTypeFieldModel<?>> mapUnsupportedFields(IndexSchemaElement root) {
		return Arrays.asList(
				ByTypeFieldModel.mapper(
						GeoPoint.class,
						GeoPoint.of( 40, 70 ),
						GeoPoint.of( 40, 75 ),
						GeoPoint.of( 40, 80 ),
						GeoPoint.of( 0, 0 ),
						GeoPoint.of( 40, 72 ),
						GeoPoint.of( 40, 77 ),
						GeoPoint.of( 89, 89 )
				)
						.map( root, "geoPoint" )
		);
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
			return (parent, name, configuration) -> {
				StandardIndexSchemaFieldTypedContext<?, String> context = parent.field( name ).asString();
				configuration.accept( context );
				IndexFieldAccessor<String> accessor = context.createAccessor();
				return new MainFieldModel( accessor, name, document1Value, document2Value, document3Value );
			};
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
		static <F> StandardFieldMapper<F, ByTypeFieldModel<F>> mapper(Class<F> type,
				F document1Value, F document2Value, F document3Value,
				F before1Value, F between1And2Value, F between2And3Value, F after3Value) {
			return mapper(
					type,
					c -> (StandardIndexSchemaFieldTypedContext<?, F>) c.as( type ),
					document1Value, document2Value, document3Value,
					before1Value, between1And2Value, between2And3Value, after3Value
			);
		}

		static <F> StandardFieldMapper<F, ByTypeFieldModel<F>> mapper(
				Class<F> type,
				Function<IndexSchemaFieldContext, StandardIndexSchemaFieldTypedContext<?, F>> configuration,
				F document1Value, F document2Value, F document3Value,
				F before1Value, F between1And2Value, F between2And3Value, F after3Value) {
			return (parent, name, additionalConfiguration) -> {
				IndexSchemaFieldContext untypedContext = parent.field( name );
				StandardIndexSchemaFieldTypedContext<?, F> context = configuration.apply( untypedContext );
				context.sortable( Sortable.YES );
				additionalConfiguration.accept( context );
				IndexFieldAccessor<F> accessor = context.createAccessor();
				return new ByTypeFieldModel<>(
						accessor, name, type,
						document1Value, document2Value, document3Value,
						before1Value, between1And2Value, between2And3Value, after3Value
				);
			};
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

		private ByTypeFieldModel(IndexFieldAccessor<F> accessor, String relativeFieldName, Class<F> type,
				F document1Value, F document2Value, F document3Value,
				F before1Value, F between1And2Value, F between2And3Value, F after3Value) {
			this.relativeFieldName = relativeFieldName;
			this.type = type;
			this.document1Value = new ValueModel<>( accessor, document1Value );
			this.document2Value = new ValueModel<>( accessor, document2Value );
			this.document3Value = new ValueModel<>( accessor, document3Value );
			this.before1Value = before1Value;
			this.between1And2Value = between1And2Value;
			this.between2And3Value = between2And3Value;
			this.after3Value = after3Value;
		}
	}
}
