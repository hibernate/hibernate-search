/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.index;

import java.util.List;
import java.util.concurrent.CompletionStage;

import org.hibernate.search.engine.backend.Backend;
import org.hibernate.search.engine.backend.analysis.AnalysisToken;
import org.hibernate.search.engine.backend.metamodel.IndexDescriptor;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * An index manager as viewed by Hibernate Search users.
 * <p>
 * This interface exposes all operations that Hibernate Search users
 * should be able to execute directly on the index manager, without having to go through mapper-specific APIs.
 */
public interface IndexManager {

	/**
	 * @return The backend in which this index manager is defined.
	 */
	Backend backend();

	/**
	 * @return A descriptor of this index, exposing in particular a list of field and their characteristics.
	 */
	IndexDescriptor descriptor();

	/**
	 * Applies the analyzer to a given string to produce a list of tokens.
	 *
	 * @param analyzerName The name of the configured analyzer to apply.
	 * @param terms The string to apply the analysis to.
	 * @return The list of tokens produced by the analyzer for a given string.
	 */
	@Incubating
	List<? extends AnalysisToken> analyze(String analyzerName, String terms);

	/**
	 * Applies the normalizer to a given string to produce a normalized token.
	 *
	 * @param normalizerName The name of the configured normalizer to apply.
	 * @param terms The string to apply the normalizer to.
	 * @return The token produced by the normalizer for a given string.
	 */
	@Incubating
	AnalysisToken normalize(String normalizerName, String terms);

	/**
	 * Applies the analyzer to a given string to produce a list of tokens in an async manner.
	 *
	 * @param analyzerName The name of the configured analyzer to apply.
	 * @param terms The string to apply the analysis to.
	 * @param operationSubmitter How to handle request to submit operation when the queue is full.
	 * @return A future that will ultimately provide the list of tokens produced by the analyzer for a given string.
	 */
	@Incubating
	CompletionStage<List<? extends AnalysisToken>> analyzeAsync(String analyzerName, String terms,
			OperationSubmitter operationSubmitter);

	/**
	 * Applies the normalizer to a given string to produce a normalized token in an async manner.
	 *
	 * @param normalizerName The name of the configured normalizer to apply.
	 * @param terms The string to apply the normalizer to.
	 * @param operationSubmitter How to handle request to submit operation when the queue is full.
	 * @return A future that will ultimately provide the token produced by the normalizer for a given string.
	 */
	@Incubating
	CompletionStage<AnalysisToken> normalizeAsync(String normalizerName, String terms, OperationSubmitter operationSubmitter);

	// TODO HSEARCH-3129 add standard APIs related to statistics?

	/**
	 * Unwrap the index manager to some implementation-specific type.
	 *
	 * @param clazz The {@link Class} representing the expected type
	 * @param <T> The expected type
	 * @return The unwrapped index manager.
	 * @throws SearchException if the index manager implementation does not support
	 * unwrapping to the given class.
	 */
	<T> T unwrap(Class<T> clazz);

}
