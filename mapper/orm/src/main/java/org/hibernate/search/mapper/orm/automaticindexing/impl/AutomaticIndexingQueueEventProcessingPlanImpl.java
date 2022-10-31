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
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingQueueEventProcessingPlan;
import org.hibernate.search.mapper.orm.common.EntityReference;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingQueueEventPayload;
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
	public void append(String entityName, String serializedId, PojoIndexingQueueEventPayload payload) {
		delegate.append( entityName, serializedId, payload );
	}

	@Override
	public CompletableFuture<MultiEntityOperationExecutionReport<EntityReference>> executeAndReport(
			OperationSubmitter operationSubmitter) {
		return delegate.executeAndReport( entityReferenceFactory, operationSubmitter );
	}

	@Override
	public String toSerializedId(String entityName, Object identifier) {
		return delegate.toSerializedId( entityName, identifier );
	}

	@Override
	public Object toIdentifier(String entityName, String serializedId) {
		return delegate.toIdentifier( entityName, serializedId );
	}
}
