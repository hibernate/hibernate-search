/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.testsupport.junit;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.util.impl.integrationtest.common.stub.backend.BackendMappingHandle;

public class V5MigrationHelperEngineMappingHandle implements BackendMappingHandle {
	@Override
	public CompletableFuture<?> backgroundIndexingCompletion() {
		throw new IllegalStateException( "We never test asynchronous indexing with the migration helper,"
				+ " so this method should never be called." );
	}
}
