/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.outboxpolling.cfg;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.cfg.spi.ParseUtils;
import org.hibernate.search.mapper.orm.outboxpolling.logging.impl.Log;
import org.hibernate.search.util.common.annotation.Incubating;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

@Incubating
public enum OutboxEventProcessingOrder {

	/**
	 * Process outbox events in the safest, most appropriate order based on the dialect and other settings.
	 * <ul>
	 *     <li>When using time-based UUIDs for outbox events ({@link UuidGenerationStrategy#TIME}), use {@link #ID} order.</li>
	 *     <li>Otherwise, if using a Microsoft SQL Server dialect, don't use any particular order ({@link #NONE}).</li>
	 *     <li>Otherwise, use {@link #TIME} order.</li>
	 * </ul>
	 */
	AUTO( "auto" ),
	/**
	 * Process outbox events in no particular order.
	 * <p>
	 * This essentially means events will be consumed in a database-specific, undetermined order.
	 * <p>
	 * In setups with multiple event processors,
	 * this reduces the rate of background failures caused by transaction deadlocks (in particular with Microsoft SQL Server),
	 * which does not technically "fix" event processing (those failures are handled automatically by trying again anyway),
	 * but may improve performance and reduce unnecessary noise in logs.
	 * <p>
	 * However, this may lead to situations where the processing of one particular event is continuously postponed
	 * due to newer events being processed before that particular event,
	 * which can be a problem in write-intensive scenarios where the event queue is never empty.
	 */
	NONE( "none" ),
	/**
	 * Process outbox events in "time" order, i.e. in the order events are created.
	 * <p>
	 * This ensures events are processed more or less in the order they were created
	 * and avoids situations where the processing of one particular event is continuously postponed
	 * due to newer events being processed before that particular event.
	 * <p>
	 * However, in setups with multiple event processors,
	 * this may increase the rate of background failures caused by transaction deadlocks (in particular with Microsoft SQL Server),
	 * which does not technically break event processing (those failures are handled automatically by trying again anyway),
	 * but may reduce performance and lead to unnecessary noise in logs.
	 */
	TIME( "time" ),
	/**
	 * Process outbox events in identifier order.
	 * <p>
	 * If outbox event identifiers are {@link UuidGenerationStrategy#TIME time-based UUIDs},
	 * this behaves similarly to {@link #TIME}, but without the risk of deadlocks.
	 * <p>
	 * If outbox event identifiers are {@link UuidGenerationStrategy#RANDOM random UUIDs},
	 * this behaves similarly to {@link #NONE}.
	 */
	ID( "id" );

	private final String externalRepresentation;

	OutboxEventProcessingOrder(String externalRepresentation) {
		this.externalRepresentation = externalRepresentation;
	}

	public String externalRepresentation() {
		return externalRepresentation;
	}

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	// This method conforms to the MicroProfile Config specification. Do not change its signature.
	public static OutboxEventProcessingOrder of(String value) {
		return ParseUtils.parseDiscreteValues(
				OutboxEventProcessingOrder.values(),
				OutboxEventProcessingOrder::externalRepresentation,
				log::invalidOutboxEventProcessingOrderName,
				value
		);
	}
}
