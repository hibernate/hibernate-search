/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.logging.impl;

import static org.hibernate.search.mapper.pojo.logging.impl.PojoMapperLog.ID_OFFSET;
import static org.hibernate.search.mapper.pojo.logging.impl.PojoMapperLog.ID_OFFSET_LEGACY_ENGINE;
import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;

import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.util.List;
import java.util.Set;

import org.hibernate.search.mapper.pojo.massindexing.impl.MassIndexingOperationHandledFailureException;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.CategorizedLogger;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.logging.impl.MessageConstants;

import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

@CategorizedLogger(
		category = MassIndexingLog.CATEGORY_NAME,
		description = """
				Logs information on various mass indexing operations (e.g. when they start/end or progress).
				+
				It may also include messages on misconfigured mass indexer.
				"""
)
@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
public interface MassIndexingLog {
	String CATEGORY_NAME = "org.hibernate.search.mapper.massindexing";

	MassIndexingLog INSTANCE = LoggerFactory.make( MassIndexingLog.class, CATEGORY_NAME, MethodHandles.lookup() );

	// -----------------------------------
	// Pre-existing messages from Search 5 (engine module)
	// DO NOT ADD ANY NEW MESSAGES HERE
	// -----------------------------------

	@LogMessage(level = INFO)
	@Message(id = ID_OFFSET_LEGACY_ENGINE + 27, value = "Mass indexing is going to index %d entities.")
	void indexingEntities(long count);

	@LogMessage(level = ERROR)
	@Message(id = ID_OFFSET_LEGACY_ENGINE + 62, value = "Mass indexing received interrupt signal: aborting.")
	void interruptedBatchIndexing();

	// -----------------------------------
	// New messages from Search 6 onwards
	// -----------------------------------

	@Message(id = ID_OFFSET + 42, value = "%1$s failure(s) occurred during mass indexing. See the logs for details."
			+ " First failure: %2$s")
	SearchException massIndexingFirstFailure(long finalFailureCount, String firstFailureMessage, @Cause Throwable firstFailure);

	@Message(id = ID_OFFSET + 101, value = "%1$s failure(s) occurred during mass indexing. See the logs for details."
			+ " First failure on entity '%2$s': %3$s")
	SearchException massIndexingFirstFailureOnEntity(long finalFailureCount,
			Object firstFailureEntity, String firstFailureMessage,
			@Cause Throwable firstFailure);

	@LogMessage(level = Logger.Level.ERROR)
	@Message(id = ID_OFFSET + 102,
			value = "The mass indexing failure handler threw an exception while handling a previous failure."
					+ " The failure may not have been reported.")
	void failureInMassIndexingFailureHandler(@Cause Throwable t);

	@Message(id = ID_OFFSET + 103, value = "Mass indexing received interrupt signal. The index is left in an unknown state!")
	SearchException massIndexingThreadInterrupted(@Cause InterruptedException e);

	@LogMessage(level = Logger.Level.WARN)
	@Message(id = ID_OFFSET + 120,
			value = "Both \"dropAndCreateSchemaOnStart()\" and \"purgeAllOnStart()\" are enabled. " +
					"Consider having just one setting enabled as after the index is recreated there is nothing to purge.")
	void redundantPurgeAfterDrop();

	@Message(id = ID_OFFSET + 125,
			value = "%1$s failures went unreported for this operation to avoid flooding."
					+ " To disable flooding protection, use 'massIndexer.failureFloodingThreshold(Long.MAX_VALUE)'.")
	SearchException notReportedFailures(long count);

	@Message(id = ID_OFFSET + 160,
			value = "Requesting a schema drop-create on start is not allowed when multitenancy is enabled. "
					+ "Schema would be dropped for all tenants, but data will only be indexed for tenant ids '%1$s'. "
					+ "Do not use the schema drop-create on start when providing tenant ids. "
					+ "If schema drop is actually required, do it through an SearchSchemaManager.")
	SearchException schemaDropNotAllowedWithMultitenancy(Set<String> tenantIds);

