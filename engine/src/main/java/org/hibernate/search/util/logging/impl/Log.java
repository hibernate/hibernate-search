/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */

package org.hibernate.search.util.logging.impl;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.lucene.index.CorruptIndexException;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.search.SearchException;
import org.hibernate.search.backend.impl.jgroups.JGroupsChannelProvider;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.errors.EmptyQueryException;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Cause;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Logger.Level;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;
import org.jgroups.Address;
import org.jgroups.SuspectedException;
import org.jgroups.TimeoutException;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.TRACE;
import static org.jboss.logging.Logger.Level.WARN;

/**
 * Log abstraction layer for Hibernate Search on top of JBoss Logging.
 *
 * @author Davide D'Alto
 * @since 4.0
 */
@MessageLogger(projectCode = "HSEARCH")
public interface Log extends BasicLogger {

	@LogMessage(level = WARN)
	@Message(id = 1, value = "initialized \"blackhole\" backend. Index changes will be prepared but discarded!")
	void initializedBlackholeBackend();

	@LogMessage(level = INFO)
	@Message(id = 2, value = "closed \"blackhole\" backend.")
	void closedBlackholeBackend();

	@LogMessage(level = WARN)
	@Message(id = 3,
			value = "update DirectoryProviders \"blackhole\" backend. Index changes will be prepared but discarded!")
	void updatedDirectoryProviders();

	@LogMessage(level = ERROR)
	@Message(id = 4, value = "Exception attempting to instantiate Similarity '%1$s' set for %2$s")
	void similarityInstantiationException(String similarityName, String beanXClassName);

	@LogMessage(level = DEBUG)
	@Message(id = 5, value = "Starting JGroups ChannelProvider")
	void jGroupsStartingChannelProvider();

	@LogMessage(level = INFO)
	@Message(id = 6, value = "Connected to cluster [ %1$s ]. The local Address is %2$s")
	void jGroupsConnectedToCluster(String clusterName, Object address);

	@LogMessage(level = WARN)
	@Message(id = 7,
			value = "FLUSH is not present in your JGroups stack! FLUSH is needed to ensure messages are not dropped while new nodes join the cluster. Will proceed, but inconsistencies may arise!")
	void jGroupsFlushNotPresentInStack();

	@Message(id = 8, value = "Error while trying to create a channel using config file: %1$s")
	SearchException jGroupsChannelCreationUsingFileError(String configuration, @Cause Throwable e);

	@Message(id = 9, value = "Error while trying to create a channel using config XML: %1$s")
	SearchException jGroupsChannelCreationUsingXmlError(String configuration, @Cause Throwable e);

	@Message(id = 10, value = "Error while trying to create a channel using config string: %1$s")
	SearchException jGroupsChannelCreationFromStringError(String configuration, @Cause Throwable e);

	@LogMessage(level = INFO)
	@Message(id = 11,
			value = "Unable to use any JGroups configuration mechanisms provided in properties %1$s. Using default JGroups configuration file!")
	void jGroupsConfigurationNotFoundInProperties(Properties props);

	@LogMessage(level = WARN)
	@Message(id = 12,
			value = "Default JGroups configuration file was not found. Attempt to start JGroups channel with default configuration!")
	void jGroupsDefaultConfigurationFileNotFound();

	@LogMessage(level = INFO)
	@Message(id = 13, value = "Disconnecting and closing JGroups Channel to cluster '%1$s'")
	void jGroupsDisconnectingAndClosingChannel(String clusterName);

	@LogMessage(level = ERROR)
	@Message(id = 14, value = "Problem closing channel; setting it to null")
	void jGroupsClosingChannelError(@Cause Exception toLog);

	@LogMessage(level = INFO)
	@Message(id = 15, value = "Received new cluster view: %1$s")
	void jGroupsReceivedNewClusterView(Object view);

	@LogMessage(level = ERROR)
	@Message(id = 16, value = "Incorrect message type: %1$s")
	void incorrectMessageType(Class<?> messageType);

	@LogMessage(level = ERROR)
	@Message(id = 17, value = "Work discarded, thread was interrupted while waiting for space to schedule: %1$s")
	void interruptedWorkError(Runnable r);

	@LogMessage(level = INFO)
	@Message(id = 18, value = "Skipping directory synchronization, previous work still in progress: %1$s")
	void skippingDirectorySynchronization(String indexName);

	@LogMessage(level = WARN)
	@Message(id = 19, value = "Unable to remove previous marker file from source of %1$s")
	void unableToRemovePreviousMarket(String indexName);

