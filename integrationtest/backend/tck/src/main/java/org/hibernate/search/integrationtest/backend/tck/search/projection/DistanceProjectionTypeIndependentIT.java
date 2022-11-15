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
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.GeoPointFieldTypeDescriptor;
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
 * for distance projections.
 * <p>
 * Behavior that is specific to the field type is tested elsewhere,
 * e.g. {@link DistanceProjectionSingleValuedBaseIT} and {@link DistanceProjectionTypeCheckingAndConversionIT}.
 */
class DistanceProjectionTypeIndependentIT {

	private static final GeoPoint SOME_POINT = GeoPoint.of( 45.749828, 4.854172 );

	@RegisterExtension
	public static final SearchSetupHelper setupHelper = SearchSetupHelper.createGlobal();

	private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@BeforeAll
	static void setup() {
		setupHelper.start().withIndex( index ).setup();
	}

	@Test
	void unknownField() {
		StubMappingScope scope = index.createScope();

		assertThatThrownBy( () -> scope.projection()
				.distance( "unknownField", SOME_POINT )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Unknown field",
						"unknownField",
						index.name()
				);
	}

	@Test
	void nullCenter() {
		StubMappingScope scope = index.createScope();

		assertThatThrownBy( () -> scope.projection()
				.distance( index.binding().geoPointField.relativeFieldName, null )
				.toProjection()
		)
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContainingAll(
						"center",
						"must not be null"
				);
	}

	@Test
	void nullUnit() {
		StubMappingScope scope = index.createScope();

		assertThatThrownBy( () -> scope.projection()
				.distance( index.binding().geoPointField.relativeFieldName, SOME_POINT ).unit( null )
				.toProjection()
		)
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContainingAll(
						"unit",
						"must not be null"
				);
	}

	@Test
	void objectField_nested() {
		String fieldPath = index.binding().nestedObject.relativeFieldName;
		StubMappingScope scope = index.createScope();

		assertThatThrownBy( () -> scope.projection()
				.distance( fieldPath, SOME_POINT ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Cannot use 'projection:distance' on field '" + fieldPath + "'" );
	}

	@Test
	void objectField_flattened() {
		String fieldPath = index.binding().flattenedObject.relativeFieldName;
		StubMappingScope scope = index.createScope();

		assertThatThrownBy( () -> scope.projection()
				.distance( fieldPath, SOME_POINT ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Cannot use 'projection:distance' on field '" + fieldPath + "'" );
	}

	private static class IndexBinding {
		final SimpleFieldModel<GeoPoint> geoPointField;

		final ObjectMapping flattenedObject;
		final ObjectMapping nestedObject;

		IndexBinding(IndexSchemaElement root) {
			geoPointField =
					SimpleFieldModel.mapper( GeoPointFieldTypeDescriptor.INSTANCE, c -> c.projectable( Projectable.YES ) )
							.map( root, "geoPoint" );

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
