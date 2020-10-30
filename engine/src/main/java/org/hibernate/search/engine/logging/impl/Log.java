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
import java.util.Set;

import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.classpath.spi.ClassLoadingException;
import org.hibernate.search.engine.logging.spi.MappableTypeModelFormatter;
import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.SearchTimeoutException;
import org.hibernate.search.util.common.logging.impl.ClassFormatter;
import org.hibernate.search.util.common.logging.impl.DurationInSecondsAndFractionsFormatter;
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
	@Message(id = ID_OFFSET_LEGACY + 230, value = "Starting executor '%1$s'" )
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
	SearchException unableToConvertConfigurationProperty(String key, Object rawValue, String errorMessage, @Cause Exception cause);

	@Message(id = ID_OFFSET + 2,
			value = "Invalid value: expected either an instance of '%1$s' or a String that can be parsed into that type. %2$s")
	SearchException invalidPropertyValue(@FormatWith(ClassFormatter.class) Class<?> expectedType, String errorMessage, @Cause Exception cause);

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
			value = "Invalid multi value: expected either a Collection or a String.")
	SearchException invalidMultiPropertyValue();

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
			value = "Invalid polygon: the first point '%1$s' should be identical to the last point '%2$s' to properly close the polygon." )
	IllegalArgumentException invalidGeoPolygonFirstPointNotIdenticalToLastPoint(GeoPoint firstPoint, GeoPoint lastPoint);

	@Message(id = ID_OFFSET + 19,
			value = "Hibernate Search encountered failures during %1$s. Stopped collecting failures after '%3$s' failures."
					+ " Failures:\n%2$s")
	SearchException collectedFailureLimitReached(String process, String renderedFailures, int failureCount);

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
					+ " Check that at least one entity is configured to target that backend.")
	SearchException noBackendRegistered(String backendName);

	@Message(id = ID_OFFSET + 34,
			value = "No index manager with name '%1$s'."
					+ " Check that at least one entity is configured to target that index.")
	SearchException noIndexManagerRegistered(String indexManagerName);

	@Message(id = ID_OFFSET + 40, value = "Unable to instantiate class '%1$s': %2$s")
	SearchException unableToInstantiateClass(String className, String causeMessage, @Cause Exception cause);

	@Message(id = ID_OFFSET + 42, value = "Invalid type '%1$s': this type cannot be assigned to type '%2$s'.")
	SearchException subtypeExpected(@FormatWith(ClassFormatter.class) Class<?> classToLoad, @FormatWith(ClassFormatter.class) Class<?> superType);

	@Message(id = ID_OFFSET + 43, value = "Invalid type '%1$s': this type is an interface. An implementation class is required.")
	SearchException implementationRequired(@FormatWith(ClassFormatter.class) Class<?> classToLoad);

	@Message(id = ID_OFFSET + 44, value = "Invalid type '%1$s': missing constructor. The type must expose a public constructor with a single parameter of type Map.")
	SearchException noPublicMapArgConstructor(@FormatWith(ClassFormatter.class) Class<?> classToLoad);

	@Message(id = ID_OFFSET + 46, value = "Infinite @IndexedEmbedded recursion involving path '%1$s' on type '%2$s'.")
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
	SearchException invalidNumberPropertyValue(@FormatWith(SimpleNameClassFormatter.class) Class<? extends Number> type, String nestedErrorMessage, @Cause Exception cause);

	@Message(id = ID_OFFSET + 59, value = "Invalid value for type '%2$s': '%1$s'. %3$s")
	SearchException invalidStringForType(String value, @FormatWith(ClassFormatter.class) Class<?> type,
			String causeMessage, @Cause Exception cause);

	@Message(id = ID_OFFSET + 60, value = "Invalid value for enum '%2$s': '%1$s'.")
	SearchException invalidStringForEnum(String value, @FormatWith(ClassFormatter.class) Class<? extends Enum> enumType, @Cause Exception cause);

	@Message(id = ID_OFFSET + 61, value = "Multiple hits when a single hit was expected.")
	SearchException nonSingleHit();

	@Message(id = ID_OFFSET + 62,
			value = "Unable to submit work to '%1$s': thread received interrupt signal."
					+ " The work has been discarded." )
	SearchException threadInterruptedWhileSubmittingWork(String orchestratorName);

	@Message(id = ID_OFFSET + 63,
			value = "Unable to submit work to '%1$s': this orchestrator is stopped."
					+ " The work has been discarded." )
	SearchException submittedWorkToStoppedOrchestrator(String orchestratorName);

	@Message(id = ID_OFFSET + 64,
			value = "Invalid geo-point value: '%1$s'."
					+ " The expected format is '<latitude as double>, <longitude as double>'.")
	SearchException unableToParseGeoPoint(String value);

	@Message(id = ID_OFFSET + 65,
			value = "Unknown aggregation key '%1$s'. This key was not used when building the search query." )
	SearchException unknownAggregationKey(AggregationKey<?> key);

	@Message(id = ID_OFFSET + 66,
			value = "Invalid configuration property checking strategy name: '%1$s'. Valid names are: %2$s.")
	SearchException invalidConfigurationPropertyCheckingStrategyName(String invalidRepresentation, List<String> validRepresentations);

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
					+ " Check that at least one entity is configured to target the default backend.")
	SearchException noDefaultBackendRegistered();

	@Message(id = ID_OFFSET + 76,
			value = "Ambiguous bean reference to type '%1$s':"
					+ " multiple beans are explicitly defined for this type in Hibernate Search's internal registry."
					+ " Explicitly defined beans: %2$s.")
	SearchException multipleConfiguredBeanReferencesForType(@FormatWith(ClassFormatter.class) Class<?> exposedType,
			List<? extends BeanReference<?>> references);

	@Message(id = ID_OFFSET + 77,
			value = "No beans defined for type '%1$s' in Hibernate Search's internal registry.")
	SearchException noConfiguredBeanReferenceForType(@FormatWith(ClassFormatter.class) Class<?> exposedType);

	@Message(id = ID_OFFSET + 78,
			value = "No beans defined for type '%1$s' and name '%2$s' in Hibernate Search's internal registry.")
	SearchException noConfiguredBeanReferenceForTypeAndName(@FormatWith(ClassFormatter.class) Class<?> exposedType,
			String nameReference);

	@Message(id = ID_OFFSET + 79,
			value = "Unable to resolve bean reference to type '%1$s' and name '%2$s'."
					+ " Failed to resolve bean from bean provider with exception: %3$s."
					+ " Failed to resolve bean from Hibernate Search's internal registry with exception: %4$s.")
	SearchException cannotResolveBeanReference(@FormatWith(ClassFormatter.class) Class<?> typeReference, String nameReference,
			String beanProviderFailureMessage, String configuredBeansFailureMessage,
			@Cause SearchException beanProviderFailure, @Suppressed RuntimeException configuredBeansFailure);

	@Message(id = ID_OFFSET + 80,
			value = "Unable to resolve bean reference to type '%1$s'."
					+ " Failed to resolve bean from bean provider with exception: %2$s."
					+ " Failed to resolve bean from Hibernate Search's internal registry with exception: %3$s.")
	SearchException cannotResolveBeanReference(@FormatWith(ClassFormatter.class) Class<?> typeReference,
			String beanProviderFailureMessage, String configuredBeansFailureMessage,
			@Cause SearchException beanProviderFailure, @Suppressed RuntimeException configuredBeansFailure);

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
	SearchException invalidOutputTypeForField(@FormatWith(ClassFormatter.class) Class<?> type,
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
	SearchException unableToCreateBeanUsingReflection(String causeMessage, @Cause Exception e);
}
