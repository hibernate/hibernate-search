/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.projection;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.KeywordStringFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModel;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * Tests related to behavior independent from the field type
 * for projections on field value.
 * <p>
 * Behavior that is specific to the field type is tested elsewhere,
 * e.g. {@link FieldSearchProjectionBaseIT} and {@link FieldSearchProjectionTypeCheckingAndConversionIT}.
 */
public class FieldSearchProjectionTypeIndependentIT {

	@ClassRule
	public static final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@BeforeClass
	public static void setup() {
		setupHelper.start().withIndex( index ).setup();
	}

	@Test
	public void unknownField() {
		StubMappingScope scope = index.createScope();

		assertThatThrownBy( () -> scope.projection()
				.field( "unknownField", Object.class )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Unknown field",
						"unknownField",
						index.name()
				);
	}

	@Test
	public void nullClass() {
		StubMappingScope scope = index.createScope();

		assertThatThrownBy( () -> scope.projection()
				.field( index.binding().string1Field.relativeFieldName, (Class<?>) null )
				.toProjection()
		)
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContainingAll(
						"must not be null",
						"clazz"
				);
	}

	@Test
	public void objectField_nested() {
		StubMappingScope scope = index.createScope();

		assertThatThrownBy( () -> scope.projection()
				.field( index.binding().nestedObject.relativeFieldName, Object.class )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Unknown field",
						"nestedObject",
						index.name()
				);
	}

	@Test
	public void objectField_flattened() {
		StubMappingScope scope = index.createScope();

		assertThatThrownBy( () -> scope.projection()
				.field( index.binding().flattenedObject.relativeFieldName, Object.class )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Unknown field",
						"flattenedObject",
						index.name()
				);
	}

	private static class IndexBinding {
		final SimpleFieldModel<String> string1Field;

		final ObjectMapping flattenedObject;
		final ObjectMapping nestedObject;

		IndexBinding(IndexSchemaElement root) {
			string1Field = SimpleFieldModel.mapper( KeywordStringFieldTypeDescriptor.INSTANCE, c -> { } )
					.map( root, "string1" );

			flattenedObject = new ObjectMapping( root, "flattenedObject", ObjectFieldStorage.FLATTENED );
			nestedObject = new ObjectMapping( root, "nestedObject", ObjectFieldStorage.NESTED );
		}
	}

	private static class ObjectMapping {
		final String relativeFieldName;
		final IndexObjectFieldReference self;

		ObjectMapping(IndexSchemaElement parent, String relativeFieldName, ObjectFieldStorage storage) {
			this.relativeFieldName = relativeFieldName;
			IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, storage );
			self = objectField.toReference();
		}
	}

}
