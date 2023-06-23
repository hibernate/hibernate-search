/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.document;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModel;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModelsByType;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Test the behavior of implementations of {@link DocumentElement}
 * when it comes to multi-valued fields.
 */
@RunWith(Parameterized.class)
public class DocumentElementMultiValuedIT<F> {

	private static List<FieldTypeDescriptor<?>> supportedTypeDescriptors() {
		return FieldTypeDescriptor.getAll();
	}

	@Parameterized.Parameters(name = "{0}")
	public static List<FieldTypeDescriptor<?>> parameters() {
		return supportedTypeDescriptors();
	}

	@ClassRule
	public static final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@BeforeClass
	public static void setup() {
		setupHelper.start().withIndex( index ).setup();
	}

	private final FieldTypeDescriptor<F> fieldType;

	public DocumentElementMultiValuedIT(FieldTypeDescriptor<F> fieldType) {
		this.fieldType = fieldType;
	}

	@Test
	public void addValue_root() {
		SimpleFieldModel<F> singleValuedFieldModel = getSingleValuedField( index.binding() );
		SimpleFieldModel<F> multiValuedFieldModel = getMultiValuedField( index.binding() );
		expectSuccess( "1", document -> {
			document.addValue( singleValuedFieldModel.reference, getValue( 0 ) );
		} );
		expectSingleValuedException( "2", singleValuedFieldModel.relativeFieldName, document -> {
			document.addValue( singleValuedFieldModel.reference, getValue( 0 ) );
			document.addValue( singleValuedFieldModel.reference, getValue( 1 ) );
		} );
		expectSuccess( "3", document -> {
			document.addValue( multiValuedFieldModel.reference, getValue( 0 ) );
		} );
		expectSuccess( "4", document -> {
			document.addValue( multiValuedFieldModel.reference, getValue( 0 ) );
			document.addValue( multiValuedFieldModel.reference, getValue( 1 ) );
			document.addValue( multiValuedFieldModel.reference, getValue( 0 ) );
		} );
	}

	@Test
	public void addObject_flattened() {
		expectSuccess( "1", document -> {
			document.addObject( index.binding().singleValuedFlattenedObject.self );
		} );
		expectSingleValuedException( "2", "singleValuedFlattenedObject", document -> {
			document.addObject( index.binding().singleValuedFlattenedObject.self );
			document.addObject( index.binding().singleValuedFlattenedObject.self );
		} );
		expectSuccess( "3", document -> {
			document.addObject( index.binding().multiValuedFlattenedObject.self );
		} );
		expectSuccess( "4", document -> {
			document.addObject( index.binding().multiValuedFlattenedObject.self );
			document.addObject( index.binding().multiValuedFlattenedObject.self );
			document.addObject( index.binding().multiValuedFlattenedObject.self );
		} );
	}

	@Test
	public void addObject_nested() {
		expectSuccess( "1", document -> {
			document.addObject( index.binding().singleValuedNestedObject.self );
		} );
		expectSingleValuedException( "2", "singleValuedNestedObject", document -> {
			document.addObject( index.binding().singleValuedNestedObject.self );
			document.addObject( index.binding().singleValuedNestedObject.self );
		} );
		expectSuccess( "3", document -> {
			document.addObject( index.binding().multiValuedNestedObject.self );
		} );
		expectSuccess( "4", document -> {
			document.addObject( index.binding().multiValuedNestedObject.self );
			document.addObject( index.binding().multiValuedNestedObject.self );
			document.addObject( index.binding().multiValuedNestedObject.self );
		} );
	}

