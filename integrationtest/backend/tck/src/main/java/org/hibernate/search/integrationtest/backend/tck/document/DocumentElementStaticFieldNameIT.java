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
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.types.dsl.SearchableProjectableIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.common.tree.TreeFilterDefinition;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEmbeddedBindingContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEntityBindingContext;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModel;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModelsByType;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingElement;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test the basic behavior of implementations of {@link DocumentElement}
 * when referencing static fields using their name.
 */
@TestForIssue(jiraKey = "HSEARCH-3273")

class DocumentElementStaticFieldNameIT<F> {

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
			setNonNullValue( document, fieldType );
		} );
	}

	/**
	 * Test that DocumentElement.addValue does not throw any exception when passing a null value.
	 */
	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void addValue_null(FieldTypeDescriptor<F, ?> fieldType) {
		executeAdd( "1", document -> {
			setNullValue( document, fieldType );
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
			setNullValue( document, fieldType );

			DocumentElement flattenedObject = document.addObject( "flattenedObject" );
			setNonNullValue( flattenedObject, fieldType );
			flattenedObject = document.addObject( "flattenedObject" );
			setNullValue( flattenedObject, fieldType );
			DocumentElement flattenedObjectSecondLevelObject =
					flattenedObject.addObject( "flattenedObject" );
			setNonNullValue( flattenedObjectSecondLevelObject, fieldType );
			flattenedObjectSecondLevelObject = flattenedObject.addObject( "nestedObject" );
			setNullValue( flattenedObjectSecondLevelObject, fieldType );

			DocumentElement nestedObject = document.addObject( "nestedObject" );
			setNonNullValue( nestedObject, fieldType );
			nestedObject = document.addObject( "nestedObject" );
			setNullValue( nestedObject, fieldType );
			DocumentElement nestedObjectSecondLevelObject =
					nestedObject.addObject( "flattenedObject" );
			setNonNullValue( nestedObjectSecondLevelObject, fieldType );
			nestedObjectSecondLevelObject = nestedObject.addObject( "nestedObject" );
			setNullValue( nestedObjectSecondLevelObject, fieldType );
		} );
	}

	/**
	 * Test that DocumentElement.addNullObject does not throw any exception.
	 */
	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void addNullObject(FieldTypeDescriptor<F, ?> fieldType) {
		executeAdd( "1", document -> {
			setNullValue( document, fieldType );

			DocumentElement flattenedObject = document.addObject( "flattenedObject" );
			document.addNullObject( "flattenedObject" );
			flattenedObject.addObject( "flattenedObject" );
			flattenedObject.addNullObject( "flattenedObject" );
			flattenedObject.addObject( "nestedObject" );
			flattenedObject.addNullObject( "nestedObject" );

			DocumentElement nestedObject = document.addObject( "nestedObject" );
			document.addNullObject( "nestedObject" );
			nestedObject.addObject( "flattenedObject" );
			nestedObject.addNullObject( "flattenedObject" );
			nestedObject.addObject( "nestedObject" );
			nestedObject.addNullObject( "nestedObject" );
		} );
	}

	/**
	 * Test that DocumentElement.addValue/addObject do not throw any exception when
	 * adding a value to a static field on an object field that excludes all static children
	 * (due to IndexedEmbedded filters).
	 */
	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void add_excludedFields(FieldTypeDescriptor<F, ?> fieldType) {
		executeAdd( "1", document -> {
			DocumentElement excludingObject = document.addObject( "excludingObject" );
			setNonNullValue( excludingObject, fieldType );
			excludingObject = document.addObject( "excludingObject" );
			setNullValue( excludingObject, fieldType );

			DocumentElement flattenedSecondLevelObject =
					excludingObject.addObject( "flattenedObject" );
			setNonNullValue( flattenedSecondLevelObject, fieldType );
			flattenedSecondLevelObject = excludingObject.addObject( "flattenedObject" );
			setNullValue( flattenedSecondLevelObject, fieldType );

			DocumentElement nestedSecondLevelObject = excludingObject.addObject( "nestedObject" );
			setNullValue( nestedSecondLevelObject, fieldType );
			nestedSecondLevelObject = excludingObject.addObject( "nestedObject" );
			setNullValue( nestedSecondLevelObject, fieldType );
		} );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void addValue_unknownField(FieldTypeDescriptor<F, ?> fieldType) {
		assertThatThrownBy( () -> executeAdd( "1", document -> {
			document.addValue( "unknownField", null );
		} ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Unknown field",
						"'unknownField'"
				);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void addObject_unknownField(FieldTypeDescriptor<F, ?> fieldType) {
		assertThatThrownBy( () -> executeAdd( "1", document -> {
			document.addObject( "unknownField" );
		} ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Unknown field",
						"'unknownField'"
				);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void addNullObject_unknownField(FieldTypeDescriptor<F, ?> fieldType) {
		assertThatThrownBy( () -> executeAdd( "1", document -> {
			document.addNullObject( "unknownField" );
		} ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Unknown field",
						"'unknownField'"
				);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void addValue_invalidValueType(FieldTypeDescriptor<F, ?> fieldType) {
		FieldTypeDescriptor<?, ?> invalidType = FieldTypeDescriptor.getIncompatible( fieldType );
		Object valueWithInvalidType = invalidType.getIndexableValues().getSingle().get( 0 );

		SimpleFieldModel<F> fieldModel = index.binding().fieldModels.get( fieldType );

		assertThatThrownBy( () -> executeAdd( "1", document -> {
			document.addValue( fieldModel.relativeFieldName, valueWithInvalidType );
		} ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Invalid value type",
						"This field's values are of type '" + fieldType.getJavaType().getName() + "'",
						"which is not assignable from '" + invalidType.getJavaType().getName() + "'",
						"'" + fieldModel.relativeFieldName + "'"
				);
	}

	private void setNonNullValue(DocumentElement document, FieldTypeDescriptor<F, ?> fieldType) {
		document.addValue( getRelativeFieldName( fieldType ), fieldType.getIndexableValues().getSingle().get( 0 ) );
	}

	private void setNullValue(DocumentElement document, FieldTypeDescriptor<F, ?> fieldType) {
		document.addValue( getRelativeFieldName( fieldType ), null );
	}

	private void executeAdd(String id, Consumer<DocumentElement> documentContributor) {
		index.index( id, documentContributor::accept );
	}

	private String getRelativeFieldName(FieldTypeDescriptor<F, ?> fieldType) {
		// Matches the name defined in AbstractObjectBinding
		return fieldType.getUniqueName();
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
