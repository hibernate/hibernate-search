/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.automaticindexing.spi;

import java.util.function.Function;

public interface AutomaticIndexingConfigurationContext {

	/**
	 * Requests that indexing events be processed directly in the current session.
	 * <p>
	 * This is incompatible with {@link #sendIndexingEventsTo(Function, boolean)}.
	 */
	void reindexInSession();

	/**
	 * Requests that indexing events be sent to a queue.
	 * <p>
	 * This is incompatible with {@link #reindexInSession()}.
	 * @param senderFactory A factory to create the {@link AutomaticIndexingQueueEventSendingPlan} to send events to.
	 * @param enlistsInTransaction Whether the event sender enlists in Hibernate ORM transactions,
	 * meaning event can (and should) be sent before the commit,
	 * which will automatically lead them to be sent on commit (or not sent at all in case of rollback).
	 */
	void sendIndexingEventsTo(Function<AutomaticIndexingEventSendingSessionContext, AutomaticIndexingQueueEventSendingPlan> senderFactory,
			boolean enlistsInTransaction);

}