	@LogMessage(level = WARN)
	@Message(id = 20, value = "Unable to create current marker in source of %1$s")
	void unableToCreateCurrentMarker(String indexName, @Cause Exception e);

	@LogMessage(level = ERROR)
	@Message(id = 21, value = "Unable to synchronize source of %1$s")
	void unableToSynchronizeSource(String indexName, @Cause Exception e);

	@LogMessage(level = WARN)
	@Message(id = 22,
			value = "Unable to determine current in source directory, will try again during the next synchronization")
	void unableToDetermineCurrentInSourceDirectory();

	@LogMessage(level = ERROR)
	@Message(id = 23, value = "Unable to compare %1$s with %2$s.")
	void unableToCompareSourceWithDestinationDirectory(String source, String destination);

	@LogMessage(level = WARN)
	@Message(id = 24, value = "Unable to reindex entity on collection change, id cannot be extracted: %1$s")
	void idCannotBeExtracted(String affectedOwnerEntityName);

	@LogMessage(level = WARN)
	@Message(id = 25, value = "Service provider has been used but not released: %1$s")
	void serviceProviderNotReleased(Class<?> class1);

	@LogMessage(level = ERROR)
	@Message(id = 26, value = "Fail to properly stop service: %1$s")
	void stopServiceFailed(Class<?> class1, @Cause Exception e);

	@LogMessage(level = INFO)
	@Message(id = 27, value = "Going to reindex %d entities")
	void indexingEntities(long count);

	@LogMessage(level = INFO)
	@Message(id = 28, value = "Reindexed %1$d entities")
	void indexingEntitiesCompleted(long nbrOfEntities);

	@LogMessage(level = INFO)
	@Message(id = 29, value = "Indexing completed. Reindexed %1$d entities. Unregistering MBean from server")
	void indexingCompletedAndMBeanUnregistered(long nbrOfEntities);

	@LogMessage(level = INFO)
	@Message(id = 30, value = "%1$d documents indexed in %2$d ms")
	void indexingDocumentsCompleted(long doneCount, long elapsedMs);

	@LogMessage(level = INFO)
	@Message(id = 31, value = "Indexing speed: %1$f documents/second; progress: %2$.2f%%")
	void indexingSpeed(float estimateSpeed, float estimatePercentileComplete);

	@LogMessage(level = WARN)
	// It's WARN only as it should not be really critical, and it is quite frequent on Windows
	@Message(id = 32, value = "Could not delete %1$s")
	void notDeleted(File file);

	@LogMessage(level = WARN)
	@Message(id = 33, value = "Could not change timestamp for %1$s. Index synchronization may be slow.")
	void notChangeTimestamp(File destFile);

	@LogMessage(level = INFO)
	@Message(id = 34, value = "Hibernate Search %1$s")
	void version(String versionString);

	@LogMessage(level = WARN)
	@Message(id = 35, value = "Could not close resource.")
	void couldNotCloseResource(@Cause Exception e);

	@LogMessage(level = WARN)
	@Message(id = 36, value = "Cannot guess the Transaction Status: not starting a JTA transaction")
	void cannotGuessTransactionStatus(@Cause Exception e);

	@LogMessage(level = WARN)
	@Message(id = 37, value = "Unable to properly close searcher during lucene query: %1$s")
	void unableToCloseSearcherDuringQuery(String query, @Cause Exception e);

	@LogMessage(level = WARN)
	@Message(id = 38, value = "Forced to use Document extraction to workaround FieldCache bug in Lucene")
	void forceToUseDocumentExtraction();

	@LogMessage(level = WARN)
	@Message(id = 39, value = "Unable to properly close searcher in ScrollableResults")
	void unableToCloseSearcherInScrollableResult(@Cause Exception e);

	@LogMessage(level = WARN)
	@Message(id = 40,
			value = "Unexpected: value is missing from FieldCache. This is likely a bug in the FieldCache implementation, " +
					"Hibernate Search might have to workaround this by slightly inaccurate faceting values or reduced performance.")
	void unexpectedValueMissingFromFieldCache();

	@LogMessage(level = INFO)
	@Message(id = 41, value = "Index directory not found, creating: '%1$s'")
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
	@Message(id = 45, value = "@ContainedIn is pointing to an entity having @ProvidedId: %1$s. " +
			"This is not supported, indexing of contained in entities will be skipped. " +
			"Indexed data of the embedded object might become out of date in objects of type ")
	void containedInPointsToProvidedId(Class<?> objectClass);

