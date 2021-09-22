/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.mapper.orm.coordination.databasepolling.logging.impl;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.WARN;

import javax.persistence.OptimisticLockException;

import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.impl.MessageConstants;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;
import org.jboss.logging.annotations.ValidIdRanges;

@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
@ValidIdRanges({
		@ValidIdRange(min = MessageConstants.MAPPER_ORM_DATABASE_POLLING_ID_RANGE_MIN, max = MessageConstants.MAPPER_ORM_DATABASE_POLLING_ID_RANGE_MAX)
})
public interface Log extends BasicLogger {

	int ID_OFFSET = MessageConstants.MAPPER_ORM_DATABASE_POLLING_ID_RANGE_MIN;

	@LogMessage(level = DEBUG)
	@Message(id = ID_OFFSET + 1, value = "Outbox-generated entity mapping: %1$s")
	void outboxGeneratedEntityMapping(String xmlMappingDefinition);

	@LogMessage(level = DEBUG)
	@Message(id = ID_OFFSET + 2, value = "Session factory closed while processing outbox events. Assuming Hibernate Search is shutting down.")
	void sessionFactoryIsClosedOnOutboxProcessing();

	@Message(id = ID_OFFSET + 3, value = "Max '%1$s' retries exhausted to process the event. Event will be lost.")
	SearchException maxRetryExhausted(int retries);

	@LogMessage(level = WARN)
	@Message(id = ID_OFFSET + 4, value = "Automatic indexing failed for event #%1$s on entity of type '%2$s' with ID '%3$s'."
			+ " Will try again soon. Attempts so far: %4$d.")
	void automaticIndexingRetry(Long eventId, String entityName, String entityId, int attempts);

	@LogMessage(level = DEBUG)
	@Message(id = ID_OFFSET + 5, value = "Starting outbox event processor '%1$s'")
	void startingOutboxEventProcessor(String name);

	@LogMessage(level = DEBUG)
	@Message(id = ID_OFFSET + 6, value = "Stopping outbox event processor '%1$s'")
	void stoppingOutboxEventProcessor(String name);

	@Message(id = ID_OFFSET + 7,
			value = "The total shard count must be strictly positive.")
	SearchException invalidTotalShardCount();

	@Message(id = ID_OFFSET + 8,
			value = "Shard indices must be between 0 (inclusive) and %1d (exclusive, set by '%2$s').")
	SearchException invalidShardIndex(int totalShardCount, String totalShardCountPropertyKey);

	@Message(id = ID_OFFSET + 9,
			value = "When using static sharding, this property must be set."
	)
	SearchException missingPropertyForStaticSharding();

	@LogMessage(level = DEBUG)
	@Message(id = ID_OFFSET + 10, value = "The indexing processor is disabled. "
			+ " Events will accumulate in the queue and indexes will not be updated,"
			+ " unless another application node connects to the same database with their indexing processor enabled.")
	void indexingProcessorDisabled();

	@LogMessage(level = DEBUG)
	@Message(id = ID_OFFSET + 11, value = "'%1$s' failed to obtain a lock on events to process; will try again later.")
	void outboxEventProcessorUnableToLock(String name, @Cause OptimisticLockException lockException);

	@Message(id = ID_OFFSET + 12, value = "Unable to serialize OutboxEvent payload with Avro")
	SearchException unableToSerializeWithAvro(@Cause Throwable e);

	@Message(id = ID_OFFSET + 13, value = "Unable to deserialize OutboxEvent payload with Avro")
	SearchException unableToDeserializeWithAvro(@Cause Throwable e);

}