	@Message(id = ID_OFFSET + 161, value = "Invalid mass indexing default clean operation name: '%1$s'."
			+ " Valid names are: %2$s.")
	SearchException invalidMassIndexingDefaultCleanOperation(String name, List<String> names);

	@Message(id = ID_OFFSET + 163,
			value = "Mass indexer running in a fail fast mode encountered a problem. Stopping the process.")
	MassIndexingOperationHandledFailureException massIndexerFailFast();

	@LogMessage(level = INFO)
	@Message(id = ID_OFFSET + 164,
			value = "Mass indexed %1$d. Speed: %3$.2f/s instant, %4$.2f/s since start. Remaining: unknown, %2$d type(s) pending.")
	void indexingProgress(long doneCount, long typesToIndex, float currentSpeed, float estimateSpeed);

	@LogMessage(level = INFO)
	@Message(id = ID_OFFSET + 165,
			value = "Mass indexed %1$d. Speed: %2$.2f/s instant, %3$.2f/s since start. Remaining: unknown.")
	void indexingProgress(long doneCount, float currentSpeed, float estimateSpeed);

	@LogMessage(level = INFO)
	@Message(id = ID_OFFSET + 166,
			value = "Mass indexed %1$.2f%% %2$d/%3$d. Speed: %4$.2f/s instant, %5$.2f/s since start. Remaining: %6$d, %7$d type(s) pending.")
	void indexingProgress(float estimatePercentileComplete, long doneCount, long totalCount, float currentSpeed,
			float estimateSpeed, long remainingCount, long typesToIndex);

	@LogMessage(level = INFO)
	@Message(id = ID_OFFSET + 167,
			value = "Mass indexed %1$.2f%% %2$d/%3$d. Speed: %4$.2f/s instant, %5$.2f/s since start. Remaining: %6$d, approx. %7$s.")
	void indexingProgressWithRemainingTime(float estimatePercentileComplete, long doneCount, long totalCount,
			float currentSpeed, float estimateSpeed, long remainingCount, Duration timeToFinish);

	@LogMessage(level = INFO)
	@Message(id = ID_OFFSET + 168, value = "Mass indexing complete in %3$s. Indexed %1$d/%2$d entities.")
	void indexingEntitiesCompleted(long indexed, long total, Duration indexingTime);

	@LogMessage(level = INFO)
	@Message(id = ID_OFFSET + 169,
			value = "Mass indexing is going to index approx. %1$d entities (%2$s). Actual number may change once the indexing starts.")
	void indexingEntitiesApprox(long count, String types);

	@LogMessage(level = DEBUG)
	@Message(id = ID_OFFSET + 172, value = "Indexing for %s is done.")
	void indexingForTypeGroupDone(String typeGroup);

	@LogMessage(level = DEBUG)
	@Message(id = ID_OFFSET + 173, value = "Identifier loading for %s started.")
	void identifierLoadingStarted(String typeGroup);

	@LogMessage(level = DEBUG)
	@Message(id = ID_OFFSET + 174, value = "Identifier loading for %s finished.")
	void identifierLoadingFinished(String typeGroup);

	@LogMessage(level = DEBUG)
	@Message(id = ID_OFFSET + 175, value = "Identifier loading produced a list of ids: %s.")
	void identifierLoadingLoadedIds(List<?> ids);

	@LogMessage(level = DEBUG)
	@Message(id = ID_OFFSET + 176, value = "Entity loading for %s started.")
	void entityLoadingStarted(String typeGroup);

	@LogMessage(level = DEBUG)
	@Message(id = ID_OFFSET + 177, value = "Entity loading for %s finished.")
	void entityLoadingFinished(String typeGroup);

	@LogMessage(level = DEBUG)
	@Message(id = ID_OFFSET + 178, value = "Entity loading will attempt to load entities for ids: %s.")
	void entityLoadingAttemptToLoadIds(List<?> ids);
}
