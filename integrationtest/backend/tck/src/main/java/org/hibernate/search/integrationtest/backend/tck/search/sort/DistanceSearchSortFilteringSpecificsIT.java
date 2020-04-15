/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.sort;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.function.Function;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.SortFinalStep;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * Tests behavior related to
 * {@link org.hibernate.search.engine.search.sort.dsl.SortFilterStep#filter(Function) filtering}
 * that is not tested in {@link DistanceSearchSortBaseIT}.
 */
public class DistanceSearchSortFilteringSpecificsIT {

	private static final String INDEX_NAME = "IndexName";

	@ClassRule
	public static SearchSetupHelper setupHelper = new SearchSetupHelper();

	private static IndexMapping indexMapping;
	private static StubMappingIndexManager indexManager;

	@BeforeClass
	public static void setup() {
		setupHelper.start()
				.withIndex(
						INDEX_NAME,
						ctx -> indexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> DistanceSearchSortFilteringSpecificsIT.indexManager = indexManager
				)
				.setup();
	}

	@Test
	public void nonNested() {
		String fieldPath = indexMapping.flattenedObject.relativeFieldName + ".geoPoint";

		assertThatThrownBy(
				() -> matchAllQuery( f -> f.distance( fieldPath, GeoPoint.of( 42.0, 42.0 ) )
						.filter( pf -> pf.exists().field( fieldPath ) ) )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Field '" + fieldPath + "' is not contained in a nested object.",
						"Sort filters are only available if the field to sort on is contained in a nested object."
				);
	}

	@Test
	public void invalidNestedPath_parent() {
		String fieldPath = indexMapping.nestedObject1.relativeFieldName + ".geoPoint";
		String fieldInParentPath = "geoPoint";

		assertThatThrownBy(
				() -> matchAllQuery( f -> f.distance( fieldPath, GeoPoint.of( 42.0, 42.0 ) )
						.filter( pf -> pf.exists().field( fieldInParentPath ) ) )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Predicate targets unexpected fields [" + fieldInParentPath + "]",
						"Only fields that are contained in the nested object with path '" + indexMapping.nestedObject1.relativeFieldName + "'"
								+ " are allowed here."
				);
	}

	@Test
	public void invalidNestedPath_sibling() {
		String fieldPath = indexMapping.nestedObject1.relativeFieldName + ".geoPoint";
		String fieldInSiblingPath = indexMapping.nestedObject2.relativeFieldName + ".geoPoint";

		assertThatThrownBy(
				() -> matchAllQuery( f -> f.distance( fieldPath, GeoPoint.of( 42.0, 42.0 ) )
						.filter( pf -> pf.exists().field( fieldInSiblingPath ) ) )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Predicate targets unexpected fields [" + fieldInSiblingPath + "]",
						"Only fields that are contained in the nested object with path '" + indexMapping.nestedObject1.relativeFieldName + "'"
								+ " are allowed here."
				);
	}

	private SearchQuery<DocumentReference> matchAllQuery(
			Function<? super SearchSortFactory, ? extends SortFinalStep> sortContributor) {
		return matchAllQuery( sortContributor, indexManager.createScope() );
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

	private static class IndexMapping extends AbstractObjectMapping {
		final FirstLevelObjectMapping flattenedObject;
		final FirstLevelObjectMapping nestedObject1;
		final FirstLevelObjectMapping nestedObject2;

		IndexMapping(IndexSchemaElement root) {
			super( root );

			flattenedObject = FirstLevelObjectMapping.create( root, "flattenedObject",
					ObjectFieldStorage.FLATTENED );
			nestedObject1 = FirstLevelObjectMapping.create( root, "nestedObject1",
					ObjectFieldStorage.NESTED );
			nestedObject2 = FirstLevelObjectMapping.create( root, "nestedObject2",
					ObjectFieldStorage.NESTED );
		}
	}

	private static class FirstLevelObjectMapping extends AbstractObjectMapping {
		final String relativeFieldName;
		final IndexObjectFieldReference self;

		public static FirstLevelObjectMapping create(IndexSchemaElement parent, String relativeFieldName,
				ObjectFieldStorage storage) {
			return create( parent, relativeFieldName, storage, false );
		}

		public static FirstLevelObjectMapping create(IndexSchemaElement parent, String relativeFieldName,
				ObjectFieldStorage storage,
				boolean multiValued) {
			IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, storage );
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
