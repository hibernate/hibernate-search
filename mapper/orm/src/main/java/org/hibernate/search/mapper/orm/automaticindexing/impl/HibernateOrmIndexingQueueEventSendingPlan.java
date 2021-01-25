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
import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingQueueEventSendingPlan;
import org.hibernate.search.mapper.pojo.route.DocumentRoutesDescriptor;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingQueueEventSendingPlan;

public class HibernateOrmIndexingQueueEventSendingPlan implements PojoIndexingQueueEventSendingPlan {

	private final AutomaticIndexingQueueEventSendingPlan delegate;

	public HibernateOrmIndexingQueueEventSendingPlan(AutomaticIndexingQueueEventSendingPlan delegate) {
		this.delegate = delegate;
	}

	@Override
	public void add(String entityName, Object identifier, String serializedId, DocumentRoutesDescriptor routes) {
		delegate.add( entityName, identifier, serializedId, routes );
	}

	@Override
	public void addOrUpdate(String entityName, Object identifier, String serializedId, DocumentRoutesDescriptor routes) {
		delegate.addOrUpdate( entityName, identifier, serializedId, routes );
	}

	@Override
	public void delete(String entityName, Object identifier, String serializedId, DocumentRoutesDescriptor routes) {
		delegate.delete( entityName, identifier, serializedId, routes );
	}

	@Override
	public void discard() {
		delegate.discard();
	}

	@Override
	public <R> CompletableFuture<MultiEntityOperationExecutionReport<R>> sendAndReport(
			EntityReferenceFactory<R> entityReferenceFactory) {
		return delegate.sendAndReport( entityReferenceFactory );
	}

}