	@LogMessage(level = WARN)
	@Message(id = 46,
			value = "FieldCache was enabled on class %1$s but for this type of identifier we can't extract values from the FieldCache: cache disabled")
	void cannotExtractValueForIdentifier(Class<?> beanClass);

	@LogMessage(level = WARN)
	@Message(id = 47, value = "Unable to close JMS connection for %1$s")
	void unableToCloseJmsConnection(String jmsQueueName, @Cause Exception e);

	@LogMessage(level = WARN)
	@Message(id = 48, value = "Unable to retrieve named analyzer: %1$s")
	void unableToRetrieveNamedAnalyzer(String value);

	@LogMessage(level = WARN)
	@Message(id = 49,
			value = "Was interrupted while waiting for index activity to finish. Index might be inconsistent or have a stale lock")
	void interruptedWhileWaitingForIndexActivity(@Cause InterruptedException e);

	@LogMessage(level = WARN)
	@Message(id = 50, value = "It appears changes are being pushed to the index out of a transaction. " +
			"Register the IndexWorkFlushEventListener listener on flush to correctly manage Collections!")
	void pushedChangesOutOfTransaction();

	@LogMessage(level = WARN)
	@Message(id = 51, value = "Received null or empty Lucene works list in message.")
	void receivedEmptyLuceneWorksInMessage();

	@LogMessage(level = WARN)
	@Message(id = 52, value = "Going to force release of the IndexWriter lock")
	void forcingReleaseIndexWriterLock();

	@LogMessage(level = WARN)
	@Message(id = 53, value = "Chunk size must be positive: using default value.")
	void checkSizeMustBePositive();

	@LogMessage(level = WARN)
	@Message(id = 54, value = "ReaderProvider contains readers not properly closed at destroy time")
	void readersNotProperlyClosedInReaderProvider();

	@LogMessage(level = WARN)
	@Message(id = 55, value = "Unable to close Lucene IndexReader")
	void unableToCloseLuceneIndexReader(@Cause Exception e);

	@LogMessage(level = WARN)
	@Message(id = 56, value = "Unable to un-register existing MBean: %1$s")
	void unableToUnregisterExistingMBean(String name, @Cause Exception e);

	@LogMessage(level = WARN)
	@Message(id = 57, value = "Property hibernate.search.autoregister_listeners is set to false." +
			" No attempt will be made to register Hibernate Search event listeners.")
	void eventListenerWontBeRegistered();

	@LogMessage(level = ERROR)
	@Message(id = 58, value = "%1$s")
	void exceptionOccurred(String errorMsg, @Cause Throwable exceptionThatOccurred);

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
	void interruptedBatchIndexing();

	@LogMessage(level = ERROR)
	@Message(id = 63, value = "Error during batch indexing: ")
	void errorDuringBatchIndexing(@Cause Throwable e);

	@LogMessage(level = ERROR)
	@Message(id = 64, value = "Error while executing runnable wrapped in a JTA transaction")
	void errorExecutingRunnableInTransaction(@Cause Throwable e);

	@LogMessage(level = ERROR)
	@Message(id = 65, value = "Error while rolling back transaction after %1$s")
	void errorRollingBackTransaction(String message, @Cause Exception e1);

	@LogMessage(level = ERROR)
	@Message(id = 66, value = "Failed to initialize SlaveDirectoryProvider %1$s")
	void failedSlaveDirectoryProviderInitialization(String indexName, @Cause Exception re);

	@LogMessage(level = ERROR)
	@Message(id = 67, value = "Unable to properly close Lucene directory %1$s")
	void unableToCloseLuceneDirectory(Object directory, @Cause Exception e);

	@LogMessage(level = ERROR)
	@Message(id = 68, value = "Unable to retrieve object from message: %1$s")
	void unableToRetrieveObjectFromMessage(Class<?> messageClass, @Cause Exception e);

	@LogMessage(level = ERROR)
	@Message(id = 69, value = "Illegal object retrieved from message")
	void illegalObjectRetrievedFromMessage(@Cause Exception e);

	@LogMessage(level = ERROR)
	@Message(id = 70, value = "Terminating batch work! Index might end up in inconsistent state.")
	void terminatingBatchWorkCanCauseInconsistentState();

	@LogMessage(level = ERROR)
	@Message(id = 71, value = "Unable to properly shut down asynchronous indexing work")
	void unableToShutdownAsynchronousIndexing(@Cause Exception e);

