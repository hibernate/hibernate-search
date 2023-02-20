/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.logging.impl;

import static org.jboss.logging.Logger.Level.DEBUG;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Collection;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.Set;

import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.spi.BeanNotFoundException;
import org.hibernate.search.engine.environment.classpath.spi.ClassLoadingException;
import org.hibernate.search.engine.logging.spi.MappableTypeModelFormatter;
import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.SearchTimeoutException;
import org.hibernate.search.util.common.logging.impl.ClassFormatter;
import org.hibernate.search.util.common.logging.impl.DurationInSecondsAndFractionsFormatter;
import org.hibernate.search.util.common.logging.impl.EventContextNoPrefixFormatter;
import org.hibernate.search.util.common.logging.impl.MessageConstants;
import org.hibernate.search.util.common.logging.impl.SimpleNameClassFormatter;
import org.hibernate.search.util.common.reporting.EventContext;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.FormatWith;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.Param;
import org.jboss.logging.annotations.Suppressed;
import org.jboss.logging.annotations.ValidIdRange;
import org.jboss.logging.annotations.ValidIdRanges;

@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
@ValidIdRanges({
		@ValidIdRange(min = MessageConstants.ENGINE_ID_RANGE_MIN, max = MessageConstants.ENGINE_ID_RANGE_MAX),
})
public interface Log extends BasicLogger {

	// -----------------------------------
	// Pre-existing messages from Search 5 (engine module)
	// DO NOT ADD ANY NEW MESSAGES HERE
	// -----------------------------------
	int ID_OFFSET_LEGACY = MessageConstants.ENGINE_ID_RANGE_MIN;

	@LogMessage(level = DEBUG)
	@Message(id = ID_OFFSET_LEGACY + 230, value = "Starting executor '%1$s'")
	void startingExecutor(String name);

	@LogMessage(level = DEBUG)
	@Message(id = ID_OFFSET_LEGACY + 231, value = "Stopping executor '%1$s'")
	void stoppingExecutor(String indexName);

	@Message(id = ID_OFFSET_LEGACY + 237,
			value = "Invalid range: at least one bound in range predicates must be non-null.")
	SearchException rangePredicateCannotMatchNullValue(@Param EventContext context);

	@Message(id = ID_OFFSET_LEGACY + 242,
			value = "Invalid type '%1$s': missing constructor. The type must expose a public, no-arguments constructor.")
	SearchException noPublicNoArgConstructor(@FormatWith(ClassFormatter.class) Class<?> clazz);

	// -----------------------------------
	// New messages from Search 6 onwards
	// -----------------------------------
	int ID_OFFSET = MessageConstants.ENGINE_ID_RANGE_MIN + 500;

	@Message(id = ID_OFFSET + 1,
			value = "Invalid value for configuration property '%1$s': '%2$s'. %3$s")
	SearchException unableToConvertConfigurationProperty(String key, Object rawValue, String errorMessage,
			@Cause Exception cause);

	@Message(id = ID_OFFSET + 2,
			value = "Invalid value: expected either an instance of '%1$s' or a String that can be parsed into that type. %2$s")
	SearchException invalidPropertyValue(@FormatWith(ClassFormatter.class) Class<?> expectedType, String errorMessage,
			@Cause Exception cause);

	@Message(id = ID_OFFSET + 3,
			value = "Invalid Boolean value: expected either a Boolean, the String 'true' or the String 'false'. %1$s")
	SearchException invalidBooleanPropertyValue(String nestedErrorMessage, @Cause Exception cause);

	@Message(id = ID_OFFSET + 4,
			value = "Invalid Integer value: expected either a Number or a String that can be parsed into an Integer. %1$s")
	SearchException invalidIntegerPropertyValue(String nestedErrorMessage, @Cause Exception cause);

	@Message(id = ID_OFFSET + 5,
			value = "Invalid Long value: expected either a Number or a String that can be parsed into a Long. %1$s")
	SearchException invalidLongPropertyValue(String nestedErrorMessage, @Cause Exception cause);

