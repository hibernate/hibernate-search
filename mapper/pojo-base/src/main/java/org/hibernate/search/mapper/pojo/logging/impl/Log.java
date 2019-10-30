/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.logging.impl;

import java.lang.annotation.Annotation;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeOptionsStep;
import org.hibernate.search.mapper.pojo.extractor.ContainerExtractor;
import org.hibernate.search.mapper.pojo.logging.spi.OptionalEmptyAsDefaultFormatter;
import org.hibernate.search.mapper.pojo.logging.spi.PojoModelPathFormatter;
import org.hibernate.search.mapper.pojo.logging.spi.PojoTypeModelFormatter;
import org.hibernate.search.mapper.pojo.mapping.impl.PojoContainedTypeManager;
import org.hibernate.search.mapper.pojo.mapping.impl.PojoIndexedTypeManager;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoGenericTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.impl.ClassFormatter;
import org.hibernate.search.util.common.logging.impl.MessageConstants;
import org.hibernate.search.util.common.logging.impl.ToStringTreeAppendableMultilineFormatter;
import org.hibernate.search.util.common.reporting.EventContext;

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
		@ValidIdRange(min = MessageConstants.MAPPER_POJO_ID_RANGE_MIN, max = MessageConstants.MAPPER_POJO_ID_RANGE_MAX),
		// Exceptions for legacy messages from Search 5
		@ValidIdRange(min = 216, max = 216),
		@ValidIdRange(min = 295, max = 295),
		@ValidIdRange(min = 297, max = 297)
		// TODO HSEARCH-3308 add exceptions here for legacy messages from Search 5. See the Lucene logger for examples.
})
public interface Log extends BasicLogger {

	// -----------------------------------
	// Pre-existing messages from Search 5
	// DO NOT ADD ANY NEW MESSAGES HERE
	// -----------------------------------
	int ID_OFFSET_1 = MessageConstants.ENGINE_ID_RANGE_MIN;

	// TODO HSEARCH-3308 migrate relevant messages from Search 5 here

	@Message(id = ID_OFFSET_1 + 216,
			value = "An IndexedEmbedded defines includePaths filters that do not match anything."
					+ " Non-matching includePaths filters: %1$s."
					+ " Encountered field paths: %2$s."
					+ " Check the filters for typos, or remove them if they are not useful."
	)
	SearchException uselessIncludePathFilters(Set<String> nonMatchingIncludePaths, Set<String> encounteredFieldPaths,
			@Param EventContext eventContext);

	@Message(id = ID_OFFSET_1 + 295, value = "String '$1%s' cannot be parsed into a '$2%s'")
	SearchException parseException(String text, @FormatWith(ClassFormatter.class) Class<?> readerClass, @Cause Exception e);

	@Message(id = ID_OFFSET_1 + 297, value = " Value of '%2$s' for type '%1$s' is too big for the conversion")
	SearchException valueTooLargeForConversionException(@FormatWith(ClassFormatter.class) Class<?> type, Object duration, @Cause Exception ae);

	// -----------------------------------
	// New messages from Search 6 onwards
	// -----------------------------------
	int ID_OFFSET_2 = MessageConstants.MAPPER_POJO_ID_RANGE_MIN;

	@Message(id = ID_OFFSET_2 + 1,
			value = "Unable to find a default identifier bridge implementation for type '%1$s'")
	SearchException unableToResolveDefaultIdentifierBridgeFromSourceType(
			@FormatWith(PojoTypeModelFormatter.class) PojoTypeModel<?> sourceType);

	@Message(id = ID_OFFSET_2 + 2,
			value = "Unable to find a default value bridge implementation for type '%1$s'")
	SearchException unableToResolveDefaultValueBridgeFromSourceType(
			@FormatWith(PojoTypeModelFormatter.class) PojoTypeModel<?> sourceType);

	@Message(id = ID_OFFSET_2 + 3,
			value = "Annotation type '%2$s' is annotated with '%1$s',"
					+ " but the binder reference is empty.")
	SearchException missingBinderReferenceInBridgeMapping(
			@FormatWith(ClassFormatter.class) Class<? extends Annotation> metaAnnotationType,
			@FormatWith(ClassFormatter.class) Class<? extends Annotation> annotationType);

