/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
	 * If multi-tenancy is enabled, use {@link #countAbortedEvents(String)} instead.
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
	 */
	long countAbortedEvents(String tenantId);

	/**
	 * Reprocess events that are at the moment in aborted state.
	 * <p>
	 * This method only works for single-tenant applications.
	 * If multi-tenancy is enabled, use {@link #reprocessAbortedEvents(String)} instead.
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
	 */
	int reprocessAbortedEvents(String tenantId);

	/**
	 * Delete all events that are at the moment in aborted state.
	 * <p>
	 * This method only works for single-tenant applications.
	 * If multi-tenancy is enabled, use {@link #clearAllAbortedEvents(String)} instead.
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
	 */
	int clearAllAbortedEvents(String tenantId);

}
