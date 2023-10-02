/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.schema.management.impl;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.reporting.spi.FailureCollector;
import org.hibernate.search.engine.reporting.spi.RootFailureCollector;
import org.hibernate.search.mapper.orm.reporting.impl.HibernateOrmEventContextMessages;
import org.hibernate.search.mapper.orm.schema.management.SearchSchemaManager;
import org.hibernate.search.mapper.pojo.schema.management.SearchSchemaCollector;
import org.hibernate.search.mapper.pojo.schema.management.spi.PojoScopeSchemaManager;
import org.hibernate.search.util.common.function.TriFunction;
import org.hibernate.search.util.common.impl.Futures;

public class SearchSchemaManagerImpl implements SearchSchemaManager {

	private final PojoScopeSchemaManager delegate;

	public SearchSchemaManagerImpl(PojoScopeSchemaManager delegate) {
		this.delegate = delegate;
	}

	@Override
	public void validate() {
		doOperation( PojoScopeSchemaManager::validate );
	}

	@Override
	public void createIfMissing() {
		doOperation( PojoScopeSchemaManager::createIfMissing );
	}

	@Override
	public void createOrValidate() {
		doOperation( PojoScopeSchemaManager::createOrValidate );
	}

	@Override
	public void createOrUpdate() {
		doOperation( PojoScopeSchemaManager::createOrUpdate );
	}

	@Override
	public void dropIfExisting() {
		doOperation( PojoScopeSchemaManager::dropIfExisting );
	}

	@Override
	public void dropAndCreate() {
		doOperation( PojoScopeSchemaManager::dropAndCreate );
	}

	@Override
	public void exportExpectedSchema(SearchSchemaCollector collector) {
		delegate.exportExpectedSchema( collector );
	}

	@Override
	public void exportExpectedSchema(Path targetDirectory) {
		delegate.exportExpectedSchema( targetDirectory );
	}

	private void doOperation(
			TriFunction<PojoScopeSchemaManager, FailureCollector, OperationSubmitter, CompletableFuture<?>> operation) {
		RootFailureCollector failureCollector = new RootFailureCollector(
				HibernateOrmEventContextMessages.INSTANCE.schemaManagement()
		);
		try {
			Futures.unwrappedExceptionJoin( operation.apply( delegate, failureCollector, OperationSubmitter.blocking() ) );
		}
		catch (RuntimeException e) {
			failureCollector.withContext( EventContexts.defaultContext() )
					.add( e );
		}
		failureCollector.checkNoFailure();
	}
}
