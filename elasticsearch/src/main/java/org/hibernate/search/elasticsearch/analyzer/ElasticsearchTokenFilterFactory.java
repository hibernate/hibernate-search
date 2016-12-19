/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.analyzer;

import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.hibernate.search.annotations.AnalyzerDef;

/**
 * A token filter factory to use with {@link AnalyzerDef} annotations
 * to define an Elasticsearch token filter.
 * <p>
 * <strong>Caution:</strong> parameter values are interpreted as JSON, though using lenient parsing
 * (quotes around strings may be left out in some cases, as when a string only contains letters).
 * <p>
 * Example:
 * <pre><code>
 * {@literal @}TokenFilterDef(
 * 		name = "custom-keep-types",
 * 		factory = ElasticsearchTokenFilterFactory.class,
 * 		params = {
 * 					{@literal @}Parameter(name = "type", value = "keep_types"),
 * 					{@literal @}Parameter(name = "types", value = "['{@literal <}NUM{@literal >}','{@literal <}DOUBLE{@literal >}']")
 * 		}
 * )
 * </code></pre>
 */
public abstract class ElasticsearchTokenFilterFactory extends TokenFilterFactory {

	private ElasticsearchTokenFilterFactory() {
		super( null );
		// Do not instantiate this type.
	}

}
