/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.schema.management;

import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.schema.management.spi.IndexSchemaManager;
import org.hibernate.search.util.impl.integrationtest.common.stub.StubUnusedContextualFailureCollector;

/**
 * Different operations to be executed on a schema.
 * <p>
 * Used in tests that check side-effects (index creation, ...).
 */
enum LuceneIndexSchemaManagerOperation {

	CREATE_IF_MISSING {
		@Override
		public CompletableFuture<?> apply(IndexSchemaManager schemaManager) {
			return schemaManager.createIfMissing();
		}
	},
	DROP_AND_CREATE {
		@Override
		public CompletableFuture<?> apply(IndexSchemaManager schemaManager) {
			return schemaManager.dropAndCreate();
		}
	},
	CREATE_OR_VALIDATE {
		@Override
		public CompletableFuture<?> apply(IndexSchemaManager schemaManager) {
			return schemaManager.createOrValidate( new StubUnusedContextualFailureCollector() );
		}
	},
	CREATE_OR_UPDATE {
		@Override
		public CompletableFuture<?> apply(IndexSchemaManager schemaManager) {
			return schemaManager.createOrUpdate();
		}
	},
	DROP_IF_EXISTING {
		@Override
		public CompletableFuture<?> apply(IndexSchemaManager schemaManager) {
			return schemaManager.dropIfExisting();
		}
	},
	VALIDATE {
		@Override
		public CompletableFuture<?> apply(IndexSchemaManager schemaManager) {
			return schemaManager.validate( new StubUnusedContextualFailureCollector() );
		}
	};

	public abstract CompletableFuture<?> apply(IndexSchemaManager schemaManager);

	public static EnumSet<LuceneIndexSchemaManagerOperation> creating() {
		return EnumSet.complementOf( EnumSet.of( DROP_IF_EXISTING, VALIDATE ) );
	}

	public static EnumSet<LuceneIndexSchemaManagerOperation> creatingOrPreserving() {
		return EnumSet.of( CREATE_IF_MISSING, CREATE_OR_UPDATE, CREATE_OR_VALIDATE );
	}

}
