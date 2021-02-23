/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.mapper.orm.automaticindexing;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.orchestration.spi.BatchedWork;

public class LocalHeapQueueIndexingEvent implements BatchedWork<LocalHeapQueueProcessor> {
	final Type eventType;
	final String entityName;
	final transient Object identifier;
	final String serializedId;
	final byte[] routes;
	final CompletableFuture<?> future;

	public LocalHeapQueueIndexingEvent(Type eventType, String entityName, Object identifier, String serializedId,
			byte[] routes) {
		this.eventType = eventType;
		this.entityName = entityName;
		this.identifier = identifier;
		this.serializedId = serializedId;
		this.routes = routes;
		future = new CompletableFuture<>();
	}

	@Override
	public String toString() {
		return "LocalHeapQueueIndexingEvent{" +
				"eventType=" + eventType +
				", entityName='" + entityName + '\'' +
				", identifier=" + identifier +
				", future=" + future +
				'}';
	}

	@Override
	public void submitTo(LocalHeapQueueProcessor processor) {
		processor.process( this );
	}

	@Override
	public void markAsFailed(Throwable t) {
		future.completeExceptionally( t );
		// We don't care about feedback
	}

	public enum Type {
		ADD,
		ADD_OR_UPDATE,
		DELETE;
	}
}
