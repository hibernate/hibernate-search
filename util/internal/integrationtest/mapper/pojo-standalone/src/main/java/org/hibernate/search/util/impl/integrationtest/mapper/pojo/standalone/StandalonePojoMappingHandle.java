/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.util.impl.integrationtest.common.stub.backend.BackendMappingHandle;

final class StandalonePojoMappingHandle implements BackendMappingHandle {
	@Override
	public CompletableFuture<?> backgroundIndexingCompletion() {
		throw new IllegalStateException( "We never test asynchronous indexing with the Standalone POJO mapper,"
				+ " so this method should never be called." );
	}
}