	@Message(id = ID_OFFSET + 6,
			value = "Invalid multi value: expected either a single value of the correct type, a Collection, or a String,"
					+ " and interpreting as a single value failed with the following exception. %1$s")
	SearchException invalidMultiPropertyValue(String nestedErrorMessage, @Cause Exception cause);

	@Message(id = ID_OFFSET + 14,
			value = "Invalid index field name '%1$s': field names cannot be null or empty.")
	SearchException relativeFieldNameCannotBeNullOrEmpty(String relativeFieldName,
			@Param EventContext context);

	@Message(id = ID_OFFSET + 15,
			value = "Invalid index field name '%1$s': field names cannot contain a dot ('.')."
					+ " Remove the dot from your field name,"
					+ " or if you are declaring the field in a bridge and want a tree of fields,"
					+ " declare an object field using the objectField() method.")
	SearchException relativeFieldNameCannotContainDot(String relativeFieldName,
			@Param EventContext context);

	@Message(id = ID_OFFSET + 16,
			value = "Invalid polygon: the first point '%1$s' should be identical to the last point '%2$s' to properly close the polygon.")
	IllegalArgumentException invalidGeoPolygonFirstPointNotIdenticalToLastPoint(GeoPoint firstPoint,
			GeoPoint lastPoint);

	@Message(id = ID_OFFSET + 19,
			value = "Hibernate Search encountered %3$s failures during %1$s."
					+ " Only the first %2$s failures are displayed here."
					+ " See the logs for extra failures.")
	String collectedFailureLimitReached(String process, int failureLimit, int failureCount);

	@Message(id = ID_OFFSET + 20,
			value = "Hibernate Search encountered failures during %1$s."
					+ " Failures:\n%2$s")
	SearchException collectedFailures(String process, String renderedFailures);

	@LogMessage(level = Logger.Level.ERROR)
	@Message(id = ID_OFFSET + 21,
			value = "Hibernate Search encountered a failure during %1$s;"
					+ " continuing for now to list all problems,"
					+ " but the process will ultimately be aborted.\n"
					+ "Context: %2$s\n"
					+ "Failure:" // The stack trace follows
	)
	void newCollectedFailure(String process, String context, @Cause Throwable failure);

	@LogMessage(level = Logger.Level.WARN)
	@Message(id = ID_OFFSET + 22, value = "Exception while collecting a failure"
			+ " -- this may indicate a bug or a missing test in Hibernate Search."
			+ " Please report it: https://hibernate.org/community/"
			+ " Nested exception: %1$s")
	void exceptionWhileCollectingFailure(String causeMessage, @Cause Throwable cause);

	@Message(id = ID_OFFSET + 25,
			value = "Invalid call of ifSupported(...) after orElse(...)."
					+ " Use a separate extension() context, or move the orElse(...) call last."
	)
	SearchException cannotCallDslExtensionIfSupportedAfterOrElse();

	@Message(id = ID_OFFSET + 26,
			value = "None of the provided extensions can be applied to the current context. "
					+ " Attempted extensions: %1$s."
					+ " If you want to ignore this, use .extension().ifSupported(...).orElse(ignored -> { })."
	)
	SearchException dslExtensionNoMatch(List<?> attemptedExtensions);

	@Message(id = ID_OFFSET + 28, value = "Security manager does not allow access to the constructor of type '%1$s': %2$s")
	SearchException securityManagerLoadingError(@FormatWith(ClassFormatter.class) Class<?> classToLoad,
			String causeMessage, @Cause Exception cause);

	@Message(id = ID_OFFSET + 30, value = "Unable to load class '%1$s': %2$s")
	ClassLoadingException unableToLoadTheClass(String className, String causeMessage, @Cause Throwable cause);

	@Message(id = ID_OFFSET + 33,
			value = "No backend with name '%1$s'."
					+ " Check that at least one entity is configured to target that backend."
					+ " The following backends can be retrieved by name: %2$s."
					+ " %3$s")
	SearchException unknownNameForBackend(String backendName, Collection<String> validBackendNames,
			String defaultBackendMessage);

	@Message(value = "The default backend can be retrieved")
	String defaultBackendAvailable();

	@Message(value = "The default backend cannot be retrieved, because no entity is mapped to that backend")
	String defaultBackendUnavailable();