	@LogMessage(level = ERROR)
	@Message(id = 72,
			value = "Couldn't open the IndexWriter because of previous error: operation skipped, index ouf of sync!")
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

	@Message(id = 76, value = "Could not open Lucene index: index data is corrupted. index name: '%1$s'")
	SearchException cantOpenCorruptedIndex(@Cause CorruptIndexException e, String indexName);

	@Message(id = 77, value = "An IOException happened while accessing the Lucene index '%1$s'")
	SearchException ioExceptionOnIndex(@Cause IOException e, String indexName);

	@LogMessage(level = ERROR)
	@Message(id = 78, value = "Timed out waiting to flush all operations to the backend of index %1$s")
	void unableToShutdownAsynchronousIndexingByTimeout(String indexName);

	@LogMessage(level = Level.DEBUG)
	@Message(id = 79, value = "Serialization protocol version %1$d.%2$d initialized")
	void serializationProtocol(int major, int minor);

	@LogMessage(level = ERROR)
	@Message(id = 80, value = "Received a remote message about an unknown index '%1$s': discarding message!")
	void messageReceivedForUndefinedIndex(String indexName);

	@LogMessage(level = WARN)
	@Message(id = 81,
			value = "The index '%1$s' is using a non-recommended combination of backend and directoryProvider implementations")
	void warnSuspiciousBackendDirectoryCombination(String indexName);

	@Message(id = 82, value = "Unable to start serialization layer")
	SearchException unableToStartSerializationLayer(@Cause Throwable e);

	@Message(id = 83, value = "Unable to serialize List<LuceneWork>")
	SearchException unableToSerializeLuceneWorks(@Cause Throwable e);

	@Message(id = 84, value = "Unable to read serialized List<LuceneWork>")
	SearchException unableToReadSerializedLuceneWorks(@Cause Throwable e);

	@Message(id = 85, value = "Attribute type is not recognized and not serializable: %1$s")
	SearchException attributeNotRecognizedNorSerializable(Class<?> attributeType);

	@Message(id = 86, value = "Unknown attribute serialized representation: %1$s")
	SearchException unknownAttributeSerializedRepresentation(String name);

	@Message(id = 87, value = "Unable to read TokenStream")
	SearchException unableToReadTokenStream();

	@Message(id = 88, value = "Unable to convert serializable TermVector to Lucene TermVector: %1$s")
	SearchException unableToConvertSerializableTermVectorToLuceneTermVector(String termVector);

	@Message(id = 89, value = "Unable to convert serializable Index to Lucene Index: %1$s")
	SearchException unableToConvertSerializableIndexToLuceneIndex(String index);

	@Message(id = 90, value = "Unable to convert serializable Store to Lucene Store: %1$s")
	SearchException unableToConvertSerializableStoreToLuceneStore(String store);

	@Message(id = 91, value = "Unknown NumericField type: %1$s")
	SearchException unknownNumericFieldType(String dataType);

	@Message(id = 92, value = "Conversion from Reader to String not yet implemented")
	SearchException conversionFromReaderToStringNotYetImplemented();

	@Message(id = 93, value = "Unknown Field type: %1$s")
	SearchException unknownFieldType(Class<?> fieldType);

	@Message(id = 94,
			value = "Cannot serialize custom Fieldable '%1$s'. Must be NumericField, Field or a Serializable Fieldable implementation.")
	SearchException cannotSerializeCustomField(Class<?> fieldType);

	@Message(id = 95, value = "Fail to serialize object of type %1$s")
	SearchException failToSerializeObject(Class<?> type, @Cause Throwable e);

	@Message(id = 96, value = "Fail to deserialize object")
	SearchException failToDeserializeObject(@Cause Throwable e);

	@Message(id = 97, value = "Unable to read file %1$s")
	SearchException unableToReadFile(String filename, @Cause Throwable e);

	@Message(id = 98, value = "Unable to parse message from protocol version %1$d.%2$d. "
			+ "Current protocol version: %3$d.%4$d")
	SearchException incompatibleProtocolVersion(int messageMajor, int messageMinor, int currentMajor, int currentMinor);

	@Message(id = 99, value = "Unable to deserialize Avro stream")
	SearchException unableToDeserializeAvroStream(@Cause Throwable e);

	@Message(id = 100, value = "Cannot deserialize operation %1$s, unknown operation.")
	SearchException cannotDeserializeOperation(String schema);