	@Message(id = ID_OFFSET_2 + 4,
			value = "Annotation type '%2$s' is annotated with '%1$s',"
					+ " but the binder reference is empty.")
	SearchException missingBinderReferenceInMarkerMapping(
			@FormatWith(ClassFormatter.class) Class<? extends Annotation> metaAnnotationType,
			@FormatWith(ClassFormatter.class) Class<? extends Annotation> annotationType);

	@Message(id = ID_OFFSET_2 + 5,
			value = "Annotation @GenericField on property '%1$s' defines both valueBridge and valueBinder."
					+ " Only one of those can be defined, not both."
	)
	SearchException invalidFieldDefiningBothBridgeReferenceAndBinderReference(String property);

	@Message(id = ID_OFFSET_2 + 6,
			value = "Annotation @DocumentId on property '%1$s' defines both identifierBridge and identifierBinder."
					+ " Only one of those can be defined, not both."
	)
	SearchException invalidDocumentIdDefiningBothBridgeReferenceAndBinderReference(String property);

	@Message(id = ID_OFFSET_2 + 7,
			value = "Invalid empty target for a scoped operation."
					+ " If you want to target all indexes, use 'Object.class' as the target type."
	)
	SearchException invalidEmptyTargetForScope();

	@Message(id = ID_OFFSET_2 + 10,
			value = "Bridge '%1$s' cannot be applied to input type '%2$s'.")
	SearchException invalidInputTypeForBridge(Object bridge,
			@FormatWith(PojoTypeModelFormatter.class) PojoTypeModel<?> typeModel);

	@Message(id = ID_OFFSET_2 + 11,
			value = "Missing field name for GeoPointBinding on type %1$s."
					+ " The field name is mandatory when the bridge is applied on an type, optional when applied on a property.")
	SearchException missingFieldNameForGeoPointBridgeOnType(String typeName);

	@Message(id = ID_OFFSET_2 + 15,
			value = "Cannot interpret the type arguments to the ContainerExtractor interface in "
					+ " implementation '%1$s'. Only the following implementations of ContainerExtractor are valid: "
					+ " 1) implementations setting both type parameters to *raw* types,"
					+ " e.g. class MyExtractor implements ContainerExtractor<MyBean, String>;"
					+ " 2) implementations setting the first type parameter to an array of an unbounded type variable,"
					+ " and setting the second parameter to the same type variable,"
					+ " e.g. MyExtractor<T> implements ContainerExtractor<T[], T>"
					+ " 3) implementations setting the first type parameter to a parameterized type"
					+ " with one argument set to an unbounded type variable and the other to unbounded wildcards,"
					+ " and setting the second type parameter to the same type variable,"
					+ " e.g. MyExtractor<T> implements ContainerExtractor<MyParameterizedBean<?, T, ?>, T>")
	SearchException cannotInferContainerExtractorClassTypePattern(
			@FormatWith(ClassFormatter.class) Class<?> extractorClass, @Cause Exception e);

	@SuppressWarnings("rawtypes")
	@Message(id = ID_OFFSET_2 + 16,
			value = "Cannot apply the requested container value extractor '%1$s' (implementation class: '%2$s') to type '%3$s'")
	SearchException invalidContainerExtractorForType(
			String extractorName,
			@FormatWith(ClassFormatter.class) Class<? extends ContainerExtractor> extractorClass,
			@FormatWith(PojoTypeModelFormatter.class) PojoGenericTypeModel<?> extractedType);

	@LogMessage(level = Logger.Level.DEBUG)
	@Message(id = ID_OFFSET_2 + 17,
			value = "Created POJO indexed type manager: %1$s")
	void createdPojoIndexedTypeManager(
			@FormatWith(ToStringTreeAppendableMultilineFormatter.class) PojoIndexedTypeManager<?, ?, ?> typeManager);

	@LogMessage(level = Logger.Level.DEBUG)
	@Message(id = ID_OFFSET_2 + 18,
			value = "Detected entity types: %1$s, indexed types: %2$s")
	void detectedEntityTypes(Set<PojoRawTypeModel<?>> entityTypes, Set<PojoRawTypeModel<?>> indexedTypes);

	@LogMessage(level = Logger.Level.DEBUG)
	@Message(id = ID_OFFSET_2 + 19,
			value = "Created POJO contained type manager: %1$s")
	void createdPojoContainedTypeManager(
			@FormatWith(ToStringTreeAppendableMultilineFormatter.class) PojoContainedTypeManager<?> typeManager);

