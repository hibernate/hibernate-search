/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.analyzer.impl;

import org.hibernate.search.analyzer.spi.AnalyzerReference;

/**
 * General interface for scope aware analyzers.
 *
 * @author Guillaume Smet
 */
public interface ScopedAnalyzer extends Cloneable {
	void setGlobalAnalyzerReference(AnalyzerReference globalAnalyzerReference);

	void addScopedAnalyzerReference(String scope, AnalyzerReference analyzerReference);

	ScopedAnalyzer clone();

	void close();
}