	@Message(id = 101, value = "Cannot deserialize field type %1$s, unknown field type.")
	SearchException cannotDeserializeField(String schema);

	@Message(id = 102, value = "Unable to serialize Lucene works in Avro")
	SearchException unableToSerializeInAvro(@Cause Throwable e);

	@Message(id = 103, value = "Unable to initialize IndexManager %1$s")
	SearchException unableToInitializeIndexManager(String indexName, @Cause Throwable e);

	@LogMessage(level = WARN)
	@Message(id = 104, value = "Ignoring backend option for index '%1$s', " +
			"configured IndexManager requires using '%2$s' instead.")
	void ignoringBackendOptionForIndex(String indexName, String forcedBackend);

	@Message(id = 105, value = "Cannot safely compute getResultSize() when a Criteria with restriction is used. " +
			"Use query.list().size() or query.getResultList().size(). Criteria at stake: %1$s")
	SearchException cannotGetResultSizeWithCriteriaAndRestriction(String criteria);

	@Message(id = 106, value = "Field %1$s looks like binary but couldn't be decompressed")
	SearchException fieldLooksBinaryButDecompressionFailed(String fieldName);

	@Message(id = 107, value = "Index names %1$s is not defined")
	SearchException requestedIndexNotDefined(String indexName);

	@LogMessage(level = Level.DEBUG)
	@Message(id = 108, value = "Shutting down backend for IndexManager '%1$s'")
	void shuttingDownBackend(String indexName);

	@Message(id = 109, value = "%1$s is not an indexed type")
	IllegalArgumentException notAnIndexedType(String entityName);

	@Message(id = 110, value = "'null' is not a valid indexed type")
	IllegalArgumentException nullIsInvalidIndexedType();

	@Message(id = 111, value = "At least one index name must be provided: can't open an IndexReader on nothing")
	IllegalArgumentException needAtLeastOneIndexName();

	@Message(id = 112, value = "At least one entity type must be provided: can't open an IndexReader on nothing")
	IllegalArgumentException needAtLeastOneIndexedEntityType();

	@Message(id = 113, value = "'null' is not a valid index name")
	IllegalArgumentException nullIsInvalidIndexName();

	@Message(id = 114, value = "Could not load resource: '%1$s'")
	SearchException unableToLoadResource(String fileName);

	@Message(id = 115, value = "Unknown @FullTextFilter: '%1$s'")
	SearchException unknownFullTextFilter(String filterName);

	/*
	 * This is not an exception factory nor a logging statement.
	 * The returned string is passed to the ErrorHandler,
	 * which is not necessarily using a logger but we still
	 * want to internationalize the message.
	 */
	@Message(id = 116, value = "Unexpected error during MassIndexer operation")
	String massIndexerUnexpectedErrorMessage();

	@Message(id = 117, value = "IOException on the IndexWriter")
	String ioExceptionOnIndexWriter();

	@Message(id = 118, value = "Exception during index Merge operation")
	String exceptionDuringIndexMergeOperation();

	@LogMessage(level = Level.DEBUG)
	@Message(id = 119, value = "Skipping optimization on index %1$s as it is already being optimized")
	void optimizationSkippedStillBusy(String indexName);

	@LogMessage(level = Level.WARN)
	@Message(id = 120, value = "There are multiple properties indexed against the same field name '%1$s', but with different indexing settings. The behaviour is undefined.")
	void inconsistentFieldConfiguration(String fieldName);

	@Message(id = 121, value = "Unable to connect to: [%1$s] JGroups channel")
	SearchException unableConnectingToJGroupsCluster(String clusterName, @Cause Throwable e);

	@Message(id = 122, value = "Unable to start JGroups channel")
	SearchException unableToStartJGroupsChannel(@Cause Throwable e);

	@Message(id = 123, value = "Unable to send Lucene update work via JGroups cluster")
	SearchException unableToSendWorkViaJGroups(@Cause Throwable e);

	@LogMessage(level = WARN)
	@Message(id = 124, value = "The option 'threadsForIndexWriter' of the MassIndexer is deprecated and is being ignored! Control the size of worker.thread_pool.size for each index instead.")
	void massIndexerIndexWriterThreadsIgnored();

	@LogMessage(level = TRACE)
	@Message(id = 125, value = "Interceptor enforces skip index operation %2$s on instance of class %1$s")
	void forceSkipIndexOperationViaInterception(Class<?> entityClass, WorkType type);

