/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.analysis;

import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * Represents a token produced by the analysis.
 *
 * @see org.hibernate.search.engine.backend.index.IndexManager#analyze(String, String)
 * @see org.hibernate.search.engine.backend.index.IndexManager#analyzeAsync(String, String, OperationSubmitter)
 * @see org.hibernate.search.engine.backend.index.IndexManager#normalize(String, String)
 * @see org.hibernate.search.engine.backend.index.IndexManager#normalizeAsync(String, String, OperationSubmitter)
 */
@Incubating
public interface AnalysisToken {

	/**
	 * @return The text value of a produced token.
	 */
	String term();

	/**
	 * @return Starting offset for this token, i.e. the position of the first character in the source text corresponding to this token.
	 * <p>
	 * Note that the difference between {@link #endOffset() the end} and {@link #startOffset() the start} offsets
	 * may differ from the token's length as some filters may have altered the term text.
	 */
	int startOffset();

	/**
	 * @return Ending offset for this token, i.e. the position of the last character in the source text corresponding to this token.
	 * <p>
	 * Note that the difference between {@link #endOffset() the end} and {@link #startOffset() the start} offsets
	 * may differ from the token's length as some filters may have altered the term text.
	 */
	int endOffset();

	/**
	 * @return The lexical type of this token.
	 * <p>
	 * Defaults to {@code "word"}.
	 */
	String type();

}
