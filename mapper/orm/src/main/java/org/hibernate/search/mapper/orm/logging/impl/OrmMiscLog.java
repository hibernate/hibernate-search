/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.logging.impl;

import static org.hibernate.search.mapper.orm.logging.impl.OrmLog.ID_OFFSET;
import static org.hibernate.search.mapper.orm.logging.impl.OrmLog.ID_OFFSET_LEGACY_ENGINE;
import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.WARN;

import java.lang.invoke.MethodHandles;

import jakarta.transaction.Synchronization;

import org.hibernate.ScrollMode;
import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.orm.search.loading.EntityLoadingCacheLookupStrategy;
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
		category = OrmMiscLog.CATEGORY_NAME,
		description = """
				The main category for the Hibernate ORM mapper-specific logs.
				It may also include logs that do not fit any other, more specific, Hibernate ORM mapper category.
				"""
)
@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
public interface OrmMiscLog {
	String CATEGORY_NAME = "org.hibernate.search.mapper.orm";

	OrmMiscLog INSTANCE = LoggerFactory.make( OrmMiscLog.class, CATEGORY_NAME, MethodHandles.lookup() );

	// -----------------------------------
	// Pre-existing messages from Search 5
	// DO NOT ADD ANY NEW MESSAGES HERE
	// -----------------------------------
	@LogMessage(level = WARN)
	@Message(id = ID_OFFSET_LEGACY_ENGINE + 36,
			value = "Unable to guess the transaction status: not starting a JTA transaction.")
	void cannotGuessTransactionStatus(@Cause Exception e);

	@LogMessage(level = WARN)
	@Message(id = ID_OFFSET_LEGACY_ENGINE + 39, value = "Unable to properly close scroll in ScrollableResults.")
	void unableToCloseSearcherInScrollableResult(@Cause Exception e);

	@Message(id = ID_OFFSET_LEGACY_ENGINE + 276, value = "No transaction active. Consider increasing the connection time-out.")
	SearchException transactionNotActiveWhileProducingIdsForBatchIndexing();

	// -----------------------------------
	// New messages from Search 6 onwards
	// -----------------------------------

	@Message(id = ID_OFFSET + 11,
			value = "Unable to create a SearchSession for sessions created using a different session factory."
					+ " Expected: '%1$s'. In use: '%2$s'.")
	SearchException usingDifferentSessionFactories(SessionFactory expectedSessionFactory,
			SessionFactory usedSessionFactory);

	@Message(id = ID_OFFSET + 16, value = "Unable to access Hibernate ORM session: %1$s")
	SearchException hibernateSessionAccessError(String causeMessage, @Cause IllegalStateException cause);

	@Message(id = ID_OFFSET + 17, value = "Underlying Hibernate ORM Session is closed.")
	SearchException hibernateSessionIsClosed(@Cause IllegalStateException cause);

	@LogMessage(level = DEBUG)
	@Message(id = ID_OFFSET + 19,
			value = "The entity loader for '%1$s' is ignoring the cache lookup strategy '%2$s',"
					+ " because document IDs are distinct from entity IDs"
					+ " and thus cannot be used for persistence context or second level cache lookups.")
	void skippingPreliminaryCacheLookupsForNonEntityIdEntityLoader(String entityName,
			EntityLoadingCacheLookupStrategy cacheLookupStrategy);

	@LogMessage(level = DEBUG)
	@Message(id = ID_OFFSET + 20,
			value = "The entity loader for '%1$s' is ignoring the second-level cache"
					+ " even though it was instructed to use it,"
					+ " because caching is not enabled for this entity type.")
	void skippingSecondLevelCacheLookupsForNonCachedEntityTypeEntityLoader(String entityName);

	@Message(id = ID_OFFSET + 21, value = "Unable to access Hibernate ORM session factory: %1$s")
	SearchException hibernateSessionFactoryAccessError(String causeMessage, @Cause IllegalStateException cause);

	@Message(id = ID_OFFSET + 23, value = "Unable to process entities for indexing before transaction completion: %1$s")
	SearchException synchronizationBeforeTransactionFailure(String causeMessage, @Cause Throwable cause);