	@Test
	public void addNullObject_flattened() {
		expectSuccess( "1", document -> {
			document.addObject( index.binding().singleValuedFlattenedObject.self );
		} );
		expectSingleValuedException( "2", "singleValuedFlattenedObject", document -> {
			document.addNullObject( index.binding().singleValuedFlattenedObject.self );
			document.addNullObject( index.binding().singleValuedFlattenedObject.self );
		} );
		expectSingleValuedException( "3", "singleValuedFlattenedObject", document -> {
			document.addObject( index.binding().singleValuedFlattenedObject.self );
			document.addNullObject( index.binding().singleValuedFlattenedObject.self );
		} );
		expectSingleValuedException( "4", "singleValuedFlattenedObject", document -> {
			document.addNullObject( index.binding().singleValuedFlattenedObject.self );
			document.addObject( index.binding().singleValuedFlattenedObject.self );
		} );
		expectSuccess( "5", document -> {
			document.addObject( index.binding().multiValuedFlattenedObject.self );
		} );
		expectSuccess( "6", document -> {
			document.addNullObject( index.binding().multiValuedFlattenedObject.self );
			document.addNullObject( index.binding().multiValuedFlattenedObject.self );
			document.addNullObject( index.binding().multiValuedFlattenedObject.self );
		} );
		expectSuccess( "7", document -> {
			document.addNullObject( index.binding().multiValuedFlattenedObject.self );
			document.addObject( index.binding().multiValuedFlattenedObject.self );
			document.addNullObject( index.binding().multiValuedFlattenedObject.self );
		} );
		expectSuccess( "8", document -> {
			document.addObject( index.binding().multiValuedFlattenedObject.self );
			document.addNullObject( index.binding().multiValuedFlattenedObject.self );
			document.addNullObject( index.binding().multiValuedFlattenedObject.self );
		} );
	}

	@Test
	public void addNullObject_nested() {
		expectSuccess( "1", document -> {
			document.addObject( index.binding().singleValuedNestedObject.self );
		} );
		expectSingleValuedException( "2", "singleValuedNestedObject", document -> {
			document.addNullObject( index.binding().singleValuedNestedObject.self );
			document.addNullObject( index.binding().singleValuedNestedObject.self );
		} );
		expectSingleValuedException( "3", "singleValuedNestedObject", document -> {
			document.addObject( index.binding().singleValuedNestedObject.self );
			document.addNullObject( index.binding().singleValuedNestedObject.self );
		} );
		expectSingleValuedException( "4", "singleValuedNestedObject", document -> {
			document.addNullObject( index.binding().singleValuedNestedObject.self );
			document.addObject( index.binding().singleValuedNestedObject.self );
		} );
		expectSuccess( "5", document -> {
			document.addObject( index.binding().multiValuedNestedObject.self );
		} );
		expectSuccess( "6", document -> {
			document.addNullObject( index.binding().multiValuedNestedObject.self );
			document.addNullObject( index.binding().multiValuedNestedObject.self );
			document.addNullObject( index.binding().multiValuedNestedObject.self );
		} );
		expectSuccess( "7", document -> {
			document.addNullObject( index.binding().multiValuedNestedObject.self );
			document.addObject( index.binding().multiValuedNestedObject.self );
			document.addNullObject( index.binding().multiValuedNestedObject.self );
		} );
		expectSuccess( "8", document -> {
			document.addObject( index.binding().multiValuedNestedObject.self );
			document.addNullObject( index.binding().multiValuedNestedObject.self );
			document.addNullObject( index.binding().multiValuedNestedObject.self );
		} );
	}

	@Test
	public void addValue_inSingleValuedFlattenedObject() {
		SimpleFieldModel<F> singleValuedFieldModel = getSingleValuedField( index.binding().singleValuedFlattenedObject );
		SimpleFieldModel<F> multiValuedFieldModel = getMultiValuedField( index.binding().singleValuedFlattenedObject );
		expectSuccess( "1", document -> {
			DocumentElement level1 = document.addObject( index.binding().singleValuedFlattenedObject.self );
			level1.addValue( singleValuedFieldModel.reference, getValue( 0 ) );
		} );
		expectSingleValuedException( "2", "singleValuedFlattenedObject." + singleValuedFieldModel.relativeFieldName,
				document -> {
					DocumentElement level1 = document.addObject( index.binding().singleValuedFlattenedObject.self );
					level1.addValue( singleValuedFieldModel.reference, getValue( 0 ) );
					level1.addValue( singleValuedFieldModel.reference, getValue( 1 ) );
				} );
		expectSuccess( "3", document -> {
			DocumentElement level1 = document.addObject( index.binding().singleValuedFlattenedObject.self );
			level1.addValue( multiValuedFieldModel.reference, getValue( 0 ) );
		} );
		expectSuccess( "4", document -> {
			DocumentElement level1 = document.addObject( index.binding().singleValuedFlattenedObject.self );
			level1.addValue( multiValuedFieldModel.reference, getValue( 0 ) );
			level1.addValue( multiValuedFieldModel.reference, getValue( 1 ) );
			level1.addValue( multiValuedFieldModel.reference, getValue( 0 ) );
		} );
	}

