/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.logging.impl;

import static org.jboss.logging.Logger.Level.DEBUG;

import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.List;
import java.util.Set;

import org.hibernate.search.engine.environment.classpath.spi.ClassLoadingException;
import org.hibernate.search.engine.logging.spi.MappableTypeModelFormatter;
import org.hibernate.search.util.common.logging.impl.SimpleNameClassFormatter;
import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.impl.MessageConstants;
import org.hibernate.search.util.common.logging.impl.ClassFormatter;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.FormatWith;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.Param;
import org.jboss.logging.annotations.ValidIdRange;
import org.jboss.logging.annotations.ValidIdRanges;

@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
@ValidIdRanges({
		@ValidIdRange(min = MessageConstants.ENGINE_ID_RANGE_MIN, max = MessageConstants.ENGINE_ID_RANGE_MAX),
		// Exceptions for legacy messages from Search 5

		// TODO HSEARCH-3308 add exceptions here for legacy messages from Search 5. See the Lucene logger for examples.
})
public interface Log extends BasicLogger {

	// -----------------------------------
	// Pre-existing messages from Search 5
	// DO NOT ADD ANY NEW MESSAGES HERE
	// -----------------------------------
	int ID_OFFSET_1 = MessageConstants.ENGINE_ID_RANGE_MIN;

	@LogMessage(level = DEBUG)
	@Message(id = ID_OFFSET_1 + 230, value = "Starting executor '%1$s'" )
	void startingExecutor(String name);

	@LogMessage(level = DEBUG)
	@Message(id = ID_OFFSET_1 + 231, value = "Stopping executor '%1$s'")
	void stoppingExecutor(String indexName);

	@Message(id = ID_OFFSET_1 + 242,
			value = "Hibernate Search failed to initialize component '%1$s' as class '%2$s' doesn't have a public no-arguments constructor")
	SearchException noPublicNoArgConstructor(String componentName, @FormatWith(ClassFormatter.class) Class<?> clazz);

	// TODO HSEARCH-3308 migrate relevant messages from Search 5 here

	// -----------------------------------
	// New messages from Search 6 onwards
	// -----------------------------------
	int ID_OFFSET_2 = MessageConstants.ENGINE_ID_RANGE_MIN + 500;

	@Message(id = ID_OFFSET_2 + 1,
			value = "Unable to convert configuration property '%1$s' with value '%2$s': %3$s")
	SearchException unableToConvertConfigurationProperty(String key, Object rawValue, String errorMessage, @Cause Exception cause);

	@Message(id = ID_OFFSET_2 + 2,
			value = "Invalid value: expected either an instance of '%1$s' or a String that can be parsed. %2$s")
	SearchException invalidPropertyValue(@FormatWith(ClassFormatter.class) Class<?> expectedType, String errorMessage, @Cause Exception cause);

	@Message(id = ID_OFFSET_2 + 3,
			value = "Invalid Boolean value: expected either a Boolean, the String 'true' or the String 'false'. %1$s")
	SearchException invalidBooleanPropertyValue(String nestedErrorMessage, @Cause Exception cause);

	@Message(id = ID_OFFSET_2 + 4,
			value = "Invalid Integer value: expected either a Number or a String that can be parsed into an Integer. %1$s")
	SearchException invalidIntegerPropertyValue(String nestedErrorMessage, @Cause Exception cause);

	@Message(id = ID_OFFSET_2 + 5,
			value = "Invalid Long value: expected either a Number or a String that can be parsed into a Long. %1$s")
	SearchException invalidLongPropertyValue(String nestedErrorMessage, @Cause Exception cause);

	@Message(id = ID_OFFSET_2 + 6,
			value = "Invalid multi value: expected either a Collection or a String.")
	SearchException invalidMultiPropertyValue();

	@Message(id = ID_OFFSET_2 + 12,
			value = "Invalid value: at least one bound in range predicates must be non-null.")
	SearchException rangePredicateCannotMatchNullValue(@Param EventContext context);

	@Message(id = ID_OFFSET_2 + 14,
			value = "Field name '%1$s' is invalid: field names cannot be null or empty.")
	SearchException relativeFieldNameCannotBeNullOrEmpty(String relativeFieldName,
			@Param EventContext context);

