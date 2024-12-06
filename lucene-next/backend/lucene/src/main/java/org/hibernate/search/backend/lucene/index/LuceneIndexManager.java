/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.index;

import java.util.concurrent.CompletionStage;

import org.hibernate.search.backend.lucene.LuceneBackend;
import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.util.common.annotation.Incubating;

import org.apache.lucene.analysis.Analyzer;

public interface LuceneIndexManager extends IndexManager {

	@Override
	LuceneBackend backend();

	/**
	 * @return The analyzer used when indexing.
	 * This analyzer behaves differently for each field,
	 * delegating to the analyzer configured in the mapping.
	 */
	Analyzer indexingAnalyzer();

	/**
	 * @return The analyzer used in search queries.
	 * This analyzer behaves differently for each field,
	 * delegating to the analyzer configured in the mapping.
	 */
	Analyzer searchAnalyzer();

	/**
	 * @return The size of the index on its storage support, in bytes.
	 */
	long computeSizeInBytes();

	/**
	 * @return A future that will ultimately provide the size of the index on its storage support, in bytes.
	 */
	CompletionStage<Long> computeSizeInBytesAsync();

	/**
	 * @param operationSubmitter How to handle request to submit operation when the queue is full.
	 * @return A future that will ultimately provide the size of the index on its storage support, in bytes.
	 */
	@Incubating
	CompletionStage<Long> computeSizeInBytesAsync(OperationSubmitter operationSubmitter);

}
