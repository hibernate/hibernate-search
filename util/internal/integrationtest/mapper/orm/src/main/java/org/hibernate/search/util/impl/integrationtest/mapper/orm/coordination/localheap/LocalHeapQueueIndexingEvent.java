/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.mapper.orm.coordination.localheap;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.backend.orchestration.spi.BatchedWork;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class LocalHeapQueueIndexingEvent implements BatchedWork<LocalHeapQueueProcessor> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	final String entityName;
	final transient Object identifier;
	final String serializedId;
	final byte[] payload;

	public LocalHeapQueueIndexingEvent(String entityName, Object identifier, String serializedId,
			byte[] payload) {
		this.entityName = entityName;
		this.identifier = identifier;
		this.serializedId = serializedId;
		this.payload = payload;
	}

	@Override
	public String toString() {
		return "LocalHeapQueueIndexingEvent{" +
				"id=" + System.identityHashCode( this ) +
				", entityName='" + entityName + '\'' +
				", identifier=" + identifier +
				'}';
	}

	@Override
	public void submitTo(LocalHeapQueueProcessor processor) {
		processor.process( this );
	}

	@Override
	public void markAsFailed(Throwable t) {
		// In a real implementation we would put this event back into a queue, to re-try later.
		// But here it's just for testing.
		log.errorf( t, "Failed to process event '%s'", this );
	}
}