	@LogMessage(level = TRACE)
	@Message(id = 126, value = "Interceptor enforces removal of index data instead of index operation %2$s on instance of class %1$s")
	void forceRemoveOnIndexOperationViaInterception(Class<?> entityClass, WorkType type);

	@LogMessage(level = TRACE)
	@Message(id = 128, value = "Interceptor enforces update of index data instead of index operation %2$s on instance of class %1$s")
	void forceUpdateOnIndexOperationViaInterception(Class<?> entityClass, WorkType type);

	@Message(id = 129, value = "Object injected for JGroups channel in " + JGroupsChannelProvider.CHANNEL_INJECT + " is of an unexpected type %1$s (expecting org.jgroups.JChannel)")
	SearchException jGroupsChannelInjectionError(@Cause Exception e, Class<?> actualType);

	@Message(id = 130, value = "JGroups channel configuration should be specified in the global section [hibernate.search.services.jgroups.], " +
			"not as an IndexManager property for index '%1$s'. See http://docs.jboss.org/hibernate/search/4.1/reference/en-US/html_single/#jgroups-backend")
	SearchException legacyJGroupsConfigurationDefined(String indexName);

	@Message(id = 131, value = "The field used for the spatial query is not using SpatialFieldBridge: %1$s.%2$s")
	SearchException targetedFieldNotSpatial(String className, String fieldName);

	@Message(id = 133, value = "@ClassBridge implementation '%1$s' should implement either org.hibernate.search.bridge.FieldBridge, org.hibernate.search.bridge.TwoWayStringBridge or org.hibernate.search.bridge.StringBridge")
	SearchException noFieldBridgeInterfaceImplementedByClassBridge(String implName);

	@Message(id = 134, value = "Unable to instantiate ClassBridge of type %1$s defined on %2$s")
	SearchException cannotInstantiateClassBridgeOfType(String implName, String className, @Cause Throwable e);

	@Message(id = 135, value = "Unable to guess FieldBridge for %2$s in %1$s")
	SearchException unableToGuessFieldBridge(String className, String fieldName);

	@Message(id = 136, value = "Unable to instantiate Spatial defined on %1$s")
	SearchException unableToInstantiateSpatial(String className, @Cause Throwable e);

	@Message(id = 137, value = "@FieldBridge with no implementation class defined in: %1$s")
	SearchException noImplementationClassInFieldBridge(String className);

	@Message(id = 138, value = "@FieldBridge implementation implements none of the field bridge interfaces: %1$s in %2$s")
	SearchException noFieldBridgeInterfaceImplementedByFieldBridge(String implName, String appliedOnName);

	@Message(id = 139, value = "Unable to instantiate FieldBridge for %1$s of class %2$s")
	SearchException unableToInstantiateFieldBridge(String appliedOnName, String appliedOnTypeName, @Cause Throwable e);

	@Message(id = 140, value = "Unknown Resolution: %1$s")
	AssertionFailure unknownResolution(String resolution);

	@Message(id = 141, value = "Unknown ArrayBridge for resolution: %1$s")
	AssertionFailure unknownArrayBridgeForResolution(String resolution);

	@Message(id = 142, value = "Unknown MapBridge for resolution: %1$s")
	AssertionFailure unknownMapBridgeForResolution(String resolution);

	@Message(id = 143, value = "Unknown IterableBridge for resolution: %1$s")
	AssertionFailure unknownIterableBridgeForResolution(String resolution);

	@Message(id = 144, value = "FieldBridge passed in is not an instance of %1$s")
	SearchException fieldBridgeNotAnInstanceof(String className);

	@Message(id = 145, value = "Spatial field name not defined for class level annotation on class %1$s")
	SearchException spatialFieldNameNotDefined(String className);

	@Message(id = 146, value = "The query string '%2$s' applied on field '%1$s' has no meaningfull tokens to be matched. Validate the query input " +
			"against the Analyzer applied on this field.")
	EmptyQueryException queryWithNoTermsAfterAnalysis(String field, String searchTerm);

	@Message(id = 147, value = "Configured JGroups channel is a Muxer! MuxId option is required: define '" + JGroupsChannelProvider.MUX_ID + "'.")
	SearchException missingJGroupsMuxId();

	@Message(id = 148, value = "MuxId '%1$d' configured on the JGroups was already taken. Can't register handler!")
	SearchException jGroupsMuxIdAlreadyTaken(short n);

