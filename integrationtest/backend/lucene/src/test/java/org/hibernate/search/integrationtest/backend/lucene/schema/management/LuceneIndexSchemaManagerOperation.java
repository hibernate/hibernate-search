/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.lucene.schema.management;

import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.schema.management.spi.IndexSchemaManager;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.util.impl.integrationtest.common.stub.StubUnusedContextualFailureCollector;

/**
 * Different operations to be executed on a schema.
 * <p>
 * Used in tests that check side-effects (index creation, ...).
 */
enum LuceneIndexSchemaManagerOperation {

	CREATE_IF_MISSING {
		@Override
		public CompletableFuture<?> apply(IndexSchemaManager schemaManager, OperationSubmitter operationSubmitter) {
			return schemaManager.createIfMissing( operationSubmitter );
		}
	},
	DROP_AND_CREATE {
		@Override
		public CompletableFuture<?> apply(IndexSchemaManager schemaManager, OperationSubmitter operationSubmitter) {
			return schemaManager.dropAndCreate( operationSubmitter );
		}
	},
	CREATE_OR_VALIDATE {
		@Override
		public CompletableFuture<?> apply(IndexSchemaManager schemaManager, OperationSubmitter operationSubmitter) {
			return schemaManager.createOrValidate( new StubUnusedContextualFailureCollector(), operationSubmitter );
		}
	},
	CREATE_OR_UPDATE {
		@Override
		public CompletableFuture<?> apply(IndexSchemaManager schemaManager, OperationSubmitter operationSubmitter) {
			return schemaManager.createOrUpdate( operationSubmitter );
		}
	},
	DROP_IF_EXISTING {
		@Override
		public CompletableFuture<?> apply(IndexSchemaManager schemaManager, OperationSubmitter operationSubmitter) {
			return schemaManager.dropIfExisting( operationSubmitter );
		}
	},
	VALIDATE {
		@Override
		public CompletableFuture<?> apply(IndexSchemaManager schemaManager, OperationSubmitter operationSubmitter) {
			return schemaManager.validate( new StubUnusedContextualFailureCollector(), operationSubmitter );
		}
	};

	public abstract CompletableFuture<?> apply(IndexSchemaManager schemaManager, OperationSubmitter operationSubmitter);

	public static EnumSet<LuceneIndexSchemaManagerOperation> creating() {
		return EnumSet.complementOf( EnumSet.of( DROP_IF_EXISTING, VALIDATE ) );
	}

	public static EnumSet<LuceneIndexSchemaManagerOperation> creatingOrPreserving() {
		return EnumSet.of( CREATE_IF_MISSING, CREATE_OR_UPDATE, CREATE_OR_VALIDATE );
	}

}
