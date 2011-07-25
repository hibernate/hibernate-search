/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009 Red Hat Inc. and/or its affiliates and other
 * contributors as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a full listing of
 * individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.hibernate.search.util.logging.impl;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.lucene.index.CorruptIndexException;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Cause;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

/**
 * Hibernate Search's log abstraction layer on top of JBoss Logging.
 *
 * @author Davide D'Alto
 * @since 4.0
 */
@MessageLogger(projectCode = "HSEARCH")
public interface Log extends BasicLogger
{

   @LogMessage(level = WARN)
   @Message(id = 1, value = "initialized \"blackhole\" backend. Index changes will be prepared but discarded!")
   void initializedBlackholeBackend();

   @LogMessage(level = INFO)
   @Message(id = 2, value = "closed \"blackhole\" backend.")
   void closedBlackholeBackend();

   @LogMessage(level = WARN)
   @Message(id = 3, value = "update DirectoryProviders \"blackhole\" backend. Index changes will be prepared but discarded!")
   void updatedDirectoryProviders();

   @LogMessage(level = ERROR)
   @Message(id = 4, value = "Exception attempting to instantiate Similarity '%1$s' set for %2$s")
   void similarityInstantiationException(String similarityName, String beanXClassName);

   @LogMessage(level = INFO)
   @Message(id = 5, value = "Starting JGroups Channel")
   void jGroupsStartingChannel();

   @LogMessage(level = INFO)
   @Message(id = 6, value = "Connected to cluster [ %1$s ]. The node address is %2$s")
   void jGroupsConnectedToCluster(String clusterName, Object address);

   @LogMessage(level = WARN)
   @Message(id = 7, value = "FLUSH is not present in your JGroups stack!  FLUSH is needed to ensure messages are not dropped while new nodes join the cluster.  Will proceed, but inconsistencies may arise!")
   void jGroupsFlushNotPresentInStack();

   @LogMessage(level = ERROR)
   @Message(id = 8, value = "Error while trying to create a channel using config files: %s")
   void jGroupsChannelCreationUsingFileError(String configuration);

   @LogMessage(level = ERROR)
   @Message(id = 9, value = "Error while trying to create a channel using config XML: %s")
   void jGroupsChannelCreationUsingXmlError(String configuration);

   @LogMessage(level = ERROR)
   @Message(id = 10, value = "Error while trying to create a channel using config string: %s")
   void jGroupsChannelCreationFromStringError(String configuration);

   @LogMessage(level = INFO)
   @Message(id = 11, value = "Unable to use any JGroups configuration mechanisms provided in properties %s. Using default JGroups configuration file!")
   void jGroupsConfigurationNotFoundInProperties(Properties props);

   @LogMessage(level = WARN)
   @Message(id = 12, value = "Default JGroups configuration file was not found. Attempt to start JGroups channel with default configuration!")
   void jGroupsDefaultConfigurationFileNotFound();

   @LogMessage(level = INFO)
   @Message(id = 13 , value = "Disconnecting and closing JGroups Channel")
   void jGroupsDisconnectingAndClosingChannel();

   @LogMessage(level = ERROR)
   @Message(id = 14, value = "Problem closing channel; setting it to null")
   void jGroupsClosingChannelError(@Cause Exception toLog);

   @LogMessage(level = INFO)
   @Message(id = 15, value = "Received new cluster view: %s")
   void jGroupsReceivedNewClusterView(Object view);

   @LogMessage(level = ERROR)
   @Message(id = 16, value = "Incorrect message type: %s")
   void incorrectMessageType(Class<?> class1);

   @LogMessage(level = ERROR)
   @Message(id = 17, value = "Work discarded, thread was interrupted while waiting for space to schedule: %s")
   void interruptedWorkError(Runnable r);

   @LogMessage(level = INFO)
   @Message(id = 18, value = "Skipping directory synchronization, previous work still in progress: %s")
   void skippingDirectorySynchronization(String indexName);

   @LogMessage(level = WARN)
   @Message(id = 19, value = "Unable to remove previous marker file from source of %s")
   void unableToRemovePreviousMarket(String indexName);

   @LogMessage(level = WARN)
   @Message(id = 20, value = "Unable to create current marker in source of %s")
   void unableToCreateCurrentMarker(String indexName, @Cause Exception e);

   @LogMessage(level = ERROR)
   @Message(id = 21, value = "Unable to synchronize source of %s")
   void unableToSynchronizeSource(String indexName, @Cause Exception e);

   @LogMessage(level = WARN)
   @Message(id = 22, value = "Unable to determine current in source directory, will try again during the next synchronization")
   void unableToDetermineCurrentInSourceDirectory();

   @LogMessage(level = ERROR)
   @Message(id = 23, value = "Unable to compare %1$s with %2$s.")
   void unableToCompareSourceWithDestinationDirectory(String source, String destination);

   @LogMessage(level = WARN)
   @Message(id = 24, value = "Unable to reindex entity on collection change, id cannot be extracted: %s")
   void idCannotBeExtracted(String affectedOwnerEntityName);

