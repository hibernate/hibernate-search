/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.analyzer;

import org.apache.lucene.analysis.util.CharFilterFactory;
import org.hibernate.search.annotations.AnalyzerDef;

/**
 * A token filter factory to use with {@link AnalyzerDef} annotations
 * to define an Elasticsearch char filter.
 * <p>
 * <strong>Caution:</strong> parameter values are interpreted as JSON, though using lenient parsing
 * (quotes around strings may be left out in some cases, as when a string only contains letters).
 * <p>
 * Example:
 * <pre><code>
 * {@literal @}CharFilterDef(
 * 		name = "custom-pattern-replace",
 * 		factory = ElasticsearchCharFilterFactory.class,
 * 		params = {
 * 				{@literal @}Parameter(name = "type", value = "pattern_replace"),
 * 				{@literal @}Parameter(name = "pattern", value = "'[^0-9]'"),
 * 				{@literal @}Parameter(name = "replacement", value = "'0'"),
 * 				{@literal @}Parameter(name = "tags", value = "'CASE_INSENSITIVE|COMMENTS'")
 * 		}
 * )
 * </code></pre>
 *
 * @author Yoann Rodiere
 */
public abstract class ElasticsearchCharFilterFactory extends CharFilterFactory {

	private ElasticsearchCharFilterFactory() {
		super( null );
		// Do not instantiate this type.
	}

}