	@Message(id = ID_OFFSET_2 + 20,
			value = "Cannot find the inverse side of the association on type '%2$s' at path '%3$s'."
					+ " Hibernate Search needs this information in order to reindex '%2$s' when '%1$s' is modified."
					+ " You can solve this error by defining the inverse side of this association, "
					+ " either with annotations specific to your integration (@OneToMany(mappedBy = ...) in Hibernate ORM) "
					+ " or with the Hibernate Search @AssociationInverseSide annotation."
					+ " Alternatively, if you do not need to reindex '%2$s' when '%1$s' is modified,"
					+ " you can disable reindexing with @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.NO)."
	)
	SearchException cannotInvertAssociationForReindexing(
			@FormatWith(PojoTypeModelFormatter.class) PojoRawTypeModel<?> inverseSideTypeModel,
			@FormatWith(PojoTypeModelFormatter.class) PojoRawTypeModel<?> typeModel,
			@FormatWith(PojoModelPathFormatter.class) PojoModelPathValueNode associationPath);

	@Message(id = ID_OFFSET_2 + 21,
			value = "Cannot apply the path '%2$s' to type '%1$s'."
					+ " This path was resolved as the inverse side of the association '%4$s' on type '%3$s'."
					+ " Hibernate Search needs to apply this path in order to reindex '%3$s' when '%1$s' is modified."
					+ " Error was: '%5$s'")
	SearchException cannotApplyImplicitInverseAssociationPath(
			@FormatWith(PojoTypeModelFormatter.class) PojoRawTypeModel<?> inverseSideTypeModel,
			@FormatWith(PojoModelPathFormatter.class) PojoModelPathValueNode inverseSideAssociationPath,
			@FormatWith(PojoTypeModelFormatter.class) PojoRawTypeModel<?> originalSideTypeModel,
			@FormatWith(PojoModelPathFormatter.class) PojoModelPathValueNode originalSideAssociationPath,
			String errorMessage,
			@Cause Exception cause);

	@Message(id = ID_OFFSET_2 + 22,
			value = "The inverse association targets type '%1$s',"
					+ " but a supertype or subtype of '%2$s' was expected.")
	SearchException incorrectTargetTypeForInverseAssociation(
			@FormatWith(PojoTypeModelFormatter.class) PojoRawTypeModel<?> inverseAssociationTargetType,
			@FormatWith(PojoTypeModelFormatter.class) PojoRawTypeModel<?> entityType);

	@Message(id = ID_OFFSET_2 + 23,
			value = "Property '%2$s' from type '%1$s' is annotated with @AssociationInverseSide,"
					+ " but the inverse path is empty.")
	SearchException missingInversePathInAssociationInverseSideMapping(
			@FormatWith(PojoTypeModelFormatter.class) PojoRawTypeModel<?> typeModel, String propertyName);

	@Message(id = ID_OFFSET_2 + 24,
			value = "Found an infinite embedded recursion involving path '%2$s' on type '%1$s'")
	SearchException infiniteRecursionForAssociationEmbeddeds(
			@FormatWith(PojoTypeModelFormatter.class) PojoRawTypeModel<?> typeModel,
			@FormatWith(PojoModelPathFormatter.class) PojoModelPathValueNode path);

	@Message(id = ID_OFFSET_2 + 26,
			value = "Annotation type '%2$s' is annotated with '%1$s',"
					+ " but both a bridge reference and a binder reference were provided."
					+ " Only one can be provided.")
	SearchException conflictingBridgeReferenceInBridgeMapping(
			@FormatWith(ClassFormatter.class) Class<? extends Annotation> metaAnnotationType,
			@FormatWith(ClassFormatter.class) Class<? extends Annotation> annotationType);

	@Message(id = ID_OFFSET_2 + 27,
			value = "Type '%1$s' is not marked as an entity type, yet it is indexed or targeted"
			+ " by an association from an indexed type. Please check your configuration.")
	SearchException missingEntityTypeMetadata(@FormatWith(PojoTypeModelFormatter.class) PojoRawTypeModel<?> typeModel);

	@Message(id = ID_OFFSET_2 + 28,
			value = "There isn't any explicit document ID mapping for indexed type '%1$s',"
					+ " and the entity ID cannot be used as a default because it is unknown.")
	SearchException missingIdentifierMapping(@FormatWith(PojoTypeModelFormatter.class) PojoRawTypeModel<?> typeModel);