	@Message(id = ID_OFFSET + 24, value = "Unable to index documents for indexing after transaction completion: %1$s")
	SearchException synchronizationAfterTransactionFailure(String causeMessage, @Cause Throwable cause);

	@Message(id = ID_OFFSET + 25, value = "Unable to handle transaction: %1$s")
	SearchException transactionHandlingException(String causeMessage, @Cause Throwable cause);

	@LogMessage(level = Logger.Level.ERROR)
	@Message(id = ID_OFFSET + 35, value = "Unable to shut down Hibernate Search:")
	void shutdownFailed(@Cause Throwable cause);

	@Message(id = ID_OFFSET + 36, value = "Cannot use scroll() with scroll mode '%1$s' with Hibernate Search queries:"
			+ " only ScrollMode.FORWARDS_ONLY is supported.")
	SearchException canOnlyUseScrollWithScrollModeForwardsOnly(ScrollMode scrollMode);

	@Message(id = ID_OFFSET + 37, value = "Cannot scroll backwards with Hibernate Search scrolls: they are forwards-only."
			+ " Ensure you always increment the scroll position, and never decrement it.")
	SearchException cannotScrollBackwards();

	@Message(id = ID_OFFSET + 38, value = "Cannot set the scroll position relative to the end with Hibernate Search scrolls."
			+ " Ensure you always pass a positive number to setRowNumber().")
	SearchException cannotSetScrollPositionRelativeToEnd();

	@Message(id = ID_OFFSET + 39, value = "Cannot use this ScrollableResults instance: it is closed.")
	SearchException cannotUseClosedScrollableResults();

	@LogMessage(level = Logger.Level.INFO)
	@Message(id = ID_OFFSET + 56, value = "Ignoring unrecognized query hint [%s]")
	void ignoringUnrecognizedQueryHint(String hintName);

	@Message(id = ID_OFFSET + 57,
			value = "Cannot set the fetch size of Hibernate Search ScrollableResults after having created them."
					+ " If you want to define the size of batches for entity loading, set loading options when defining the query instead,"
					+ " for example with .loading(o -> o.fetchSize(50))."
					+ " See the reference documentation for more information.")
	SearchException cannotSetFetchSize();

	@LogMessage(level = Logger.Level.TRACE)
	@Message(id = ID_OFFSET + 127,
			value = "TransactionFactory does not require a TransactionManager: don't wrap in a JTA transaction")
	void transactionManagerNotRequired();

	@LogMessage(level = Logger.Level.TRACE)
	@Message(id = ID_OFFSET + 128, value = "No TransactionManager found, do not start a surrounding JTA transaction")
	void transactionManagerNotFound();

	@LogMessage(level = Logger.Level.TRACE)
	@Message(id = ID_OFFSET + 129, value = "No Transaction in progress, needs to start a JTA transaction")
	void noInProgressTransaction();

	@LogMessage(level = Logger.Level.TRACE)
	@Message(id = ID_OFFSET + 130, value = "Transaction in progress, no need to start a JTA transaction")
	void transactionAlreadyInProgress();

	@LogMessage(level = Logger.Level.TRACE)
	@Message(id = ID_OFFSET + 131, value = "Transaction's afterCompletion is expected to be executed"
			+ " through the AfterTransactionCompletionProcess interface, ignoring: %s")
	void syncAdapterIgnoringAfterCompletion(Synchronization delegate);

	@LogMessage(level = Logger.Level.TRACE)
	@Message(id = ID_OFFSET + 132, value = "Transaction's beforeCompletion() phase already been processed, ignoring: %s")
	void syncAdapterIgnoringBeforeCompletionAlreadyExecuted(Synchronization delegate);

	@LogMessage(level = Logger.Level.TRACE)
	@Message(id = ID_OFFSET + 133, value = "Transaction's afterCompletion() phase already been processed, ignoring: %s")
	void syncAdapterIgnoringAfterCompletionAlreadyExecuted(Synchronization delegate);

	@Message(id = ID_OFFSET + 143,
			value = "Hibernate Search does not support working with %s session type")
	SearchException unsupportedSessionType(Class<?> session);
}
