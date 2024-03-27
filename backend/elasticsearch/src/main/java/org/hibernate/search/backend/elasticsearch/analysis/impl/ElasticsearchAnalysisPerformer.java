/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.analysis.impl;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexModel;
import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchParallelWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.factory.impl.ElasticsearchWorkFactory;
import org.hibernate.search.backend.elasticsearch.work.impl.NonBulkableWork;
import org.hibernate.search.engine.backend.analysis.AnalysisToken;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.util.common.AssertionFailure;

public class ElasticsearchAnalysisPerformer {
	private final ElasticsearchIndexModel elasticsearchIndexModel;
	private final ElasticsearchWorkFactory workFactory;
	private final ElasticsearchParallelWorkOrchestrator workOrchestrator;

	public ElasticsearchAnalysisPerformer(ElasticsearchIndexModel elasticsearchIndexModel,
			ElasticsearchWorkFactory workFactory, ElasticsearchParallelWorkOrchestrator workOrchestrator) {
		this.elasticsearchIndexModel = elasticsearchIndexModel;
		this.workFactory = workFactory;
		this.workOrchestrator = workOrchestrator;
	}

	public CompletableFuture<List<? extends AnalysisToken>> analyze(String analyzerName, String terms,
			OperationSubmitter operationSubmitter) {
		return doAnalyze( analyzerName, null, terms, operationSubmitter );
	}

	public CompletableFuture<AnalysisToken> normalize(String normalizerName, String terms,
			OperationSubmitter operationSubmitter) {

		return doAnalyze( null, normalizerName, terms, operationSubmitter )
				.thenApply( tokens -> {
					if ( tokens.size() != 1 ) {
						throw new AssertionFailure( "Applying an normalizer to a string should have produced a single token." +
								" Instead applying " + normalizerName + " to '" + terms + "' produced: " + tokens );
					}
					return tokens.get( 0 );
				} );
	}

	private CompletableFuture<List<? extends AnalysisToken>> doAnalyze(String analyzerName, String normalizerName,
			String string, OperationSubmitter operationSubmitter) {

		URLEncodedString indexName = elasticsearchIndexModel.names().read();
		NonBulkableWork<List<? extends AnalysisToken>> work = workFactory.analyze(
				indexName, string, analyzerName, normalizerName )
				.build();

		return workOrchestrator.submit( work, operationSubmitter );
	}
}