	@Message(id = ID_OFFSET_2 + 15,
			value = "Field name '%1$s' is invalid: field names cannot contain a dot ('.')."
					+ " Remove the dot from your field name,"
					+ " or if you are declaring the field in a bridge and want a tree of fields,"
					+ " declare an object field using the objectField() method.")
	SearchException relativeFieldNameCannotContainDot(String relativeFieldName,
			@Param EventContext context);

	@Message(id = ID_OFFSET_2 + 16,
			value = "Invalid polygon: the first point '%1$s' should be identical to the last point '%2$s' to properly close the polygon." )
	IllegalArgumentException invalidGeoPolygonFirstPointNotIdenticalToLastPoint(GeoPoint firstPoint, GeoPoint lastPoint);

	@Message(id = ID_OFFSET_2 + 19,
			value = "Hibernate Search encountered failures during %1$s. Stopped collecting failures after '%3$s' failures."
					+ " Failures:\n%2$s")
	SearchException collectedFailureLimitReached(String process, String renderedFailures, int failureCount);

	@Message(id = ID_OFFSET_2 + 20,
			value = "Hibernate Search encountered failures during %1$s."
					+ " Failures:\n%2$s")
	SearchException collectedFailures(String process, String renderedFailures);

	@LogMessage(level = Logger.Level.ERROR)
	@Message(id = ID_OFFSET_2 + 21,
			value = "Hibernate Search encountered a failure during %1$s;"
					+ " continuing for now to list all problems,"
					+ " but the process will ultimately be aborted.\n"
					+ "Context: %2$s\n"
					+ "Failure:" // The stack trace follows
	)
	void newCollectedFailure(String process, String context, @Cause Throwable failure);

	@LogMessage(level = Logger.Level.DEBUG)
	@Message(id = ID_OFFSET_2 + 22,
			value = "Unexpected empty event context; there is a bug in Hibernate Search, please report it")
	void unexpectedEmptyEventContext(@Cause Throwable exceptionForStackTrace);

	@Message(id = ID_OFFSET_2 + 25,
			value = "Cannot call ifSupported(...) after orElse(...)."
					+ " Use a separate extension() context, or move the orElse(...) call last."
	)
	SearchException cannotCallDslExtensionIfSupportedAfterOrElse();

	@Message(id = ID_OFFSET_2 + 26,
			value = "None of the provided extensions can be applied to the current context. "
					+ " Attempted extensions: %1$s."
					+ " If you want to ignore this, use .extension().ifSupported(...).orElse(ignored -> { })."
	)
	SearchException dslExtensionNoMatch(List<?> attemptedExtensions);

	@Message(id = ID_OFFSET_2 + 27, value = "Unable to instantiate %1$s, class '%2$s': %3$s")
	SearchException unableToInstantiateComponent(String componentDescription, @FormatWith(ClassFormatter.class) Class<?> classToLoad, String causeMessage, @Cause Exception cause);

	@Message(id = ID_OFFSET_2 + 28, value = "%2$s defined for component %1$s could not be instantiated because of a security manager error")
	SearchException securityManagerLoadingError(String componentDescription, @FormatWith(ClassFormatter.class) Class<?> classToLoad, @Cause Exception cause);

	@Message(id = ID_OFFSET_2 + 29, value = "Unable to find %1$s implementation class: %2$s")
	SearchException unableToFindComponentImplementation(String componentDescription, String classNameToLoad, @Cause Exception cause);

	@Message(id = ID_OFFSET_2 + 30, value = "Unable to load class [%1$s]")
	ClassLoadingException unableToLoadTheClass(String className, @Cause Throwable cause);

	@Message(id = ID_OFFSET_2 + 33, value = "No backend registered for backend name: '%1$s'.")
	SearchException noBackendRegistered(String backendName);

	@Message(id = ID_OFFSET_2 + 34, value = "No index manager registered for index manager name: '%1$s'.")
	SearchException noIndexManagerRegistered(String indexManagerName);

	@Message(id = ID_OFFSET_2 + 40, value = "Unable to instantiate class '%1$s': %2$s.")
	SearchException unableToInstantiateClass(@FormatWith(ClassFormatter.class) Class<?> classToLoad, String causeMessage, @Cause Exception cause);

	@Message(id = ID_OFFSET_2 + 41, value = "Wrong configuration of %1$s: class %2$s does not implement interface %3$s.")
	SearchException interfaceImplementedExpected(String component, @FormatWith(ClassFormatter.class) Class<?> classToLoad, @FormatWith(ClassFormatter.class) Class<?> superType);

