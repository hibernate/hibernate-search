/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.search;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexFilterReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.factories.FilterFactory;
import org.hibernate.search.engine.search.predicate.factories.FilterFactoryContext;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.SubTest;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class LuceneFilterSearchPredicateIT {

	private static final String INDEX_NAME = "IndexName";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	private StubMappingIndexManager indexManager;

	@Before
	public void setup() {
		setupHelper.start()
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
				.where( f -> {
					return f.bool()
						.minimumShouldMatchNumber( 3 )
						.should( f.def( "match_fieldName" ).param( "match", "blablabla" ) )
						.should( f.def( "nested.match_fieldName" ).param( "match", "blablabla" ) );
				} ).toQuery()
		)
			.assertThrown()
			.isInstanceOf( SearchException.class )
			.hasMessageContaining( "Computed minimum for minimumShouldMatch constraint is out of bounds" );
	}

	private static class IndexMapping {
		final IndexFieldReference<String> field;
		final IndexFieldReference<String> nested_field;
		final IndexFilterReference<TestFilterFactory> filter;
		final IndexFilterReference<TestFilterFactory> nested_filter;

		IndexMapping(IndexSchemaElement root) {
			filter = root.filter( "match_fieldName", new TestFilterFactory() )
				.param( "test", 2 )
				.toReference();

			field = root.field( "fieldName", c -> c.asString() ).toReference();

			IndexSchemaObjectField nest = root.objectField(
				"nested", ObjectFieldStorage.NESTED ).multiValued();

			nested_filter = nest.filter( "match_fieldName", new TestFilterFactory() )
				.param( "other_value", "blablabla" )
				.toReference();

			nested_field = nest.field( "fieldName", c -> c.asString() ).toReference();

			nest.toReference();

		}
	}

	public static class TestFilterFactory implements FilterFactory {

		@Override
		public SearchPredicate create(FilterFactoryContext ctx) {

			SearchPredicate filter;
			String nestedPath = ctx.getNestedPath();
			String fieldPath = ctx.resolvePath( "fieldName" );
			if ( nestedPath != null ) {
				filter = ctx.predicate().nested()
					.objectField( nestedPath )
					.nest( f -> f
					.match().field( fieldPath )
					.matching( ctx.param( "match" ) ) )
					.toPredicate();
			}
			else {
				filter = ctx.predicate()
					.match().field( fieldPath )
					.matching( ctx.param( "match" ) )
					.toPredicate();
			}

			return filter;
		}
	}

}
