/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.schema.management.spi;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.reporting.spi.FailureCollector;

/**
 * A schema manager for all indexes targeted by a given POJO scope.
 *
 * @see org.hibernate.search.mapper.pojo.scope.spi.PojoScopeDelegate
 * @see org.hibernate.search.engine.backend.schema.management.spi.IndexSchemaManager
 */
public interface PojoScopeSchemaManager {

	CompletableFuture<?> createIfMissing(FailureCollector failureCollector, OperationSubmitter operationSubmitter);

	@Deprecated
	default CompletableFuture<?> createIfMissing(FailureCollector failureCollector) {
		return createIfMissing( failureCollector, OperationSubmitter.BLOCKING );
	}

	CompletableFuture<?> createOrValidate(FailureCollector failureCollector, OperationSubmitter operationSubmitter);

	@Deprecated
	default CompletableFuture<?> createOrValidate(FailureCollector failureCollector) {
		return createOrValidate( failureCollector, OperationSubmitter.BLOCKING );
	}

	CompletableFuture<?> createOrUpdate(FailureCollector failureCollector, OperationSubmitter operationSubmitter);

	@Deprecated
	default CompletableFuture<?> createOrUpdate(FailureCollector failureCollector) {
		return createOrUpdate( failureCollector, OperationSubmitter.BLOCKING );
	}

	CompletableFuture<?> dropAndCreate(FailureCollector failureCollector, OperationSubmitter operationSubmitter);

	@Deprecated
	default CompletableFuture<?> dropAndCreate(FailureCollector failureCollector) {
		return dropAndCreate( failureCollector, OperationSubmitter.BLOCKING );
	}

	CompletableFuture<?> dropIfExisting(FailureCollector failureCollector, OperationSubmitter operationSubmitter);

	@Deprecated
	default CompletableFuture<?> dropIfExisting(FailureCollector failureCollector) {
		return dropIfExisting( failureCollector, OperationSubmitter.BLOCKING );
	}

	CompletableFuture<?> validate(FailureCollector failureCollector, OperationSubmitter operationSubmitter);

	@Deprecated
	default CompletableFuture<?> validate(FailureCollector failureCollector) {
		return validate( failureCollector, OperationSubmitter.BLOCKING );
	}


}