	@Test
	public void addValue_inMultiValuedFlattenedObject() {
		SimpleFieldModel<F> singleValuedFieldModel = getSingleValuedField( index.binding().multiValuedFlattenedObject );
		SimpleFieldModel<F> multiValuedFieldModel = getMultiValuedField( index.binding().multiValuedFlattenedObject );
		expectSuccess( "1", document -> {
			DocumentElement level1 = document.addObject( index.binding().multiValuedFlattenedObject.self );
			level1.addValue( singleValuedFieldModel.reference, getValue( 0 ) );
		} );
		expectSingleValuedException( "2", "multiValuedFlattenedObject." + singleValuedFieldModel.relativeFieldName,
				document -> {
					DocumentElement level1 = document.addObject( index.binding().multiValuedFlattenedObject.self );
					level1.addValue( singleValuedFieldModel.reference, getValue( 0 ) );
					level1.addValue( singleValuedFieldModel.reference, getValue( 1 ) );
				} );
		expectSuccess( "3", document -> {
			DocumentElement level1 = document.addObject( index.binding().multiValuedFlattenedObject.self );
			level1.addValue( multiValuedFieldModel.reference, getValue( 0 ) );
		} );
		expectSuccess( "4", document -> {
			DocumentElement level1 = document.addObject( index.binding().multiValuedFlattenedObject.self );
			level1.addValue( multiValuedFieldModel.reference, getValue( 0 ) );
			level1.addValue( multiValuedFieldModel.reference, getValue( 1 ) );
			level1.addValue( multiValuedFieldModel.reference, getValue( 0 ) );
		} );
	}

	@Test
	public void addValue_inSingleValuedNestedObject() {
		SimpleFieldModel<F> singleValuedFieldModel = getSingleValuedField( index.binding().singleValuedNestedObject );
		SimpleFieldModel<F> multiValuedFieldModel = getMultiValuedField( index.binding().singleValuedNestedObject );
		expectSuccess( "1", document -> {
			DocumentElement level1 = document.addObject( index.binding().singleValuedNestedObject.self );
			level1.addValue( singleValuedFieldModel.reference, getValue( 0 ) );
		} );
		expectSingleValuedException( "2", "singleValuedNestedObject." + singleValuedFieldModel.relativeFieldName, document -> {
			DocumentElement level1 = document.addObject( index.binding().singleValuedNestedObject.self );
			level1.addValue( singleValuedFieldModel.reference, getValue( 0 ) );
			level1.addValue( singleValuedFieldModel.reference, getValue( 1 ) );
		} );
		expectSuccess( "3", document -> {
			DocumentElement level1 = document.addObject( index.binding().singleValuedNestedObject.self );
			level1.addValue( multiValuedFieldModel.reference, getValue( 0 ) );
		} );
		expectSuccess( "4", document -> {
			DocumentElement level1 = document.addObject( index.binding().singleValuedNestedObject.self );
			level1.addValue( multiValuedFieldModel.reference, getValue( 0 ) );
			level1.addValue( multiValuedFieldModel.reference, getValue( 1 ) );
			level1.addValue( multiValuedFieldModel.reference, getValue( 0 ) );
		} );
	}

