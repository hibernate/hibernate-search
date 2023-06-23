/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.document;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.common.tree.TreeFilterDefinition;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEmbeddedBindingContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEntityBindingContext;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModelsByType;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingElement;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Test the basic behavior of implementations of {@link DocumentElement}
 * when referencing fields using a {@link IndexFieldReference}.
 */
@RunWith(Parameterized.class)
public class DocumentElementFieldReferenceIT<F> {

	private static List<FieldTypeDescriptor<?>> supportedTypeDescriptors() {
		return FieldTypeDescriptor.getAll();
	}

	@Parameterized.Parameters(name = "{0}")
	public static List<FieldTypeDescriptor<?>> parameters() {
		return supportedTypeDescriptors();
	}

	@ClassRule
	public static final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.ofAdvanced( IndexBinding::new );

	@BeforeClass
	public static void setup() {
		setupHelper.start().withIndex( index ).setup();
	}

	private final FieldTypeDescriptor<F> fieldType;

	public DocumentElementFieldReferenceIT(FieldTypeDescriptor<F> fieldType) {
		this.fieldType = fieldType;
	}

	/**
	 * Test that DocumentElement.addValue does not throw any exception when passing a non-null value.
	 */
	@Test
	public void addValue_nonNull() {
		executeAdd( "1", document -> {
			setNonNullValue( index.binding(), document );
		} );
	}

	/**
	 * Test that DocumentElement.addValue does not throw any exception when passing a null value.
	 */
	@Test
	public void addValue_null() {
		executeAdd( "1", document -> {
			setNullValue( index.binding(), document );
		} );
	}

	/**
	 * Test that DocumentElement.addObject does not throw any exception,
	 * add that DocumentElement.addValue does not throw an exception for returned objects.
	 */
	@Test
	public void addObject() {
		executeAdd( "1", document -> {
			setNullValue( index.binding(), document );

			DocumentElement flattenedObject = document.addObject( index.binding().flattenedObject.self );
			setNonNullValue( index.binding().flattenedObject, flattenedObject );
			flattenedObject = document.addObject( index.binding().flattenedObject.self );
			setNullValue( index.binding().flattenedObject, flattenedObject );
			DocumentElement flattenedObjectSecondLevelObject =
					flattenedObject.addObject( index.binding().flattenedObject.flattenedObject.self );
			setNonNullValue( index.binding().flattenedObject.flattenedObject, flattenedObjectSecondLevelObject );
			flattenedObjectSecondLevelObject = flattenedObject.addObject( index.binding().flattenedObject.nestedObject.self );
			setNullValue( index.binding().flattenedObject.nestedObject, flattenedObjectSecondLevelObject );

			DocumentElement nestedObject = document.addObject( index.binding().nestedObject.self );
			setNonNullValue( index.binding().nestedObject, nestedObject );
			nestedObject = document.addObject( index.binding().nestedObject.self );
			setNullValue( index.binding().nestedObject, nestedObject );
			DocumentElement nestedObjectSecondLevelObject =
					nestedObject.addObject( index.binding().nestedObject.flattenedObject.self );
			setNonNullValue( index.binding().nestedObject.flattenedObject, nestedObjectSecondLevelObject );
			nestedObjectSecondLevelObject = nestedObject.addObject( index.binding().nestedObject.nestedObject.self );
			setNullValue( index.binding().nestedObject.nestedObject, nestedObjectSecondLevelObject );
		} );
	}

	/**
	 * Test that DocumentElement.addNullObject does not throw any exception.
	 */
	@Test
	public void addNullObject() {
		executeAdd( "1", document -> {
			setNullValue( index.binding(), document );

			DocumentElement flattenedObject = document.addObject( index.binding().flattenedObject.self );
			document.addNullObject( index.binding().flattenedObject.self );
			flattenedObject.addObject( index.binding().flattenedObject.flattenedObject.self );
			flattenedObject.addNullObject( index.binding().flattenedObject.flattenedObject.self );
			flattenedObject.addObject( index.binding().flattenedObject.nestedObject.self );
			flattenedObject.addNullObject( index.binding().flattenedObject.nestedObject.self );

			DocumentElement nestedObject = document.addObject( index.binding().nestedObject.self );
			document.addNullObject( index.binding().nestedObject.self );
			nestedObject.addObject( index.binding().nestedObject.flattenedObject.self );
			nestedObject.addNullObject( index.binding().nestedObject.flattenedObject.self );
			nestedObject.addObject( index.binding().nestedObject.nestedObject.self );
			nestedObject.addNullObject( index.binding().nestedObject.nestedObject.self );
		} );
	}

	/**
	 * Test that DocumentElement.addValue/addObject do not throw any exception when
	 * adding a value to a static field on an object field that excludes all static children
	 * (due to IndexedEmbedded filters).
	 */
	@Test
	public void addValue_excludedFields() {
		executeAdd( "1", document -> {
			DocumentElement excludingObject = document.addObject( index.binding().excludingObject.self );
			setNonNullValue( index.binding().excludingObject, excludingObject );
			excludingObject = document.addObject( index.binding().excludingObject.self );
			setNullValue( index.binding().excludingObject, excludingObject );

			DocumentElement flattenedSecondLevelObject =
					excludingObject.addObject( index.binding().excludingObject.flattenedObject.self );
			setNonNullValue( index.binding().excludingObject.flattenedObject, flattenedSecondLevelObject );
			flattenedSecondLevelObject = excludingObject.addObject( index.binding().excludingObject.flattenedObject.self );
			setNullValue( index.binding().excludingObject.flattenedObject, flattenedSecondLevelObject );

			DocumentElement nestedSecondLevelObject =
					excludingObject.addObject( index.binding().excludingObject.nestedObject.self );
			setNullValue( index.binding().excludingObject.nestedObject, nestedSecondLevelObject );
			nestedSecondLevelObject = excludingObject.addObject( index.binding().excludingObject.nestedObject.self );
			setNullValue( index.binding().excludingObject.nestedObject, nestedSecondLevelObject );
		} );
	}