	@Message(id = ID_OFFSET_2 + 42, value = "Wrong configuration of %1$s: class %2$s is not a subtype of %3$s.")
	SearchException subtypeExpected(String component, @FormatWith(ClassFormatter.class) Class<?> classToLoad, @FormatWith(ClassFormatter.class) Class<?> superType);

	@Message(id = ID_OFFSET_2 + 43, value = "%2$s defined for component %1$s is an interface: implementation required.")
	SearchException implementationRequired(String component, @FormatWith(ClassFormatter.class) Class<?> classToLoad);

	@Message(id = ID_OFFSET_2 + 44, value = "%2$s defined for component %1$s is missing an appropriate constructor: expected a public constructor with a single parameter of type Map.")
	SearchException missingConstructor(String component, @FormatWith(ClassFormatter.class) Class<?> classToLoad);

	@Message(id = ID_OFFSET_2 + 45, value = "Unable to load class for %1$s. Configured implementation %2$s  is not assignable to type %3$s.")
	SearchException notAssignableImplementation(String component, String classToLoad, @FormatWith(ClassFormatter.class) Class<?> superType);

	@Message(id = ID_OFFSET_2 + 46, value = "Found an infinite IndexedEmbedded recursion involving path '%1$s' on type '%2$s'.")
	SearchException indexedEmbeddedCyclicRecursion(String cyclicRecursionPath, @FormatWith(MappableTypeModelFormatter.class) MappableTypeModel parentTypeModel);

	@Message(id = ID_OFFSET_2 + 47,
			value = "Invalid BeanReference value: expected an instance of '%1$s', BeanReference, String or Class. %2$s")
	SearchException invalidBeanReferencePropertyValue(@FormatWith(ClassFormatter.class) Class<?> expectedType,
			String nestedErrorMessage, @Cause Exception cause);

	@Message(id = ID_OFFSET_2 + 48,
			value = "Invalid bean type: type '%2$s' is not assignable to '%1$s'.")
	SearchException invalidBeanType(
			@FormatWith(ClassFormatter.class) Class<?> expectedSuperType,
			@FormatWith(ClassFormatter.class) Class<?> actualType);

	@Message(id = ID_OFFSET_2 + 49,
			value = "Missing backend type for backend '%1$s'."
					+ " Set the property '%2$s' to a supported value."
	)
	SearchException backendTypeCannotBeNullOrEmpty(String backendName, String key);

	@Message(id = ID_OFFSET_2 + 51,
			value = "It is not possible to use per-field boosts together with withConstantScore option"
	)
	SearchException perFieldBoostWithConstantScore();

	@Message(id = ID_OFFSET_2 + 53,
			value = "Invalid slop: %1$d. The slop must be positive or zero.")
	SearchException invalidPhrasePredicateSlop(int slop);

	@Message(id = ID_OFFSET_2 + 54,
			value = "Invalid maximum edit distance: %1$d. The value must be 0, 1 or 2.")
	SearchException invalidFuzzyMaximumEditDistance(int maximumEditDistance);

	@Message(id = ID_OFFSET_2 + 55,
			value = "Invalid exact prefix length: %1$d. The value must be positive or zero.")
	SearchException invalidExactPrefixLength(int exactPrefixLength);

	@Message(id = ID_OFFSET_2 + 57, value = "'%1$s' instance cannot be parsed from value: '%2$s', using the expected formatter: '%3$s'.")
	SearchException unableToParseTemporal(@FormatWith(SimpleNameClassFormatter.class) Class<? extends TemporalAccessor> type, String value, DateTimeFormatter formatter,
			@Cause Exception cause);

	@Message(id = ID_OFFSET_2 + 58, value = "Invalid %1$s value: expected either a Number or a String that can be parsed into a %1$s. %2$s")
	SearchException invalidNumberPropertyValue(@FormatWith(SimpleNameClassFormatter.class) Class<? extends Number> type, String nestedErrorMessage, @Cause Exception cause);

	@Message(id = ID_OFFSET_2 + 59, value = "Invalid '%1$s' value for type '%2$s'.")
	SearchException invalidStringForType(String value, @FormatWith(ClassFormatter.class) Class<?> type, @Cause Exception cause);

