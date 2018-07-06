/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.dsl;

import java.util.function.BiFunction;
import java.util.function.Function;

import org.apache.lucene.index.IndexableField;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldContext;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldTerminalContext;


/**
 * @author Guillaume Smet
 */
public interface LuceneIndexSchemaFieldContext extends IndexSchemaFieldContext {

	/**
	 * Declares a native field.
	 *
	 * @param fieldProducer A bifunction producing a field. The first parameter is the absolute path of the field, the second one is the value.
	 * @param fieldValueExtractor A function responsible for extracting the value from the field. The parameter is the Lucene field itself.
	 * @param <V> The type of the value.
	 * @return The DSL context.
	 */
	<V> IndexSchemaFieldTerminalContext<V> asLuceneField(BiFunction<String, V, IndexableField> fieldProducer,
			Function<IndexableField, V> fieldValueExtractor);

}
