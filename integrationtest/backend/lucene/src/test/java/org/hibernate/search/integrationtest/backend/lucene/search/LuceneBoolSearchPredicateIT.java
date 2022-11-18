/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.search;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class LuceneBoolSearchPredicateIT {

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@BeforeEach
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

	private static class IndexBinding {
		final IndexFieldReference<String> field;

		IndexBinding(IndexSchemaElement root) {
			field = root.field( "fieldName", c -> c.asString() ).toReference();
		}
	}
}
