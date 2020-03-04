/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.document;

import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.referenceProvider;

import java.util.function.Consumer;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexBindingContext;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingIndexManager;
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test the behavior of implementations of {@link DocumentElement}
 * when it comes to multi-valued fields.
 */
public class DocumentElementMultiValuedIT {

	private static final String INDEX_NAME = "IndexName";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	private IndexMapping indexMapping;
	private StubMappingIndexManager indexManager;

	@Before
	public void setup() {
		setupHelper.start()
				.withIndex(
						INDEX_NAME,
						ctx -> this.indexMapping = new IndexMapping( ctx ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();
	}

	@Test
	public void addValue_root() {
		expectSuccess( "1", document -> {
			document.addValue( indexMapping.singleValuedString, "1" );
		} );
		expectSingleValuedException( "2", "singleValuedString", document -> {
			document.addValue( indexMapping.singleValuedString, "1" );
			document.addValue( indexMapping.singleValuedString, "2" );
		} );
		expectSuccess( "3", document -> {
			document.addValue( indexMapping.multiValuedString, "1" );
		} );
		expectSuccess( "4", document -> {
			document.addValue( indexMapping.multiValuedString, "1" );
			document.addValue( indexMapping.multiValuedString, "2" );
			document.addValue( indexMapping.multiValuedString, "3" );
		} );
	}

	@Test
	public void addObject_flattened() {
		expectSuccess( "1", document -> {
			document.addObject( indexMapping.singleValuedFlattenedObject.self );
		} );
		expectSingleValuedException( "2", "singleValuedFlattenedObject", document -> {
			document.addObject( indexMapping.singleValuedFlattenedObject.self );
			document.addObject( indexMapping.singleValuedFlattenedObject.self );
		} );
		expectSuccess( "3", document -> {
			document.addObject( indexMapping.multiValuedFlattenedObject.self );
		} );
		expectSuccess( "4", document -> {
			document.addObject( indexMapping.multiValuedFlattenedObject.self );
			document.addObject( indexMapping.multiValuedFlattenedObject.self );
			document.addObject( indexMapping.multiValuedFlattenedObject.self );
		} );
	}

	@Test
	public void addObject_nested() {
		expectSuccess( "1", document -> {
			document.addObject( indexMapping.singleValuedNestedObject.self );
		} );
		expectSingleValuedException( "2", "singleValuedNestedObject", document -> {
			document.addObject( indexMapping.singleValuedNestedObject.self );
			document.addObject( indexMapping.singleValuedNestedObject.self );
		} );
		expectSuccess( "3", document -> {
			document.addObject( indexMapping.multiValuedNestedObject.self );
		} );
		expectSuccess( "4", document -> {
			document.addObject( indexMapping.multiValuedNestedObject.self );
			document.addObject( indexMapping.multiValuedNestedObject.self );
			document.addObject( indexMapping.multiValuedNestedObject.self );
		} );
	}

	@Test
	public void addNullObject_flattened() {
		expectSuccess( "1", document -> {
			document.addObject( indexMapping.singleValuedFlattenedObject.self );
		} );
		expectSingleValuedException( "2", "singleValuedFlattenedObject", document -> {
			document.addNullObject( indexMapping.singleValuedFlattenedObject.self );
			document.addNullObject( indexMapping.singleValuedFlattenedObject.self );
		} );
		expectSingleValuedException( "3", "singleValuedFlattenedObject", document -> {
			document.addObject( indexMapping.singleValuedFlattenedObject.self );
			document.addNullObject( indexMapping.singleValuedFlattenedObject.self );
		} );
		expectSingleValuedException( "4", "singleValuedFlattenedObject", document -> {
			document.addNullObject( indexMapping.singleValuedFlattenedObject.self );
			document.addObject( indexMapping.singleValuedFlattenedObject.self );
		} );
		expectSuccess( "5", document -> {
			document.addObject( indexMapping.multiValuedFlattenedObject.self );
		} );
		expectSuccess( "6", document -> {
			document.addNullObject( indexMapping.multiValuedFlattenedObject.self );
			document.addNullObject( indexMapping.multiValuedFlattenedObject.self );
			document.addNullObject( indexMapping.multiValuedFlattenedObject.self );
		} );
		expectSuccess( "7", document -> {
			document.addNullObject( indexMapping.multiValuedFlattenedObject.self );
			document.addObject( indexMapping.multiValuedFlattenedObject.self );
			document.addNullObject( indexMapping.multiValuedFlattenedObject.self );
		} );
		expectSuccess( "8", document -> {
			document.addObject( indexMapping.multiValuedFlattenedObject.self );
			document.addNullObject( indexMapping.multiValuedFlattenedObject.self );
			document.addNullObject( indexMapping.multiValuedFlattenedObject.self );
		} );
	}

	@Test
	public void addNullObject_nested() {
		expectSuccess( "1", document -> {
			document.addObject( indexMapping.singleValuedNestedObject.self );
		} );
		expectSingleValuedException( "2", "singleValuedNestedObject", document -> {
			document.addNullObject( indexMapping.singleValuedNestedObject.self );
			document.addNullObject( indexMapping.singleValuedNestedObject.self );
		} );
		expectSingleValuedException( "3", "singleValuedNestedObject", document -> {
			document.addObject( indexMapping.singleValuedNestedObject.self );
			document.addNullObject( indexMapping.singleValuedNestedObject.self );
		} );
		expectSingleValuedException( "4", "singleValuedNestedObject", document -> {
			document.addNullObject( indexMapping.singleValuedNestedObject.self );
			document.addObject( indexMapping.singleValuedNestedObject.self );
		} );
		expectSuccess( "5", document -> {
			document.addObject( indexMapping.multiValuedNestedObject.self );
		} );
		expectSuccess( "6", document -> {
			document.addNullObject( indexMapping.multiValuedNestedObject.self );
			document.addNullObject( indexMapping.multiValuedNestedObject.self );
			document.addNullObject( indexMapping.multiValuedNestedObject.self );
		} );
		expectSuccess( "7", document -> {
			document.addNullObject( indexMapping.multiValuedNestedObject.self );
			document.addObject( indexMapping.multiValuedNestedObject.self );
			document.addNullObject( indexMapping.multiValuedNestedObject.self );
		} );
		expectSuccess( "8", document -> {
			document.addObject( indexMapping.multiValuedNestedObject.self );
			document.addNullObject( indexMapping.multiValuedNestedObject.self );
			document.addNullObject( indexMapping.multiValuedNestedObject.self );
		} );
	}

	@Test
	public void addValue_inSingleValuedFlattenedObject() {
		expectSuccess( "1", document -> {
			DocumentElement level1 = document.addObject( indexMapping.singleValuedFlattenedObject.self );
			level1.addValue( indexMapping.singleValuedFlattenedObject.singleValuedString, "1" );
		} );
		expectSingleValuedException( "2", "singleValuedFlattenedObject.singleValuedString", document -> {
			DocumentElement level1 = document.addObject( indexMapping.singleValuedFlattenedObject.self );
			level1.addValue( indexMapping.singleValuedFlattenedObject.singleValuedString, "1" );
			level1.addValue( indexMapping.singleValuedFlattenedObject.singleValuedString, "2" );
		} );
		expectSuccess( "3", document -> {
			DocumentElement level1 = document.addObject( indexMapping.singleValuedFlattenedObject.self );
			level1.addValue( indexMapping.singleValuedFlattenedObject.multiValuedString, "1" );
		} );
		expectSuccess( "4", document -> {
			DocumentElement level1 = document.addObject( indexMapping.singleValuedFlattenedObject.self );
			level1.addValue( indexMapping.singleValuedFlattenedObject.multiValuedString, "1" );
			level1.addValue( indexMapping.singleValuedFlattenedObject.multiValuedString, "2" );
			level1.addValue( indexMapping.singleValuedFlattenedObject.multiValuedString, "3" );
		} );
	}

	@Test
	public void addValue_inMultiValuedFlattenedObject() {
		expectSuccess( "1", document -> {
			DocumentElement level1 = document.addObject( indexMapping.multiValuedFlattenedObject.self );
			level1.addValue( indexMapping.multiValuedFlattenedObject.singleValuedString, "1" );
		} );
		expectSingleValuedException( "2", "multiValuedFlattenedObject.singleValuedString", document -> {
			DocumentElement level1 = document.addObject( indexMapping.multiValuedFlattenedObject.self );
			level1.addValue( indexMapping.multiValuedFlattenedObject.singleValuedString, "1" );
			level1.addValue( indexMapping.multiValuedFlattenedObject.singleValuedString, "2" );
		} );
		expectSuccess( "3", document -> {
			DocumentElement level1 = document.addObject( indexMapping.multiValuedFlattenedObject.self );
			level1.addValue( indexMapping.multiValuedFlattenedObject.multiValuedString, "1" );
		} );
		expectSuccess( "4", document -> {
			DocumentElement level1 = document.addObject( indexMapping.multiValuedFlattenedObject.self );
			level1.addValue( indexMapping.multiValuedFlattenedObject.multiValuedString, "1" );
			level1.addValue( indexMapping.multiValuedFlattenedObject.multiValuedString, "2" );
			level1.addValue( indexMapping.multiValuedFlattenedObject.multiValuedString, "3" );
		} );
	}

	@Test
	public void addValue_inSingleValuedNestedObject() {
		expectSuccess( "1", document -> {
			DocumentElement level1 = document.addObject( indexMapping.singleValuedNestedObject.self );
			level1.addValue( indexMapping.singleValuedNestedObject.singleValuedString, "1" );
		} );
		expectSingleValuedException( "2", "singleValuedNestedObject.singleValuedString", document -> {
			DocumentElement level1 = document.addObject( indexMapping.singleValuedNestedObject.self );
			level1.addValue( indexMapping.singleValuedNestedObject.singleValuedString, "1" );
			level1.addValue( indexMapping.singleValuedNestedObject.singleValuedString, "2" );
		} );
		expectSuccess( "3", document -> {
			DocumentElement level1 = document.addObject( indexMapping.singleValuedNestedObject.self );
			level1.addValue( indexMapping.singleValuedNestedObject.multiValuedString, "1" );
		} );
		expectSuccess( "4", document -> {
			DocumentElement level1 = document.addObject( indexMapping.singleValuedNestedObject.self );
			level1.addValue( indexMapping.singleValuedNestedObject.multiValuedString, "1" );
			level1.addValue( indexMapping.singleValuedNestedObject.multiValuedString, "2" );
			level1.addValue( indexMapping.singleValuedNestedObject.multiValuedString, "3" );
		} );
	}

	@Test
	public void addValue_inMultiValuedNestedObject() {
		expectSuccess( "1", document -> {
			DocumentElement level1 = document.addObject( indexMapping.multiValuedNestedObject.self );
			level1.addValue( indexMapping.multiValuedNestedObject.singleValuedString, "1" );
		} );
		expectSingleValuedException( "2", "multiValuedNestedObject.singleValuedString", document -> {
			DocumentElement level1 = document.addObject( indexMapping.multiValuedNestedObject.self );
			level1.addValue( indexMapping.multiValuedNestedObject.singleValuedString, "1" );
			level1.addValue( indexMapping.multiValuedNestedObject.singleValuedString, "2" );
		} );
		expectSuccess( "3", document -> {
			DocumentElement level1 = document.addObject( indexMapping.multiValuedNestedObject.self );
			level1.addValue( indexMapping.multiValuedNestedObject.multiValuedString, "1" );
		} );
		expectSuccess( "4", document -> {
			DocumentElement level1 = document.addObject( indexMapping.multiValuedNestedObject.self );
			level1.addValue( indexMapping.multiValuedNestedObject.multiValuedString, "1" );
			level1.addValue( indexMapping.multiValuedNestedObject.multiValuedString, "2" );
			level1.addValue( indexMapping.multiValuedNestedObject.multiValuedString, "3" );
		} );
	}

	private void expectSuccess(String id, Consumer<DocumentElement> documentContributor) {
		executeAdd( id, documentContributor );
	}

	private void expectSingleValuedException(String id, String absoluteFieldPath, Consumer<DocumentElement> documentContributor) {
		SubTest.expectException(
				"Multiple values written to field '" + absoluteFieldPath + "'",
				() -> executeAdd( id, documentContributor )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Multiple values were added to single-valued field '" + absoluteFieldPath + "'." )
				.hasMessageContaining( "Declare the field as multi-valued in order to allow this." );
	}

	private void executeAdd(String id, Consumer<DocumentElement> documentContributor) {
		IndexIndexingPlan plan = indexManager.createIndexingPlan();
		plan.add( referenceProvider( id ), documentContributor::accept );
		plan.execute().join();
	}

	private abstract static class AbstractObjectMapping {
		final IndexFieldReference<String> singleValuedString;
		final IndexFieldReference<String> multiValuedString;

		AbstractObjectMapping(IndexSchemaElement schemaElement) {
			singleValuedString = schemaElement.field( "singleValuedString", f -> f.asString() ).toReference();
			multiValuedString = schemaElement.field( "multiValuedString", f -> f.asString() ).multiValued().toReference();
		}
	}

	private static class IndexMapping extends AbstractObjectMapping {
		final FirstLevelObjectMapping singleValuedFlattenedObject;
		final FirstLevelObjectMapping multiValuedFlattenedObject;
		final FirstLevelObjectMapping singleValuedNestedObject;
		final FirstLevelObjectMapping multiValuedNestedObject;

		IndexMapping(IndexBindingContext ctx) {
			super( ctx.getSchemaElement() );
			IndexSchemaElement root = ctx.getSchemaElement();

			singleValuedFlattenedObject = new FirstLevelObjectMapping(
					root.objectField( "singleValuedFlattenedObject", ObjectFieldStorage.FLATTENED )
			);
			multiValuedFlattenedObject = new FirstLevelObjectMapping(
					root.objectField( "multiValuedFlattenedObject", ObjectFieldStorage.FLATTENED )
							.multiValued()
			);
			singleValuedNestedObject = new FirstLevelObjectMapping(
					root.objectField( "singleValuedNestedObject", ObjectFieldStorage.NESTED )
			);
			multiValuedNestedObject = new FirstLevelObjectMapping(
					root.objectField( "multiValuedNestedObject", ObjectFieldStorage.NESTED )
							.multiValued()
			);
		}
	}

	private static class FirstLevelObjectMapping extends AbstractObjectMapping {
		final IndexObjectFieldReference self;
		FirstLevelObjectMapping(IndexSchemaObjectField objectField) {
			super( objectField );
			self = objectField.toReference();
		}
	}
}
