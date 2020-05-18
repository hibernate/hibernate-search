/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.query.SearchQuery;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class MatchAllSearchPredicateIT {

	private static final String DOCUMENT_1 = "1";
	private static final String STRING_1 = "aaa";

	private static final String DOCUMENT_2 = "2";
	private static final String STRING_2 = "bbb";

	private static final String DOCUMENT_3 = "3";
	private static final String STRING_3 = "ccc";

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@Before
	public void setup() {
		setupHelper.start().withIndex( index ).setup();

		initData();
	}

	@Test
	public void matchAll() {
		StubMappingScope scope = index.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.matchAll() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );
	}

	@Test
	public void matchAll_except() {
		StubMappingScope scope = index.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.matchAll().except( c2 -> c2.match().field( "string" ).matching( STRING_1 ) ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_2, DOCUMENT_3 );

		SearchPredicate searchPredicate = scope.predicate().match().field( "string" ).matching( STRING_2 ).toPredicate();
		query = scope.query()
				.where( f -> f.matchAll().except( searchPredicate ) )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_3 );
	}

	@Test
	public void matchAll_multipleExcepts() {
		StubMappingScope scope = index.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.matchAll()
						.except( f.match().field( "string" ).matching( STRING_1 ) )
						.except( f.match().field( "string" ).matching( STRING_2 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_3 );

		SearchPredicate searchPredicate1 = scope.predicate().match().field( "string" ).matching( STRING_3 ).toPredicate();
		SearchPredicate searchPredicate2 = scope.predicate().match().field( "string" ).matching( STRING_2 ).toPredicate();

		query = scope.query()
				.where( f -> f.matchAll().except( searchPredicate1 ).except( searchPredicate2 ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );
	}

	private void initData() {
		index.bulkIndexer()
				.add( DOCUMENT_1, document -> {
					document.addValue( index.binding().string, STRING_1 );
				} )
				.add( DOCUMENT_2, document -> {
					document.addValue( index.binding().string, STRING_2 );
				} )
				.add( DOCUMENT_3, document -> {
					document.addValue( index.binding().string, STRING_3 );
				} )
				.join();

		// Check that all documents are searchable
		StubMappingScope scope = index.createScope();
		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.matchAll() )
				.toQuery();
		assertThat( query ).hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );
	}

	private static class IndexBinding {
		final IndexFieldReference<String> string;

		IndexBinding(IndexSchemaElement root) {
			string = root.field( "string", f -> f.asString() ).toReference();
		}
	}
}