	@Message(id = ID_OFFSET_2 + 29,
			value = "Property '%2$s' from type '%1$s' is annotated with @IndexingDependency,"
					+ " but 'derivedFrom' contains an empty path.")
	SearchException missingPathInIndexingDependencyDerivedFrom(
			@FormatWith(PojoTypeModelFormatter.class) PojoRawTypeModel<?> typeModel, String propertyName);

	@Message(id = ID_OFFSET_2 + 30,
			value = "Found a cyclic dependency between derived properties involving path '%2$s' on type '%1$s'."
					+ " Derived properties cannot be marked as derived from themselves, even indirectly through other "
					+ " derived properties."
					+ " If your model actually contains such cyclic dependency, "
					+ " you should consider disabling automatic reindexing, at least partially "
					+ " using @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.NO) on one of the properties in the cycle."
	)
	SearchException infiniteRecursionForDerivedFrom(
			@FormatWith(PojoTypeModelFormatter.class) PojoRawTypeModel<?> typeModel,
			@FormatWith(PojoModelPathFormatter.class) PojoModelPathValueNode path);

	@Message(id = ID_OFFSET_2 + 31,
			value = "This property's mapping expects a standard String type for the index field,"
					+ " but the assigned value bridge or value binder declares a non-standard or non-String type."
					+ " Make sure to use a compatible bridge or binder."
					+ " Details: encountered type DSL step '%1$s',"
					+ " which does not extend the expected '%2$s' interface."
	)
	SearchException invalidFieldEncodingForStringFieldMapping(IndexFieldTypeOptionsStep<?, ?> step,
			@FormatWith(ClassFormatter.class) Class<?> expectedContextType);

	@Message(id = ID_OFFSET_2 + 34, value = "Could not find a property with the '%1$s' marker for field '%2$s' (marker set: '%3$s').")
	SearchException propertyMarkerNotFound(String markerName, String fieldName, String markerSet);

	@Message(id = ID_OFFSET_2 + 35, value = "Found multiple properties with the '%1$s' marker for field '%2$s' (marker set: '%3$s').")
	SearchException multiplePropertiesForMarker(String markerName, String fieldName, String markerSet);

	@Message(id = ID_OFFSET_2 + 36,
			value = "Some of the given types cannot be targeted."
					+ " These types are not indexed, nor is any of their subtypes: %1$s."
					+ " Note that some of them are indexed-embedded in an indexed entity, but this is not enough to be targeted"
					+ " (only indexed types can be targeted): %2$s."
	)
	SearchException invalidScopeTarget(Collection<Class<?>> nonIndexedTypes, Collection<Class<?>> containedTypes);

	@Message(id = ID_OFFSET_2 + 37, value = "Cannot work on type %1$s, because it is not indexed, neither directly nor as a contained entity in another type.")
	SearchException notIndexedTypeNorAsDelegate(@FormatWith(ClassFormatter.class) Class<?> targetedType);

	@Message(id = ID_OFFSET_2 + 38, value = "The identifier for this entity should always be provided, but the provided identifier was null." )
	SearchException nullProvidedIdentifier();

	@Message(id = ID_OFFSET_2 + 39, value = "Requested incompatible type for '%1$s': '%2$s'")
	SearchException incompatibleRequestedType(@FormatWith(PojoModelPathFormatter.class) PojoModelPathValueNode accessor, @FormatWith(ClassFormatter.class) Class<?> requestedType);

	@Message(id = ID_OFFSET_2 + 40, value = "Cannot work on type %1$s, because it is not directly indexed.")
	SearchException notDirectlyIndexedType(@FormatWith(ClassFormatter.class) Class<?> targetedType);

	@Message(id = ID_OFFSET_2 + 41,
			value = "A chain of multiple container extractors cannot include the default extractors."
					+ " Either use only the default extractors, or explicitly reference every single extractor to be applied instead."
	)
	SearchException cannotUseDefaultExtractorsInMultiExtractorChain();

	@Message(id = ID_OFFSET_2 + 43, value = "Error creating URL from String '%1$s'.")
	SearchException malformedURL(String value, @Cause MalformedURLException e);

	@Message(id = ID_OFFSET_2 + 44, value = "Error creating URI from String '%1$s'.")
	SearchException badURISyntax(String value, @Cause URISyntaxException e);

