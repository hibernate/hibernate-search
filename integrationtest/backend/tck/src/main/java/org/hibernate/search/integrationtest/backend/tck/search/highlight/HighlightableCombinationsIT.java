/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.highlight;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import java.util.Arrays;
import java.util.Collections;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Highlightable;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.Rule;
import org.junit.Test;

public class HighlightableCombinationsIT {

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Test
	public void noWithOtherOptions() {
		class IndexNoPlainBinding {
			final IndexFieldReference<String> stringField;

			IndexNoPlainBinding(IndexSchemaElement root) {
				stringField = root.field( "string", f -> f.asString()
						.highlightable( Arrays.asList( Highlightable.NO, Highlightable.PLAIN ) )
						.analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
				).toReference();
			}
		}

		assertThatThrownBy(
				() -> setupHelper.start().withIndex( SimpleMappedIndex.of( IndexNoPlainBinding::new ) ).setup() )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Cannot use 'NO' in combination with other highlightable values",
						"Applied values are: '[NO, PLAIN]'"
				);

		class IndexNoUnifiedFastVectorBinding {
			final IndexFieldReference<String> stringField;

			IndexNoUnifiedFastVectorBinding(IndexSchemaElement root) {
				stringField = root.field( "string", f -> f.asString()
						.highlightable( Arrays.asList( Highlightable.NO, Highlightable.UNIFIED, Highlightable.FAST_VECTOR ) )
						.analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
				).toReference();
			}
		}

		assertThatThrownBy(
				() -> setupHelper.start().withIndex( SimpleMappedIndex.of( IndexNoUnifiedFastVectorBinding::new ) )
						.setup() )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Cannot use 'NO' in combination with other highlightable values",
						"Applied values are: '[NO, UNIFIED, FAST_VECTOR]'"
				);
	}

	@Test
	public void empty() {
		class IndexBinding {
			final IndexFieldReference<String> stringField;

			IndexBinding(IndexSchemaElement root) {
				stringField = root.field( "string", f -> f.asString()
						.highlightable( Collections.emptyList() )
						.analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
				).toReference();
			}
		}

		assertThatThrownBy(
				() -> setupHelper.start().withIndex( SimpleMappedIndex.of( IndexBinding::new ) ).setup() )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Setting the `highlightable` attribute to an empty array is not supported",
						"Set the value to `NO` if the field does not require the highlight projection"
				);
	}

}
