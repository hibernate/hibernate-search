/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.integration.impl;


import org.hibernate.search.engine.impl.AnalyzerRegistry;
import org.hibernate.search.engine.impl.NormalizerRegistry;

/**
 * Groups metadata relative to a specific integration (Lucene, Elasticsearch, ...).
 *
 * @author Yoann Rodiere
 */
public interface SearchIntegration {

	AnalyzerRegistry getAnalyzerRegistry();

	NormalizerRegistry getNormalizerRegistry();

	void close();

}
