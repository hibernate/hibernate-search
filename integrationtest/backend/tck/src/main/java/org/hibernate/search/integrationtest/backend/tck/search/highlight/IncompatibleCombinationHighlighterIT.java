/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.highlight;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.Collections;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Highlightable;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class IncompatibleCombinationHighlighterIT {

	@RegisterExtension
	public static final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	@Test
	void incompatibleHighlightableConfiguration() {
		class IncompatibleTypeIndexBinding {
			final IndexFieldReference<String> stringField;

			IncompatibleTypeIndexBinding(IndexSchemaElement root) {
				stringField = root.field( "string", f -> f.asString()
						.analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
						.highlightable( Arrays.asList( Highlightable.NO, Highlightable.ANY ) ) )
						.toReference();
			}
		}

		SimpleMappedIndex<IncompatibleTypeIndexBinding> index = SimpleMappedIndex.of(
				IncompatibleTypeIndexBinding::new );

		assertThatThrownBy( () -> setupHelper.start().withIndex( index ).setup() )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Cannot use 'NO' in combination with other highlightable values",
						"Applied values are: '[NO, ANY]'"
				);
	}

	@Test
	void incompatibleHighlighter() {
		class IncompatibleTypeIndexBinding {
			final IndexFieldReference<String> stringField;

			IncompatibleTypeIndexBinding(IndexSchemaElement root) {
				stringField = root.field( "string", f -> f.asString()
						.analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
						.highlightable( Collections.singletonList( Highlightable.PLAIN ) ) )
						.toReference();
			}
		}
		SimpleMappedIndex<IncompatibleTypeIndexBinding> index = SimpleMappedIndex.of(
				IncompatibleTypeIndexBinding::new );
		assertThatThrownBy( () -> {
			setupHelper.start().withIndex( index ).setup();

			index.createScope().query()
					.select( f -> f.highlight( "string" ) )
					.where( f -> f.matchAll() )
					.highlighter( h -> h.unified() )
					.toQuery();
		} )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"'UNIFIED' highlighter type cannot be applied to 'string' field",
						"'string' must have either 'ANY' or 'UNIFIED' among the configured highlightable values."
				);
	}
}