	@Message(id = ID_OFFSET + 34,
			value = "No index manager with name '%1$s'."
					+ " Check that at least one entity is configured to target that index."
					+ " The following indexes can be retrieved by name: %2$s.")
	SearchException unknownNameForIndexManager(String indexManagerName, Collection<String> validIndexNames);

	@Message(id = ID_OFFSET + 40, value = "Unable to instantiate class '%1$s': %2$s")
	SearchException unableToInstantiateClass(String className, String causeMessage, @Cause Exception cause);

	@Message(id = ID_OFFSET + 42, value = "Invalid type '%1$s': this type cannot be assigned to type '%2$s'.")
	SearchException subtypeExpected(@FormatWith(ClassFormatter.class) Class<?> classToLoad,
			@FormatWith(ClassFormatter.class) Class<?> superType);

	@Message(id = ID_OFFSET + 43, value = "Invalid type '%1$s': this type is an interface. An implementation class is required.")
	SearchException implementationRequired(@FormatWith(ClassFormatter.class) Class<?> classToLoad);

	@Message(id = ID_OFFSET + 44, value = "Invalid type '%1$s': missing constructor. The type must expose a public constructor with a single parameter of type Map.")
	SearchException noPublicMapArgConstructor(@FormatWith(ClassFormatter.class) Class<?> classToLoad);

	@Message(id = ID_OFFSET + 46, value = "Cyclic @IndexedEmbedded recursion starting from type '%2$s'."
			+ " Path starting from that type and ending with a cycle: '%1$s'."
			+ " A type cannot declare an unrestricted @IndexedEmbedded to itself, even indirectly."
			+ " To break the cycle, you should consider adding filters to your @IndexedEmbedded: includePaths, includeDepth, ...")
	SearchException indexedEmbeddedCyclicRecursion(String cyclicRecursionPath,
			@FormatWith(MappableTypeModelFormatter.class) MappableTypeModel parentTypeModel);

	@Message(id = ID_OFFSET + 47,
			value = "Invalid BeanReference value: expected an instance of '%1$s', BeanReference, String or Class. %2$s")
	SearchException invalidBeanReferencePropertyValue(@FormatWith(ClassFormatter.class) Class<?> expectedType,
			String nestedErrorMessage, @Cause Exception cause);

	@Message(id = ID_OFFSET + 51,
			value = "Invalid use of per-field boost: the predicate score is constant."
					+ " Cannot assign a different boost to each field when the predicate score is constant.")
	SearchException perFieldBoostWithConstantScore();

	@Message(id = ID_OFFSET + 53,
			value = "Invalid slop: %1$d. The slop must be positive or zero.")
	SearchException invalidPhrasePredicateSlop(int slop);

	@Message(id = ID_OFFSET + 54,
			value = "Invalid maximum edit distance: %1$d. The value must be 0, 1 or 2.")
	SearchException invalidFuzzyMaximumEditDistance(int maximumEditDistance);

	@Message(id = ID_OFFSET + 55,
			value = "Invalid exact prefix length: %1$d. The value must be positive or zero.")
	SearchException invalidExactPrefixLength(int exactPrefixLength);

	@Message(id = ID_OFFSET + 57, value = "Invalid value for type '%1$s': '%2$s'."
			+ " The expected format is '%3$s'.")
	SearchException unableToParseTemporal(
			@FormatWith(SimpleNameClassFormatter.class) Class<? extends TemporalAccessor> type, String value,
			DateTimeFormatter formatter, @Cause Exception cause);

	@Message(id = ID_OFFSET + 58, value = "Invalid %1$s value: expected either a Number or a String that can be parsed into a %1$s. %2$s")
	SearchException invalidNumberPropertyValue(@FormatWith(SimpleNameClassFormatter.class) Class<? extends Number> type,
			String nestedErrorMessage, @Cause Exception cause);

	@Message(id = ID_OFFSET + 59, value = "Invalid string for type '%2$s': '%1$s'. %3$s")
	SearchException invalidStringForType(String value, @FormatWith(ClassFormatter.class) Class<?> type,
			String causeMessage, @Cause Exception cause);

