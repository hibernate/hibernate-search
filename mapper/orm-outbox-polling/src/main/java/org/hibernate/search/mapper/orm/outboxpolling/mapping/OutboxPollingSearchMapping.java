/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.outboxpolling.mapping;

import org.hibernate.search.util.common.annotation.Incubating;

/**
 * Exposes some utilities to handle aborted events,
 * which are events that have been processed many times without success.
 */
@Incubating
public interface OutboxPollingSearchMapping {

	/**
	 * Returns the number of events that are at the moment in aborted state.
	 * <p>
	 * This method only works for single-tenant applications.
	 * If multi-tenancy is enabled, use {@link #countAbortedEvents(Object)} instead.
	 *
	 * @return The number of aborted events
	 */
	long countAbortedEvents();

	/**
	 * Returns the number of events that are at the moment in aborted state.
	 * <p>
	 * This method only works for multi-tenant applications.
	 * If multi-tenancy is disabled, use {@link #countAbortedEvents()} instead.
	 *
	 * @param tenantId The identifier of the tenant whose index content should be targeted
	 * @return The number of aborted events
	 * @deprecated Use {@link #countAbortedEvents(Object)} instead.
	 */
	@Deprecated(forRemoval = true)
	long countAbortedEvents(String tenantId);

	/**
	 * Returns the number of events that are at the moment in aborted state.
	 * <p>
	 * This method only works for multi-tenant applications.
	 * If multi-tenancy is disabled, use {@link #countAbortedEvents()} instead.
	 *
	 * @param tenantId The identifier of the tenant whose index content should be targeted
	 * @return The number of aborted events
	 */
	long countAbortedEvents(Object tenantId);

	/**
	 * Reprocess events that are at the moment in aborted state.
	 * <p>
	 * This method only works for single-tenant applications.
	 * If multi-tenancy is enabled, use {@link #reprocessAbortedEvents(Object)} instead.
	 *
	 * @return The number of reprocessed events
	 */
	int reprocessAbortedEvents();

	/**
	 * Reprocess events that are at the moment in aborted state.
	 * <p>
	 * This method only works for multi-tenant applications.
	 * If multi-tenancy is disabled, use {@link #reprocessAbortedEvents()} instead.
	 *
	 * @param tenantId The identifier of the tenant whose index content should be targeted
	 * @return The number of reprocessed events
	 * @deprecated Use {@link #reprocessAbortedEvents(Object)} instead.
	 */
	@Deprecated(forRemoval = true)
	int reprocessAbortedEvents(String tenantId);

	/**
	 * Reprocess events that are at the moment in aborted state.
	 * <p>
	 * This method only works for multi-tenant applications.
	 * If multi-tenancy is disabled, use {@link #reprocessAbortedEvents()} instead.
	 *
	 * @param tenantId The identifier of the tenant whose index content should be targeted
	 * @return The number of reprocessed events
	 */
	int reprocessAbortedEvents(Object tenantId);

	/**
	 * Delete all events that are at the moment in aborted state.
	 * <p>
	 * This method only works for single-tenant applications.
	 * If multi-tenancy is enabled, use {@link #clearAllAbortedEvents(Object)} instead.
	 *
	 * @return The number of deleted events
	 */
	int clearAllAbortedEvents();

	/**
	 * Delete all events that are at the moment in aborted state.
	 * <p>
	 * This method only works for multi-tenant applications.
	 * If multi-tenancy is disabled, use {@link #clearAllAbortedEvents()} instead.
	 *
	 * @param tenantId The identifier of the tenant whose index content should be targeted
	 * @return The number of deleted events
	 * @deprecated Use {@link #clearAllAbortedEvents(Object)} instead.
	 */
	@Deprecated(forRemoval = true)
	int clearAllAbortedEvents(String tenantId);

	/**
	 * Delete all events that are at the moment in aborted state.
	 * <p>
	 * This method only works for multi-tenant applications.
	 * If multi-tenancy is disabled, use {@link #clearAllAbortedEvents()} instead.
	 *
	 * @param tenantId The identifier of the tenant whose index content should be targeted
	 * @return The number of deleted events
	 */
	int clearAllAbortedEvents(Object tenantId);

}
