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
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.GeoPointFieldTypeDescriptor;
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
 * for distance projections.
 * <p>
 * Behavior that is specific to the field type is tested elsewhere,
 * e.g. {@link DistanceSearchProjectionSingleValuedBaseIT} and {@link DistanceSearchProjectionTypeCheckingAndConversionIT}.
 */
public class DistanceSearchProjectionTypeIndependentIT {

	private static final GeoPoint SOME_POINT = GeoPoint.of( 45.749828, 4.854172 );

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
	public void nullCenter() {
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
	public void nullUnit() {
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
	public void objectField_nested() {
		StubMappingScope scope = index.createScope();

		assertThatThrownBy( () -> scope.projection()
				.distance( index.binding().nestedObject.relativeFieldName, SOME_POINT ) )
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
				.distance( index.binding().flattenedObject.relativeFieldName, SOME_POINT ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Unknown field",
						"flattenedObject",
						index.name()
				);
	}

	private static class IndexBinding {
		final SimpleFieldModel<GeoPoint> geoPointField;

		final ObjectMapping flattenedObject;
		final ObjectMapping nestedObject;

		IndexBinding(IndexSchemaElement root) {
			geoPointField = SimpleFieldModel.mapper( GeoPointFieldTypeDescriptor.INSTANCE, c -> c.projectable( Projectable.YES ) )
					.map( root, "geoPoint" );

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
