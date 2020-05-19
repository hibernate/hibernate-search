/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.schema.management.impl;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.reporting.spi.FailureCollector;
import org.hibernate.search.engine.reporting.spi.RootFailureCollector;
import org.hibernate.search.mapper.orm.logging.impl.HibernateOrmEventContextMessages;
import org.hibernate.search.mapper.orm.schema.management.SearchSchemaManager;
import org.hibernate.search.mapper.pojo.schema.management.spi.PojoScopeSchemaManager;
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

	private void doOperation(BiFunction<PojoScopeSchemaManager, FailureCollector, CompletableFuture<?>> operation) {
		RootFailureCollector failureCollector = new RootFailureCollector(
				HibernateOrmEventContextMessages.INSTANCE.schemaManagement()
		);
		try {
			Futures.unwrappedExceptionJoin( operation.apply( delegate, failureCollector ) );
		}
		catch (RuntimeException e) {
			failureCollector.withContext( EventContexts.defaultContext() )
					.add( e );
		}
		failureCollector.checkNoFailure();
	}
}