	@Message(id = ID_OFFSET + 60, value = "Invalid value for enum '%2$s': '%1$s'.")
	SearchException invalidStringForEnum(String value,
			@FormatWith(ClassFormatter.class) Class<? extends Enum<?>> enumType, @Cause Exception cause);

	@Message(id = ID_OFFSET + 61, value = "Multiple hits when a single hit was expected.")
	SearchException nonSingleHit();

	@Message(id = ID_OFFSET + 62,
			value = "Unable to submit work to '%1$s': thread received interrupt signal."
					+ " The work has been discarded.")
	SearchException threadInterruptedWhileSubmittingWork(String orchestratorName);

	@Message(id = ID_OFFSET + 63,
			value = "Unable to submit work to '%1$s': this orchestrator is stopped."
					+ " The work has been discarded.")
	SearchException submittedWorkToStoppedOrchestrator(String orchestratorName);

	@Message(id = ID_OFFSET + 64,
			value = "Invalid geo-point value: '%1$s'."
					+ " The expected format is '<latitude as double>, <longitude as double>'.")
	SearchException unableToParseGeoPoint(String value);

	@Message(id = ID_OFFSET + 65,
			value = "Unknown aggregation key '%1$s'. This key was not used when building the search query.")
	SearchException unknownAggregationKey(AggregationKey<?> key);

	@Message(id = ID_OFFSET + 66,
			value = "Invalid configuration property checking strategy name: '%1$s'. Valid names are: %2$s.")
	SearchException invalidConfigurationPropertyCheckingStrategyName(String invalidRepresentation,
			List<String> validRepresentations);

	@LogMessage(level = Logger.Level.INFO)
	@Message(id = ID_OFFSET + 67,
			value = "Configuration property tracking is disabled; unused properties will not be logged.")
	void configurationPropertyTrackingDisabled();

	@LogMessage(level = Logger.Level.WARN)
	@Message(id = ID_OFFSET + 68,
			value = "Invalid configuration passed to Hibernate Search: some properties in the given configuration are not used."
					+ " There might be misspelled property keys in your configuration."
					+ " Unused properties: %1$s."
					+ " To disable this warning, set the property '%2$s' to '%3$s'.")
	void configurationPropertyTrackingUnusedProperties(Set<String> propertyKeys, String disableWarningKey,
			String disableWarningValue);

	@LogMessage(level = Logger.Level.ERROR)
	@Message(id = ID_OFFSET + 69,
			value = "The background failure handler threw an exception while handling a previous failure."
					+ " The failure may not have been reported.")
	void failureInFailureHandler(@Cause Throwable t);

	@Message(id = ID_OFFSET + 70,
			value = "Invalid index field template name '%1$s': field template names cannot be null or empty.")
	SearchException fieldTemplateNameCannotBeNullOrEmpty(String templateName, @Param EventContext context);

	@Message(id = ID_OFFSET + 71,
			value = "Invalid index field template name '%1$s': field template names cannot contain a dot ('.').")
	SearchException fieldTemplateNameCannotContainDot(String relativeFieldName,
			@Param EventContext context);

	@Message(id = ID_OFFSET + 72,
			value = "Inconsistent index data: a supposedly single-valued field returned multiple values. Values: [%1$s, %2$s].")
	SearchException unexpectedMultiValuedField(Object value1, Object value2);

	@Message(id = ID_OFFSET + 73,
			value = "Invalid configuration passed to Hibernate Search: some properties in the given configuration are obsolete."
					+ "Configuration properties changed between Hibernate Search 5 and Hibernate Search 6"
					+ " Check out the reference documentation and upgrade your configuration."
					+ " Obsolete properties: %1$s.")
	SearchException obsoleteConfigurationPropertiesFromSearch5(Set<String> propertyKeys);

	@Message(id = ID_OFFSET + 75,
			value = "No default backend."
					+ " Check that at least one entity is configured to target the default backend."
					+ " The following backends can be retrieved by name: %1$s.")
	SearchException noDefaultBackendRegistered(Collection<String> validBackendNames);

