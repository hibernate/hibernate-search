/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.logging.impl;

import static org.hibernate.search.backend.lucene.logging.impl.LuceneLog.ID_OFFSET;
import static org.hibernate.search.backend.lucene.logging.impl.LuceneLog.ID_OFFSET_LEGACY_ENGINE;
import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.TRACE;
import static org.jboss.logging.Logger.Level.WARN;

import java.lang.invoke.MethodHandles;
import java.nio.file.Path;

import org.hibernate.search.backend.lucene.lowlevel.reader.impl.HibernateSearchMultiReader;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.CategorizedLogger;
import org.hibernate.search.util.common.logging.impl.ClassFormatter;
import org.hibernate.search.util.common.logging.impl.EventContextFormatter;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.logging.impl.MessageConstants;
import org.hibernate.search.util.common.reporting.EventContext;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.FormatWith;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.Param;

import org.apache.lucene.store.Directory;

@CategorizedLogger(
		category = LuceneMiscLog.CATEGORY_NAME,
		description = """
				The main category for the Lucene backend-specific logs.
				It may also include logs that do not fit any other, more specific, Lucene category.
				"""
)
@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
public interface LuceneMiscLog extends BasicLogger {
	String CATEGORY_NAME = "org.hibernate.search.lucene";

	LuceneMiscLog INSTANCE = LoggerFactory.make( LuceneMiscLog.class, CATEGORY_NAME, MethodHandles.lookup() );

	// -----------------------------------
	// Pre-existing messages from Search 5 (engine module)
	// DO NOT ADD ANY NEW MESSAGES HERE
	// -----------------------------------

	@LogMessage(level = INFO)
	@Message(id = ID_OFFSET_LEGACY_ENGINE + 41, value = "Index directory does not exist, creating: '%1$s'")
	void indexDirectoryNotFoundCreatingNewOne(Path absolutePath);

	@LogMessage(level = WARN)
	@Message(id = ID_OFFSET_LEGACY_ENGINE + 52,
			value = "An index writer operation failed. Resetting the index writer and forcing release of locks. %1$s")
	void indexWriterResetAfterFailure(@FormatWith(EventContextFormatter.class) EventContext context);

	@LogMessage(level = Logger.Level.WARN)
	@Message(id = ID_OFFSET_LEGACY_ENGINE + 55,
			value = "Unable to close the index reader. %1$s")
	void unableToCloseIndexReader(@FormatWith(EventContextFormatter.class) EventContext context, @Cause Exception e);

	@Message(id = ID_OFFSET_LEGACY_ENGINE + 114,
			value = "Resource does not exist in classpath: '%1$s'")
	SearchException unableToLoadResource(String fileName);

	@LogMessage(level = Logger.Level.WARN)
	@Message(id = ID_OFFSET_LEGACY_ENGINE + 225,
			value = "Unable to acquire lock on the index while initializing directory '%s'."
					+ " Either the directory wasn't properly closed last time it was used due to a critical failure,"
					+ " or another instance of Hibernate Search is using it concurrently"
					+ " (which is not supported)."
					+ " If you experience indexing failures on this index"
					+ " you will need to remove the lock, and might need to rebuild the index.")
	void lockingFailureDuringInitialization(String directoryDescription, @Param EventContext context, @Cause Exception e);

	@Message(id = ID_OFFSET_LEGACY_ENGINE + 284,
			value = "Unable to open index readers: %1$s")
	SearchException unableToOpenIndexReaders(String causeMessage, @Param EventContext context, @Cause Exception cause);

	// -----------------------------------
	// New messages from Search 6 onwards
	// -----------------------------------

	@Message(id = ID_OFFSET + 1,
			value = "Path '%1$s' exists but does not point to a writable directory.")
	SearchException pathIsNotWriteableDirectory(Path rootDirectory);

	@Message(id = ID_OFFSET + 5,
			value = "Invalid target for Lucene extension: '%1$s'."
					+ " This extension can only be applied to components created by a Lucene backend.")
	SearchException luceneExtensionOnUnknownType(Object context);

	@Message(id = ID_OFFSET + 15,
			value = "Unable to initialize index directory: %1$s")
	SearchException unableToInitializeIndexDirectory(String causeMessage,
			@Param EventContext context, @Cause Exception cause);

