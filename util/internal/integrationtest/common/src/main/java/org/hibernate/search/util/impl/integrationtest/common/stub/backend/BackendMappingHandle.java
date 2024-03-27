/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend;

import java.util.concurrent.CompletableFuture;

/**
 * A way for backends to access mapping-level information/features.
 * <p>
 * Only useful in tests, in particular in backend mocks.
 */
public interface BackendMappingHandle {

	/**
	 * @return A future that completes when all indexing works submitted to background executors so far
	 * are completely executed.
	 * Works submitted to the executors after entering this method may delay the wait.
	 */
	CompletableFuture<?> backgroundIndexingCompletion();

}
