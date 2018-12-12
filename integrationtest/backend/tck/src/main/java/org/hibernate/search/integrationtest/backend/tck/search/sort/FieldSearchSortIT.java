/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.sort;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.IndexObjectFieldAccessor;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldContext;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.document.model.dsl.Sortable;
import org.hibernate.search.engine.backend.document.model.dsl.StandardIndexSchemaFieldTypedContext;
import org.hibernate.search.engine.backend.index.spi.IndexWorkPlan;
import org.hibernate.search.integrationtest.backend.tck.test.types.expectations.FieldSortExpectations;
import org.hibernate.search.integrationtest.backend.tck.test.types.FieldTypeDescriptor;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingSearchTarget;
import org.hibernate.search.engine.logging.spi.EventContexts;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;
import org.hibernate.search.integrationtest.backend.tck.util.InvalidType;
import org.hibernate.search.integrationtest.backend.tck.util.StandardFieldMapper;
import org.hibernate.search.integrationtest.backend.tck.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.util.ValueWrapper;
import org.hibernate.search.integrationtest.backend.tck.util.rule.SearchSetupHelper;
import org.hibernate.search.util.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.test.SubTest;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class FieldSearchSortIT {

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
	private StubMappingIndexManager indexManager;

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
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();
		return searchTarget.query()
				.asReference()
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
			if (
					( TckConfiguration.get().getBackendFeatures().stringTypeOnMissingValueUse() || !String.class.equals( fieldModel.type ) )
					&& ( TckConfiguration.get().getBackendFeatures().localDateTypeOnMissingValueUse() || !LocalDate.class.equals( fieldModel.type ) )
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

			if (
					( TckConfiguration.get().getBackendFeatures().stringTypeOnMissingValueUse() || !String.class.equals( fieldModel.type ) )
					&& ( TckConfiguration.get().getBackendFeatures().localDateTypeOnMissingValueUse() || !LocalDate.class.equals( fieldModel.type ) )
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
				.predicate( f -> f.matchAll().toPredicate() )
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
				.predicate( f -> f.matchAll().toPredicate() )
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
				.predicate( f -> f.matchAll().toPredicate() )
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

		// Check that all documents are searchable
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();
		SearchQuery<DocumentReference> query = searchTarget.query()
				.asReference()
				.predicate( f -> f.matchAll().toPredicate() )
				.build();
		assertThat( query ).hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, EMPTY );
	}

	private static class IndexMapping {
		final List<ByTypeFieldModel<?>> supportedFieldModels;
		final List<ByTypeFieldModel<?>> supportedFieldWithDslConverterModels;
		final List<ByTypeFieldModel<?>> unsupportedFieldModels;
		final List<ByTypeFieldModel<?>> supportedNonSortableFieldModels;

		final MainFieldModel identicalForFirstTwo;
		final MainFieldModel identicalForLastTwo;

		final ObjectMapping flattenedObject;
		final ObjectMapping nestedObject;

		IndexMapping(IndexSchemaElement root) {
			supportedFieldModels = mapByTypeFields(
					root, "supported_", ignored -> { },
					FieldSortExpectations::isFieldSortSupported
			);
			supportedFieldWithDslConverterModels = mapByTypeFields(
					root, "supported_converted_", c -> c.dslConverter( ValueWrapper.toIndexFieldConverter() ),
					FieldSortExpectations::isFieldSortSupported
			);
			unsupportedFieldModels = mapByTypeFields(
					root, "unsupported_", ignored -> { },
					e -> !e.isFieldSortSupported()
			);
			supportedNonSortableFieldModels = mapByTypeFields(
					root, "supported_nonSortable_", c -> c.sortable( Sortable.NO ),
					FieldSortExpectations::isFieldSortSupported
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
		final List<ByTypeFieldModel<?>> supportedFieldModels;
		final List<ByTypeFieldModel<?>> unsupportedFieldModels;

		ObjectMapping(IndexSchemaElement parent, String relativeFieldName, ObjectFieldStorage storage) {
			this.relativeFieldName = relativeFieldName;
			IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, storage );
			self = objectField.createAccessor();
			supportedFieldModels = mapByTypeFields(
					objectField, "supported_", ignored -> { },
					FieldSortExpectations::isFieldSortSupported
			);
			unsupportedFieldModels = mapByTypeFields(
					objectField, "unsupported_", ignored -> { },
					e -> !e.isFieldSortSupported()
			);
		}
	}

	private static List<ByTypeFieldModel<?>> mapByTypeFields(IndexSchemaElement root, String prefix,
			Consumer<StandardIndexSchemaFieldTypedContext<?, ?>> additionalConfiguration,
			Predicate<FieldSortExpectations<?>> predicate) {
		return FieldTypeDescriptor.getAll().stream()
				.filter(
						typeDescriptor -> typeDescriptor.getFieldSortExpectations().isPresent()
								&& predicate.test( typeDescriptor.getFieldSortExpectations().get() )
				)
				.map( typeDescriptor -> mapByTypeField( root, prefix, typeDescriptor, additionalConfiguration ) )
				.collect( Collectors.toList() );
	}

	private static <F> ByTypeFieldModel<F> mapByTypeField(IndexSchemaElement parent, String prefix,
			FieldTypeDescriptor<F> typeDescriptor,
			Consumer<StandardIndexSchemaFieldTypedContext<?, ?>> additionalConfiguration) {
		String name = prefix + typeDescriptor.getUniqueName();
		FieldSortExpectations<F> expectations = typeDescriptor.getFieldSortExpectations().get(); // Safe, see caller
		return new ByTypeFieldModel<>( parent, name, typeDescriptor, expectations, additionalConfiguration );
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
		final String relativeFieldName;
		final Class<F> type;

		final ValueModel<F> document1Value;
		final ValueModel<F> document2Value;
		final ValueModel<F> document3Value;

		final F before1Value;
		final F between1And2Value;
		final F between2And3Value;
		final F after3Value;

		private ByTypeFieldModel(IndexSchemaElement parent, String relativeFieldName,
				FieldTypeDescriptor<F> typeDescriptor, FieldSortExpectations<F> expectations,
				Consumer<StandardIndexSchemaFieldTypedContext<?, ?>> additionalConfiguration) {
			IndexSchemaFieldContext untypedContext = parent.field( relativeFieldName );
			StandardIndexSchemaFieldTypedContext<?, F> context = typeDescriptor.configure( untypedContext );
			context.sortable( Sortable.YES );
			additionalConfiguration.accept( context );
			IndexFieldAccessor<F> accessor = context.createAccessor();
			this.relativeFieldName = relativeFieldName;
			this.type = typeDescriptor.getJavaType();
			this.document1Value = new ValueModel<>( accessor, expectations.getDocument1Value() );
			this.document2Value = new ValueModel<>( accessor, expectations.getDocument2Value() );
			this.document3Value = new ValueModel<>( accessor, expectations.getDocument3Value() );
			this.before1Value = expectations.getBeforeDocument1Value();
			this.between1And2Value = expectations.getBetweenDocument1And2Value();
			this.between2And3Value = expectations.getBetweenDocument2And3Value();
			this.after3Value = expectations.getAfterDocument3Value();
		}
	}
}
