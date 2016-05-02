/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.spi;

/**
 * A strategy for applying analyzers.
 *
 * @author Gunnar Morling
 * @hsearch.experimental This type is under active development as part of the addition of the Elasticsearch backend. You
 * should be prepared for incompatible changes in future releases.
 */
public enum AnalyzerExecutionStrategy {

	/**
	 * Analyzers will be applied locally, when talking to an embedded Lucene index.
	 */
	EMBEDDED,

	/**
	 * Analyzers will be applied remotely, e.g. when talking to an instance of Elasticsearch or Solr.
	 */
	REMOTE;
}
