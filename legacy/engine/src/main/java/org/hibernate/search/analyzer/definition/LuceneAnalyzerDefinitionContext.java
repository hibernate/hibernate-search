/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.analyzer.definition;

import org.apache.lucene.analysis.util.TokenizerFactory;

/**
 * @author Yoann Rodiere
 *
 * @hsearch.experimental The specific API of this DSL is a prototype.
 * Please let us know what you like and what you don't like, and bear in mind
 * that this will likely change in any future version.
 */
public interface LuceneAnalyzerDefinitionContext extends LuceneAnalysisDefinitionRegistryBuilder {

	/**
	 * Set the tokenizer to use.
	 *
	 * @param factory The factory that will create the tokenizer.
	 * @return A context allowing to further define the analyzer.
	 */
	LuceneAnalyzerDefinitionWithTokenizerContext tokenizer(Class<? extends TokenizerFactory> factory);

}
