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

class LuceneNormalizerInstanceBuilder implements LuceneAnalyzerBuilder {
	private final String name;
	private final Analyzer instance;

	LuceneNormalizerInstanceBuilder(String name, Analyzer instance) {
		this.name = name;
		Contracts.assertNotNull( instance, "instance" );
		this.instance = instance;
	}

	@Override
	public Analyzer build(LuceneAnalysisComponentFactory factory) {
		return factory.wrapNormalizer( name, instance );
	}
}
