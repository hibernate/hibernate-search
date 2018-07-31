/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.dsl;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldContext;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldTerminalContext;
import org.hibernate.search.backend.lucene.document.model.LuceneFieldContributor;
import org.hibernate.search.backend.lucene.document.model.LuceneFieldValueExtractor;


/**
 * @author Guillaume Smet
 */
public interface LuceneIndexSchemaFieldContext extends IndexSchemaFieldContext {

	/**
	 * Declares a native field, on which projection is allowed.
	 *
	 * @param fieldContributor The field contributor.
	 * @param fieldValueExtractor The field value extractor used when projecting on this field.
	 * @param <F> The type of the value.
	 * @return The DSL context.
	 */
	<F> IndexSchemaFieldTerminalContext<F> asLuceneField(LuceneFieldContributor<F> fieldContributor,
			LuceneFieldValueExtractor<F> fieldValueExtractor);

	/**
	 * Declares a native field on which projection is not allowed.
	 *
	 * @param fieldContributor The field contributor.
	 * @param <F> The type of the value.
	 * @return The DSL context.
	 */
	default <F> IndexSchemaFieldTerminalContext<F> asLuceneField(LuceneFieldContributor<F> fieldContributor) {
		return asLuceneField( fieldContributor, null );
	}

}
