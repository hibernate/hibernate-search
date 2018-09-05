/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.analyzer;

import org.apache.lucene.analysis.util.TokenizerFactory;
import org.hibernate.search.annotations.AnalyzerDef;

/**
 * A token filter factory to use with {@link AnalyzerDef} annotations
 * to define an Elasticsearch tokenizer.
 * <p>
 * <strong>Caution:</strong> parameter values are interpreted as JSON, though using lenient parsing
 * (quotes around strings may be left out in some cases, as when a string only contains letters).
 * <p>
 * Example:
 * <pre><code>
 * {@literal @}TokenizerDef(
 * 		name = "custom-edgeNGram",
 * 		factory = ElasticsearchTokenizerFactory.class,
 * 		params = {
 * 					{@literal @}Parameter(name = "type", value = "edgeNGram"),
 * 					{@literal @}Parameter(name = "min_gram", value = "1"),
 * 					{@literal @}Parameter(name = "max_gram", value = "10")
 * 		}
 * )
 * </code></pre>
 *
 * @author Yoann Rodiere
 */
public abstract class ElasticsearchTokenizerFactory extends TokenizerFactory {

	private ElasticsearchTokenizerFactory() {
		super( null );
		// Do not instantiate this type.
	}

}