	@Message(id = ID_OFFSET + 33,
			value = "Invalid requested type for this backend: '%1$s'."
					+ " Lucene backends can only be unwrapped to '%2$s'.")
	SearchException backendUnwrappingWithUnknownType(@FormatWith(ClassFormatter.class) Class<?> requestedClass,
			@FormatWith(ClassFormatter.class) Class<?> actualClass,
			@Param EventContext context);

	@Message(id = ID_OFFSET + 51,
			value = "Invalid requested type for this index manager: '%1$s'."
					+ " Lucene index managers can only be unwrapped to '%2$s'.")
	SearchException indexManagerUnwrappingWithUnknownType(@FormatWith(ClassFormatter.class) Class<?> requestedClass,
			@FormatWith(ClassFormatter.class) Class<?> actualClass, @Param EventContext context);

	@Message(id = ID_OFFSET + 61, value = "Unable to shut down index: %1$s")
	SearchException unableToShutdownShard(String causeMessage, @Param EventContext context, @Cause Exception cause);

	@Message(id = ID_OFFSET + 78,
			value = "Unable to merge index segments: %1$s")
	SearchException unableToMergeSegments(String causeMessage, @Param EventContext context, @Cause Exception cause);

	@Message(id = ID_OFFSET + 79,
			value = "Unable to close the index writer after write failures: %1$s")
	SearchException unableToCloseIndexWriterAfterFailures(String causeMessage, @Param EventContext context,
			@Cause Exception cause);

	@Message(id = ID_OFFSET + 109,
			value = "Index does not exist for directory '%1$s'")
	SearchException missingIndex(Directory directory, @Param EventContext context);

	@Message(id = ID_OFFSET + 110,
			value = "Unable to validate index directory: %1$s")
	SearchException unableToValidateIndexDirectory(String causeMessage,
			@Param EventContext context, @Cause Exception cause);

	@Message(id = ID_OFFSET + 111,
			value = "Unable to drop index directory: %1$s")
	SearchException unableToDropIndexDirectory(String causeMessage,
			@Param EventContext context, @Cause Exception cause);

	@Message(id = ID_OFFSET + 118,
			value = "A failure occurred during a low-level write operation"
					+ " and the index writer had to be reset."
					+ " Some write operations may have been lost as a result."
					+ " Failure: %1$s")
	SearchException uncommittedOperationsBecauseOfFailure(String causeMessage,
			@Param EventContext context, @Cause Throwable cause);

	@Message(id = ID_OFFSET + 141,
			value = "Unable to compute size of index: %1$s")
	SearchException unableToComputeIndexSize(String causeMessage, @Param EventContext context, @Cause Exception cause);

	@Message(id = ID_OFFSET + 154,
			value = "Unable to start index: %1$s")
	SearchException unableToStartShard(String causeMessage, @Cause Exception cause);

	@Message(id = ID_OFFSET + 156, value = "Nonblocking operation submitter is not supported.")
	SearchException nonblockingOperationSubmitterNotSupported();

	@Message(id = ID_OFFSET + 157, value = "Unable to export the schema for '%1$s' index: %2$s")
	SearchException unableToExportSchema(String indexName, String message, @Cause Exception cause);

	@Message(id = ID_OFFSET + 187,
			value = "Unable to refresh an index reader: %1$s")
	SearchException unableToRefresh(String causeMessage, @Param EventContext context, @Cause Exception cause);

	@LogMessage(level = TRACE)
	@Message(id = ID_OFFSET + 189, value = "Closing MultiReader: %s")
	void closingMultiReader(HibernateSearchMultiReader hibernateSearchMultiReader);

	@LogMessage(level = TRACE)
	@Message(id = ID_OFFSET + 190, value = "MultiReader closed: %s")
	void closedMultiReader(HibernateSearchMultiReader hibernateSearchMultiReader);

	@LogMessage(level = TRACE)
	@Message(id = ID_OFFSET + 191, value = "IndexWriter closed")
	void closedIndexWriter();

	@LogMessage(level = TRACE)
	@Message(id = ID_OFFSET + 192, value = "IndexWriter opened")
	void openedIndexWriter();

	@LogMessage(level = DEBUG)
	@Message(id = ID_OFFSET + 193, value = "Set index writer parameter %s to value : %s. %s")
	void indexWriterSetParameter(String settingName, Object value, String context);
}