	@Message(id = 149, value = "Unable to determine ClassBridge for %1$s")
	SearchException unableToDetermineClassBridge(String className);

	@Message(id = 150, value = "Unable to get input stream from blob data")
	SearchException unableToGetInputStreamFromBlob(@Cause Throwable e);

	@Message(id = 151, value = "Unable to get input stream from blob data")
	SearchException unsupportedTikaBridgeType();

	@Message(id = 152, value = "File %1$s does not exist")
	SearchException fileDoesNotExist(String fileName);

	@Message(id = 153, value = "%1$s is a directory and not a file")
	SearchException fileIsADirectory(String fileName);

	@Message(id = 154, value = "File %1$s is not readable")
	SearchException fileIsNotReadable(String fileName);

	@Message(id = 155, value = "Unable to configure %1$s")
	SearchException unableToConfigureTikaBridge(String bridgeName, @Cause Throwable e);

	@Message(id = 156, value = "Cannot read %1$s field from a %2$s object : does getter exists and is it public ?")
	SearchException cannotReadFieldForClass(String fieldName, String className);

	@Message(id = 157, value = "Class %1$s does not implement the Coordinates interface")
	SearchException cannotExtractCoordinateFromObject(String className);

	@Message(id = 158, value = "Class %1$s cannot have two @Spatial using default/same name")
	SearchException cannotHaveTwoSpatialsWithDefaultOrSameName(String className);

	@Message(id = 159, value = "Cannot find a Coordinates interface nor @Latitude/@Longitude annotations bound to " +
			"the @Spatial name '%1$s' for class %2$s. It might be a typo (or a lapse) in @Latitude.of / @Longitude.of " +
			"attributes.")
	SearchException cannotFindCoordinatesNorLatLongForSpatial(String spatialName, String className);

	@Message(id = 160, value = "@Latitude definition for class '%1$s' is ambiguous: specified on both fields '%2$s' and '%3$s'")
	SearchException ambiguousLatitudeDefinition(String beanXClassName, String firstField, String secondField);

	@Message(id = 161, value = "@Longitude definition for class '%1$s' is ambiguous: specified on both fields '%2$s' and '%3$s'")
	SearchException ambiguousLongitudeDefinition(String beanXClassName, String firstField, String secondField);

	@Message(id = 162, value = "Unable to open JMS connection on queue '%2$s' for index '%1$s'")
	SearchException unableToOpenJMSConnection(String indexName, String jmsQueueName, @Cause Throwable e);

	@Message(id = 163, value = "Unable to send Search work to JMS queue '%2$s' for index '%1$s'")
	SearchException unableToSendJMSWork(String indexName, String jmsQueueName, @Cause Throwable e);

	@Message(id = 164, value = "Unable to lookup Search queue '%1$s' and connection factory '%2$s' for index '%3$s'")
	SearchException jmsLookupException(String jmsQueueName, String jmsConnectionFactoryName, String indexName, @Cause Throwable e);

	@Message(id = 165, value = "Illegal state for service initialization")
	SearchException illegalServiceBuildPhase();

	@LogMessage(level = Level.DEBUG)
	@Message(id = 166, value = "IndexManager factory resolved alias '%1$s' to '%2$s'.")
	void indexManagerAliasResolved(String alias, Class im);

	@Message(id = 167, value = "More than one @DocumentId specified on entity '%1$s'")
	SearchException duplicateDocumentIdFound(String beanXClassName);

	@LogMessage(level = Level.INFO)
	@Message(id = 168, value = "Serialization service %2$s being used for index '%1$s'")
	void indexManagerUsesSerializationService(String indexName, String serializerDescription);

	@Message(id = 169, value = "FieldBridge '%1$s' does not have a objectToString method: field '%2$s' in '%3$s'" +
			" The FieldBridge must be a TwoWayFieldBridge or you have to enable the ignoreFieldBridge option when defining a Query")
	SearchException fieldBridgeNotTwoWay(Class<? extends FieldBridge> bridgeClass, String fieldName, XClass beanXClass);

	@LogMessage(level = Level.DEBUG)
	@Message(id = 170, value = "Starting JGroups channel using configuration '%1$s'")
	void startingJGroupsChannel(Object cfg);

	@LogMessage(level = Level.DEBUG)
	@Message(id = 171, value = "Using JGroups channel having configuration '%1$s'")
	void jgroupsFullConfiguration(String printProtocolSpecAsXML);