	@Message(id = ID_OFFSET + 76,
			value = "Ambiguous bean reference to type '%1$s':"
					+ " multiple beans are explicitly defined for this type in Hibernate Search's internal registry."
					+ " Explicitly defined beans: %2$s.")
	BeanNotFoundException multipleConfiguredBeanReferencesForType(
			@FormatWith(ClassFormatter.class) Class<?> exposedType,
			List<? extends BeanReference<?>> references);

	@Message(id = ID_OFFSET + 77,
			value = "No beans defined for type '%1$s' in Hibernate Search's internal registry.")
	BeanNotFoundException noConfiguredBeanReferenceForType(@FormatWith(ClassFormatter.class) Class<?> exposedType);

	@Message(id = ID_OFFSET + 78,
			value = "No beans defined for type '%1$s' and name '%2$s' in Hibernate Search's internal registry.")
	BeanNotFoundException noConfiguredBeanReferenceForTypeAndName(
			@FormatWith(ClassFormatter.class) Class<?> exposedType,
			String nameReference);

	@Message(id = ID_OFFSET + 79,
			value = "Unable to resolve bean reference to type '%1$s' and name '%2$s'. %3$s")
	BeanNotFoundException cannotResolveBeanReference(@FormatWith(ClassFormatter.class) Class<?> typeReference,
			String nameReference, String failureMessages, @Cause RuntimeException mainFailure,
			@Suppressed Collection<? extends RuntimeException> otherFailures);

	@Message(id = ID_OFFSET + 80,
			value = "Unable to resolve bean reference to type '%1$s'. %2$s")
	BeanNotFoundException cannotResolveBeanReference(@FormatWith(ClassFormatter.class) Class<?> typeReference,
			String failureMessages, @Cause RuntimeException beanProviderFailure,
			@Suppressed Collection<? extends RuntimeException> otherFailures);

	// No ID here: this message is always embedded in one of the two exceptions above
	@Message(value = "Failed to resolve bean from Hibernate Search's internal registry with exception: %1$s")
	String failedToResolveBeanUsingInternalRegistry(String exceptionMessage);

	// No ID here: this message is always embedded in one of the two exceptions above
	@Message(value = "Failed to resolve bean from bean manager with exception: %1$s")
	String failedToResolveBeanUsingBeanManager(String exceptionMessage);

	// No ID here: this message is always embedded in one of the two exceptions above
	@Message(value = "Failed to resolve bean using reflection with exception: %1$s")
	String failedToResolveBeanUsingReflection(String exceptionMessage);

	@Message(id = ID_OFFSET + 81,
			value = "Unable to resolve backend type:"
					+ " configuration property '%1$s' is not set, and there isn't any backend in the classpath."
					+ " Check that you added the desired backend to your project's dependencies.")
	SearchException noBackendFactoryRegistered(String propertyKey);

	@Message(id = ID_OFFSET + 82,
			value = "Ambiguous backend type:"
					+ " configuration property '%1$s' is not set, and multiple backend types are present in the classpath."
					+ " Set property '%1$s' to one of the following to select the backend type: %2$s")
	SearchException multipleBackendFactoriesRegistered(String propertyKey, Collection<String> backendTypeNames);

	@Message(id = ID_OFFSET + 83, value = "Invalid type for DSL arguments: '%1$s'. Expected '%2$s' or a subtype.")
	SearchException invalidDslArgumentType(@FormatWith(ClassFormatter.class) Class<?> type,
			@FormatWith(ClassFormatter.class) Class<?> correctType,
			@Param EventContext context);

	@Message(id = ID_OFFSET + 84, value = "Invalid type for returned values: '%1$s'. Expected '%2$s' or a supertype.")
	SearchException invalidReturnType(@FormatWith(ClassFormatter.class) Class<?> type,
			@FormatWith(ClassFormatter.class) Class<?> correctType,
			@Param EventContext context);

	@Message(id = ID_OFFSET + 86, value = "Operation exceeded the timeout of %1$s.")
	SearchTimeoutException timedOut(@FormatWith(DurationInSecondsAndFractionsFormatter.class) Duration timeout,
			@Cause Exception cause);

