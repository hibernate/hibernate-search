/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.document;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.types.dsl.SearchableProjectableIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.common.tree.TreeFilterDefinition;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEmbeddedBindingContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEntityBindingContext;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModelsByType;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingElement;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test the basic behavior of implementations of {@link DocumentElement}
 * when referencing fields using a {@link IndexFieldReference}.
 */

class DocumentElementFieldReferenceIT<F> {

	private static List<
			FieldTypeDescriptor<?, ? extends SearchableProjectableIndexFieldTypeOptionsStep<?, ?>>> supportedTypeDescriptors() {
		return FieldTypeDescriptor.getAll();
	}

	public static List<? extends Arguments> params() {
		return supportedTypeDescriptors().stream()
				.map( Arguments::of )
				.collect( Collectors.toList() );
	}

	@RegisterExtension
	public static final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.ofAdvanced( IndexBinding::new );

	@BeforeAll
	static void setup() {
		setupHelper.start().withIndex( index ).setup();
	}

	/**
	 * Test that DocumentElement.addValue does not throw any exception when passing a non-null value.
	 */
	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void addValue_nonNull(FieldTypeDescriptor<F, ?> fieldType) {
		executeAdd( "1", document -> {
			setNonNullValue( index.binding(), document, fieldType );
		} );
	}

	/**
	 * Test that DocumentElement.addValue does not throw any exception when passing a null value.
	 */
	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void addValue_null(FieldTypeDescriptor<F, ?> fieldType) {
		executeAdd( "1", document -> {
			setNullValue( index.binding(), document, fieldType );
		} );
	}

	/**
	 * Test that DocumentElement.addObject does not throw any exception,
	 * add that DocumentElement.addValue does not throw an exception for returned objects.
	 */
	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void addObject(FieldTypeDescriptor<F, ?> fieldType) {
		assumeTrue( fieldType.isMultivaluable() );
		executeAdd( "1", document -> {
			setNullValue( index.binding(), document, fieldType );

			DocumentElement flattenedObject = document.addObject( index.binding().flattenedObject.self );
			setNonNullValue( index.binding().flattenedObject, flattenedObject, fieldType );
			flattenedObject = document.addObject( index.binding().flattenedObject.self );
			setNullValue( index.binding().flattenedObject, flattenedObject, fieldType );
			DocumentElement flattenedObjectSecondLevelObject =
					flattenedObject.addObject( index.binding().flattenedObject.flattenedObject.self );
			setNonNullValue( index.binding().flattenedObject.flattenedObject, flattenedObjectSecondLevelObject,
					fieldType
			);
			flattenedObjectSecondLevelObject = flattenedObject.addObject( index.binding().flattenedObject.nestedObject.self );
			setNullValue( index.binding().flattenedObject.nestedObject, flattenedObjectSecondLevelObject, fieldType );

			DocumentElement nestedObject = document.addObject( index.binding().nestedObject.self );
			setNonNullValue( index.binding().nestedObject, nestedObject, fieldType );
			nestedObject = document.addObject( index.binding().nestedObject.self );
			setNullValue( index.binding().nestedObject, nestedObject, fieldType );
			DocumentElement nestedObjectSecondLevelObject =
					nestedObject.addObject( index.binding().nestedObject.flattenedObject.self );
			setNonNullValue( index.binding().nestedObject.flattenedObject, nestedObjectSecondLevelObject, fieldType );
			nestedObjectSecondLevelObject = nestedObject.addObject( index.binding().nestedObject.nestedObject.self );
			setNullValue( index.binding().nestedObject.nestedObject, nestedObjectSecondLevelObject, fieldType );
		} );
	}

	/**
	 * Test that DocumentElement.addNullObject does not throw any exception.
	 */
	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void addNullObject(FieldTypeDescriptor<F, ?> fieldType) {
		executeAdd( "1", document -> {
			setNullValue( index.binding(), document, fieldType );

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
	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void addValue_excludedFields(FieldTypeDescriptor<F, ?> fieldType) {
		executeAdd( "1", document -> {
			DocumentElement excludingObject = document.addObject( index.binding().excludingObject.self );
			setNonNullValue( index.binding().excludingObject, excludingObject, fieldType );
			excludingObject = document.addObject( index.binding().excludingObject.self );
			setNullValue( index.binding().excludingObject, excludingObject, fieldType );

			DocumentElement flattenedSecondLevelObject =
					excludingObject.addObject( index.binding().excludingObject.flattenedObject.self );
			setNonNullValue( index.binding().excludingObject.flattenedObject, flattenedSecondLevelObject, fieldType );
			flattenedSecondLevelObject = excludingObject.addObject( index.binding().excludingObject.flattenedObject.self );
			setNullValue( index.binding().excludingObject.flattenedObject, flattenedSecondLevelObject, fieldType );

			DocumentElement nestedSecondLevelObject =
					excludingObject.addObject( index.binding().excludingObject.nestedObject.self );
			setNullValue( index.binding().excludingObject.nestedObject, nestedSecondLevelObject, fieldType );
			nestedSecondLevelObject = excludingObject.addObject( index.binding().excludingObject.nestedObject.self );
			setNullValue( index.binding().excludingObject.nestedObject, nestedSecondLevelObject, fieldType );
		} );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void invalidFieldForDocumentElement_flattenedObjectChild(FieldTypeDescriptor<F, ?> fieldType) {
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

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void invalidFieldForDocumentElement_nestedObjectChild(FieldTypeDescriptor<F, ?> fieldType) {
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

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void invalidFieldForDocumentElement_rootChild(FieldTypeDescriptor<F, ?> fieldType) {
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

	private void setNonNullValue(AbstractObjectBinding binding, DocumentElement document,
			FieldTypeDescriptor<F, ?> fieldType) {
		document.addValue( binding.fieldModels.get( fieldType ).reference,
				fieldType.getIndexableValues().getSingle().get( 0 ) );
	}

	private void setNullValue(AbstractObjectBinding binding, DocumentElement document,
			FieldTypeDescriptor<F, ?> fieldType) {
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
