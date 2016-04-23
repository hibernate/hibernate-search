/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.util.logging.impl;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XMember;
import org.hibernate.search.backend.spi.DeletionQuery;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.exception.EmptyQueryException;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.store.DirectoryProvider;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger.Level;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.FormatWith;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

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

	int JGROUPS_BACKEND_MESSAGES_START_ID = 200000;
	int AVRO_SERIALIZATION_MESSAGES_START_ID = 300000;

	@LogMessage(level = WARN)
	@Message(id = 1, value = "initialized \"blackhole\" backend. Index changes will be prepared but discarded!")
	void initializedBlackholeBackend();

	@LogMessage(level = INFO)
	@Message(id = 2, value = "closed \"blackhole\" backend.")
	void closedBlackholeBackend();

	@LogMessage(level = ERROR)
	@Message(id = 16, value = "Incorrect message type: %1$s")
	void incorrectMessageType(@FormatWith(ClassFormatter.class) Class<?> messageType);

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
	void serviceProviderNotReleased(@FormatWith(ClassFormatter.class) Class<?> class1);

	@LogMessage(level = ERROR)
	@Message(id = 26, value = "Fail to properly stop service: %1$s")
	void stopServiceFailed(@FormatWith(ClassFormatter.class) Class<?> class1, @Cause Exception e);

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
	@Message(id = 44, value = "Abstract classes cannot be indexed directly. Only concrete subclasses can be indexed. @Indexed on '%s' is superfluous and should be removed.")
	void abstractClassesCannotInsertDocuments(String clazz);

	@LogMessage(level = WARN)
	@Message(id = 45, value = "@ContainedIn is pointing to an entity having @ProvidedId: %1$s. " +
			"This is not supported, indexing of contained in entities will be skipped. " +
			"Indexed data of the embedded object might become out of date in objects of type ")
	void containedInPointsToProvidedId(@FormatWith(ClassFormatter.class) Class<?> objectClass);

	@LogMessage(level = WARN)
	@Message(id = 46,
			value = "FieldCache was enabled on class %1$s but for this type of identifier we can't extract values from the FieldCache: cache disabled")
	void cannotExtractValueForIdentifier(@FormatWith(ClassFormatter.class) Class<?> beanClass);

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

	@LogMessage(level = ERROR)
	@Message(id = 58, value = "%1$s")
	void exceptionOccurred(String errorMsg, @Cause Throwable exceptionThatOccurred);

	@LogMessage(level = ERROR)
	@Message(id = 59, value = "Worker raises an exception on close()")
	void workerException(@Cause Exception e);

	@LogMessage(level = ERROR)
	@Message(id = 62, value = "Batch indexing was interrupted")
	void interruptedBatchIndexing();

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
	void unableToRetrieveObjectFromMessage(@FormatWith(ClassFormatter.class) Class<?> messageClass, @Cause Exception e);

	@LogMessage(level = ERROR)
	@Message(id = 69, value = "Illegal object retrieved from message")
	void illegalObjectRetrievedFromMessage(@Cause Exception e);

	@LogMessage(level = ERROR)
	@Message(id = 72,
			value = "Couldn't open the IndexWriter because of previous error: operation skipped, index ouf of sync!")
	void cannotOpenIndexWriterCausePreviousError();

	@LogMessage(level = ERROR)
	@Message(id = 73, value = "Error in backend")
	void backendError(@Cause Exception e);

	@LogMessage(level = WARN)
	@Message(id = 75, value = "Configuration setting " + Environment.LUCENE_MATCH_VERSION
			+ " was not specified: using LUCENE_CURRENT.")
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

	@Message(id = 83, value = "Unable to serialize List<LuceneWork>")
	SearchException unableToSerializeLuceneWorks(@Cause Throwable e);

	@Message(id = 84, value = "Unable to read serialized List<LuceneWork>")
	SearchException unableToReadSerializedLuceneWorks(@Cause Throwable e);

	@Message(id = 85, value = "Attribute type is not recognized and not serializable: %1$s")
	SearchException attributeNotRecognizedNorSerializable(@FormatWith(ClassFormatter.class) Class<?> attributeType);

	@Message(id = 86, value = "Unknown attribute serialized representation: %1$s")
	SearchException unknownAttributeSerializedRepresentation(String name);

	@Message(id = 87, value = "Unable to read TokenStream")
	SearchException unableToReadTokenStream();

	@Message(id = 90, value = "Unable to convert serializable Store to Lucene Store: %1$s")
	SearchException unableToConvertSerializableStoreToLuceneStore(String store);

	@Message(id = 91, value = "Unknown NumericField type: %1$s")
	SearchException unknownNumericFieldType(String dataType);

	@Message(id = 92, value = "Conversion from Reader to String not yet implemented")
	SearchException conversionFromReaderToStringNotYetImplemented();

	@Message(id = 93, value = "Unknown Field type: %1$s")
	SearchException unknownFieldType(@FormatWith(ClassFormatter.class) Class<?> fieldType);

	@Message(id = 94,
			value = "Cannot serialize custom Fieldable '%1$s'. Must be NumericField, Field or a Serializable Fieldable implementation.")
	SearchException cannotSerializeCustomField(@FormatWith(ClassFormatter.class) Class<?> fieldType);

	@Message(id = 95, value = "Fail to serialize object of type %1$s")
	SearchException failToSerializeObject(@FormatWith(ClassFormatter.class) Class<?> type, @Cause Throwable e);

	@Message(id = 96, value = "Fail to deserialize object")
	SearchException failToDeserializeObject(@Cause Throwable e);

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

	@Message(id = 103, value = "Unable to initialize IndexManager named '%1$s'")
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
	@Message(id = 120, value = "There are multiple properties indexed against the same field name '%1$s.%2$s', but with different indexing settings. The behaviour is undefined.")
	void inconsistentFieldConfiguration(String className, String fieldName);

	@LogMessage(level = TRACE)
	@Message(id = 125, value = "Interceptor enforces skip index operation %2$s on instance of class %1$s")
	void forceSkipIndexOperationViaInterception(@FormatWith(ClassFormatter.class) Class<?> entityClass, WorkType type);

	@LogMessage(level = TRACE)
	@Message(id = 126, value = "Interceptor enforces removal of index data instead of index operation %2$s on instance of class %1$s")
	void forceRemoveOnIndexOperationViaInterception(@FormatWith(ClassFormatter.class) Class<?> entityClass, WorkType type);

	@LogMessage(level = TRACE)
	@Message(id = 128, value = "Interceptor enforces update of index data instead of index operation %2$s on instance of class %1$s")
	void forceUpdateOnIndexOperationViaInterception(@FormatWith(ClassFormatter.class) Class<?> entityClass, WorkType type);

	@Message(id = 131, value = "The field '%1$s#%2$s' used for the spatial query is not configured as spatial field. Check the proper use of @Spatial respectively SpatialFieldBridge")
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

	@Message(id = 144, value = "FieldBridge passed in is not an instance of %1$s")
	SearchException fieldBridgeNotAnInstanceof(String className);

	@Message(id = 146, value = "The query string '%2$s' applied on field '%1$s' has no meaningful tokens to be matched. Validate the query input " +
			"against the Analyzer applied on this field.")
	EmptyQueryException queryWithNoTermsAfterAnalysis(String field, String searchTerm);

	@Message(id = 149, value = "Unable to determine ClassBridge for %1$s")
	SearchException unableToDetermineClassBridge(String className);

	@Message(id = 150, value = "Unable to get input stream from blob data")
	SearchException unableToGetInputStreamFromBlob(@Cause Throwable e);

	@Message(id = 151, value = "Unable to get input stream from object of type %1$s")
	SearchException unsupportedTikaBridgeType(@FormatWith(ClassFormatter.class) Class<?> objectType);

	@Message(id = 152, value = "File %1$s does not exist")
	SearchException fileDoesNotExist(String fileName);

	@Message(id = 153, value = "%1$s is a directory and not a file")
	SearchException fileIsADirectory(String fileName);

	@Message(id = 154, value = "File %1$s is not readable")
	SearchException fileIsNotReadable(String fileName);

	@Message(id = 155, value = "Unable to configure %1$s")
	SearchException unableToConfigureTikaBridge(String bridgeName, @Cause Throwable e);

	@Message(id = 156, value = "Cannot read %1$s field from a %2$s object: does getter exist and is it public ?")
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

	@Message(id = 164, value = "Unable to lookup Search queue '%1$s' for index '%2$s'")
	SearchException jmsQueueLookupException(String jmsQueueName, String indexName, @Cause Throwable e);

	@LogMessage(level = Level.DEBUG)
	@Message(id = 166, value = "IndexManager factory resolved alias '%1$s' to '%2$s'.")
	void indexManagerAliasResolved(String alias, @FormatWith(ClassFormatter.class) Class<?> im);

	@Message(id = 167, value = "More than one @DocumentId specified on entity '%1$s'")
	SearchException duplicateDocumentIdFound(String beanXClassName);

	@LogMessage(level = Level.INFO)
	@Message(id = 168, value = "Serialization service %2$s being used for index '%1$s'")
	void indexManagerUsesSerializationService(String indexName, String serializerDescription);

	@Message(id = 169, value = "FieldBridge '%1$s' does not have a objectToString method: field '%2$s' in '%3$s'" +
			" The FieldBridge must be a TwoWayFieldBridge or you have to enable the ignoreFieldBridge option when defining a Query")
	SearchException fieldBridgeNotTwoWay(@FormatWith(ClassFormatter.class) Class<? extends FieldBridge> bridgeClass, String fieldName, XClass beanXClass);

	@Message(id = 176, value = "Document could not be parsed")
	SearchException unableToParseDocument(@Cause Throwable cause);

	@Message(id = 177, value = "Unable to find a valid document id for entity '%1$s'")
	SearchException noDocumentIdFoundException(String entityName);

	@Message(id = 178, value = "Unable to create a FullTextSession from a null Session")
	IllegalArgumentException getNullSessionPassedToFullTextSessionCreationException();

	@Message(id = 179, value = "Unable to create a FullTextEntityManager from a null EntityManager")
	IllegalArgumentException getNullEntityManagerPassedToFullEntityManagerCreationException();

	@Message(id = 180, value = "Unable to cast %s of type %s to %s")
	ClassCastException getUnableToNarrowFieldDescriptorException(String actualDescriptorType, String type, String expectedType);

	@Message(id = 181, value = "'null' is not a valid property name")
	IllegalArgumentException getPropertyNameCannotBeNullException();

	@Message(id = 182, value = "'null' is not a valid field name")
	IllegalArgumentException getFieldNameCannotBeNullException();

	@Message(id = 183, value = "Unable to index instance of type %s while batch indexing: %s")
	String massIndexerUnableToIndexInstance(String clazz, String value);

	@Message(id = 184, value = "Cannot define an entity with 0 shard on '%1$s'")
	SearchException entityWithNoShard(@FormatWith(ClassFormatter.class) Class<?> type);

	@Message(id = 186, value = "[AssertionFailure: open a bug report] SearchFactory from entityIndexBinding is not assignable to WorkerBuilderContext. Actual class is %1$s")
	SearchException assertionFailureCannotCastToWorkerBuilderContext(@FormatWith(ClassFormatter.class) Class<?> type);

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

	@Message(id = 195, value = "Multiple service implementations detected for service '%1$s': '%2$s'")
	SearchException getMultipleServiceImplementationsException(String service, String foundServices);

	@Message(id = 196, value = "No service implementations for service '%1$s' can be found")
	SearchException getNoServiceImplementationFoundException(String service);

	@Message(id = 197, value = "Unable to create JGroups backend. Are you sure you have the JGroups dependencies on the classpath?")
	SearchException getUnableToCreateJGroupsBackendException(@Cause Throwable throwable);

	@Message(id = 198, value = "Unexpected status '%s' for service '%s'. Check for circular dependencies or unreleased resources in your services.")
	SearchException getUnexpectedServiceStatusException(String status, String service);

	@Message(id = 199, value = "Configuration property '%s' should not be empty: illegal format.")
	SearchException configurationPropertyCantBeEmpty(String key);

	@Message(id = 201, value = "The edit distance must be either 1 or 2")
	SearchException incorrectEditDistance();

	@Message(id = 202, value = "Unable to find entity $1%s with id $2%s")
	SearchException entityWithIdNotFound(@FormatWith(ClassFormatter.class) Class<?> entityType, String id);

	@Message(id = 203, value = "No field from %s can be used for More Like This queries. They are neither stored or including the term vectors.")
	SearchException noFieldCompatibleForMoreLikeThis(@FormatWith(ClassFormatter.class) Class<?> entityType);

	@Message(id = 205, value = "An IOException happened while accessing the Lucene indexes related to '%1$s'")
	SearchException ioExceptionOnIndexOfEntity(@Cause IOException e, @FormatWith(ClassFormatter.class) Class<?> entityType);

	@Message(id = 206, value = "MoreLikeThis queries require a TFIDFSimilarity for entity '$1%s'")
	SearchException requireTFIDFSimilarity(@FormatWith(ClassFormatter.class) Class<?> beanClass);

	@Message(id = 207, value = "Field %s of entity %s cannot be used in a MoreLikeThis query: the term vector (preferred) or the value itself need to be stored.")
	SearchException fieldNotStoredNorTermVectorCannotBeUsedInMoreLikeThis(String fieldName, @FormatWith(ClassFormatter.class) Class<?> entityType);

	@Message(id = 208, value = "ClassLoaderService cannot be provided via SearchConfiguration#getProvidedServices. Use SearchConfiguration#getClassLoaderService!")
	SearchException classLoaderServiceContainedInProvidedServicesException();

	@Message(id = 209, value = "It is not allowed to request further services after ServiceManager#releaseAll has been called.")
	IllegalStateException serviceRequestedAfterReleasedAllWasCalled();

	@Message(id = 210, value = "Provided service '%s' implements '%s'. Provided services are not allowed to implement either Startable or Stoppable.")
	SearchException providedServicesCannotImplementStartableOrStoppable(String service, String implementedInterface);

	@Message(id = 211, value = "An exception occurred while the MassIndexer was fetching the primary identifiers list")
	String massIndexerExceptionWhileFetchingIds();

	@Message(id = 212, value = "An exception occurred while the MassIndexer was transforming identifiers to Lucene Documents")
	String massIndexerExceptionWhileTransformingIds();

	@Message(id = 213, value = "Field %s of entity %s cannot be used in a MoreLikeThis query. Ids and embedded ids are excluded.")
	SearchException fieldIdCannotBeUsedInMoreLikeThis(String fieldName, @FormatWith(ClassFormatter.class) Class<?> entityType);

	@Message(id = 214, value = "Field %s of entity %s cannot be used in a MoreLikeThis query. Numeric fields are not considered for the moment.")
	SearchException numericFieldCannotBeUsedInMoreLikeThis(String fieldName, @FormatWith(ClassFormatter.class) Class<?> entityType);

	@Message(id = 215, value = "Multiple matching FieldBridges found for %s of return type %s: %s" )
	SearchException multipleMatchingFieldBridges(XMember member, XClass memberType, String listOfFieldBridges);

	@Message(id = 216, value = "Found invalid @IndexedEmbedded->paths elements configured for member '%s' of class '%s'. The invalid paths are [%s]" )
	SearchException invalidIncludePathConfiguration(String member, String clazz, String invalidPaths);

	@Message(id = 217, value = "Invalid value '%s' for setting '%s'. Check the documentation for allowed values." )
	SearchException invalidPropertyValue(String value, String property);

	@Message(id = 218, value = "More like this query cannot be created, because the index does not contain a field '%s' for the type '%s" )
	SearchException unknownFieldNameForMoreLikeThisQuery(String field, String type);

	@Message(id = 219, value = "Could not lookup initial JNDI context for the JMS ConnectionFactory named '%s' for the index '%s" )
	SearchException jmsInitialContextException(String jmsConnectionFactoryName, String indexName, @Cause Exception e);

	@Message(id = 220, value = "Could not lookup JMS ConnectionFactory named '%1s' for the index '%2s" )
	SearchException jmsQueueFactoryLookupException(String jmsConnectionFactoryName, String indexName, @Cause Exception e);

	@Message(id = 221, value = "Circular reference. Duplicate use of %1s in root entity %2s#%3s Set the @IndexedEmbedded.depth value explicitly to fix the problem.")
	SearchException detectInfiniteTypeLoopInIndexedEmbedded(String elementClass, String rootEntity, String path);

	@Message(id = 222, value = "The SearchFactory was not initialized" )
	SearchException searchIntegratorNotInitialized();

	@Message(id = 223, value = "The Service org.hibernate.search.hcore.impl.SearchFactoryReference was not found in the Hibernate ORM Service Registry."
			+ " This might be caused by the Hibernate ORM classloader not having visibility on Hibernate Search" )
	SearchException searchFactoryReferenceServiceNotFound();

	@Message(id = 224, value = "Non optional parameter named '%s' was null" )
	AssertionFailure parametersShouldNotBeNull(String parameterName);

	@LogMessage(level = Level.WARN)
	@Message(id = 225, value = "An index locking error occurred during initialization of Directory '%s'. This might indicate a concurrent initialization; "
			+ "If you experience errors on this index you might need to remove the lock, or rebuild the index." )
	void lockingFailureDuringInitialization(String directoryDescription);

	@LogMessage(level = TRACE)
	@Message(id = 226, value = "%s: %s" )
	void logInfoStreamMessage(String componentName, String message);

	@Message(id = 227, value = "A BooleanQuery is not valid without at least one clause. Use at least one of should(Query) or must(Query)." )
	SearchException booleanQueryWithoutClauses();

	@Message(id = 228, value = "Property " + Environment.LUCENE_MATCH_VERSION + " set to value '%s' is not in a valid format to express a Lucene version: %s" )
	SearchException illegalLuceneVersionFormat(String property, String luceneErrorMessage);

	@Message(id = 229, value = "Property " + Environment.INDEX_FLUSH_INTERVAL + "for the index '%s' needs to be positive." )
	SearchException flushIntervalNeedsToBePositive(String indexName);

	@LogMessage(level = DEBUG)
	@Message(id = 230, value = "Starting sync consumer thread for index '%s'" )
	void startingSyncConsumerThread(String indexName);

	@LogMessage(level = DEBUG)
	@Message(id = 231, value = "Stopping sync consumer thread for index '%s'" )
	void stoppingSyncConsumerThread(String indexName);

	@Message(id = 232, value = "The specified query '%s' contains a numeric sub query which targets the string encoded field(s) '%s'. Check your query or try limiting the targeted entities." )
	SearchException stringEncodedFieldsAreTargetedWithNumericQuery(String query, String numericFields);

	@Message(id = 233, value = "The specified query '%s' contains a string based sub query which targets the numeric encoded field(s) '%s'. Check your query or try limiting the targeted entities." )
	SearchException numericEncodedFieldsAreTargetedWithStringQuery(String query, String numericFields);

	@Message(id = 234, value = "None of the specified entity types ('%s') or any of their subclasses are indexed." )
	IllegalArgumentException targetedEntityTypesNotIndexed(String targetedEntities);

	@LogMessage(level = Level.DEBUG)
	@Message(id = 235, value = "Backend for index '%s' started: using a Synchronous batching backend." )
	void luceneBackendInitializedSynchronously(String indexName);

	@LogMessage(level = Level.DEBUG)
	@Message(id = 236, value = "Backend for index '%s' started: using an Asynchronous backend with periodic commits." )
	void luceneBackendInitializedAsynchronously(String indexName);

	@Message(id = 237, value = "Cannot create numeric range query for field '%s', since from and to values are null" )
	SearchException rangeQueryWithNullToAndFromValue(String fieldName);

	@Message(id = 238, value = "Cannot create numeric range query for field '%s', since values are not numeric (Date, int, long, short or double)")
	SearchException numericRangeQueryWithNonNumericToAndFromValues(String fieldName);

	@Message(id = 239, value = "Unknown field encoding type: %1$s")
	AssertionFailure unknownEncodingType(String encoding);

	@Message(id = 240, value = "Unable to parse value '%2$s' of field '%1$s' into a Date")
	SearchException invalidStringDateFieldInDocument(String fieldName, String value);

	@Message(id = 241, value = "Multiple @Factory methods defined in %s")
	SearchException multipleFactoryMethodsInClass(String className);

	@Message(id = 242, value = "Search requires '%s' to have a public no-arg constructor in order to instantiate it")
	SearchException noPublicNoArgConstructor(String className);

	@Message(id = 243, value = "Unable to access class '%s'")
	SearchException unableToAccessClass(String className);

	@Message(id = 244, value = "Factory methods must return an object. '%1$s#%2$s' does not")
	SearchException factoryMethodsMustReturnAnObject(String className, String methodName);

	@Message(id = 245, value = "Unable to access method '%1$s#%2$s'")
	SearchException unableToAccessMethod(String className, String methodName);

	@Message(id = 246, value = "An exception occurred while invoking '%1$s#%2$s'")
	SearchException exceptionDuringFactoryMethodExecution(@Cause Exception e, String className, String methodName);

	@Message(id = 247, value = "An indexed field defined on '%1$s:%2$s' tries to override the id field settings. The document id field settings cannot be modified. Use a different field name.")
	SearchException fieldTriesToOverrideIdFieldSettings(String className, String propertyName);

	@LogMessage(level = Level.TRACE)
	@Message(id = 248, value = "WorkList should never be empty. Stacktrace below \n %s" )
	void workListShouldNeverBeEmpty(String stackTrace);

	@LogMessage(level = Level.INFO)
	@Message(id = 249, value = "Cannot do fast deletes on index '%s'. Entities in this index are conflicting or the index can accept unknown entities." )
	void singleTermDeleteDisabled(String indexName);

	@Message(id = 250, value = "Unsupported value type for configuration property " + Environment.ERROR_HANDLER + ": %1$s")
	SearchException unsupportedErrorHandlerConfigurationValueType(@FormatWith(ClassFormatter.class) Class<?> errorHandlerValueType);

	@Message(id = 251, value = "Unable to set filter parameter '%2$s' on filter class %1$s")
	SearchException unableToSetFilterParameter(@FormatWith(ClassFormatter.class) Class<?> filterClass, String parameterName, @Cause Exception e);

	@Message(id = 252, value = "Unable to initialize directory provider %1$s for index %2$s")
	SearchException cannotInitializeDirectoryProvider(@FormatWith(ClassFormatter.class) Class<? extends DirectoryProvider> directoryProviderType, String indexName, @Cause Exception e);

	@Message(id = 253, value = "To use '%1$s' as a locking strategy, an indexBase path must be set")
	SearchException indexBasePathRequiredForLockingStrategy(String strategy);

	@Message(id = 254, value = "Unknown indexing mode: %1$s")
	SearchException unknownIndexingMode(String indexingMode);

	@Message(id = 255, value = "Unknown DocValues type: %1$s")
	SearchException unknownDocValuesTypeType(String docValuesType);

	@Message(id = 256, value = "'%1$s' is an unexpected type for a binary doc value")
	SearchException unexpectedBinaryDocValuesTypeType(String docValuesType);

	@Message(id = 257, value = "'%1$s' is an unexpected type for a numeric doc value")
	SearchException unexpectedNumericDocValuesTypeType(String docValuesType);

	@LogMessage(level = Level.DEBUG)
	@Message(id = 258, value = "Attempting to load a field named '%s' from the Lucene Document. This Document instance doesn't have such a field." )
	void loadingNonExistentField(String name);

	@Message(id = 259, value = "Unable to delete all %s matching Query: %s")
	SearchException unableToDeleteByQuery(@FormatWith(ClassFormatter.class) Class<?> entityClass, DeletionQuery deletionQuery, @Cause Exception e );

	// Only used in ORM; Defining it here for now until there is a Log interface in hibernate-search-orm
	@LogMessage(level = Level.WARN)
	@Message(id = 260, value = "A criteria for loading query results has been specified via "
			+ "FullTextQuery#setCriteriaQuery(), but query results originate from several id spaces. The given "
			+ "criteria object can therefore not be be applied.")
	void givenCriteriaObjectCannotBeApplied();

	@Message(id = 261, value = "An unknown DeletionQuery key was specified during de-serialization of a message from another node: %d")
	SearchException unknownDeletionQueryKeySpecified(int queryKey);

	@Message(id = 262, value = "@NumericField annotation is used on %1$s#%2$s without a matching @Field annotation")
	SearchException numericFieldAnnotationWithoutMatchingField(@FormatWith(ClassFormatter.class) Class<?> entityClass, String memberName);

	@Message(id = 263, value = "@Facet annotation is used on %1$s#%2$s without a matching @Field annotation")
	SearchException facetAnnotationWithoutMatchingField(String className, String memberName);

	@Message(id = 264, value = "@Facet is not supported for type '%1$s'. See %2$s#%3$s")
	SearchException unsupportedFieldTypeForFaceting(String valueType, String className, String memberName);

	@Message(id = 265, value = "Unable to build Lucene Document due to facet indexing error")
	SearchException errorDuringFacetingIndexing(@Cause Exception e );

	@Message(id = 266, value = "'%s' is not a valid type for a facet range request. Numbers (byte, short, int, long, float, double and their wrappers) as well as dates are supported")
	SearchException unsupportedFacetRangeParameter(String type);

	@Message(id = 267, value = "Unable to index date facet '%1$s' for field '%2$s', since the matching field is not using a numeric field bridge")
	SearchException numericDateFacetForNonNumericField(String facetName, String fieldName);

	@Message(id = 268, value = "Facet request '%1$s' tries to facet on  field '%2$s' which either does not exist or is not configured for faceting (via @Facet). Check your configuration.")
	SearchException unknownFieldNameForFaceting(String facetName, String facetFieldName);

	@Message(id = 269, value = "'%1$s' is not a supported type for a range faceting request parameter. Supported types are: '%2$s'")
	SearchException unsupportedParameterTypeForRangeFaceting(String facetRangeParameterType, String supportedTypes);

	@Message(id = 270, value = "At least of of the facets ranges in facet request '%1$s' contains neither start nor end value")
	SearchException noStartOrEndSpecifiedForRangeQuery(String facetRequestName);

	@Message(id = 271, value = "RANGE_DEFINITION_ORDER is not a valid sort order for a discrete faceting request.")
	SearchException rangeDefinitionOrderRequestedForDiscreteFacetRequest();

	@Message(id = 272, value = "Entity '%1$s' is not an indexed entity. Unable to create faceting request")
	SearchException attemptToCreateFacetingRequestForUnindexedEntity(String entityName);

	@Message(id = 273, value = "The indexed field '%1$s' in '%2$s' is analyzed and marked for faceting. Only un-analyzed fields can be faceted.")
	SearchException attemptToFacetOnAnalyzedField(String fieldName, String entityName);

	@LogMessage(level = Level.DEBUG)
	@Message(id = 274, value = "Executing Lucene query '%s'" )
	void executingLuceneQuery(Query luceneQuery);

	@Message(id = 275, value = "SerializationProvider service not found on the classpath. You should check that an implementation exists and it's correctly registered as a service."
			+ " If that's not the case, you can also create a custom implementation or add \"org.hibernate:hibernate-search-serialization-avro\" on the classpath")
	SearchException serializationProviderNotFoundException(@Cause Exception cause);

	@Message(id = 276, value = "No transaction is active while indexing entity type '%1$s'; Consider increasing the connection time-out")
	SearchException transactionNotActiveWhileProducingIdsForBatchIndexing(@FormatWith(ClassFormatter.class) Class<?> entityClass);

	@Message(id = 277, value = "Worker configured to be enlisted in transaction but the backend %1$s is not transactional for index %2$s")
	SearchException backendNonTransactional(String indexName, String backend);

	@Message(id = 278, value = "Can't build query for type '%1$s' which is neither indexed nor has any indexed sub-types.")
	SearchException cantQueryUnindexedType(String canonicalEntityName);

	@Message(id = 279, value = "Unable to load the UTF-8 Charset!")
	AssertionFailure assertionNotLoadingUTF8Charset(@Cause UnsupportedEncodingException e);

	@Message(id = 280, value = "Source directory does not exist: '%1$s")
	SearchException sourceDirectoryNotExisting(String directory);

	@Message(id = 281, value = "Unable to read directory: '%1$s")
	SearchException directoryIsNotReadable(String directory);

	@Message(id = 282, value = "Distance sort can only be used with spatial fields: '%1$s' is not spatial")
	SearchException distanceSortRequiresSpatialField(String field);

	@Message(id = 283, value = "Sorting using '%1$s' requires an indexed field: '%2$s' is not valid")
	SearchException sortRequiresIndexedField(@FormatWith(ClassFormatter.class) Class<?> sortFieldType, String field);

	@Message(id = 284, value = "An IOException happened while opening multiple indexes" )
	SearchException ioExceptionOnMultiReaderRefresh(@Cause IOException e);

	@Message(id = 285, value = "Could not create uninverting reader for reader %s" )
	SearchException couldNotCreateUninvertingReader(DirectoryReader reader, @Cause IOException e);

	@LogMessage(level = Level.WARN)
	@Message(id = 286, value = "Could not create uninverting reader for reader of type %s; Only directory readers are supported" )
	void readerTypeUnsupportedForInverting(@FormatWith(ClassFormatter.class) Class<? extends IndexReader> readerClass);

	@Message(id = 287, value = "Unsupported sort type for field %1$s: %2$s" )
	SearchException sortFieldTypeUnsupported(String fieldName, SortField.Type type);

	@LogMessage(level = Level.WARN)
	@Message(id = 288, value = "The configuration property '%s' no longer applies and will be ignored." )
	void deprecatedConfigurationPropertyIsIgnored(String string);

	@LogMessage(level = Level.WARN)
	@Message(id = 289, value = "Requested sort field(s) %3$s are not configured for entity type %1$s mapped to index %2$s, thus an uninverting reader must be created. You should declare the missing sort fields using @SortableField." )
	void uncoveredSortsRequested(@FormatWith(ClassFormatter.class) Class<?> entityType, String indexName, String uncoveredSorts);

	@Message(id = 290, value = "The 'indexNullAs' property for field '%2$s' needs to represent a Double Number to match the field type of the index. "
			+ "Please change value from '%1$s' to represent a Double." )
	SearchException nullMarkerNeedsToRepresentADoubleNumber(String proposedTokenValue, String fieldName);

	@Message(id = 291, value = "The 'indexNullAs' property for field '%2$s' needs to represent a Float Number to match the field type of the index. "
			+ "Please change value from '%1$s' to represent a Float." )
	SearchException nullMarkerNeedsToRepresentAFloatNumber(String proposedTokenValue, String fieldName);

	@Message(id = 292, value = "The 'indexNullAs' property for field '%2$s' needs to represent an Integer Number to match the field type of the index. "
			+ "Please change value from '%1$s' to represent an Integer." )
	SearchException nullMarkerNeedsToRepresentAnIntegerNumber(String proposedTokenValue, String fieldName);

	@Message(id = 293, value = "The 'indexNullAs' property for field '%2$s' needs to represent a Long Number to match the field type of the index. "
			+ "Please change value from '%1$s' to represent a Long." )
	SearchException nullMarkerNeedsToRepresentALongNumber(String proposedTokenValue, String fieldName);

	@Message(id = 294, value = "Unable to search for null token on field '%1$s' if field bridge is ignored.")
	SearchException unableToSearchOnNullValueWithoutFieldBridge(String fieldName);

	@Message(id = 295, value = "String '$1%s' cannot be parsed into a '$2%s'")
	SearchException parseException(String text, @FormatWith(ClassFormatter.class) Class<?> readerClass, @Cause Exception e);

	@LogMessage(level = Level.DEBUG)
	@Message(id = 296, value = "Package java.time not found on the classpath; the built-in bridge won't be available")
	void javaTimeBridgeWontBeAdded(@Cause Exception e);

	@Message(id = 297, value = " Value of '%2$s' for type '%1$s' is too big for the conversion")
	SearchException valueTooLargeForConversionException(@FormatWith(ClassFormatter.class) Class<?> type, Object duration, @Cause Exception ae);

	@Message(id = 298, value = "Inconsisent configuration of sort fields %2$s for index '%1$s'. Make sure they are configured using @SortableField for all entities mapped to this index.")
	SearchException inconsistentSortableFieldConfigurationForSharedIndex(String indexName, String requestedSortFields);

	@Message(id = 299, value = "@SortableField declared on %s#%s references to undeclared field '%s'" )
	SearchException sortableFieldRefersToUndefinedField(@FormatWith(ClassFormatter.class) Class<?> entityType, String property, String sortedFieldName);

	@Message(id = 300, value = "Several @NumericField annotations used on %1$s#%2$s refer to the same field")
	SearchException severalNumericFieldAnnotationsForSameField(@FormatWith(ClassFormatter.class) Class<?> entityClass, String memberName);

	@Message(id = 301, value = "Requested sort field(s) %3$s are not configured for entity type %1$s mapped to index %2$s, thus an uninverting reader must be created. You should declare the missing sort fields using @SortableField." )
	SearchException uncoveredSortsRequestedWithUninvertingNotAllowed(@FormatWith(ClassFormatter.class) Class<?> entityType, String indexName, String uncoveredSorts);

	@Message(id = 302, value = "Cannot execute query '%2$s', as targeted entity type '%1$s' is indexed through a non directory-based backend")
	SearchException cannotRunLuceneQueryTargetingEntityIndexedWithNonDirectoryBasedIndexManager(@FormatWith(ClassFormatter.class) Class<?> entityType, String query);

	@LogMessage(level = Level.WARN)
	@Message(id = 303, value = "Timeout while waiting for indexing resources to properly flush and close on shut down of"
			+ "indexing backend for index '%s'. Some pending index writes might have been lost.")
	void timedOutWaitingShutdown(String indexName);

	@LogMessage(level = Level.DEBUG)
	@Message(id = 304, value = "Closing index writer for IndexManager '%1$s'")
	void closingIndexWriter(String indexName);

	@Message(id = 307, value = "Sort type %1$s is not compatible with %2$s type of field '%3$s'.")
	SearchException sortTypeDoesNotMatchFieldType(String sortType, String fieldType, String fieldName);

}