	@LogMessage(level = Level.DEBUG)
	@Message(id = 172, value = "JGroups backend configured for index '%1$s' using block_for_ack '%2$s'")
	void jgroupsBlockWaitingForAck(String indexName, boolean block);

	@Message(id = 173, value = "Remote JGroups peer '%1$s' is suspected to have left '")
	SuspectedException jgroupsSuspectingPeer(Address sender);

	@Message(id = 174, value = "Timeout sending synchronous message to JGroups peer '%1$s''")
	TimeoutException jgroupsRpcTimeout(Address sender);

	@Message(id = 175, value = "Exception reported from remote JGroups node '%1$s' : '%2$s'")
	SearchException jgroupsRemoteException(Address sender, Throwable exception, @Cause Throwable cause);

	@Message(id = 176, value = "Document could not be parsed")
	SearchException unableToParseDocument(@Cause Throwable cause);

	@Message(id = 177, value = "Unable to find a valid document id for entity '%1$s'")
	SearchException noDocumentIdFoundException(String entityName);

	@Message(id = 178, value = "Unable to create a FullTextSession from a null Session")
	IllegalArgumentException getNullSessionPassedToFullTextSessionCreationException();

	@Message(id = 179, value = "Unable to create a FullTextEntityManager from a null EntityManager")
	IllegalArgumentException getNullSessionPassedToFullEntityManagerCreationException();

	@Message(id = 180, value = "Unable to cast %s of type %s to %s")
	ClassCastException getUnableToNarrowFieldDescriptorException(String actualDescriptorType, String type, String expectedType);

	@Message(id = 181, value = "'null' is not a valid property name")
	IllegalArgumentException getPropertyNameCannotBeNullException();

	@Message(id = 182, value = "'null' is not a valid field name")
	IllegalArgumentException getFieldNameCannotBeNullException();

	@Message(id = 183, value = "Unable to index instance of type %s while batch indexing: %s")
	String massIndexerUnableToIndexInstance(String clazz, String value);

	@Message(id = 184, value = "Cannot define an entity with 0 shard on '%1$s'")
	SearchException entityWithNoShard(Class<?> type);

	@Message(id = 185, value = "Cannot set a sharding strategy when using dynamic sharding on '%1$s'")
	SearchException illegalStrategyWhenUsingDynamicSharding(Class<?> type);

	@Message(id = 186, value = "[AssertionFailure: open a bug report] SearchFactory from entityIndexBinder is not assignable to WorkerBuilderContext. Actual class is %1$s")
	SearchException assertionFailureCannotCastToWorkerBuilderContext(Class<?> type);

	@Message(id = 187, value = "Multiple inconsistent similarities defined in the class hierarchy of %s")
	SearchException getMultipleInconsistentSimilaritiesInClassHierarchyException(String className);

	@Message(id = 188, value = "Inconsistent similarities defined via annotations (%s) and configuration (%s)")
	SearchException getInconsistentSimilaritySettingBetweenAnnotationsAndConfigPropertiesException(String annotationBasedSimilarity, String configLevelSimilarity);

	@Message(id = 189, value = "Multiple entities are sharing the same index but are declaring an " +
			"inconsistent Similarity. When overriding default Similarity make sure that all types sharing a same index " +
			"declare the same Similarity implementation. %s defines similarity %s and %s defines similarity %s")
	SearchException getMultipleEntitiesShareIndexWithInconsistentSimilarityException(String class1, String similarity1, String class2, String similarity2);

	@Message(id = 190, value = "Unable to start HibernateSessionFactoryServiceProvider. There is no session factory in the context. Are you sure you have Hibernate ORM enabled?")
	SearchException getNoSessionFactoryInContextException();

	@Message(id = 191, value = "The number of shards must be >= 1. %s is an illegal value.")
	SearchException getInvalidShardCountException(int value);

	@Message(id = 192, value = "%s")
	SearchException getInvalidIntegerValueException(String msg, @Cause Throwable throwable);

	@LogMessage(level = Level.INFO)
	@Message(id = 193, value = "Selected sharding strategy is IdHashShardingStrategy. However, there is only 1 shard configured. Have you set the 'nbr_of_shards' property?")
	void idHashShardingWithSingleShard();

	@Message(id = 194, value = "Unable to load configured class '%s' as 'sharding_strategy'")
	SearchException getUnableToLoadShardingStrategyClassException(String className);

	@Message(id = 195, value = "Configuration property '%s' should not be empty: illegal format.")
	SearchException configuratioPropertyCantBeEmpty(String key);

}
