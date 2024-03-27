/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.coordination.common.spi;

import java.util.function.Function;

import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingEventSendingSessionContext;
import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingQueueEventSendingPlan;
import org.hibernate.search.mapper.orm.bootstrap.spi.HibernateSearchOrmMappingProducer;

public interface CoordinationConfigurationContext {

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
	void sendIndexingEventsTo(
			Function<AutomaticIndexingEventSendingSessionContext, AutomaticIndexingQueueEventSendingPlan> senderFactory,
			boolean enlistsInTransaction);

	/**
	 * Adds a mapping producer, to register entities automatically without user intervention.
	 * @param producer A mapping producer.
	 */
	void mappingProducer(HibernateSearchOrmMappingProducer producer);

}
