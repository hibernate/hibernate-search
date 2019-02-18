/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.analysis.model.dsl.impl;

import org.hibernate.search.backend.lucene.analysis.impl.LuceneAnalysisComponentFactory;
import org.hibernate.search.util.common.impl.Contracts;

import org.apache.lucene.analysis.Analyzer;

class LuceneAnalyzerInstanceContext implements LuceneAnalyzerBuilder {
	private final Analyzer instance;

	LuceneAnalyzerInstanceContext(Analyzer instance) {
		Contracts.assertNotNull( instance, "instance" );
		this.instance = instance;
	}

	@Override
	public Analyzer build(LuceneAnalysisComponentFactory factory) {
		return instance;
	}
}