	@Message(id = ID_OFFSET_2 + 45,
			value = "A PojoModelPath must include at least one property."
	)
	SearchException cannotDefinePojoModelPathWithoutProperty();

	@Message(id = ID_OFFSET_2 + 46,
			value = "Cannot apply the path '%2$s' to type '%1$s'."
					+ " This path was declared as a path to collect entities of type '%3$s' to be reindexed."
					+ " Hibernate Search needs to apply this path in order to reindex '%3$s' when '%1$s' is modified."
					+ " Error was: '%4$s'")
	SearchException cannotApplyExplicitInverseAssociationPath(
			@FormatWith(PojoTypeModelFormatter.class) PojoRawTypeModel<?> inverseSideTypeModel,
			@FormatWith(PojoModelPathFormatter.class) PojoModelPathValueNode inverseSideAssociationPath,
			@FormatWith(PojoTypeModelFormatter.class) PojoRawTypeModel<?> originalSideTypeModel,
			String errorMessage,
			@Cause Exception cause);

	@Message(id = ID_OFFSET_2 + 47,
			value = "'fromOtherEntity' can only be used when the bridged element has an entity type,"
					+ " but the bridged element has type '%1$s',"
					+ " which is not an entity type.")
	SearchException cannotDefineOtherEntityDependencyOnNonEntityBridgedType(
			@FormatWith(PojoTypeModelFormatter.class) PojoRawTypeModel<?> bridgedTypeModel);

	@Message(id = ID_OFFSET_2 + 48,
			value = "'fromOtherEntity' expects an entity type; type '%1$s' is not an entity type.")
	SearchException cannotDefineOtherEntityDependencyFromNonEntityType(
			@FormatWith(PojoTypeModelFormatter.class) PojoRawTypeModel<?> otherType);

	@Message(id = ID_OFFSET_2 + 49,
			value = "The binder did not declare any dependency to the entity model during binding."
					+ " Declare dependencies using context.getDependencies().use(...) or,"
					+ " if the bridge really does not depend on the entity model, context.getDependencies().useRootOnly()."
	)
	SearchException missingBridgeDependencyDeclaration();

	@Message(id = ID_OFFSET_2 + 50,
			value = "The binder called context.getDependencies().useRootOnly() during binding,"
					+ " but also declared extra dependencies to the entity model."
	)
	SearchException inconsistentBridgeDependencyDeclaration();

	@Message(id = ID_OFFSET_2 + 51,
			value = "This property's mapping expects a scaled number type (BigDecimal or BigInteger) for the index field,"
					+ " but the assigned value bridge or value binder declares a non-scaled type."
					+ " Make sure to use a compatible bridge or binder."
					+ " Details: encountered type DSL step '%1$s',"
					+ " which does not extend the expected '%2$s' interface."
	)
	SearchException invalidFieldEncodingForScaledNumberFieldMapping(IndexFieldTypeOptionsStep<?, ?> step,
			@FormatWith(ClassFormatter.class) Class<?> expectedContextType);

	@Message(id = ID_OFFSET_2 + 52,
			value = "Extractors cannot be defined explicitly when extract = ContainerExtract.NO."
					+ " Either leave 'extract' to its default value to define extractors explicitly"
					+ " or leave the 'extractor' list to its default, empty value to disable extraction."
	)
	SearchException cannotReferenceExtractorsWhenExtractionDisabled();

	@Message(id = ID_OFFSET_2 + 53,
			value = "Cannot resolve container extractor name '%1$s'."
					+ " Check that this name matches a container extractor,"
					+ " either a builtin one whose name is a constant in '%2$s'"
					+ " or a custom one that was properly registered."
	)
	SearchException cannotResolveContainerExtractorName(String extractorName,
			@FormatWith(ClassFormatter.class) Class<?> builtinExtractorConstantsClass);

	@Message(id = ID_OFFSET_2 + 54,
			value = "Binder '%1$s' cannot be initialized with annotations of type '%2$s'.")
	SearchException invalidAnnotationTypeForBinder(Object binder,
			@FormatWith(ClassFormatter.class) Class<?> annotationType);

