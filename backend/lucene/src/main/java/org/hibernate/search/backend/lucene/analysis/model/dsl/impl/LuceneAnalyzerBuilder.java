/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.analysis.model.dsl.impl;

import org.hibernate.search.backend.lucene.analysis.impl.LuceneAnalysisComponentFactory;

import org.apache.lucene.analysis.Analyzer;

interface LuceneAnalyzerBuilder extends LuceneAnalysisComponentBuilder<Analyzer> {

	@Override
	Analyzer build(LuceneAnalysisComponentFactory factory);

}
