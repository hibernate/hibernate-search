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
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.common.tree.TreeFilterDefinition;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEntityBindingContext;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingElement;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Test the basic behavior of implementations of {@link DocumentElement}
 * when referencing dynamic fields using their name.
 */
@RunWith(Parameterized.class)
@TestForIssue(jiraKey = "HSEARCH-3273")
public class DocumentElementDynamicFieldNameIT<F> {

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

	public DocumentElementDynamicFieldNameIT(FieldTypeDescriptor<F> fieldType) {
		this.fieldType = fieldType;
	}

	/**
	 * Test that DocumentElement.addValue does not throw any exception when passing a non-null value.
	 */
	@Test
	public void addValue_nonNull() {
		executeAdd( "1", document -> {
			setNonNullValue( document );
		} );
	}

	/**
	 * Test that DocumentElement.addValue does not throw any exception when passing a null value.
	 */
	@Test
	public void addValue_null() {
		executeAdd( "1", document -> {
			setNullValue( document );
		} );
	}

	/**
	 * Test that DocumentElement.addObject does not throw any exception,
	 * add that DocumentElement.addValue does not throw an exception for returned objects.
	 */
	@Test
	public void addObject() {
		executeAdd( "1", document -> {
			setNullValue( document );

			DocumentElement flattenedObject = document.addObject( "flattenedObject" );
			setNonNullValue( flattenedObject );
			flattenedObject = document.addObject( "flattenedObject" );
			setNullValue( flattenedObject );
			DocumentElement flattenedObjectSecondLevelObject =
					flattenedObject.addObject( "flattenedObject" );
			setNonNullValue( flattenedObjectSecondLevelObject );
			flattenedObjectSecondLevelObject = flattenedObject.addObject( "nestedObject" );
			setNullValue( flattenedObjectSecondLevelObject );

			DocumentElement nestedObject = document.addObject( "nestedObject" );
			setNonNullValue( nestedObject );
			nestedObject = document.addObject( "nestedObject" );
			setNullValue( nestedObject );
			DocumentElement nestedObjectSecondLevelObject =
					nestedObject.addObject( "flattenedObject" );
			setNonNullValue( nestedObjectSecondLevelObject );
			nestedObjectSecondLevelObject = nestedObject.addObject( "nestedObject" );
			setNullValue( nestedObjectSecondLevelObject );
		} );
	}

	/**
	 * Test that DocumentElement.addNullObject does not throw any exception.
	 */
	@Test
	public void addNullObject() {
		executeAdd( "1", document -> {
			setNullValue( document );

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
	 * adding a value to a dynamic field on an object field that excludes all static children
	 * (due to IndexedEmbedded filters).
	 */
	@Test
	public void add_excludedFields() {
		executeAdd( "1", document -> {
			DocumentElement excludingObject = document.addObject( "excludingObject" );
			setNonNullValue( excludingObject );
			excludingObject = document.addObject( "excludingObject" );
			setNullValue( excludingObject );

			DocumentElement flattenedSecondLevelObject =
					excludingObject.addObject( "flattenedObject" );
			setNonNullValue( flattenedSecondLevelObject );
			flattenedSecondLevelObject = excludingObject.addObject( "flattenedObject" );
			setNullValue( flattenedSecondLevelObject );

			DocumentElement nestedSecondLevelObject = excludingObject.addObject( "nestedObject" );
			setNullValue( nestedSecondLevelObject );
			nestedSecondLevelObject = excludingObject.addObject( "nestedObject" );
			setNullValue( nestedSecondLevelObject );
		} );
	}

	@Test
	public void addValue_unknownField() {
		assertThatThrownBy( () -> executeAdd( "1", document -> {
			document.addValue( "unknownField", null );
		} ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Unknown field",
						"'unknownField'"
				);
	}

	@Test
	public void addObject_unknownField() {
		assertThatThrownBy( () -> executeAdd( "1", document -> {
			document.addObject( "unknownField" );
		} ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Unknown field",
						"'unknownField'"
				);
	}

	@Test
	public void addNullObject_unknownField() {
		assertThatThrownBy( () -> executeAdd( "1", document -> {
			document.addNullObject( "unknownField" );
		} ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Unknown field",
						"'unknownField'"
				);
	}

	@Test
	public void addValue_invalidType() {
		FieldTypeDescriptor<?> invalidType = FieldTypeDescriptor.getIncompatible( fieldType );
		Object valueWithInvalidType = invalidType.getIndexableValues().getSingle().get( 0 );

		String relativeFieldName = getRelativeFieldName();

		assertThatThrownBy( () -> executeAdd( "1", document -> {
			document.addValue( relativeFieldName, valueWithInvalidType );
		} ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Invalid value type",
						"This field's values are of type '" + fieldType.getJavaType().getName() + "'",
						"which is not assignable from '" + invalidType.getJavaType().getName() + "'",
						"'" + relativeFieldName + "'"
				);
	}

	private void setNonNullValue(DocumentElement document) {
		document.addValue( getRelativeFieldName(), fieldType.getIndexableValues().getSingle().get( 0 ) );
	}

	private void setNullValue(DocumentElement document) {
		document.addValue( getRelativeFieldName(), null );
	}

	private void executeAdd(String id, Consumer<DocumentElement> documentContributor) {
		index.index( id, documentContributor::accept );
	}

	private String getRelativeFieldName() {
		// Matches the template defined in IndexBinding
		return "foo_" + fieldType.getUniqueName();
	}

	private static class IndexBinding {
		IndexBinding(IndexedEntityBindingContext ctx) {
			IndexSchemaElement root = ctx.schemaElement();
			supportedTypeDescriptors().forEach( fieldType -> {
				root.fieldTemplate( fieldType.getUniqueName(), f -> fieldType.configure( f ) )
						.matchingPathGlob( "*_" + fieldType.getUniqueName() );
			} );
			root.objectFieldTemplate( "flattenedObject_direct", ObjectStructure.FLATTENED )
					.matchingPathGlob( "flattenedObject" )
					.multiValued();
			root.objectFieldTemplate( "flattenedObject_indirect", ObjectStructure.FLATTENED )
					.matchingPathGlob( "*.flattenedObject" )
					.multiValued();
			root.objectFieldTemplate( "nestedObject_direct", ObjectStructure.NESTED )
					.matchingPathGlob( "nestedObject" )
					.multiValued();
			root.objectFieldTemplate( "nestedObject_indirect", ObjectStructure.NESTED )
					.matchingPathGlob( "*.nestedObject" )
					.multiValued();

			// Simulate an embedded context which excludes every subfield
			TreeFilterDefinition filterDefinition =
					new TreeFilterDefinition( null,
							Collections.singleton( "pathThatDoesNotMatchAnything" ), Collections.emptySet() );
			// Ignore the result, we'll just reference "excludingObject" by its name
			ctx.addIndexedEmbeddedIfIncluded( new StubMappingElement(),
					"excludingObject.", ObjectStructure.FLATTENED, filterDefinition, true ).get();
		}
	}
}
