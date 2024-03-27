/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.orchestration.spi;

/**
 * A work ready to be submitted to a {@link BatchingExecutor},
 * and eventually processed in a batch by a {@link BatchedWorkProcessor}.
 *
 * @param <P> The type of processor this work can be submitted to.
 *
 * @see BatchingExecutor
 */
public interface BatchedWork<P> {

	void submitTo(P processor);

	void markAsFailed(Throwable t);

}
