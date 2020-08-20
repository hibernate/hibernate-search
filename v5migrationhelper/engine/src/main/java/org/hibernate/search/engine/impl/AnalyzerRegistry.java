/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.impl;

import java.util.Collection;
import java.util.Map;

import org.hibernate.search.analyzer.spi.AnalyzerReference;

/**
 * A registry of analyzers.
 *
 * @author Yoann Rodiere
 */
public interface AnalyzerRegistry {

	AnalyzerReference getDefaultAnalyzerReference();

	AnalyzerReference getPassThroughAnalyzerReference();

	AnalyzerReference getAnalyzerReference(String name);

	Map<String, AnalyzerReference> getNamedAnalyzerReferences();

	AnalyzerReference getAnalyzerReference(Class<?> luceneAnalyzerClass);

	Map<Class<?>, AnalyzerReference> getLuceneClassAnalyzerReferences();

	Collection<AnalyzerReference> getScopedAnalyzerReferences();

	/**
	 * Close analyzers if possible
	 */
	void close();

}
