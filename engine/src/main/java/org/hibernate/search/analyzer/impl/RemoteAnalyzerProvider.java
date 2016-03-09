/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.analyzer.impl;

/**
 * Applies to index managers that are bound to backends supporting built-in analyzer definitions.
 * <p>
 * For example, Elasticsearch comes with whitespace and languages analyzers without a definition in the classpath.
 *
 * @author Davide D'Alto
 */
public interface RemoteAnalyzerProvider {

	/**
	 * Create an instance of the required analyzer.
	 *
	 * @param name the name of the remote analyzer
	 * @return an instance of the required analyzer.
	 */
	AnalyzerReference getRemoteAnalyzer(String name);
}
