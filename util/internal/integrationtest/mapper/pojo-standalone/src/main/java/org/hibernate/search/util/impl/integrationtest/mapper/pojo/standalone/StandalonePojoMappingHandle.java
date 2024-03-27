/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.util.impl.integrationtest.common.stub.backend.BackendMappingHandle;

public final class StandalonePojoMappingHandle implements BackendMappingHandle {
	@Override
	public CompletableFuture<?> backgroundIndexingCompletion() {
		throw new IllegalStateException( "We never test asynchronous indexing with the Standalone POJO mapper,"
				+ " so this method should never be called." );
	}
}
