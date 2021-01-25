/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.automaticindexing.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.common.spi.EntityReferenceFactory;
import org.hibernate.search.engine.backend.common.spi.MultiEntityOperationExecutionReport;
import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingQueueEventProcessingPlan;
import org.hibernate.search.mapper.orm.common.EntityReference;
import org.hibernate.search.mapper.pojo.route.DocumentRoutesDescriptor;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingQueueEventProcessingPlan;

public class AutomaticIndexingQueueEventProcessingPlanImpl implements AutomaticIndexingQueueEventProcessingPlan {

	private final PojoIndexingQueueEventProcessingPlan delegate;
	private final EntityReferenceFactory<EntityReference> entityReferenceFactory;

	public AutomaticIndexingQueueEventProcessingPlanImpl(PojoIndexingQueueEventProcessingPlan delegate,
			EntityReferenceFactory<EntityReference> entityReferenceFactory) {
		this.delegate = delegate;
		this.entityReferenceFactory = entityReferenceFactory;
	}

	@Override
	public void add(String entityName, String serializedId, DocumentRoutesDescriptor routes) {
		delegate.add( entityName, serializedId, routes );
	}

	@Override
	public void addOrUpdate(String entityName, String serializedId, DocumentRoutesDescriptor routes) {
		delegate.addOrUpdate( entityName, serializedId, routes );
	}

	@Override
	public void delete(String entityName, String serializedId, DocumentRoutesDescriptor routes) {
		delegate.delete( entityName, serializedId, routes );
	}

	@Override
	public CompletableFuture<MultiEntityOperationExecutionReport<EntityReference>> executeAndReport() {
		return delegate.executeAndReport( entityReferenceFactory );
	}
}