	@Message(id = ID_OFFSET + 87,
			value = "Unable to provide the exact total hit count: only a lower-bound approximation is available."
					+ " This is generally the result of setting query options such as a timeout or the total hit count threshold."
					+ " Either unset these options, or retrieve the lower-bound hit count approximation through '.total().hitCountLowerBound()'.")
	SearchException notExactTotalHitCount();

	@Message(id = ID_OFFSET + 88,
			value = "Multiple entity types mapped to index '%1$s': '%2$s', '%3$s'."
					+ " Each indexed type must be mapped to its own, dedicated index.")
	SearchException twoTypesTargetSameIndex(String indexName, String mappedTypeName, String anotherMappedTypeName);

	@Message(id = ID_OFFSET + 89, value = "Unable to create bean using reflection: %1$s")
	BeanNotFoundException unableToCreateBeanUsingReflection(String causeMessage, @Cause Exception e);

	@Message(id = ID_OFFSET + 90, value = "No configured bean manager.")
	BeanNotFoundException noConfiguredBeanManager();

	@Message(id = ID_OFFSET + 91, value = "Unable to resolve '%2$s' to a class extending '%1$s': %3$s")
	BeanNotFoundException unableToResolveToClassName(@FormatWith(ClassFormatter.class) Class<?> typReference,
			String nameReference, String causeMessage, @Cause Exception e);

	@Message(id = ID_OFFSET + 92, value = "Invalid bean reference: '%1$s'."
			+ " The reference is prefixed with '%2$s', which is not a valid bean retrieval prefix."
			+ " If you want to reference a bean by name, and the name contains a colon, use 'bean:%1$s'."
			+ " Otherwise, use a valid bean retrieval prefix among the following: %3$s.")
	BeanNotFoundException invalidBeanRetrieval(String beanReference, String invalidPrefix,
			List<String> validPrefixes, @Cause Exception e);

	@Message(id = ID_OFFSET + 93,
			value = "Named predicate name '%1$s' is invalid: field names cannot be null or empty.")
	SearchException relativeNamedPredicateNameCannotBeNullOrEmpty(String relativeNamedPredicateName,
			@Param EventContext context);

	@Message(id = ID_OFFSET + 94,
			value = "Named predicate name '%1$s' is invalid: field names cannot contain a dot ('.')."
					+ " Remove the dot from your named predicate name.")
	SearchException relativeNamedPredicateNameCannotContainDot(String relativeNamedPredicateName,
			@Param EventContext context);

	@Message(id = ID_OFFSET + 96, value = "Different mappings trying to define two backends " +
			"with the same name '%1$s' but having different expectations on multi-tenancy.")
	SearchException differentMultiTenancyNamedBackend(String backendName);

	@Message(id = ID_OFFSET + 97, value = "Different mappings trying to define default backends " +
			"having different expectations on multi-tenancy.")
	SearchException differentMultiTenancyDefaultBackend();

	@Message(id = ID_OFFSET + 98,
			value = "Invalid type: %1$s is not composite.")
	SearchException invalidIndexNodeTypeNotComposite(
			@FormatWith(EventContextNoPrefixFormatter.class) EventContext elementContext);

	@Message(id = ID_OFFSET + 99,
			value = "Invalid type: %1$s is not an object field.")
	SearchException invalidIndexNodeTypeNotObjectField(
			@FormatWith(EventContextNoPrefixFormatter.class) EventContext elementContext);

	@Message(id = ID_OFFSET + 100,
			value = "Invalid type: %1$s is not a value field.")
	SearchException invalidIndexNodeTypeNotValueField(
			@FormatWith(EventContextNoPrefixFormatter.class) EventContext elementContext);

	@Message(id = ID_OFFSET + 101,
			value = "Inconsistent configuration for %1$s in a search query across multiple indexes: %2$s")
	SearchException inconsistentConfigurationInContextForSearch(
			@FormatWith(EventContextNoPrefixFormatter.class) EventContext elementContext, String causeMessage,
			@Param EventContext elementContextAsParam, @Cause SearchException cause);

