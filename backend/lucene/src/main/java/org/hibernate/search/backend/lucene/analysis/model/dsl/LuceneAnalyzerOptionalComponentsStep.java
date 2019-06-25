/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.analysis.model.dsl;

/**
 * The step in an analyzer definition
 * where tokenizer paramaters can be set,
 * and where optional components such as char filters or token filters can be added.
 */
public interface LuceneAnalyzerOptionalComponentsStep extends LuceneAnalysisOptionalComponentsStep {

	/**
	 * Set a tokenizer parameter.
	 *
	 * @param name The name of the parameter.
	 * @param value The value of the parameter.
	 * @return {@code this}, for method chaining.
	 */
	LuceneAnalyzerOptionalComponentsStep param(String name, String value);

}