   @LogMessage(level = WARN)
   @Message(id = 25, value = "Service provider has been used but not released: %s")
   void serviceProviderNotReleased(Class<?> class1);

   @LogMessage(level = ERROR)
   @Message(id = 26, value = "Fail to properly stop service: %s")
   void stopServiceFailed(Class<?> class1, @Cause Exception e);

   @LogMessage(level = INFO)
   @Message(id = 27, value = "Going to reindex %d entities")
   void indexingEntities(long count);

   @LogMessage(level = INFO)
   @Message(id = 28, value = "Reindexed %d entities")
   void indexingEntitiesCompleted(long l);

   @LogMessage(level = INFO)
   @Message(id = 29, value = "Indexing completed. Reindexed %d entities. Unregistering MBean from server")
   void indexingCompletedAndMBeanUnregistered(long l);

   @LogMessage(level = INFO)
   @Message(id = 30, value = "%1$d documents indexed in %2$d ms")
   void indexingDocumentsCompleted(long doneCount, long elapsedMs);

   @LogMessage(level = INFO)
   @Message(id = 31, value = "Indexing speed: %1$f documents/second; progress: %2$f%%")
   void indexingSpeed(float estimateSpeed, float estimatePercentileComplete);

   @LogMessage(level = ERROR)
   @Message(id = 32, value = "Could not delete %s")
   void notDeleted(File file);

   @LogMessage(level = WARN)
   @Message(id = 33, value = "Could not change timestamp for %s. Index synchronization may be slow.")
   void notChangeTimestamp(File destFile);

   @LogMessage(level = INFO)
   @Message(id = 34, value = "Hibernate Search %s")
   void version(String versionString);

   @LogMessage(level = WARN)
   @Message(id = 35, value = "could not close resource: ")
   void couldNotCloseResource(@Cause Exception e);

   @LogMessage(level = WARN)
   @Message(id = 36, value = "Cannot guess the Transaction Status: not starting a JTA transaction")
   void cannotGuessTransactionStatus(@Cause Exception e);

   @LogMessage(level = WARN)
   @Message(id = 37, value = "Unable to properly close searcher during lucene query: %s")
   void unableToCloseSearcherDuringQuery(String query, @Cause Exception e);

   @LogMessage(level = WARN)
   @Message(id = 38, value = "Forced to use Document extraction to workaround FieldCache bug in Lucene")
   void forceToUseDocumentExtraction();

   @LogMessage(level = WARN)
   @Message(id = 39, value = "Unable to properly close searcher in ScrollableResults")
   void unableToCloseSearcherInScrollableResult(@Cause Exception e);

   @LogMessage(level = WARN)
   @Message(id = 40, value = "Unexpected: value is missing from FieldCache. This is likely a bug in the FieldCache implementation, " +
   		"Hibernate Search might have to workaround this by slightly inaccurate faceting values or reduced performance.")
   void unexpectedValueMissingFromFieldCache();

   @LogMessage(level = WARN)
   @Message(id = 41, value = "Index directory not found, creating: '%s'")
   void indexDirectoryNotFoundCreatingNewOne(String absolutePath);

   @LogMessage(level = WARN)
   @Message(id = 42, value = "No current marker in source directory. Has the master being started already?")
   void noCurrentMarkerInSourceDirectory();

   @LogMessage(level = INFO)
   @Message(id = 43, value = "Found current marker in source directory - initialization succeeded")
   void foundCurrentMarker();

   @LogMessage(level = WARN)
   @Message(id = 44, value = "Abstract classes can never insert index documents. Remove @Indexed.")
   void abstractClassesCannotInsertDocuments();

   @LogMessage(level = WARN)
   @Message(id = 45, value = "@ContainedIn is pointing to an entity having @ProvidedId. " +
   		"This is not supported, indexing of contained in entities will be skipped. " +
   		"Indexed data of the embedded object might become out of date in objects of type ")
   void containedInPointsToProvidedId(Class<?> objectClass);

   @LogMessage(level = WARN)
   @Message(id = 46, value = "FieldCache was enabled on class %s but for this type of identifier we can't extract values from the FieldCache: cache disabled")
   void cannotExtractValueForIdentifier(Class<?> beanClass);

   @LogMessage(level = WARN)
   @Message(id = 47, value = "Unable to close JMS connection for %s")
   void unableToCloseJmsConnection(String jmsQueueName, @Cause Exception e);

   @LogMessage(level = WARN)
   @Message(id = 48, value = "Unable to retrieve named analyzer: %s")
   void unableToRetrieveNamedAnalyzer(String value);

   @LogMessage(level = WARN)
   @Message(id = 49, value = "Was interrupted while waiting for index activity to finish. Index might be inconsistent or have a stale lock")
   void interruptedWhileWaitingForIndexActivity();

   @LogMessage(level = WARN)
   @Message(id = 50, value = "It appears changes are being pushed to the index out of a transaction. " +
   "Register the IndexWorkFlushEventListener listener on flush to correctly manage Collections!")
   void pushedChangesOutOfTransaction();

   @LogMessage(level = WARN)
   @Message(id = 51, value = "Received null or empty Lucene works list in message.")
   void receivedEmptyLuceneWOrksInMessage();

