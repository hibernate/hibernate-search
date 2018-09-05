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

	/**
	 * @return A builder for copying the referenced analyzer, altering some scopes as necessary.
	 */
	CopyBuilder startCopy();

	/**
	 * Interface for building a reference to a scope aware analyzer when bootstrapping.
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

	/**
	 * Interface for building a copy of a reference to a scope aware analyzer at runtime.
	 * <p>
	 * This is mainly used to override analyzers for some scopes.
	 *
	 * @author Guillaume Smet
	 * @author Yoann Rodiere
	 * @hsearch.experimental This type is under active development as part of the Elasticsearch integration. You
	 * should be prepared for incompatible changes in future releases.
	 */
	public interface CopyBuilder {
		void addAnalyzerReference(String scope, AnalyzerReference analyzerReference);

		ScopedAnalyzerReference build();
	}

}
