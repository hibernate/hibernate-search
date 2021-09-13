/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
