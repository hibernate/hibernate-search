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
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.jupiter.api.Test;

public abstract class AbstractProjectionInvalidFieldIT {

	private final SimpleMappedIndex<IndexBinding> index;

	protected AbstractProjectionInvalidFieldIT(SimpleMappedIndex<IndexBinding> index) {
		this.index = index;
	}

	@Test
	void use_unknownField() {
		SearchProjectionFactory<?, ?> f = index.createScope().projection();

		assertThatThrownBy( () -> tryProjection( f, "unknown_field" ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" )
				.hasMessageContaining( "'unknown_field'" );
	}

	@Test
	void use_objectField_nested() {
		SearchProjectionFactory<?, ?> f = index.createScope().projection();

		String fieldPath = index.binding().nested.relativeFieldName;

		assertThatThrownBy( () -> tryProjection( f, fieldPath ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Cannot use '" + projectionTrait() + "' on field '" + fieldPath + "'" );
	}

	@Test
	void use_objectField_flattened() {
		SearchProjectionFactory<?, ?> f = index.createScope().projection();

		String fieldPath = index.binding().flattened.relativeFieldName;

		assertThatThrownBy( () -> tryProjection( f, fieldPath ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Cannot use '" + projectionTrait() + "' on field '" + fieldPath + "'" );
	}

	protected abstract void tryProjection(SearchProjectionFactory<?, ?> f, String fieldPath);

	protected abstract String projectionTrait();

	public static final class IndexBinding {
		private final ObjectBinding nested;
		private final ObjectBinding flattened;

		public IndexBinding(IndexSchemaElement root) {
			nested = new ObjectBinding( root, "nested", ObjectStructure.NESTED );
			flattened = new ObjectBinding( root, "flattened", ObjectStructure.FLATTENED );
		}
	}

	private static class ObjectBinding {
		final String relativeFieldName;
		final IndexObjectFieldReference self;

		private ObjectBinding(IndexSchemaElement parent, String relativeFieldName, ObjectStructure structure) {
			this.relativeFieldName = relativeFieldName;
			IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, structure );
			self = objectField.toReference();
		}
	}
}
