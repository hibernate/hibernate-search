/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.analyzer.definition;

import org.apache.lucene.analysis.util.CharFilterFactory;
import org.apache.lucene.analysis.util.TokenFilterFactory;

/**
 * @author Yoann Rodiere
 *
 * @hsearch.experimental The specific API of this DSL is a prototype.
 * Please let us know what you like and what you don't like, and bear in mind
 * that this will likely change in any future version.
 */
public interface LuceneCompositeAnalysisDefinitionContext extends LuceneAnalysisDefinitionRegistryBuilder {

	/**
	 * Add a char filter that the analyzer will use.
	 *
	 * @param factory The factory that will create the char filter.
	 * @return A context allowing to further define this analyzer or the char filter.
	 */
	LuceneCharFilterDefinitionContext charFilter(Class<? extends CharFilterFactory> factory);

	/**
	 * Add a token filter that the analyzer will use.
	 *
	 * @param factory The factory that will create the token filter.
	 * @return A context allowing to further define this analyzer or the token filter.
	 */
	LuceneTokenFilterDefinitionContext tokenFilter(Class<? extends TokenFilterFactory> factory);

}
