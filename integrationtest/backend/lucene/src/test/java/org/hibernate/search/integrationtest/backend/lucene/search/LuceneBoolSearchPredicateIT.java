/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class LuceneBoolSearchPredicateIT {

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@Before
	public void setup() {
		setupHelper.start().withIndex( index ).setup();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3535")
	public void minimumShouldMatch_outOfBounds() {
		StubMappingScope scope = index.createScope();

		assertThatThrownBy(
				() -> scope.query()
						.where( f -> f.bool()
								.minimumShouldMatchNumber( 3 )
								.should( f.match().field( "fieldName" ).matching( "blablabla" ) )
						)
						.toQuery(),
				"bool() predicate with a minimumShouldMatch constraint providing an out-of-bounds value"
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Computed minimum for minimumShouldMatch constraint is out of bounds" )
				.hasMessageContaining( "expected a number between '1' and '1', got '3'" );
	}

	@Test
	public void resultingQueryOptimization() {
		SearchPredicateFactory f = index.createScope().predicate();
		assertThat(
				index.query()
						.where( f.bool().must( f.not( f.not( f.not( f.not( f.not( f.not( f.not( f.match().field( "fieldName" ).matching( "test" ) ) ) ) ) ) ) ) ).toPredicate() )
						.toQuery()
						.queryString()
		).isEqualTo( "+(-fieldName:test #*:*)" );

		assertThat(
				index.query()
						.where( f.not( f.bool().must( f.not( f.not( f.not( f.not( f.not( f.not( f.not( f.match().field( "fieldName" ).matching( "test" ) ) ) ) ) ) ) ) ) ).toPredicate() )
						.toQuery()
						.queryString()
		).isEqualTo( "+fieldName:test" );

		assertThat(
				index.query()
						.where( f.bool()
								.must( f.match().field( "fieldName" ).matching( "test1" ) )
								.must( f.not( f.match().field( "fieldName" ).matching( "test2" ) ) )
								.mustNot( f.not( f.match().field( "fieldName" ).matching( "test3" ) ) )
								.mustNot( f.matchNone() )
								.toPredicate()
						)
						.toQuery()
						.queryString()
		).isEqualTo( "+(+fieldName:test1 +fieldName:test3 -MatchNoDocsQuery(\"\") -fieldName:test2)" );
	}

	@Test
	public void resultingQueryOptimizationWithBoost() {
		SearchPredicateFactory f = index.createScope().predicate();
		// by default Lucene bool query would have a filter on match all
		assertThat(
				index.query()
						.where( f.not( f.match().field( "fieldName" ).matching( "test" ) ).toPredicate() )
						.toQuery()
						.queryString()
		).isEqualTo( "+(-fieldName:test #*:*)" );

		// but having boost in the not predicate should result in having a *:* as "must"
		assertThat(
				index.query()
						.where(
								f.not( f.match().field( "fieldName" ).matching( "test" ) ).boost( 5.0F ).toPredicate()
						)
						.toQuery()
						.queryString()
		).isEqualTo( "+(-fieldName:test +*:*)^5.0" );
	}

	private static class IndexBinding {
		final IndexFieldReference<String> field;

		IndexBinding(IndexSchemaElement root) {
			field = root.field( "fieldName", c -> c.asString() ).toReference();
		}
	}
}
