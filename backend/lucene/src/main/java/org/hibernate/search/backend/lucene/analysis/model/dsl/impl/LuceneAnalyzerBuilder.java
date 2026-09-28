package org.hibernate.search.backend.lucene.analysis.model.dsl.impl;

import org.hibernate.search.backend.lucene.analysis.impl.LuceneAnalysisComponentFactory;

import org.apache.lucene.analysis.Analyzer;

interface LuceneAnalyzerBuilder extends LuceneAnalysisComponentBuilder<Analyzer> {

	@Override
	Analyzer build(LuceneAnalysisComponentFactory factory);

}
