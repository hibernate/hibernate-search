/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.sort;

import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Tests related to behavior independent from the field type
 * for sorts by field value.
 * <p>
 * Behavior that is specific to the field type is tested elsewhere,
 * e.g. {@link FieldSearchSortBaseIT} and {@link FieldSearchSortUnsupportedTypesIT}.
 */
public class FieldSearchSortTypeIndependentIT {

	private static final String INDEX_NAME = "IndexName";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private IndexMapping indexMapping;
	private StubMappingIndexManager indexManager;

	@Before
	public void setup() {
		setupHelper.start()
				.withIndex(
						INDEX_NAME,
						ctx -> this.indexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();
	}

	@Test
	public void unknownField() {
		StubMappingScope scope = indexManager.createScope();

		String absoluteFieldPath = "unknownField";

		thrown.expect( SearchException.class );
		thrown.expectMessage( "Unknown field" );
		thrown.expectMessage( absoluteFieldPath );
		thrown.expectMessage( INDEX_NAME );

		scope.query()
				.where( f -> f.matchAll() )
				.sort( f -> f.field( absoluteFieldPath ) )
				.toQuery();
	}

	@Test
	public void objectField_nested() {
		StubMappingScope scope = indexManager.createScope();

		String absoluteFieldPath = indexMapping.nestedObject.relativeFieldName;

		thrown.expect( SearchException.class );
		thrown.expectMessage( "Unknown field" );
		thrown.expectMessage( absoluteFieldPath );
		thrown.expectMessage( INDEX_NAME );

		scope.query()
				.where( f -> f.matchAll() )
				.sort( f -> f.field( absoluteFieldPath ) )
				.toQuery();
	}

	@Test
	public void objectField_flattened() {
		StubMappingScope scope = indexManager.createScope();

		String absoluteFieldPath = indexMapping.flattenedObject.relativeFieldName;

		thrown.expect( SearchException.class );
		thrown.expectMessage( "Unknown field" );
		thrown.expectMessage( absoluteFieldPath );
		thrown.expectMessage( INDEX_NAME );

		scope.query()
				.where( f -> f.matchAll() )
				.sort( f -> f.field( absoluteFieldPath ) )
				.toQuery();
	}

	private static class IndexMapping {
		final ObjectMapping flattenedObject;
		final ObjectMapping nestedObject;

		IndexMapping(IndexSchemaElement root) {
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
