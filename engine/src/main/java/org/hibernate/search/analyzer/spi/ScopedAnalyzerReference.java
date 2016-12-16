/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.analyzer.spi;

/**
 * Reference to a scoped analyzer implementation.
 *
 * @author Davide D'Alto
 *
 * @hsearch.experimental This type is under active development as part of the Elasticsearch integration. You
 * should be prepared for incompatible changes in future releases.
 */
public interface ScopedAnalyzerReference extends AnalyzerReference {

	@Override
	ScopedAnalyzer getAnalyzer();

	/**
	 * @return A builder for copying the referenced analyzer, altering some scopes as necessary.
	 */
	Builder startCopy();

	/**
	 * Interface for building scope aware analyzers.
	 *
	 * @author Guillaume Smet
	 * @author Yoann Rodiere
	 * @hsearch.experimental This type is under active development as part of the Elasticsearch integration. You
	 * should be prepared for incompatible changes in future releases.
	 */
	public interface Builder {
		AnalyzerReference getGlobalAnalyzerReference();

		void setGlobalAnalyzerReference(AnalyzerReference globalAnalyzerReference);

		void addAnalyzerReference(String scope, AnalyzerReference analyzerReference);

		ScopedAnalyzerReference build();
	}

}
