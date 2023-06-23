/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.projection;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.IndexableGeoPointWithDistanceFromCenterValues.CENTER_POINT_1;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;
import static org.junit.Assume.assumeFalse;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.types.converter.FromDocumentValueConverter;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentValueConvertContext;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.GeoPointFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.IndexableGeoPointWithDistanceFromCenterValues;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModel;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.ValueWrapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.assertion.TestComparators;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * Tests behavior related to type checking and type conversion of
 * projections on the distance between a field value and a given point.
 */
public class DistanceProjectionTypeCheckingAndConversionIT {

	private static final GeoPointFieldTypeDescriptor fieldType = GeoPointFieldTypeDescriptor.INSTANCE;

	private static final String DOCUMENT_1 = "1";
	private static final String DOCUMENT_2 = "2";
	private static final String DOCUMENT_3 = "3";
	private static final String EMPTY = "empty";

	private static final String COMPATIBLE_INDEX_DOCUMENT_1 = "compatible_1";
	private static final String RAW_FIELD_COMPATIBLE_INDEX_DOCUMENT_1 = "raw_field_compatible_1";
	private static final String MISSING_FIELD_INDEX_DOCUMENT_1 = "missing_field_1";

	@ClassRule
	public static final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private static final SimpleMappedIndex<IndexBinding> mainIndex =
			SimpleMappedIndex.of( IndexBinding::new ).name( "main" );
	private static final SimpleMappedIndex<CompatibleIndexBinding> compatibleIndex =
			SimpleMappedIndex.of( CompatibleIndexBinding::new ).name( "compatible" );
	private static final SimpleMappedIndex<RawFieldCompatibleIndexBinding> rawFieldCompatibleIndex =
			SimpleMappedIndex.of( RawFieldCompatibleIndexBinding::new ).name( "rawFieldCompatible" );
	private static final SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex =
			SimpleMappedIndex.of( MissingFieldIndexBinding::new ).name( "missingField" );
	private static final SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex =
			SimpleMappedIndex.of( IncompatibleIndexBinding::new ).name( "incompatible" );

	@BeforeClass
	public static void setup() {
		setupHelper.start()
				.withIndexes( mainIndex, compatibleIndex, rawFieldCompatibleIndex, missingFieldIndex, incompatibleIndex )
				.setup();

		initData();
	}

