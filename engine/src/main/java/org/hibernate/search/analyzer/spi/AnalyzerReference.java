/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.analyzer.spi;

/**
 * Reference to an analyzer implementation.
 *
 * @author Davide D'Alto
 *
 * @hsearch.experimental This type is under active development as part of the Elasticsearch integration. You
 * should be prepared for incompatible changes in future releases.
 */
public interface AnalyzerReference {

	/**
	 * @return The referenced analyzer.
	 */
	Object getAnalyzer();

	/**
	 * Check if the analyzer can be represented using a specific class.
	 *
	 * @param analyzerType an {@link AnalyzerReference} type
	 * @return true if this implementation can be represented as an instance of T
	 */
	<T extends AnalyzerReference> boolean is(Class<T> analyzerType);

	/**
	 * Convert this instance to T
	 *
	 * @param <T> an {@link AnalyzerReference} type
	 * @param analyzerType the class T
	 * @return this instance as T
	 */
	<T extends AnalyzerReference> T unwrap(Class<T> analyzerType);

	/**
	 * Close analyzer if possible
	 */
	void close();
}