	@Test
	public void addValue_inMultiValuedNestedObject() {
		SimpleFieldModel<F> singleValuedFieldModel = getSingleValuedField( index.binding().multiValuedNestedObject );
		SimpleFieldModel<F> multiValuedFieldModel = getMultiValuedField( index.binding().multiValuedNestedObject );
		expectSuccess( "1", document -> {
			DocumentElement level1 = document.addObject( index.binding().multiValuedNestedObject.self );
			level1.addValue( singleValuedFieldModel.reference, getValue( 0 ) );
		} );
		expectSingleValuedException( "2", "multiValuedNestedObject." + singleValuedFieldModel.relativeFieldName, document -> {
			DocumentElement level1 = document.addObject( index.binding().multiValuedNestedObject.self );
			level1.addValue( singleValuedFieldModel.reference, getValue( 0 ) );
			level1.addValue( singleValuedFieldModel.reference, getValue( 1 ) );
		} );
		expectSuccess( "3", document -> {
			DocumentElement level1 = document.addObject( index.binding().multiValuedNestedObject.self );
			level1.addValue( multiValuedFieldModel.reference, getValue( 0 ) );
		} );
		expectSuccess( "4", document -> {
			DocumentElement level1 = document.addObject( index.binding().multiValuedNestedObject.self );
			level1.addValue( multiValuedFieldModel.reference, getValue( 0 ) );
			level1.addValue( multiValuedFieldModel.reference, getValue( 1 ) );
			level1.addValue( multiValuedFieldModel.reference, getValue( 0 ) );
		} );
	}

	private void expectSuccess(String id, Consumer<DocumentElement> documentContributor) {
		executeAdd( id, documentContributor );
	}

	private void expectSingleValuedException(String id, String absoluteFieldPath,
			Consumer<DocumentElement> documentContributor) {
		assertThatThrownBy(
				() -> executeAdd( id, documentContributor ),
				"Multiple values written to field '" + absoluteFieldPath + "'"
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Multiple values assigned to field '" + absoluteFieldPath + "'",
						"this field is single-valued",
						"Declare the field as multi-valued in order to allow this."
				);
	}

	private void executeAdd(String id, Consumer<DocumentElement> documentContributor) {
		index.index( id, documentContributor::accept );
	}

	private F getValue(int ordinal) {
		return fieldType.getIndexableValues().getSingle().get( ordinal );
	}

	private SimpleFieldModel<F> getSingleValuedField(AbstractObjectBinding binding) {
		return binding.singleValuedFieldModels.get( fieldType );
	}

	private SimpleFieldModel<F> getMultiValuedField(AbstractObjectBinding binding) {
		return binding.multiValuedFieldModels.get( fieldType );
	}

	private abstract static class AbstractObjectBinding {
		final SimpleFieldModelsByType singleValuedFieldModels;
		final SimpleFieldModelsByType multiValuedFieldModels;

		AbstractObjectBinding(IndexSchemaElement schemaElement) {
			this.singleValuedFieldModels = SimpleFieldModelsByType.mapAll( supportedTypeDescriptors(),
					schemaElement, "single_" );
			this.multiValuedFieldModels = SimpleFieldModelsByType.mapAllMultiValued( supportedTypeDescriptors(),
					schemaElement, "multi_" );
		}
	}

	private static class IndexBinding extends AbstractObjectBinding {
		final FirstLevelObjectBinding singleValuedFlattenedObject;
		final FirstLevelObjectBinding multiValuedFlattenedObject;
		final FirstLevelObjectBinding singleValuedNestedObject;
		final FirstLevelObjectBinding multiValuedNestedObject;

		IndexBinding(IndexSchemaElement root) {
			super( root );

			singleValuedFlattenedObject = new FirstLevelObjectBinding(
					root.objectField( "singleValuedFlattenedObject", ObjectStructure.FLATTENED )
			);
			multiValuedFlattenedObject = new FirstLevelObjectBinding(
					root.objectField( "multiValuedFlattenedObject", ObjectStructure.FLATTENED )
							.multiValued()
			);
			singleValuedNestedObject = new FirstLevelObjectBinding(
					root.objectField( "singleValuedNestedObject", ObjectStructure.NESTED )
			);
			multiValuedNestedObject = new FirstLevelObjectBinding(
					root.objectField( "multiValuedNestedObject", ObjectStructure.NESTED )
							.multiValued()
			);
		}
	}

	private static class FirstLevelObjectBinding extends AbstractObjectBinding {
		final IndexObjectFieldReference self;

		FirstLevelObjectBinding(IndexSchemaObjectField objectField) {
			super( objectField );
			self = objectField.toReference();
		}
	}
}
