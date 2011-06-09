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
package org.hibernate.search.util.logging;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

import java.io.File;
import java.util.Properties;

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
   @Message(value = "initialized \"blackhole\" backend. Index changes will be prepared but discarded!", id = 1)
   void initializedBlackholeBackend();

   @LogMessage(level = INFO)
   @Message(value = "closed \"blackhole\" backend.", id = 2)
   void closedBlackholeBackend();

   @LogMessage(level = WARN)
   @Message(value = "update DirectoryProviders \"blackhole\" backend. Index changes will be prepared but discarded!", id = 3)
   void updatedDirectoryProviders();
   
   @LogMessage(level = ERROR)
   @Message(value = "Exception attempting to instantiate Similarity '%s' set for %s", id = 4)
   void similarityInstantiationException(String similarityName, String beanXClassName);

   @LogMessage(level = INFO)
   @Message(value = "Starting JGroups Channel", id = 5)
   void jGroupsStartingChannel();

   @LogMessage(level = INFO)
   @Message(value = "Connected to cluster [ %s ]. The node address is $s", id = 6) 
   void jGroupsConnectedToCluster(String clusterName, Object address);
   
   @LogMessage(level = WARN)
   @Message(value = "FLUSH is not present in your JGroups stack!  FLUSH is needed to ensure messages are not dropped while new nodes join the cluster.  Will proceed, but inconsistencies may arise!", id = 7)
   void jGroupsFlushNotPresentInStack();

   @LogMessage(level = ERROR)
   @Message(value = "Error while trying to create a channel using config files: %s", id = 8)
   void jGroupsChannelCreationUsingFileError(String configuration);

   @LogMessage(level = ERROR)
   @Message(value = "Error while trying to create a channel using config XML: %s", id = 9)
   void jGroupsChannelCreationUsingXmlError(String configuration);

   @LogMessage(level = ERROR)
   @Message(value = "Error while trying to create a channel using config string: %s", id = 10) 
   void jGroupsChannelCreationFromStringError(String configuration);

   @LogMessage(level = INFO)
   @Message(value = "Unable to use any JGroups configuration mechanisms provided in properties %s. Using default JGroups configuration file!", id = 11)
   void jGroupsConfigurationNotFoundInProperties(Properties props);

   @LogMessage(level = WARN)
   @Message(value = "Default JGroups configuration file was not found. Attempt to start JGroups channel with default configuration!", id = 12)
   void jGroupsDefaultConfigurationFileNotFound();
   
   @LogMessage(level = INFO)
   @Message(value = "Disconnecting and closing JGroups Channel", id = 13 )
   void jGroupsDisconnectingAndClosingChannel();

   @LogMessage(level = ERROR)
   @Message(value = "Problem closing channel; setting it to null", id = 14)
   void jGroupsClosingChannelError(@Cause Exception toLog);

   @LogMessage(level = INFO)
   @Message(value = "Received new cluster view: %s", id = 15)
   void jGroupsReceivedNewClusterView(Object view);

   @LogMessage(level = ERROR)
   @Message(value = "Incorrect message type: %s", id = 16) 
   void incorrectMessageType(Class<? extends javax.jms.Message> class1);

   @LogMessage(level = ERROR)
   @Message(value = "Work discarded, thread was interrupted while waiting for space to schedule: %s", id = 17) 
   void interruptedWokError(Runnable r);

   @LogMessage(level = INFO)
   @Message(value = "Skipping directory synchronization, previous work still in progress: %s", id = 18)  
   void skippingDirectorySynchronization(String indexName);

   @LogMessage(level = WARN)
   @Message(value = "Unable to remove previous marker file from source of %s", id = 19)
   void unableToRemovePreviousMarket(String indexName);

   @LogMessage(level = WARN)
   @Message(value = "Unable to create current marker in source of %s", id = 20) 
   void unableToCreateCurrentMarker(String indexName, @Cause Exception e);

   @LogMessage(level = ERROR)
   @Message(value = "Unable to synchronize source of %s", id = 21)
   void unableToSynchronizeSource(String indexName, @Cause Exception e);

   @LogMessage(level = WARN)
   @Message(value = "Unable to determine current in source directory, will try again during the next synchronization", id = 22)
   void unableToDetermineCurrentInSourceDirectory();

   @LogMessage(level = ERROR)
   @Message(value = "Unable to compare %s with %s.", id = 23)
   void unableToCompareSourceWithDestinationDirectory(String source, String destination);

   @LogMessage(level = WARN)
   @Message(value = "Unable to reindex entity on collection change, id cannot be extracted: %s", id = 24)
   void idCannotBeExtracted(String affectedOwnerEntityName);

   @LogMessage(level = WARN)
   @Message(value = "Service provider has been used but not released: %s", id = 25)   
   void serviceProviderNotReleased(Class<?> class1);

   @LogMessage(level = ERROR)
   @Message(value = "Fail to properly stop service: %s", id = 26)
   void stopServiceFailed(Class<?> class1, @Cause Exception e);
   
   @LogMessage(level = INFO)
   @Message(value = "Going to reindex %d entities", id = 27)
   void indexingEntities(long count);

   @LogMessage(level = INFO)
   @Message(value = "Reindexed %d entities", id = 28)
   void indexingEntitiesCompleted(long l);

   @LogMessage(level = INFO)
   @Message(value = "Indexing completed. Reindexed %d entities. Unregistering MBean from server", id = 29)
   void indexingCompletedAndMBeanUnregistered(long l);
   
   @LogMessage(level = INFO)
   @Message(value = "%d documents indexed in %d ms", id = 30)
   void indexingDocumentsCompleted(long doneCount, long elapsedMs);

   @LogMessage(level = INFO)
   @Message(value = "Indexing speed: %f documents/second; progress: %f%", id = 31) 
   void indexingSpeed(float estimateSpeed, float estimatePercentileComplete);

   @LogMessage(level = ERROR)
   @Message(value = "Could not delete %s", id = 32) 
   void notDeleted(File file);

   @LogMessage(level = WARN)
   @Message(value = "Could not change timestamp for %s. Index synchronization may be slow.", id = 33)  
   void notChangeTimestamp(File destFile);

   @LogMessage(level = INFO)
   @Message(value = "Hibernate Search %s", id = 34)
   void version(String versionString);

   @LogMessage(level = WARN)
   @Message(value = "could not close resource: ", id = 35)
   void couldNotCloseResource(@Cause Exception e);
   
   @LogMessage(level = WARN)
   @Message(value = "Cannot guess the Transaction Status: not starting a JTA transaction", id = 36)
   void cannotGuessTransactionStatus(@Cause Exception e);

   @LogMessage(level = WARN)
   @Message(value = "Unable to properly close searcher during lucene query: %s", id = 37)
   void unableToCloseSearcherDuringQuery(String query, @Cause Exception e);
   
   @LogMessage(level = WARN)
   @Message(value = "Forced to use Document extraction to workaround FieldCache bug in Lucene", id = 38)
   void forceToUseDocumentExtraction();

   @LogMessage(level = WARN)
   @Message(value = "Unable to properly close searcher in ScrollableResults", id = 39)
   void unableToCloseSearcherInScrollableResult(@Cause Exception e);

   @LogMessage(level = WARN)
   @Message(value = "Unexpected: value is missing from FieldCache. This is likely a bug in the FieldCache implementation, " +
   		"Hibernate Search might have to workaround this by slightly inaccurate faceting values or reduced performance.", id = 40)
   void unexpectedValueMissingFromFieldCache();

   @LogMessage(level = WARN)
   @Message(value = "Index directory not found, creating: '%s'", id = 41)
   void indexDirectoryNotFoundCreatingNewOne(String absolutePath);

   @LogMessage(level = WARN)
   @Message(value = "No current marker in source directory. Has the master being started already?", id = 42)
   void noCurrentMarkerInSourceDirectory();

   @LogMessage(level = INFO)
   @Message(value = "Found current marker in source directory - initialization succeeded", id = 43)
   void foundCurrentMarker();

   @LogMessage(level = WARN)
   @Message(value = "Abstract classes can never insert index documents. Remove @Indexed.", id = 44)
   void abstractClassesCannotInsertDocuments();

   @LogMessage(level = WARN)
   @Message(value = "@ContainedIn is pointing to an entity having @ProvidedId. " +
   		"This is not supported, indexing of contained in entities will be skipped. " +
   		"Indexed data of the embedded object might become out of date in objects of type ", id = 45)
   void containedInPointsToProvidedId(Class<?> objectClass);

   @LogMessage(level = WARN)
   @Message(value = "FieldCache was enabled on class %s but for this type of identifier we can't extract values from the FieldCache: cache disabled", id = 46)
   void cannotExtractValueForIdentifier(Class<?> beanClass);

   @LogMessage(level = WARN)
   @Message(value = "Unable to close JMS connection for %s", id = 47) 
   void unableToCloseJmsConnection(String jmsQueueName, @Cause Exception e);

   @LogMessage(level = WARN)
   @Message(value = "Unable to retrieve named analyzer: %s", id = 48)
   void unableToRetrieveNamedAnalyzer(String value);

   @LogMessage(level = WARN)
   @Message(value = "Was interrupted while waiting for index activity to finish. Index might be inconsistent or have a stale lock", id = 49)
   void interruptedWhileWaitingForIndexActivity();

   @LogMessage(level = WARN)
   @Message(value = "It appears changes are being pushed to the index out of a transaction. " +
   "Register the IndexWorkFlushEventListener listener on flush to correctly manage Collections!", id = 50)
   void pushedChangesOutOfTransaction();

   @LogMessage(level = WARN)
   @Message(value = "Received null or empty Lucene works list in message.", id = 51)
   void receivedEmptyLuceneWOrksInMessage();

   @LogMessage(level = WARN)
   @Message(value = "Going to force release of the IndexWriter lock", id = 52)
   void forcingReleaseIndexWriterLock();

   @LogMessage(level = WARN)
   @Message(value = "Chunk size must be positive: using default value.", id = 53)
   void checkSizeMustBePositive();

   @LogMessage(level = WARN)
   @Message(value = "ReaderProvider contains readers not properly closed at destroy time", id = 54)
   void readersNotProperlyClosedinReaderProvider();

   @LogMessage(level = WARN)
   @Message(value = "Unable to close Lucene IndexReader", id = 55)
   void unableToCLoseLuceneIndexReader(@Cause Exception e);

   @LogMessage(level = WARN)
   @Message(value = "Unable to un-register existing MBean: ", id = 56)
   void unableToUnregisterExistingMBean(String name, @Cause Exception e);

   @LogMessage(level = WARN)
   @Message(value = "Property hibernate.search.autoregister_listeners is set to false." +
           " No attempt will be made to register Hibernate Search event listeners.", id = 57)
   void eventListenerWontBeRegistered();

   @LogMessage(level = ERROR)
   @Message(value = "%s", id = 58)
   void exceptionOccured(String errorMsg, @Cause Throwable exceptionThatOccurred);

   @LogMessage(level = ERROR)
   @Message(value = "Worker raises an exception on close()", id = 59)
   void workerException(@Cause Exception e);

   @LogMessage(level = ERROR)
   @Message(value = "ReaderProvider raises an exception on destroy()", id = 60)
   void readerProviderExceptionOnDestroy(@Cause Exception e);

   @LogMessage(level = ERROR)
   @Message(value = "DirectoryProvider raises an exception on stop() ", id = 61)
   void directoryProviderExceptionOnStop(@Cause Exception e);

   @LogMessage(level = ERROR)
   @Message(value = "Batch indexing was interrupted", id = 62)
   void interruptedBatchIndexig();

   @LogMessage(level = ERROR)
   @Message(value = "Error during batch indexing: ", id = 63)
   void errorDuringBatchIndexing(@Cause Throwable e);

   @LogMessage(level = ERROR)
   @Message(value = "Error while executing runnable wrapped in a JTA transaction", id = 64)
   void errorExecutingRunnableInTransaction(@Cause Throwable e);

   @LogMessage(level = ERROR)
   @Message(value = "Error while rollbacking transaction after %s", id = 65) 
   void errorRollbackingTransaction(String message, @Cause Exception e1);

   @LogMessage(level = ERROR)
   @Message(value = "Failed to initialize SlaveDirectoryProvider %s", id = 66)
   void failedSlaveDirectoryProviderInitialization(String indexName, @Cause Exception re);

   @LogMessage(level = ERROR)
   @Message(value = "Unable to properly close Lucene directory %s", id = 67)
   void unableToCloseLuceneDirectory(Object directory, @Cause Exception e);

   @LogMessage(level = ERROR)
   @Message(value = "Unable to retrieve object from message: %s", id = 68)
   void unableToRetrieveObjectFromMessage(Class<?> messageClass, @Cause Exception e);

   @LogMessage(level = ERROR)
   @Message(value = "Illegal object retrieved from message", id = 69)
   void illegalObjectRetrievedFromMessage(@Cause Exception e);

   @LogMessage(level = ERROR)
   @Message(value = "Terminating batch work! Index might end up in inconsistent state.", id = 70)
   void terminatingBatchWorkCanCauseInconsistentState();

   @LogMessage(level = ERROR)
   @Message(value = "Unable to properly shut down asynchronous indexing work", id = 71)
   void unableToShutdownAsyncronousIndexing(@Cause Exception e);

   @LogMessage(level = ERROR)
   @Message(value = "Couldn't open the IndexWriter because of previous error: operation skipped, index ouf of sync!", id = 72)
   void cannotOpenIndexWriterCausePreviousError();

   @LogMessage(level = ERROR)
   @Message(value = "Error in backend", id = 73)
   void backendError(@Cause Exception e);
   
}