	@Test
	public void nonProjectable() {
		StubMappingScope scope = mainIndex.createScope();

		String fieldPath = getNonProjectableFieldPath();

		assertThatThrownBy( () -> scope.projection()
				.distance( fieldPath, CENTER_POINT_1 ).toProjection() )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Cannot use 'projection:distance' on field '" + fieldPath + "'",
						"Make sure the field is marked as searchable/sortable/projectable/aggregable/highlightable (whichever is relevant)"
				);
	}

	@Test
	public void projectableDefault() {
		assumeFalse(
				"Skipping this test as the backend makes fields projectable by default.",
				TckConfiguration.get().getBackendFeatures().fieldsProjectableByDefault()
		);
		StubMappingScope scope = mainIndex.createScope();

		String fieldPath = getProjectableDefaultFieldPath();

		assertThatThrownBy( () -> scope.projection()
				.distance( fieldPath, CENTER_POINT_1 ).toProjection() )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Cannot use 'projection:distance' on field '" + fieldPath + "'",
						"Make sure the field is marked as searchable/sortable/projectable/aggregable/highlightable (whichever is relevant)"
				);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3391")
	public void multiValuedField_singleValuedProjection() {
		StubMappingScope scope = mainIndex.createScope();

		String fieldPath = mainIndex.binding().fieldWithMultipleValuesModel.relativeFieldName;

		assertThatThrownBy( () -> scope.projection()
				.field( fieldPath, fieldType.getJavaType() ).toProjection() )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Invalid cardinality for projection on field '" + fieldPath + "'",
						"the projection is single-valued, but this field is multi-valued",
						"Make sure to call '.multi()' when you create the projection"
				);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3391")
	public void singleValuedFieldInMultiValuedObjectField_flattened_singleValuedProjection() {
		String fieldPath = mainIndex.binding().flattenedObjectWithMultipleValues.relativeFieldName
				+ "." + mainIndex.binding().flattenedObjectWithMultipleValues.fieldModel.relativeFieldName;

		assertThatThrownBy( () -> mainIndex.query()
				.select( f -> f.field( fieldPath, fieldType.getJavaType() ) )
				.where( f -> f.matchAll() )
				.toQuery() )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining(
						"Invalid cardinality for projection on field '" + fieldPath + "'",
						"the projection is single-valued, but this field is multi-valued",
						"Make sure to call '.multi()' when you create the projection"
				);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3391")
	public void singleValuedFieldInMultiValuedObjectField_nested_singleValuedProjection() {
		String fieldPath = mainIndex.binding().nestedObjectWithMultipleValues.relativeFieldName
				+ "." + mainIndex.binding().nestedObjectWithMultipleValues.fieldModel.relativeFieldName;

		assertThatThrownBy( () -> mainIndex.query()
				.select( f -> f.field( fieldPath, fieldType.getJavaType() ) )
				.where( f -> f.matchAll() )
				.toQuery() )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining(
						"Invalid cardinality for projection on field '" + fieldPath + "'",
						"the projection is single-valued, but this field is multi-valued",
						"Make sure to call '.multi()' when you create the projection"
				);
	}

	@Test
	public void withProjectionConverters() {
		StubMappingScope scope = mainIndex.createScope();

		String fieldPath = getFieldWithConverterPath();

		assertThatQuery( scope.query()
				.select( f -> f.distance( fieldPath, CENTER_POINT_1 ) )
				.where( f -> f.matchAll() )
				.toQuery() )
				.hits().asIs()
				.usingElementComparator( TestComparators.APPROX_M_COMPARATOR )
				.containsExactlyInAnyOrder(
						getFieldDistance( 1 ),
						getFieldDistance( 2 ),
						getFieldDistance( 3 ),
						null // Empty document
				);
	}

	@Test
	public void multiIndex_withCompatibleIndex() {
		StubMappingScope scope = mainIndex.createScope( compatibleIndex );

		assertThatQuery( scope.query()
				.select( f -> f.distance( getFieldPath(), CENTER_POINT_1 ) )
				.where( f -> f.matchAll() )
				.toQuery() )
				.hits().asIs()
				.usingElementComparator( TestComparators.APPROX_M_COMPARATOR )
				.containsExactlyInAnyOrder(
						getFieldDistance( 1 ),
						getFieldDistance( 2 ),
						getFieldDistance( 3 ),
						null, // Empty document
						getFieldDistance( 1 ) // From the "compatible" index
				);
	}

	@Test
	public void multiIndex_withRawFieldCompatibleIndex() {
		StubMappingScope scope = mainIndex.createScope( rawFieldCompatibleIndex );

		assertThatQuery( scope.query()
				.select( f -> f.distance( getFieldWithConverterPath(), CENTER_POINT_1 ) )
				.where( f -> f.matchAll() )
				.toQuery() )
				.hits().asIs()
				.usingElementComparator( TestComparators.APPROX_M_COMPARATOR )
				.containsExactlyInAnyOrder(
						getFieldDistance( 1 ),
						getFieldDistance( 2 ),
						getFieldDistance( 3 ),
						null, // Empty document
						getFieldDistance( 1 ) // From the "compatible" index
				);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4173")
	public void multiIndex_withMissingFieldIndex() {
		StubMappingScope scope = mainIndex.createScope( missingFieldIndex );

		assertThatQuery( scope.query()
				.select( f -> f.distance( getFieldWithConverterPath(), CENTER_POINT_1 ) )
				.where( f -> f.matchAll() )
				.toQuery() )
				.hits().asIs()
				.usingElementComparator( TestComparators.APPROX_M_COMPARATOR )
				.containsExactlyInAnyOrder(
						getFieldDistance( 1 ),
						getFieldDistance( 2 ),
						getFieldDistance( 3 ),
						null, // Empty document
						null // From the "missing field" index
				);
	}

	@Test
	public void multiIndex_withIncompatibleIndex() {
		StubMappingScope scope = mainIndex.createScope( incompatibleIndex );

		String fieldPath = getFieldPath();

		assertThatThrownBy( () -> scope.projection().distance( fieldPath, CENTER_POINT_1 ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Inconsistent configuration for field '" + fieldPath + "' in a search query across multiple indexes",
						"Inconsistent support for 'projection:distance'"
				);
	}

	@Test
	public void multiIndex_withIncompatibleIndex_inNestedObject() {
		StubMappingScope scope = incompatibleIndex.createScope( mainIndex );

		String fieldPath = mainIndex.binding().nestedObject.relativeFieldName + "."
				+ mainIndex.binding().nestedObject.fieldModel.relativeFieldName;

		assertThatThrownBy( () -> scope.projection().distance( fieldPath, CENTER_POINT_1 ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Inconsistent configuration for field '" + fieldPath + "' in a search query across multiple indexes",
						"Attribute 'nested", "' differs:"
				);
	}

	private String getFieldPath() {
		return mainIndex.binding().fieldModel.relativeFieldName;
	}

	private String getFieldWithConverterPath() {
		return mainIndex.binding().fieldWithConverterModel.relativeFieldName;
	}

	private String getNonProjectableFieldPath() {
		return mainIndex.binding().fieldWithProjectionDisabledModel.relativeFieldName;
	}

	private String getProjectableDefaultFieldPath() {
		return mainIndex.binding().fieldWithDefaultProjectionModel.relativeFieldName;
	}

	private static GeoPoint getFieldValue(int documentNumber) {
		return IndexableGeoPointWithDistanceFromCenterValues.INSTANCE.getSingle().get( documentNumber - 1 );
	}

	private static double getFieldDistance(int documentNumber) {
		return IndexableGeoPointWithDistanceFromCenterValues.INSTANCE.getSingleDistancesFromCenterPoint1().get(
				documentNumber - 1 );
	}

	private static <F> void initDocument(IndexBinding binding, DocumentElement document, int documentNumber) {
		addFieldValue( document, binding.fieldModel, documentNumber );
		addFieldValue( document, binding.fieldWithConverterModel, documentNumber );

		// Note: this object must be single-valued for these tests
		DocumentElement flattenedObject = document.addObject( binding.flattenedObject.self );
		addFieldValue( flattenedObject, binding.flattenedObject.fieldModel, documentNumber );

		// Note: this object must be single-valued for these tests
		DocumentElement nestedObject = document.addObject( binding.nestedObject.self );
		addFieldValue( nestedObject, binding.nestedObject.fieldModel, documentNumber );
	}

	private static void addFieldValue(DocumentElement documentElement, SimpleFieldModel<GeoPoint> fieldModel,
			int documentNumber) {
		documentElement.addValue( fieldModel.reference, getFieldValue( documentNumber ) );
	}

	private static void initData() {
		BulkIndexer mainIndexer = mainIndex.bulkIndexer()
				.add( DOCUMENT_1, document -> initDocument( mainIndex.binding(), document, 1 ) )
				.add( DOCUMENT_2, document -> initDocument( mainIndex.binding(), document, 2 ) )
				.add( DOCUMENT_3, document -> initDocument( mainIndex.binding(), document, 3 ) )
				.add( EMPTY, document -> {} );
		BulkIndexer compatibleIndexer = compatibleIndex.bulkIndexer()
				.add( COMPATIBLE_INDEX_DOCUMENT_1, document -> {
					addFieldValue( document, compatibleIndex.binding().fieldModel, 1 );
					addFieldValue( document, compatibleIndex.binding().fieldWithConverterModel, 1 );
				} );
		BulkIndexer rawFieldCompatibleIndexer = rawFieldCompatibleIndex.bulkIndexer()
				.add(
						RAW_FIELD_COMPATIBLE_INDEX_DOCUMENT_1,
						document -> addFieldValue(
								document, rawFieldCompatibleIndex.binding().fieldWithConverterModel, 1 )
				);
		BulkIndexer missingFieldIndexer = missingFieldIndex.bulkIndexer()
				.add( MISSING_FIELD_INDEX_DOCUMENT_1, document -> {} );
		mainIndexer.join( compatibleIndexer, rawFieldCompatibleIndexer, missingFieldIndexer );
	}

	private static class IndexBinding {
		final SimpleFieldModel<GeoPoint> fieldModel;
		final SimpleFieldModel<GeoPoint> fieldWithConverterModel;
		final SimpleFieldModel<GeoPoint> fieldWithProjectionDisabledModel;
		final SimpleFieldModel<GeoPoint> fieldWithDefaultProjectionModel;
		final SimpleFieldModel<GeoPoint> fieldWithMultipleValuesModel;

		final ObjectBinding flattenedObject;
		final ObjectBinding nestedObject;

		final ObjectBinding flattenedObjectWithMultipleValues;
		final ObjectBinding nestedObjectWithMultipleValues;

		IndexBinding(IndexSchemaElement root) {
			fieldModel = SimpleFieldModel.mapper( fieldType )
					.map( root, "unconverted", c -> c.projectable( Projectable.YES ) );
			fieldWithConverterModel = SimpleFieldModel.mapper( fieldType )
					.map( root, "converted", c -> c.projectable( Projectable.YES )
							.dslConverter( ValueWrapper.class, ValueWrapper.toDocumentValueConverter() )
							.projectionConverter( ValueWrapper.class, ValueWrapper.fromDocumentValueConverter() ) );
			fieldWithProjectionDisabledModel = SimpleFieldModel.mapper( fieldType )
					.map( root, "nonProjectable", c -> c.projectable( Projectable.NO ) );
			fieldWithDefaultProjectionModel = SimpleFieldModel.mapper( fieldType )
					.map( root, "projectableDefault", c -> c.projectable( Projectable.DEFAULT ) );
			fieldWithMultipleValuesModel = SimpleFieldModel.mapper( fieldType )
					.mapMultiValued( root, "multiValued", c -> c.projectable( Projectable.YES ) );

			flattenedObject = new ObjectBinding( root, "flattenedObject", ObjectStructure.FLATTENED, false );
			nestedObject = new ObjectBinding( root, "nestedObject", ObjectStructure.NESTED, false );

			flattenedObjectWithMultipleValues = new ObjectBinding( root, "multiValued_flattenedObject",
					ObjectStructure.FLATTENED, true
			);
			nestedObjectWithMultipleValues = new ObjectBinding( root, "multiValued_nestedObject",
					ObjectStructure.NESTED, true
			);
		}
	}

	private static class ObjectBinding {
		final String relativeFieldName;
		final IndexObjectFieldReference self;
		final SimpleFieldModel<GeoPoint> fieldModel;

		ObjectBinding(IndexSchemaElement parent, String relativeFieldName, ObjectStructure structure,
				boolean multiValued) {
			this.relativeFieldName = relativeFieldName;
			IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, structure );
			if ( multiValued ) {
				objectField.multiValued();
			}
			self = objectField.toReference();
			fieldModel = SimpleFieldModel.mapper( fieldType )
					.map( objectField, "unconverted", c -> c.projectable( Projectable.YES ) );
		}
	}

	private static class CompatibleIndexBinding {
		final SimpleFieldModel<GeoPoint> fieldModel;
		final SimpleFieldModel<GeoPoint> fieldWithConverterModel;

		CompatibleIndexBinding(IndexSchemaElement root) {
			fieldModel = SimpleFieldModel.mapper( fieldType )
					.map( root, "unconverted", c -> {
						c.projectable( Projectable.YES );
						addIrrelevantOptions( c );
					} );
			fieldWithConverterModel = SimpleFieldModel.mapper( fieldType )
					.map( root, "converted", c -> {
						c.projectable( Projectable.YES )
								.dslConverter( ValueWrapper.class, ValueWrapper.toDocumentValueConverter() )
								.projectionConverter( ValueWrapper.class, ValueWrapper.fromDocumentValueConverter() );
						addIrrelevantOptions( c );
					} );
		}

		// See HSEARCH-3307: this checks that irrelevant options are ignored when checking cross-index field compatibility
		protected void addIrrelevantOptions(StandardIndexFieldTypeOptionsStep<?, ?> c) {
			c.searchable( Searchable.NO );
			c.sortable( Sortable.YES );
			c.aggregable( Aggregable.YES );
		}
	}

	private static class RawFieldCompatibleIndexBinding {
		final SimpleFieldModel<GeoPoint> fieldWithConverterModel;

		RawFieldCompatibleIndexBinding(IndexSchemaElement root) {
			/*
			 * Add a field with the same name as the fieldWithConverterModel from IndexBinding,
			 * but with an incompatible projection converter.
			 */
			fieldWithConverterModel = SimpleFieldModel.mapper( fieldType )
					.map( root, "converted", c -> c.projectable( Projectable.YES )
							.projectionConverter( ValueWrapper.class, new IncompatibleProjectionConverter() ) );
		}

		@SuppressWarnings("rawtypes")
		private static class IncompatibleProjectionConverter
				implements FromDocumentValueConverter<Object, ValueWrapper> {
			@Override
			public ValueWrapper fromDocumentValue(Object value, FromDocumentValueConvertContext context) {
				return null;
			}
		}
	}

	private static class MissingFieldIndexBinding {
		MissingFieldIndexBinding(IndexSchemaElement root) {
		}
	}

	private static class IncompatibleIndexBinding {
		final ObjectBinding flattenedObject;

		IncompatibleIndexBinding(IndexSchemaElement root) {
			/*
			 * Add a field with the same name as the fieldsModel from IndexBinding,
			 * but with an incompatible type.
			 */
			SimpleFieldModel.mapper( FieldTypeDescriptor.getIncompatible( fieldType ) )
					.map( root, "unconverted", c -> c.projectable( Projectable.YES ) );

			/*
			 * Add an object with the same name of nestedObject of IndexBinding,
			 * but we're using here a flattened structure.
			 * If we try to project on a field within this object,
			 * it will lead to an inconsistency exception.
			 */
			flattenedObject = new ObjectBinding( root, "nestedObject", ObjectStructure.FLATTENED, false );
		}
	}
}
