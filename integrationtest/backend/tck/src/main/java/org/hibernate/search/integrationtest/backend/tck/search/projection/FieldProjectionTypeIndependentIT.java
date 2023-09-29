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
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.KeywordStringFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModel;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Tests related to behavior independent from the field type
 * for projections on field value.
 * <p>
 * Behavior that is specific to the field type is tested elsewhere,
 * e.g. {@link FieldProjectionSingleValuedBaseIT} and {@link FieldProjectionTypeCheckingAndConversionIT}.
 */

class FieldProjectionTypeIndependentIT {

	@RegisterExtension
	public static final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@BeforeAll
	static void setup() {
		setupHelper.start().withIndex( index ).setup();
	}

	@Test
	void unknownField() {
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
	void nullClass() {
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
	void objectField_nested() {
		String fieldPath = index.binding().nestedObject.relativeFieldName;
		StubMappingScope scope = index.createScope();

		assertThatThrownBy( () -> scope.projection()
				.field( fieldPath, Object.class )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Cannot use 'projection:field' on field '" + fieldPath + "'" );
	}

	@Test
	void objectField_flattened() {
		String fieldPath = index.binding().flattenedObject.relativeFieldName;
		StubMappingScope scope = index.createScope();

		assertThatThrownBy( () -> scope.projection()
				.field( fieldPath, Object.class )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Cannot use 'projection:field' on field '" + fieldPath + "'" );
	}

	private static class IndexBinding {
		final SimpleFieldModel<String> string1Field;

		final ObjectMapping flattenedObject;
		final ObjectMapping nestedObject;

		IndexBinding(IndexSchemaElement root) {
			string1Field = SimpleFieldModel.mapper( KeywordStringFieldTypeDescriptor.INSTANCE, c -> {} )
					.map( root, "string1" );

			flattenedObject = new ObjectMapping( root, "flattenedObject", ObjectStructure.FLATTENED );
			nestedObject = new ObjectMapping( root, "nestedObject", ObjectStructure.NESTED );
		}
	}

	private static class ObjectMapping {
		final String relativeFieldName;
		final IndexObjectFieldReference self;

		ObjectMapping(IndexSchemaElement parent, String relativeFieldName, ObjectStructure structure) {
			this.relativeFieldName = relativeFieldName;
			IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, structure );
			self = objectField.toReference();
		}
	}

}