	@Test
	public void invalidFieldForDocumentElement_flattenedObjectChild() {
		IndexFieldReference<F> reference = index.binding().flattenedObject.fieldModels.get( fieldType ).reference;
		assertThatThrownBy(
				() -> executeAdd( "1", document -> {
					document.addValue( reference, null );
				} ),
				"Parent mismatch with reference " + reference
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Invalid field reference for this document element" )
				.hasMessageContaining( "this document element has path 'flattenedObject'" )
				.hasMessageContaining( "but the referenced field has a parent with path 'null'" );
	}

	@Test
	public void invalidFieldForDocumentElement_nestedObjectChild() {
		IndexFieldReference<F> reference = index.binding().nestedObject.fieldModels.get( fieldType ).reference;
		assertThatThrownBy(
				() -> executeAdd( "1", document -> {
					document.addValue( reference, null );
				} ),
				"Parent mismatch with reference " + reference
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Invalid field reference for this document element" )
				.hasMessageContaining( "this document element has path 'nestedObject'" )
				.hasMessageContaining( "but the referenced field has a parent with path 'null'" );
	}

	@Test
	public void invalidFieldForDocumentElement_rootChild() {
		IndexFieldReference<F> reference = index.binding().fieldModels.get( fieldType ).reference;
		assertThatThrownBy(
				() -> executeAdd( "1", document -> {
					DocumentElement flattenedObject = document.addObject( index.binding().flattenedObject.self );
					flattenedObject.addValue( reference, null );
				} ),
				"Parent mismatch with reference " + reference
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Invalid field reference for this document element" )
				.hasMessageContaining( "this document element has path 'null'" )
				.hasMessageContaining( "but the referenced field has a parent with path 'flattenedObject'" );
	}

	private void setNonNullValue(AbstractObjectBinding binding, DocumentElement document) {
		document.addValue( binding.fieldModels.get( fieldType ).reference,
				fieldType.getIndexableValues().getSingle().get( 0 ) );
	}

	private void setNullValue(AbstractObjectBinding binding, DocumentElement document) {
		document.addValue( binding.fieldModels.get( fieldType ).reference, null );
	}

	private void executeAdd(String id, Consumer<DocumentElement> documentContributor) {
		index.index( id, documentContributor::accept );
	}

	private static class AbstractObjectBinding {
		final SimpleFieldModelsByType fieldModels;

		AbstractObjectBinding(IndexSchemaElement root) {
			this.fieldModels = SimpleFieldModelsByType.mapAll( supportedTypeDescriptors(), root, "" );
		}
	}

	private static class IndexBinding extends AbstractObjectBinding {
		final FirstLevelObjectBinding flattenedObject;
		final FirstLevelObjectBinding nestedObject;
		final FirstLevelObjectBinding excludingObject;

		IndexBinding(IndexedEntityBindingContext ctx) {
			super( ctx.schemaElement() );
			IndexSchemaElement root = ctx.schemaElement();
			IndexSchemaObjectField flattenedObjectField = root.objectField( "flattenedObject", ObjectStructure.FLATTENED )
					.multiValued();
			flattenedObject = new FirstLevelObjectBinding( flattenedObjectField );
			IndexSchemaObjectField nestedObjectField = root.objectField( "nestedObject", ObjectStructure.NESTED )
					.multiValued();
			nestedObject = new FirstLevelObjectBinding( nestedObjectField );

			// Simulate an embedded context which excludes every subfield
			TreeFilterDefinition filterDefinition =
					new TreeFilterDefinition( null,
							Collections.singleton( "pathThatDoesNotMatchAnything" ), Collections.emptySet() );
			IndexedEmbeddedBindingContext excludingEmbeddedContext =
					ctx.addIndexedEmbeddedIfIncluded( new StubMappingElement(),
							"excludingObject.", ObjectStructure.FLATTENED, filterDefinition, true ).get();
			excludingObject = new FirstLevelObjectBinding(
					excludingEmbeddedContext.schemaElement(),
					excludingEmbeddedContext.parentIndexObjectReferences().iterator().next()
			);
		}
	}

	private static class FirstLevelObjectBinding extends AbstractObjectBinding {
		final IndexObjectFieldReference self;

		final SecondLevelObjectBinding flattenedObject;
		final SecondLevelObjectBinding nestedObject;

		FirstLevelObjectBinding(IndexSchemaObjectField objectField) {
			this( objectField, objectField.toReference() );
		}

		FirstLevelObjectBinding(IndexSchemaElement objectField, IndexObjectFieldReference objectFieldReference) {
			super( objectField );
			self = objectFieldReference;
			IndexSchemaObjectField flattenedObjectField =
					objectField.objectField( "flattenedObject", ObjectStructure.FLATTENED )
							.multiValued();
			flattenedObject = new SecondLevelObjectBinding( flattenedObjectField );
			IndexSchemaObjectField nestedObjectField = objectField.objectField( "nestedObject", ObjectStructure.NESTED )
					.multiValued();
			nestedObject = new SecondLevelObjectBinding( nestedObjectField );
		}
	}

	private static class SecondLevelObjectBinding extends AbstractObjectBinding {
		final IndexObjectFieldReference self;

		SecondLevelObjectBinding(IndexSchemaObjectField objectField) {
			super( objectField );
			self = objectField.toReference();
		}
	}
}