	@Message(id = ID_OFFSET_2 + 60, value = "Invalid '%1$s' value for enum '%2$s'.")
	SearchException invalidStringForEnum(String value, @FormatWith(ClassFormatter.class) Class<? extends Enum> enumType, @Cause Exception cause);

	@Message(id = ID_OFFSET_2 + 61, value = "Multiple hits when a single hit was expected: got %1$s hits.")
	SearchException nonSingleHit(long totalHitCount);

	@Message(id = ID_OFFSET_2 + 62,
			value = "The thread was interrupted while a work was being submitted to '%1$s'."
					+ " The work has been discarded." )
	SearchException threadInterruptedWhileSubmittingWork(String orchestratorName);

	@Message(id = ID_OFFSET_2 + 63,
			value = "A work was submitted to '%1$s', but this orchestrator was stopped."
					+ " The work has been discarded." )
	SearchException submittedWorkToStoppedOrchestrator(String orchestratorName);

	@Message(id = ID_OFFSET_2 + 64, value = "Unable to parse the provided geo-point value: '%1$s'. The expected format is latitude, longitude.")
	SearchException unableToParseGeoPoint(String value);

	@Message(id = ID_OFFSET_2 + 65,
			value = "Unknown aggregation key '%1$s'. This key was not used when building the search query." )
	SearchException unknownAggregationKey(AggregationKey<?> key);

	@Message(id = ID_OFFSET_2 + 66,
			value = "Invalid configuration property checking strategy name: '%1$s'. Valid names are: %2$s.")
	SearchException invalidConfigurationPropertyCheckingStrategyName(String invalidRepresentation, List<String> validRepresentations);

	@LogMessage(level = Logger.Level.INFO)
	@Message(id = ID_OFFSET_2 + 67,
			value = "Configuration property tracking is disabled; unused properties will not be logged.")
	void configurationPropertyTrackingDisabled();

	@LogMessage(level = Logger.Level.WARN)
	@Message(id = ID_OFFSET_2 + 68,
			value = "Some properties in the Hibernate Search configuration were not used;"
					+ " there might be misspelled property keys in your configuration. Unused properties were: %1$s."
					+ " To disable this warning, set the property '%2$s' to '%3$s'.")
	void configurationPropertyTrackingUnusedProperties(Set<String> propertyKeys, String disableWarningKey,
			String disableWarningValue);

	@LogMessage(level = Logger.Level.ERROR)
	@Message(id = ID_OFFSET_2 + 69,
			value = "The background failure handler threw an exception while handling a previous failure."
					+ " The failure may not have been reported.")
	void failureInFailureHandler(@Cause Throwable t);

	@Message(id = ID_OFFSET_2 + 70,
			value = "Field template name '%1$s' is invalid: field template names cannot be null or empty.")
	SearchException fieldTemplateNameCannotBeNullOrEmpty(String templateName, @Param EventContext context);

	@Message(id = ID_OFFSET_2 + 71,
			value = "Field template name '%1$s' is invalid: field template names cannot contain a dot ('.').")
	SearchException fieldTemplateNameCannotContainDot(String relativeFieldName,
			@Param EventContext context);

	@Message(id = ID_OFFSET_2 + 72,
			value = "Found multiple values while projecting on a supposedly single-valued field: [%1$s, %2$s].")
	SearchException unexpectedMultiValuedField(Object value1, Object value2);

	@Message(id = ID_OFFSET_2 + 73,
			value = "Some properties in the given Hibernate Search configuration were only valid in Hibernate Search 5"
					+ " and are now obsolete: configuration properties changed in Hibernate Search 6."
					+ " Check out the reference documentation and upgrade your configuration."
					+ " Obsolete properties: %1$s.")
	SearchException obsoleteConfigurationPropertiesFromSearch5(Set<String> propertyKeys);

	@LogMessage(level = Logger.Level.WARN)
	@Message(id = ID_OFFSET_2 + 74,
			value = "Using configuration property '%1$s' to set the name of the default backend to '%2$s'."
					+ " This configuration property is deprecated and shouldn't be used anymore."
					+ " Instead, do not assign a name your default backend"
					+ " and configure it using the 'hibernate.search.backend' prefix,"
					+ " e.g. hibernate.search.backend.type = elasticsearch.")
	void deprecatedExplicitDefaultBackendName(String propertyKey, String explicitDefaultBackendName);
}
