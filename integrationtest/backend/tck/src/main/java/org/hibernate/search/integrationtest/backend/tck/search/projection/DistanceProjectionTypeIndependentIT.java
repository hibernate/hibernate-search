/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.projection;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.GeoPointFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModel;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
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
	public static final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@BeforeAll
	static void setup() {
		setupHelper.start().withIndex( index ).setup();
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

	private static class IndexBinding {
		final SimpleFieldModel<GeoPoint> geoPointField;


		IndexBinding(IndexSchemaElement root) {
			geoPointField =
					SimpleFieldModel.mapper( GeoPointFieldTypeDescriptor.INSTANCE, c -> c.projectable( Projectable.YES ) )
							.map( root, "geoPoint" );
		}
	}

}