	@Message(id = ID_OFFSET + 102,
			value = "Inconsistent support for '%1$s': %2$s")
	SearchException inconsistentSupportForQueryElement(SearchQueryElementTypeKey<?> key,
			String causeMessage, @Cause SearchException cause);

	@Message(id = ID_OFFSET + 103,
			value = "Attribute '%1$s' differs: '%2$s' vs. '%3$s'.")
	SearchException differentAttribute(String attributeName, Object component1, Object component2);

	@Message(id = ID_OFFSET + 104, value = "Cannot use '%2$s' on %1$s: %3$s")
	SearchException cannotUseQueryElementForIndexNode(
			@FormatWith(EventContextNoPrefixFormatter.class) EventContext elementContext,
			SearchQueryElementTypeKey<?> key, String hint, @Param EventContext context, @Cause Exception cause);

	@Message(value = "Make sure the field is marked as searchable/sortable/projectable/aggregable (whichever is relevant)."
			+ " If it already is, then '%1$s' is not available for fields of this type.")
	String missingSupportHintForValueField(SearchQueryElementTypeKey<?> key);

	@Message(value = "Some object field features require a nested structure;"
			+ " try setting the field structure to 'NESTED' and reindexing all your data."
			+ " If you are trying to use another feature, it probably isn't available for this field.")
	String missingSupportHintForCompositeNode();

	@Message(id = ID_OFFSET + 106,
			value = "'%1$s' can be used in some of the targeted indexes, but not all of them. %2$s")
	SearchException partialSupportForQueryElement(SearchQueryElementTypeKey<?> key, String hint);

	@Message(value = "Make sure the field is marked as searchable/sortable/projectable/aggregable"
			+ " (whichever is relevant) in all indexes,"
			+ " and that the field has the same type in all indexes.")
	String partialSupportHintForValueField();

	@Message(value = "If you are trying to use the 'nested' predicate,"
			+ " set the field structure is to 'NESTED' in all indexes, then reindex all your data.")
	String partialSupportHintForCompositeNode();

	@Message(id = ID_OFFSET + 109,
			value = "This field is a value field in some indexes, but an object field in other indexes.")
	SearchException conflictingFieldModel();

	@Message(id = ID_OFFSET + 110,
			value = "Unknown field '%1$s'.")
	SearchException unknownFieldForSearch(String absoluteFieldPath, @Param EventContext context);

	@Message(id = ID_OFFSET + 111,
			value = "Invalid target fields:"
					+ " fields [%1$s, %3$s] are in different nested documents (%2$s vs. %4$s)."
					+ " All target fields must be in the same document.")
	SearchException targetFieldsSpanningMultipleNestedPaths(String fieldPath1,
			@FormatWith(EventContextNoPrefixFormatter.class) EventContext nestedPath1,
			String fieldPath2, @FormatWith(EventContextNoPrefixFormatter.class) EventContext nestedPath2);

	@Message(id = ID_OFFSET + 112,
			value = "Unable to close saved value for key %1$s: %2$s")
	SearchException unableToCloseSavedValue(String keyName, String message, @Cause Exception e);

	@Message(id = ID_OFFSET + 113,
			value = "Unable to access the Search integration: initialization hasn't completed yet.")
	SearchException noIntegrationBecauseInitializationNotComplete();

	@Message(id = ID_OFFSET + 114,
			value = "Cannot project on entity type '%1$s': this type cannot be loaded from an external datasource,"
					+ " and the documents from the index cannot be projected to its Java class '%2$s'."
					+ " %3$s")
	SearchException cannotCreateEntityProjection(String name, @FormatWith(ClassFormatter.class) Class<?> javaClass,
			String hint);

	@Message(id = ID_OFFSET + 115,
			value = "Unable to resolve field '%1$s': %2$s")
	SearchException unableToResolveField(String absolutePath, String causeMessage, @Cause SearchException e,
			@Param EventContext context);

	@LogMessage(level = Logger.Level.WARN)
	@Message(id = ID_OFFSET + 116,
			value = "Ignoring ServiceConfigurationError caught while trying to instantiate service '%s'.")
	void ignoringServiceConfigurationError(Class<?> serviceContract, @Cause ServiceConfigurationError error);
}