   @LogMessage(level = WARN)
   @Message(id = 52, value = "Going to force release of the IndexWriter lock")
   void forcingReleaseIndexWriterLock();

   @LogMessage(level = WARN)
   @Message(id = 53, value = "Chunk size must be positive: using default value.")
   void checkSizeMustBePositive();

   @LogMessage(level = WARN)
   @Message(id = 54, value = "ReaderProvider contains readers not properly closed at destroy time")
   void readersNotProperlyClosedinReaderProvider();

   @LogMessage(level = WARN)
   @Message(id = 55, value = "Unable to close Lucene IndexReader")
   void unableToCLoseLuceneIndexReader(@Cause Exception e);

   @LogMessage(level = WARN)
   @Message(id = 56, value = "Unable to un-register existing MBean: ")
   void unableToUnregisterExistingMBean(String name, @Cause Exception e);

   @LogMessage(level = WARN)
   @Message(id = 57, value = "Property hibernate.search.autoregister_listeners is set to false." +
           " No attempt will be made to register Hibernate Search event listeners.")
   void eventListenerWontBeRegistered();

   @LogMessage(level = ERROR)
   @Message(id = 58, value = "%s")
   void exceptionOccured(String errorMsg, @Cause Throwable exceptionThatOccurred);

   @LogMessage(level = ERROR)
   @Message(id = 59, value = "Worker raises an exception on close()")
   void workerException(@Cause Exception e);

   @LogMessage(level = ERROR)
   @Message(id = 60, value = "ReaderProvider raises an exception on destroy()")
   void readerProviderExceptionOnDestroy(@Cause Exception e);

   @LogMessage(level = ERROR)
   @Message(id = 61, value = "DirectoryProvider raises an exception on stop() ")
   void directoryProviderExceptionOnStop(@Cause Exception e);

   @LogMessage(level = ERROR)
   @Message(id = 62, value = "Batch indexing was interrupted")
   void interruptedBatchIndexig();

   @LogMessage(level = ERROR)
   @Message(id = 63, value = "Error during batch indexing: ")
   void errorDuringBatchIndexing(@Cause Throwable e);

   @LogMessage(level = ERROR)
   @Message(id = 64, value = "Error while executing runnable wrapped in a JTA transaction")
   void errorExecutingRunnableInTransaction(@Cause Throwable e);

   @LogMessage(level = ERROR)
   @Message(id = 65, value = "Error while rollbacking transaction after %s")
   void errorRollbackingTransaction(String message, @Cause Exception e1);

   @LogMessage(level = ERROR)
   @Message(id = 66, value = "Failed to initialize SlaveDirectoryProvider %s")
   void failedSlaveDirectoryProviderInitialization(String indexName, @Cause Exception re);

   @LogMessage(level = ERROR)
   @Message(id = 67, value = "Unable to properly close Lucene directory %s")
   void unableToCloseLuceneDirectory(Object directory, @Cause Exception e);

   @LogMessage(level = ERROR)
   @Message(id = 68, value = "Unable to retrieve object from message: %s")
   void unableToRetrieveObjectFromMessage(Class<?> messageClass, @Cause Exception e);

   @LogMessage(level = ERROR)
   @Message(id = 69, value = "Illegal object retrieved from message")
   void illegalObjectRetrievedFromMessage(@Cause Exception e);

   @LogMessage(level = ERROR)
   @Message(id = 70, value = "Terminating batch work! Index might end up in inconsistent state.")
   void terminatingBatchWorkCanCauseInconsistentState();

   @LogMessage(level = ERROR)
   @Message(id = 71, value = "Unable to properly shut down asynchronous indexing work")
   void unableToShutdownAsyncronousIndexing(@Cause Exception e);

   @LogMessage(level = ERROR)
   @Message(id = 72, value = "Couldn't open the IndexWriter because of previous error: operation skipped, index ouf of sync!")
   void cannotOpenIndexWriterCausePreviousError();

   @LogMessage(level = ERROR)
   @Message(id = 73, value = "Error in backend")
   void backendError(@Cause Exception e);
   
   @LogMessage(level = ERROR)
   @Message(id = 74, value = "Unexpected error in Lucene Backend:")
   void unexpectedErrorInLuceneBackend(@Cause Throwable tw);

   @LogMessage(level = WARN)
   @Message(id = 75, value = "Configuration setting " + org.hibernate.search.Environment.LUCENE_MATCH_VERSION
      + " was not specified, using LUCENE_CURRENT.")
   void recommendConfiguringLuceneVersion();

   @LogMessage(level = ERROR)
   @Message(id = 76, value = "Could not open Lucene index: data is corrupted")
   void cantOpenCorruptedIndex(@Cause CorruptIndexException e);
   
   @LogMessage(level = ERROR)
   @Message(id = 77, value = "An IOException happened while accessing the Lucene index")
   void ioExceptionOnIndex(@Cause IOException e);

   @LogMessage(level = ERROR)
   @Message(id = 78, value = "Timed out waiting to flush all operations to the backend of index %s")
   void unableToShutdownAsyncronousIndexingByTimeout(String indexName);

}
