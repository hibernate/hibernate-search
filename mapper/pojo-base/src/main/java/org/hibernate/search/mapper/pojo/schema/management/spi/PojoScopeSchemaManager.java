/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.schema.management.spi;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.reporting.spi.FailureCollector;

/**
 * A schema manager for all indexes targeted by a given POJO scope.
 *
 * @see org.hibernate.search.mapper.pojo.scope.spi.PojoScopeDelegate
 * @see org.hibernate.search.engine.backend.schema.management.spi.IndexSchemaManager
 */
public interface PojoScopeSchemaManager {

	CompletableFuture<?> createIfMissing(FailureCollector failureCollector);

	CompletableFuture<?> createOrValidate(FailureCollector failureCollector);

	CompletableFuture<?> createOrUpdate(FailureCollector failureCollector);

	CompletableFuture<?> dropAndCreate(FailureCollector failureCollector);

	CompletableFuture<?> dropIfExisting(FailureCollector failureCollector);

	CompletableFuture<?> validate(FailureCollector failureCollector);

}
