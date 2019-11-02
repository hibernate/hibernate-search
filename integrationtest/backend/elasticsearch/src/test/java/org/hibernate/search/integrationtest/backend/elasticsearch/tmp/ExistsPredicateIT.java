/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.tmp;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchHitsAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import java.util.List;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingScope;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ExistsPredicateIT {

	private static final String INDEX_NAME = "IndexName";

	// this document is empty
	private static final String DOCUMENT_0 = "0";

	// this document has only first level fields: string and numeric
	private static final String DOCUMENT_1 = "1";

	// this document has also an empty nested and an empty flattened objects
	private static final String DOCUMENT_2 = "2";

	// this document has also second level fields:
	// string and numeric within both nested and flattened objects
	private static final String DOCUMENT_3 = "3";

	public static final String ANY_STRING = "Any String";
	public static final int ANY_INTEGER = 173173;

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

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

		initData();
	}

	@Test
	public void matchAll() {
		StubMappingScope scope = indexManager.createScope();

		List<DocumentReference> docs = scope.query().asEntityReference()
				.predicate( p -> p.matchAll() )
				.fetchAllHits();

		assertThat( docs ).hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_0, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );
	}

	@Test
	public void string() {
		StubMappingScope scope = indexManager.createScope();

		List<DocumentReference> docs = scope.query().asEntityReference()
				.predicate( p -> p.exists().field( "string" ) )
				.fetchAllHits();

		assertThat( docs ).hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );
	}

	@Test
	public void numeric() {
		StubMappingScope scope = indexManager.createScope();

		List<DocumentReference> docs = scope.query().asEntityReference()
				.predicate( p -> p.exists().field( "numeric" ) )
				.fetchAllHits();

		assertThat( docs ).hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );
	}

	@Test
	public void nested() {
		StubMappingScope scope = indexManager.createScope();

		List<DocumentReference> docs = scope.query().asEntityReference()
				.predicate( p -> p.nested().objectField( "nested" ).nest( f -> f.exists().field( "nested" ) ) )
				.fetchAllHits();

		// DOCUMENT_2 won't be matched either, since it hasn't any not-null field
		assertThat( docs ).hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_3 );
	}

	@Test
	public void flattened() {
		StubMappingScope scope = indexManager.createScope();

		List<DocumentReference> docs = scope.query().asEntityReference()
				.predicate( p -> p.exists().field( "flattened" ) )
				.fetchAllHits();

		// DOCUMENT_2 won't be matched either, since it hasn't any not-null field
		assertThat( docs ).hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_3 );
	}

	@Test
	public void nestedString() {
		StubMappingScope scope = indexManager.createScope();

		List<DocumentReference> docs = scope.query().asEntityReference()
				.predicate( p -> p.nested().objectField( "nested" ).nest( f -> f.exists().field( "nested.string" ) ) )
				.fetchAllHits();

		assertThat( docs ).hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_3 );
	}

	@Test
	public void nestedNumeric() {
		StubMappingScope scope = indexManager.createScope();

		List<DocumentReference> docs = scope.query().asEntityReference()
				.predicate( p -> p.nested().objectField( "nested" ).nest( f -> f.exists().field( "nested.numeric" ) ) )
				.fetchAllHits();

		assertThat( docs ).hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_3 );
	}

	@Test
	public void flattenedString() {
		StubMappingScope scope = indexManager.createScope();

		List<DocumentReference> docs = scope.query().asEntityReference()
				.predicate( p -> p.exists().field( "flattened.string" ) )
				.fetchAllHits();

		assertThat( docs ).hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_3 );
	}

	@Test
	public void flattenedNumeric() {
		StubMappingScope scope = indexManager.createScope();

		List<DocumentReference> docs = scope.query().asEntityReference()
				.predicate( p -> p.exists().field( "flattened.numeric" ) )
				.fetchAllHits();

		assertThat( docs ).hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_3 );
	}

	private void initData() {
		IndexIndexingPlan<? extends DocumentElement> plan = indexManager.createIndexingPlan();
		plan.add( referenceProvider( DOCUMENT_0 ), document -> { } );

		plan.add( referenceProvider( DOCUMENT_1 ), document -> {
			document.addValue( indexMapping.string, ANY_STRING );
			document.addValue( indexMapping.numeric, ANY_INTEGER );
		} );
		plan.add( referenceProvider( DOCUMENT_2 ), document -> {
			document.addValue( indexMapping.string, ANY_STRING );
			document.addValue( indexMapping.numeric, ANY_INTEGER );

			document.addObject( indexMapping.nested );
			document.addObject( indexMapping.flattened );
		} );
		plan.add( referenceProvider( DOCUMENT_3 ), document -> {
			document.addValue( indexMapping.string, ANY_STRING );
			document.addValue( indexMapping.numeric, ANY_INTEGER );

			DocumentElement nestedDocument = document.addObject( indexMapping.nested );
			nestedDocument.addValue( indexMapping.nestedString, ANY_STRING );
			nestedDocument.addValue( indexMapping.nestedNumeric, ANY_INTEGER );

			DocumentElement flattedDocument = document.addObject( indexMapping.flattened );
			flattedDocument.addValue( indexMapping.flattenedString, ANY_STRING );
			flattedDocument.addValue( indexMapping.flattenedNumeric, ANY_INTEGER );
		} );

		plan.execute().join();
	}

	private static class IndexMapping {
		final IndexFieldReference<String> string;
		final IndexFieldReference<Integer> numeric;

		final IndexObjectFieldReference nested;
		final IndexFieldReference<String> nestedString;
		final IndexFieldReference<Integer> nestedNumeric;

		final IndexObjectFieldReference flattened;
		final IndexFieldReference<String> flattenedString;
		final IndexFieldReference<Integer> flattenedNumeric;

		IndexMapping(IndexSchemaElement root) {
			string = root.field( "string", f -> f.asString() ).toReference();
			numeric = root.field( "numeric", f -> f.asInteger() ).toReference();

			IndexSchemaObjectField nestedObject = root.objectField( "nested", ObjectFieldStorage.NESTED );
			nested = nestedObject.toReference();
			nestedString = nestedObject.field( "string", f -> f.asString() ).toReference();
			nestedNumeric = nestedObject.field( "numeric", f -> f.asInteger() ).toReference();

			IndexSchemaObjectField flattenedObject = root.objectField( "flattened", ObjectFieldStorage.FLATTENED );
			flattened = flattenedObject.toReference();
			flattenedString = flattenedObject.field( "string", f -> f.asString() ).toReference();
			flattenedNumeric = flattenedObject.field( "numeric", f -> f.asInteger() ).toReference();
		}
	}
}
