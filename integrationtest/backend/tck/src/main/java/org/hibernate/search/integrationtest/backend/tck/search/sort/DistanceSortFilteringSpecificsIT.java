/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.sort;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.function.Function;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.SortFinalStep;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Tests behavior related to
 * {@link org.hibernate.search.engine.search.sort.dsl.SortFilterStep#filter(Function) filtering}
 * that is not tested in {@link DistanceSortSpecificsIT}.
 */

class DistanceSortFilteringSpecificsIT {

	@RegisterExtension
	public static SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@BeforeAll
	static void setup() {
		setupHelper.start().withIndex( index ).setup();
	}

	@Test
	void nonNested() {
		String fieldPath = index.binding().flattenedObject.relativeFieldName + ".geoPoint";

		assertThatThrownBy(
				() -> matchAllQuery( f -> f.distance( fieldPath, GeoPoint.of( 42.0, 42.0 ) )
						.filter( pf -> pf.exists().field( fieldPath ) ) )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Invalid sort filter: field '" + fieldPath + "' is not contained in a nested object.",
						"Sort filters are only available if the field to sort on is contained in a nested object."
				);
	}

	@Test
	void invalidNestedPath_parent() {
		String fieldPath = index.binding().nestedObject1.relativeFieldName + ".geoPoint";
		String fieldInParentPath = "geoPoint";

		assertThatThrownBy(
				() -> matchAllQuery( f -> f.distance( fieldPath, GeoPoint.of( 42.0, 42.0 ) )
						.filter( pf -> pf.exists().field( fieldInParentPath ) ) )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Invalid search predicate",
						"This predicate targets fields [" + fieldInParentPath + "]",
						"only fields that are contained in the nested object with path '"
								+ index.binding().nestedObject1.relativeFieldName + "'"
								+ " are allowed here." );
	}

	@Test
	void invalidNestedPath_sibling() {
		String fieldPath = index.binding().nestedObject1.relativeFieldName + ".geoPoint";
		String fieldInSiblingPath = index.binding().nestedObject2.relativeFieldName + ".geoPoint";

		assertThatThrownBy(
				() -> matchAllQuery( f -> f.distance( fieldPath, GeoPoint.of( 42.0, 42.0 ) )
						.filter( pf -> pf.exists().field( fieldInSiblingPath ) ) )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Invalid search predicate",
						"This predicate targets fields [" + fieldInSiblingPath + "]",
						"only fields that are contained in the nested object with path '"
								+ index.binding().nestedObject1.relativeFieldName + "'"
								+ " are allowed here." );
	}

	private SearchQuery<DocumentReference> matchAllQuery(
			Function<? super SearchSortFactory, ? extends SortFinalStep> sortContributor) {
		return matchAllQuery( sortContributor, index.createScope() );
	}

	private SearchQuery<DocumentReference> matchAllQuery(
			Function<? super SearchSortFactory, ? extends SortFinalStep> sortContributor, StubMappingScope scope) {
		return scope.query()
				.where( f -> f.matchAll() )
				.sort( sortContributor )
				.toQuery();
	}

	private static class AbstractObjectMapping {
		final IndexFieldReference<GeoPoint> geoPoint;

		AbstractObjectMapping(IndexSchemaElement self) {
			geoPoint = self.field( "geoPoint", f -> f.asGeoPoint().sortable( Sortable.YES ) )
					.toReference();
		}
	}

	private static class IndexBinding extends AbstractObjectMapping {
		final FirstLevelObjectMapping flattenedObject;
		final FirstLevelObjectMapping nestedObject1;
		final FirstLevelObjectMapping nestedObject2;

		IndexBinding(IndexSchemaElement root) {
			super( root );

			flattenedObject = FirstLevelObjectMapping.create( root, "flattenedObject",
					ObjectStructure.FLATTENED );
			nestedObject1 = FirstLevelObjectMapping.create( root, "nestedObject1",
					ObjectStructure.NESTED );
			nestedObject2 = FirstLevelObjectMapping.create( root, "nestedObject2",
					ObjectStructure.NESTED );
		}
	}

	private static class FirstLevelObjectMapping extends AbstractObjectMapping {
		final String relativeFieldName;
		final IndexObjectFieldReference self;

		public static FirstLevelObjectMapping create(IndexSchemaElement parent, String relativeFieldName,
				ObjectStructure structure) {
			return create( parent, relativeFieldName, structure, false );
		}

		public static FirstLevelObjectMapping create(IndexSchemaElement parent, String relativeFieldName,
				ObjectStructure structure,
				boolean multiValued) {
			IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, structure );
			if ( multiValued ) {
				objectField.multiValued();
			}
			return new FirstLevelObjectMapping( relativeFieldName, objectField );
		}

		private FirstLevelObjectMapping(String relativeFieldName, IndexSchemaObjectField objectField) {
			super( objectField );
			this.relativeFieldName = relativeFieldName;
			self = objectField.toReference();
		}
	}
}
