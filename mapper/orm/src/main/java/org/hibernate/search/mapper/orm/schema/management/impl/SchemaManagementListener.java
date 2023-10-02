/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.schema.management.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.mapper.mapping.spi.MappingPreStopContext;
import org.hibernate.search.engine.mapper.mapping.spi.MappingStartContext;
import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;
import org.hibernate.search.mapper.orm.schema.management.SchemaManagementStrategyName;
import org.hibernate.search.mapper.pojo.schema.management.spi.PojoScopeSchemaManager;
import org.hibernate.search.util.common.AssertionFailure;

public class SchemaManagementListener {

	private final SchemaManagementStrategyName strategyName;

	public SchemaManagementListener(SchemaManagementStrategyName strategyName) {
		this.strategyName = strategyName;
	}

	public CompletableFuture<?> onStart(MappingStartContext context, PojoScopeSchemaManager manager) {
		ContextualFailureCollector failureCollector = context.failureCollector();
		switch ( strategyName ) {
			case CREATE:
				return manager.createIfMissing( failureCollector, OperationSubmitter.blocking() );
			case DROP_AND_CREATE:
			case DROP_AND_CREATE_AND_DROP:
				return manager.dropAndCreate( failureCollector, OperationSubmitter.blocking() );
			case CREATE_OR_UPDATE:
				return manager.createOrUpdate( failureCollector, OperationSubmitter.blocking() );
			case CREATE_OR_VALIDATE:
				return manager.createOrValidate( failureCollector, OperationSubmitter.blocking() );
			case VALIDATE:
				return manager.validate( failureCollector, OperationSubmitter.blocking() );
			case NONE:
				// Nothing to do
				return CompletableFuture.completedFuture( null );
			default:
				throw new AssertionFailure( "Unexpected schema management strategy: " + strategyName );
		}
	}

	public CompletableFuture<?> onStop(MappingPreStopContext context, PojoScopeSchemaManager manager) {
		ContextualFailureCollector failureCollector = context.failureCollector();
		switch ( strategyName ) {
			case DROP_AND_CREATE_AND_DROP:
				return manager.dropIfExisting( failureCollector, OperationSubmitter.blocking() );
			case CREATE:
			case DROP_AND_CREATE:
			case CREATE_OR_UPDATE:
			case CREATE_OR_VALIDATE:
			case VALIDATE:
			case NONE:
				// Nothing to do
				return CompletableFuture.completedFuture( null );
			default:
				throw new AssertionFailure( "Unexpected schema management strategy: " + strategyName );
		}
	}

}
