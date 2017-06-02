/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.impl;

import java.util.Map;

import org.hibernate.search.analyzer.spi.AnalyzerReference;

/**
 * A registry of normalizers (i.e. analyzers that do not tokenize).
 *
 * @author Yoann Rodiere
 */
public interface NormalizerRegistry {

	AnalyzerReference getNamedNormalizerReference(String name);

	Map<String, AnalyzerReference> getNamedNormalizerReferences();

	AnalyzerReference getLuceneClassNormalizerReference(Class<?> luceneAnalyzerClass);

	Map<Class<?>, AnalyzerReference> getLuceneClassNormalizerReferences();

	/**
	 * Close normalizers if possible
	 */
	void close();

}
