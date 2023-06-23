/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.AnalyzedStringFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModel;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModelsByType;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class ExistsPredicateSpecificsIT<F> {
	private static final List<FieldTypeDescriptor<?>> supportedFieldTypes = FieldTypeDescriptor.getAll();
	private static final List<FieldTypeDescriptor<?>> supportedFieldTypesWithDocValues = new ArrayList<>();
	private static final List<DataSet<?>> dataSets = new ArrayList<>();
	private static final List<Object[]> parameters = new ArrayList<>();
	static {
		for ( FieldTypeDescriptor<?> fieldType : supportedFieldTypes ) {
			if ( fieldType.isFieldSortSupported() ) {
				supportedFieldTypesWithDocValues.add( fieldType );
			}
			DataSet<?> dataSet = new DataSet<>( fieldType );
			dataSets.add( dataSet );
			parameters.add( new Object[] { dataSet } );
		}
	}

	@Parameterized.Parameters(name = "{0}")
	public static List<Object[]> parameters() {
		return parameters;
	}

	@ClassRule
	public static final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private static final SimpleMappedIndex<IndexBinding> mainIndex = SimpleMappedIndex.of( IndexBinding::new )
			.name( "main" );
	private static final SimpleMappedIndex<DifferentTypeIndexBinding> differentFieldTypeIndex =
			SimpleMappedIndex.of( DifferentTypeIndexBinding::new ).name( "differentFieldType" );

	@BeforeClass
	public static void setup() {
		setupHelper.start().withIndexes( mainIndex, differentFieldTypeIndex ).setup();

		BulkIndexer mainIndexer = mainIndex.bulkIndexer();
		BulkIndexer differentFieldTypeIndexer = differentFieldTypeIndex.bulkIndexer();
		dataSets.forEach( dataSet -> dataSet.contribute( mainIndexer, differentFieldTypeIndexer ) );
		mainIndexer.join( differentFieldTypeIndexer );
	}

	private final DataSet<F> dataSet;

	public ExistsPredicateSpecificsIT(DataSet<F> dataSet) {
		this.dataSet = dataSet;
	}

	/**
	 * There's no such thing as a "missing" predicate,
	 * but let's check that negating the "exists" predicate works as intended.
	 */
	@Test
	public void missing() {
		String fieldPath = mainIndex.binding().fieldWithDefaults.get( dataSet.fieldType ).relativeFieldName;

		assertThatQuery( mainIndex.query()
				.where( f -> f.not( f.exists().field( fieldPath ) ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), dataSet.docId( 2 ), dataSet.docId( 3 ) );
	}

	/**
	 * Fields with docvalues may be optimized and use a different Lucene query.
	 * Make sure to test the optimization as well.
	 */
	@Test
	public void withDocValues() {
		assumeDocValuesAllowed();

		String fieldPath = mainIndex.binding().fieldWithDocValues.get( dataSet.fieldType ).relativeFieldName;

		assertThatQuery( mainIndex.query()
				.where( f -> f.exists().field( fieldPath ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), dataSet.docId( 0 ), dataSet.docId( 1 ) );
	}

	@Test
	public void inFlattenedObject_withDocValues() {
		assumeDocValuesAllowed();

		String fieldPath = mainIndex.binding().flattenedObject.relativeFieldName + "."
				+ mainIndex.binding().flattenedObject.fieldWithDocValues.get( dataSet.fieldType ).relativeFieldName;

		assertThatQuery( mainIndex.query()
				.where( f -> f.exists().field( fieldPath ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), dataSet.docId( 0 ), dataSet.docId( 1 ) );
	}

	@Test
	public void inNestedObject_withDocValues() {
		assumeDocValuesAllowed();

		String fieldPath = mainIndex.binding().nestedObject.relativeFieldName + "."
				+ mainIndex.binding().nestedObject.fieldWithDocValues.get( dataSet.fieldType ).relativeFieldName;

		assertThatQuery( mainIndex.query()
				.where( f -> f.not( f.exists().field( fieldPath ) ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), dataSet.docId( 2 ), dataSet.docId( 3 ) );
	}

	/**
	 * If we require a field not to exist in a nested object,
	 * a document will match if *any* of its nested objects lacks the field.
	 */
	@Test
	public void inNestedPredicate_missing() {
		String fieldPath = mainIndex.binding().nestedObject.relativeFieldName + "."
				+ mainIndex.binding().nestedObject.fieldWithDefaults.get( dataSet.fieldType ).relativeFieldName;

		assertThatQuery( mainIndex.query()
				.where( f -> f.nested( mainIndex.binding().nestedObject.relativeFieldName )
						.add( f.matchAll().except( f.exists().field( fieldPath ) ) ) )
				.routing( dataSet.routingKey ) )
				// No match for document 0, since all of its nested objects have this field
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), dataSet.docId( 1 ), dataSet.docId( 2 ) );
	}

	/**
	 * The "exists" predicate can work with indexes whose underlying field has a different type,
	 * provided the implementation of the exists predicate is the same (i.e. docValues, norms, ...).
	 */
	@Test
	public void multiIndex_differentFieldType() {
		assumeFalse( "This test is only relevant if the field type does not use norms",
				dataSet.fieldType.equals( AnalyzedStringFieldTypeDescriptor.INSTANCE ) );

		StubMappingScope scope = mainIndex.createScope( differentFieldTypeIndex );

		String fieldPath = mainIndex.binding().fieldWithDefaults.get( dataSet.fieldType ).relativeFieldName;

		assertThatQuery( scope.query()
				.where( f -> f.exists().field( fieldPath ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( b -> {
					b.doc( mainIndex.typeName(), dataSet.docId( 0 ) );
					b.doc( mainIndex.typeName(), dataSet.docId( 1 ) );
					b.doc( differentFieldTypeIndex.typeName(), dataSet.docId( 0 ) );
				} );
	}

	/**
	 * The "exists" predicate can work with indexes whose underlying field has a different type,
	 * provided the implementation of the exists predicate is the same (i.e. docValues, norms, ...).
	 */
	@Test
	public void multiIndex_differentFieldType_withDocValues() {
		assumeDocValuesAllowed();

		StubMappingScope scope = mainIndex.createScope( differentFieldTypeIndex );

		String fieldPath = mainIndex.binding().fieldWithDocValues.get( dataSet.fieldType ).relativeFieldName;

		assertThatQuery( scope.query()
				.where( f -> f.exists().field( fieldPath ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( b -> {
					b.doc( mainIndex.typeName(), dataSet.docId( 0 ) );
					b.doc( mainIndex.typeName(), dataSet.docId( 1 ) );
					b.doc( differentFieldTypeIndex.typeName(), dataSet.docId( 0 ) );
				} );
	}

	private void assumeDocValuesAllowed() {
		assumeTrue( "This test is only relevant if the field type supports doc values",
				supportedFieldTypesWithDocValues.contains( dataSet.fieldType ) );
	}

	private static class IndexBinding {
		final SimpleFieldModelsByType fieldWithDefaults;
		final SimpleFieldModelsByType fieldWithDocValues;

		final ObjectMapping flattenedObject;
		final ObjectMapping nestedObject;

		IndexBinding(IndexSchemaElement root) {
			fieldWithDefaults = SimpleFieldModelsByType.mapAll( supportedFieldTypes, root, "fieldWithDefaults_" );
			fieldWithDocValues = SimpleFieldModelsByType.mapAll( supportedFieldTypesWithDocValues, root, "fieldWithDocValues_",
					c -> c.sortable( Sortable.YES ) );
			flattenedObject = new ObjectMapping( root, "flattenedObject", ObjectStructure.FLATTENED );
			nestedObject = new ObjectMapping( root, "nestedObject", ObjectStructure.NESTED );
		}
	}

	private static class ObjectMapping {
		final String relativeFieldName;
		final IndexObjectFieldReference self;
		final SimpleFieldModelsByType fieldWithDefaults;
		final SimpleFieldModelsByType fieldWithDocValues;

		ObjectMapping(IndexSchemaElement parent, String relativeFieldName, ObjectStructure structure) {
			this.relativeFieldName = relativeFieldName;
			IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, structure )
					.multiValued();
			self = objectField.toReference();
			fieldWithDefaults = SimpleFieldModelsByType.mapAll( supportedFieldTypes, objectField, "fieldWithDefaults_" );
			fieldWithDocValues =
					SimpleFieldModelsByType.mapAll( supportedFieldTypesWithDocValues, objectField, "fieldWithDocValues_",
							c -> c.sortable( Sortable.YES ) );
		}
	}

	private static class DifferentTypeIndexBinding {
		private final Map<FieldTypeDescriptor<?>, SimpleFieldModel<?>> fieldWithDefaultsByOriginalType = new LinkedHashMap<>();
		private final Map<FieldTypeDescriptor<?>, SimpleFieldModel<?>> fieldWithDocValuesByOriginalType = new LinkedHashMap<>();

		DifferentTypeIndexBinding(IndexSchemaElement root) {
			supportedFieldTypes.forEach( fieldType -> {
				FieldTypeDescriptor<?> replacingType = FieldTypeDescriptor.getIncompatible( fieldType );
				fieldWithDefaultsByOriginalType.put( fieldType, SimpleFieldModel.mapper( replacingType )
						.map( root, "fieldWithDefaults_" + fieldType.getUniqueName() ) );
			} );
			supportedFieldTypesWithDocValues.forEach( fieldType -> {
				FieldTypeDescriptor<?> replacingType = null;
				for ( FieldTypeDescriptor<?> candidate : supportedFieldTypesWithDocValues ) {
					if ( !fieldType.equals( candidate ) ) {
						replacingType = candidate;
					}
				}
				fieldWithDocValuesByOriginalType.put( fieldType, SimpleFieldModel.mapper( replacingType,
						c -> c.sortable( Sortable.YES ) )
						.map( root, "fieldWithDocValues_" + fieldType.getUniqueName() ) );
			} );
		}
	}

	private static final class DataSet<F> extends AbstractPredicateDataSet {
		protected final FieldTypeDescriptor<F> fieldType;

		public DataSet(FieldTypeDescriptor<F> fieldType) {
			super( fieldType.getUniqueName() );
			this.fieldType = fieldType;
		}

		public void contribute(BulkIndexer mainIndexer, BulkIndexer differentFieldTypeIndexer) {
			List<F> values = fieldType.getIndexableValues().getSingle();
			F value1 = values.get( 0 );
			F value2 = values.get( 1 );

			final boolean docValues = supportedFieldTypesWithDocValues.contains( fieldType );

			mainIndexer
					.add( docId( 0 ), routingKey, document -> {
						document.addValue( mainIndex.binding().fieldWithDefaults.get( fieldType ).reference, value1 );
						if ( docValues ) {
							document.addValue( mainIndex.binding().fieldWithDocValues.get( fieldType ).reference, value1 );
						}

						// Add one object with value1, and another with value2
						DocumentElement flattenedObject1 = document.addObject( mainIndex.binding().flattenedObject.self );
						flattenedObject1.addValue(
								mainIndex.binding().flattenedObject.fieldWithDefaults.get( fieldType ).reference, value1 );
						if ( docValues ) {
							flattenedObject1.addValue(
									mainIndex.binding().flattenedObject.fieldWithDocValues.get( fieldType ).reference, value1 );
						}
						DocumentElement flattenedObject2 = document.addObject( mainIndex.binding().flattenedObject.self );
						flattenedObject2.addValue(
								mainIndex.binding().flattenedObject.fieldWithDefaults.get( fieldType ).reference, value1 );
						if ( docValues ) {
							flattenedObject2.addValue(
									mainIndex.binding().flattenedObject.fieldWithDocValues.get( fieldType ).reference, value1 );
						}

						// Same for the nested object
						DocumentElement nestedObject1 = document.addObject( mainIndex.binding().nestedObject.self );
						nestedObject1.addValue( mainIndex.binding().nestedObject.fieldWithDefaults.get( fieldType ).reference,
								value1 );
						if ( docValues ) {
							nestedObject1.addValue(
									mainIndex.binding().nestedObject.fieldWithDocValues.get( fieldType ).reference, value1 );
						}
						DocumentElement nestedObject2 = document.addObject( mainIndex.binding().nestedObject.self );
						nestedObject2.addValue( mainIndex.binding().nestedObject.fieldWithDefaults.get( fieldType ).reference,
								value1 );
						if ( docValues ) {
							nestedObject2.addValue(
									mainIndex.binding().nestedObject.fieldWithDocValues.get( fieldType ).reference, value1 );
						}
					} )
					.add( docId( 1 ), routingKey, document -> {
						document.addValue( mainIndex.binding().fieldWithDefaults.get( fieldType ).reference, value2 );
						if ( docValues ) {
							document.addValue( mainIndex.binding().fieldWithDocValues.get( fieldType ).reference, value2 );
						}

						// Add one empty object, and another with value2
						document.addObject( mainIndex.binding().flattenedObject.self );
						DocumentElement flattenedObject2 = document.addObject( mainIndex.binding().flattenedObject.self );
						flattenedObject2.addValue(
								mainIndex.binding().flattenedObject.fieldWithDefaults.get( fieldType ).reference, value2 );
						if ( docValues ) {
							flattenedObject2.addValue(
									mainIndex.binding().flattenedObject.fieldWithDocValues.get( fieldType ).reference, value2 );
						}

						// Same for the nested object
						document.addObject( mainIndex.binding().nestedObject.self );
						DocumentElement nestedObject2 = document.addObject( mainIndex.binding().nestedObject.self );
						nestedObject2.addValue( mainIndex.binding().nestedObject.fieldWithDefaults.get( fieldType ).reference,
								value2 );
						if ( docValues ) {
							nestedObject2.addValue(
									mainIndex.binding().nestedObject.fieldWithDocValues.get( fieldType ).reference, value2 );
						}
					} )
					.add( docId( 2 ), routingKey, document -> {
						// Add null values for fields: they should be considered as missing too.
						if ( TckConfiguration.get().getBackendFeatures()
								.supportsExistsForFieldWithoutDocValues( fieldType.getJavaType() ) ) {
							document.addValue( mainIndex.binding().fieldWithDefaults.get( fieldType ).reference,
									null );
						}
						if ( docValues ) {
							document.addValue( mainIndex.binding().fieldWithDocValues.get( fieldType ).reference, null );
						}

						// Add two empty objects
						document.addObject( mainIndex.binding().flattenedObject.self );
						document.addObject( mainIndex.binding().flattenedObject.self );

						// Same for the nested object
						document.addObject( mainIndex.binding().nestedObject.self );
						document.addObject( mainIndex.binding().nestedObject.self );
					} )
					.add( docId( 3 ), routingKey, document -> {} );

			differentFieldTypeIndexer
					.add( docId( 0 ), routingKey, document -> {
						addDifferentTypeValue( document,
								differentFieldTypeIndex.binding().fieldWithDefaultsByOriginalType.get( fieldType ) );
						if ( docValues ) {
							addDifferentTypeValue( document,
									differentFieldTypeIndex.binding().fieldWithDocValuesByOriginalType.get( fieldType ) );
						}
					} )
					.add( docId( 1 ), routingKey, document -> {} );
		}

		private <T> void addDifferentTypeValue(DocumentElement document, SimpleFieldModel<T> field) {
			document.addValue( field.reference, field.typeDescriptor.getIndexableValues().getSingle().get( 0 ) );
		}
	}
}
