/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

public class MatchAllPredicateSpecificsIT {

	private static final String DOCUMENT_1 = "1";
	private static final String STRING_1 = "aaa";

	private static final String DOCUMENT_2 = "2";
	private static final String STRING_2 = "bbb";

	private static final String DOCUMENT_3 = "3";
	private static final String STRING_3 = "ccc";

	@ClassRule
	public static final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@BeforeClass
	public static void setup() {
		setupHelper.start().withIndex( index ).setup();

		initData();
	}

	@Test
	public void matchAll() {
		assertThatQuery( index.query()
				.where( f -> f.matchAll() ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );
	}

	@Test
	public void except() {
		assertThatQuery( index.query()
				.where( f -> f.matchAll().except( c2 -> c2.match().field( "string" ).matching( STRING_1 ) ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_2, DOCUMENT_3 );

		SearchPredicateFactory f1 = index.createScope().predicate();
		SearchPredicate searchPredicate = f1.match().field( "string" ).matching( STRING_2 ).toPredicate();

		assertThatQuery( index.query()
				.where( f -> f.matchAll().except( searchPredicate ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_3 );
	}

	@Test
	public void multipleExcepts() {
		assertThatQuery( index.query()
				.where( f -> f.matchAll()
						.except( f.match().field( "string" ).matching( STRING_1 ) )
						.except( f.match().field( "string" ).matching( STRING_2 ) ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_3 );

		SearchPredicateFactory f1 = index.createScope().predicate();
		SearchPredicate searchPredicate1 = f1.match().field( "string" ).matching( STRING_3 ).toPredicate();
		SearchPredicate searchPredicate2 = f1.match().field( "string" ).matching( STRING_2 ).toPredicate();

		assertThatQuery( index.query()
				.where( f -> f.matchAll().except( searchPredicate1 ).except( searchPredicate2 ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );
	}

	private static void initData() {
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
	}

	private static class IndexBinding {
		final IndexFieldReference<String> string;

		IndexBinding(IndexSchemaElement root) {
			string = root.field( "string", f -> f.asString() ).toReference();
		}
	}
}
