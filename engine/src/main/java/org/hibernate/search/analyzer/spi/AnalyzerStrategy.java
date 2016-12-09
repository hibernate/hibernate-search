/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.analyzer.spi;

import java.util.Collection;
import java.util.Map;

import org.hibernate.search.annotations.AnalyzerDef;

/**
 * A strategy for applying analyzers.
 *
 * @author Gunnar Morling
 * @author Yoann Rodiere
 * @hsearch.experimental This type is under active development as part of the Elasticsearch integration. You
 * should be prepared for incompatible changes in future releases.
 */
public interface AnalyzerStrategy<T extends AnalyzerReference> {

	/**
	 * @return a reference to the default analyzer, the one to be used when no specific configuration is set
	 * on a given field.
	 */
	T createDefaultAnalyzerReference();

	/**
	 * @return a reference to an analyzer that applies no operation whatsoever to the flux.
	 * This is useful for queries operating on non-tokenized fields.
	 */
	T createPassThroughAnalyzerReference();

	/**
	 * @return a reference to an analyzer with the given name.
	 */
	T createAnalyzerReference(String name);

	/**
	 * @return a reference to an instance of the given analyzer class.
	 */
	T createAnalyzerReference(Class<?> analyzerClass);

	/**
	 * Initializes named references created by this strategy, i.e. make them point to the actual analyzer definition.
	 * @param references The references to initialize
	 * @param analyzerDefinitions The analyzer definitions gathered through the Hibernate Search mappings.
	 * It is guaranteed to contain one analyzer definition for each reference name.
	 */
	void initializeNamedAnalyzerReferences(Collection<T> references, Map<String, AnalyzerDef> analyzerDefinitions);

}
