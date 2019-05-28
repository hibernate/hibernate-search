/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.search;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingScope;
import org.hibernate.search.util.impl.test.SubTest;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class LuceneBoolSearchPredicateIT {

	private static final String INDEX_NAME = "IndexName";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	private StubMappingIndexManager indexManager;

	@Before
	public void setup() {
		setupHelper.withDefaultConfiguration()
				.withIndex(
						INDEX_NAME,
						ctx -> new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3535")
	public void minimumShouldMatch_outOfBounds() {
		StubMappingScope scope = indexManager.createScope();

		SubTest.expectException(
				"bool() predicate with a minimumShouldMatch constraint providing an out-of-bounds value",
				() -> scope.query()
						.predicate( f -> f.bool()
								.minimumShouldMatchNumber( 3 )
								.should( f.match().onField( "fieldName" ).matching( "blablabla" ) )
						)
						.toQuery()
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Computed minimum for minimumShouldMatch constraint is out of bounds" )
				.hasMessageContaining( "expected a number between '1' and '1', got '3'" );
	}

	private static class IndexMapping {
		final IndexFieldReference<String> field;

		IndexMapping(IndexSchemaElement root) {
			field = root.field( "fieldName", c -> c.asString() ).toReference();
		}
	}
}
