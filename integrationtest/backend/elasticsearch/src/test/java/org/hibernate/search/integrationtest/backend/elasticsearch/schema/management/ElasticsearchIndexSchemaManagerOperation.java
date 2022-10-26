/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.schema.management;

import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.schema.management.spi.IndexSchemaManager;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.util.impl.integrationtest.common.stub.StubUnusedContextualFailureCollector;

/**
 * Different operations to be executed on a schema.
 * <p>
 * Used in tests that check side-effects (index creation, ...).
 * Not useful when testing validation in depth, for example, since the failure collector is stubbed.
 */
enum ElasticsearchIndexSchemaManagerOperation {

	CREATE_IF_MISSING {
		@Override
		public CompletableFuture<?> apply(IndexSchemaManager schemaManager) {
			return schemaManager.createIfMissing( OperationSubmitter.BLOCKING );
		}
	},
	DROP_AND_CREATE {
		@Override
		public CompletableFuture<?> apply(IndexSchemaManager schemaManager) {
			return schemaManager.dropAndCreate( OperationSubmitter.BLOCKING );
		}
	},
	CREATE_OR_VALIDATE {
		@Override
		public CompletableFuture<?> apply(IndexSchemaManager schemaManager) {
			return schemaManager.createOrValidate( new StubUnusedContextualFailureCollector(), OperationSubmitter.BLOCKING );
		}
	},
	CREATE_OR_UPDATE {
		@Override
		public CompletableFuture<?> apply(IndexSchemaManager schemaManager) {
			return schemaManager.createOrUpdate( OperationSubmitter.BLOCKING );
		}
	},
	DROP_IF_EXISTING {
		@Override
		public CompletableFuture<?> apply(IndexSchemaManager schemaManager) {
			return schemaManager.dropIfExisting( OperationSubmitter.BLOCKING );
		}
	},
	VALIDATE {
		@Override
		public CompletableFuture<?> apply(IndexSchemaManager schemaManager) {
			return schemaManager.validate( new StubUnusedContextualFailureCollector(), OperationSubmitter.BLOCKING );
		}
	};

	public abstract CompletableFuture<?> apply(IndexSchemaManager schemaManager);

	public static EnumSet<ElasticsearchIndexSchemaManagerOperation> creating() {
		return EnumSet.complementOf( EnumSet.of( DROP_IF_EXISTING, VALIDATE ) );
	}

	public static EnumSet<ElasticsearchIndexSchemaManagerOperation> creatingOrPreserving() {
		return EnumSet.of( CREATE_IF_MISSING, CREATE_OR_VALIDATE );
	}

	public static EnumSet<ElasticsearchIndexSchemaManagerOperation> dropping() {
		return EnumSet.of( DROP_IF_EXISTING, DROP_AND_CREATE );
	}

	public static EnumSet<ElasticsearchIndexSchemaManagerOperation> aliasInspecting() {
		return EnumSet.allOf( ElasticsearchIndexSchemaManagerOperation.class );
	}

	public static EnumSet<ElasticsearchIndexSchemaManagerOperation> statusChecking() {
		return EnumSet.complementOf(
				EnumSet.of( DROP_IF_EXISTING )
		);
	}

}
