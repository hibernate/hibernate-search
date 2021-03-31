/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TermsPredicateSpecificsIT {

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@Before
	public void before() {
		setupHelper.start().withIndex( index ).setup();
	}

	@Test
	public void emptyTerms_matchingAny() {
		StubMappingScope scope = index.createScope();

		assertThatThrownBy( () -> scope.query()
				.where( f -> f.terms().field( "myField" ).matchingAny( Collections.emptyList() ) )
		)
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "'terms' must not be null or empty" );
	}

	@Test
	public void emptyTerms_matchingAll() {
		StubMappingScope scope = index.createScope();

		assertThatThrownBy( () -> scope.query()
				.where( f -> f.terms().field( "myField" ).matchingAll( Collections.emptyList() ) )
		)
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "'terms' must not be null or empty" );
	}

	private static final class IndexBinding {
		private final IndexFieldReference<String> myField;

		IndexBinding(IndexSchemaElement root) {
			myField = root.field( "myField", f -> f.asString() ).toReference();
		}
	}
}