	@Message(id = ID_OFFSET_2 + 55,
			value = "Type '%1$s' is contained in an indexed type but is not itself indexed,"
					+ " thus entity with identifier '%2$s' cannot be purged."
					+ " Use delete() and pass the entity instead of just the identifier."
	)
	SearchException cannotPurgeNonIndexedContainedType(
			@FormatWith(ClassFormatter.class) Class<?> typeManager, Object providedId);

	@Message(id = ID_OFFSET_2 + 56, value = "Could not access to the org.hibernate.annotations.common.reflection.java.JavaXMember#getMember method")
	SearchException cannotAccessPropertyMember(@Cause Exception cause);

	@Message(id = ID_OFFSET_2 + 57, value = "The underlying member of property '%1$s' is supposed to be a method")
	SearchException cannotAccessPropertyMethod(String propertyName);

	@Message(id = ID_OFFSET_2 + 58,
			value = "The bind() method of binder '%1$s' is not implemented correctly:"
					+ " it did not call context.setBridge().")
	SearchException missingBridgeForBinder(Object binder);

	@Message(id = ID_OFFSET_2 + 59,
			value = "The bind() method of binder '%1$s' is not implemented correctly:"
					+ " it did not call context.setMarker(...).")
	SearchException missingMarkerForBinder(Object binder);

	@Message(id = ID_OFFSET_2 + 60,
			value = "Entity processing was triggered while already processing entities, which is not supported."
					+ " Make sure you do not change entities within an entity getter or a custom bridge used for indexing,"
					+ " and avoid any event that could trigger entity processing."
					+ " Hibernate ORM flushes, in particular, must be avoided in entity getters and bridges.")
	SearchException recursiveIndexingPlanProcess();

	@Message(id = ID_OFFSET_2 + 61, value = "Type '%1$s' cannot be indexed-embedded, because no index mapping (@GenericField, @FullTextField, ...) is defined for that type.")
	SearchException invalidIndexedEmbedded(@FormatWith(PojoTypeModelFormatter.class) PojoTypeModel<?> typeModel);

	@Message(id = ID_OFFSET_2 + 62,
			value = "Cannot map type '%1$s' to an index, because this type is abstract."
					+ " Index mappings are not inherited: they apply to exact instances of a given type."
					+ " As a result, mapping an abstract type to an index does not make sense,"
					+ " since the index would always be empty.")
	SearchException cannotMapAbstractTypeToIndex(
			@FormatWith(PojoTypeModelFormatter.class) PojoRawTypeModel<?> typeModel);

	@Message(id = ID_OFFSET_2 + 63, value = "Trying to map type to multiple indexes:"
			+ " '%2$s' in backend '%1$s',"
			+ " '%4$s' in backend '%3$s.")
	SearchException multipleIndexMapping(
			@FormatWith(OptionalEmptyAsDefaultFormatter.class) Optional<String> backendName,
			@FormatWith(OptionalEmptyAsDefaultFormatter.class) Optional<String> indexName,
			@FormatWith(OptionalEmptyAsDefaultFormatter.class) Optional<String> otherBackendName,
			@FormatWith(OptionalEmptyAsDefaultFormatter.class) Optional<String> otherIndexName);

	@Message(id = ID_OFFSET_2 + 64, value = "Multiple entity names assigned to the same type: '%1$s', '%2$s'.")
	SearchException multipleEntityNames(String entityName, String otherEntityName);

	@Message(id = ID_OFFSET_2 + 65,
			value = "This property's mapping expects a standard type for the index field,"
					+ " but the assigned value bridge or value binder declares a non-standard type."
					+ " Make sure to use a compatible bridge or binder."
					+ " Details: encountered type DSL step '%1$s',"
					+ " which does not extend the expected '%2$s' interface."
	)
	SearchException invalidFieldEncodingForStandardFieldMapping(IndexFieldTypeOptionsStep<?, ?> step,
			@FormatWith(ClassFormatter.class) Class<?> expectedContextType);

	@Message(id = ID_OFFSET_2 + 66,
			value = "This property's mapping expects a non-standard type for the index field,"
					+ " but the assigned value bridge or value binder declares a standard type."
					+ " Switch to a standard field annotation such as @GenericField."
					+ " Details: encountered type DSL step '%1$s',"
					+ " which does extend the '%2$s' interface."
	)
	SearchException invalidFieldEncodingForNonStandardFieldMapping(IndexFieldTypeOptionsStep<?, ?> step,
			@FormatWith(ClassFormatter.class) Class<?> expectedContextType);

}
