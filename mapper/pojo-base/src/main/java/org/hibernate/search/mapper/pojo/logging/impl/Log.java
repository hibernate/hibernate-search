/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.logging.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Set;

import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeOptionsStep;
import org.hibernate.search.mapper.pojo.extractor.ContainerExtractor;
import org.hibernate.search.mapper.pojo.logging.spi.PojoModelPathFormatter;
import org.hibernate.search.mapper.pojo.logging.spi.PojoTypeModelFormatter;
import org.hibernate.search.mapper.pojo.mapping.impl.PojoContainedTypeManager;
import org.hibernate.search.mapper.pojo.mapping.impl.PojoIndexedTypeManager;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoGenericTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.impl.ClassFormatter;
import org.hibernate.search.util.common.logging.impl.MessageConstants;
import org.hibernate.search.util.common.logging.impl.SimpleNameClassFormatter;
import org.hibernate.search.util.common.logging.impl.ToStringTreeAppendableMultilineFormatter;
import org.hibernate.search.util.common.logging.impl.TypeFormatter;
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
		// Exceptions for legacy messages from Search 5 (engine module)
		@ValidIdRange(min = 135, max = 135),
		@ValidIdRange(min = 159, max = 159),
		@ValidIdRange(min = 160, max = 160),
		@ValidIdRange(min = 177, max = 177),
		@ValidIdRange(min = 216, max = 216),
		@ValidIdRange(min = 221, max = 221),
		@ValidIdRange(min = 234, max = 234),
		@ValidIdRange(min = 295, max = 295),
		@ValidIdRange(min = 297, max = 297)
})
public interface Log extends BasicLogger {

	// -----------------------------------
	// Pre-existing messages from Search 5 (engine module)
	// DO NOT ADD ANY NEW MESSAGES HERE
	// -----------------------------------
	int ID_OFFSET_LEGACY_ENGINE = MessageConstants.ENGINE_ID_RANGE_MIN;

	@Message(id = ID_OFFSET_LEGACY_ENGINE + 135,
			value = "No default value bridge implementation for type '%1$s'."
					+ " Use a custom bridge.")
	SearchException unableToResolveDefaultValueBridgeFromSourceType(
			@FormatWith(PojoTypeModelFormatter.class) PojoTypeModel<?> sourceType);

	@Message(id = ID_OFFSET_LEGACY_ENGINE + 159,
			value = "No property annotated with %1$s(markerSet = \"%3$s\")."
					+ " There must be exactly one such property in order to map it to geo-point field '%2$s'.")
	SearchException unableToFindLongitudeOrLatitudeProperty(String annotation, String fieldName, String markerSet);

	@Message(id = ID_OFFSET_LEGACY_ENGINE + 160,
			value = "Multiple properties annotated with %1$s(markerSet = \"%3$s\")."
					+ " There must be exactly one such property in order to map it to geo-point field '%2$s'.")
	SearchException multipleLatitudeOrLongitudeProperties(String annotation, String fieldName, String markerSet);

	@Message(id = ID_OFFSET_LEGACY_ENGINE + 177,
			value = "Unable to define a document identifier for indexed type '%1$s',"
					+ " The property representing the entity identifier is unknown."
					+ " Define the document identifier explicitly by annotating"
					+ " a property whose values are unique with @DocumentId.")
	SearchException missingIdentifierMapping(@FormatWith(PojoTypeModelFormatter.class) PojoRawTypeModel<?> typeModel);

	@Message(id = ID_OFFSET_LEGACY_ENGINE + 216,
			value = "An @IndexedEmbedded defines includePaths filters that do not match anything."
					+ " Non-matching includePaths filters: %1$s."
					+ " Encountered field paths: %2$s."
					+ " Check the filters for typos, or remove them if they are not useful."
	)
	SearchException uselessIncludePathFilters(Set<String> nonMatchingIncludePaths, Set<String> encounteredFieldPaths,
			@Param EventContext eventContext);

	@Message(id = ID_OFFSET_LEGACY_ENGINE + 221,
			value = "Infinite embedded recursion involving path '%2$s' on type '%1$s'")
	SearchException infiniteRecursionForAssociationEmbeddeds(
			@FormatWith(PojoTypeModelFormatter.class) PojoRawTypeModel<?> typeModel,
			@FormatWith(PojoModelPathFormatter.class) PojoModelPathValueNode path);

	@Message(id = ID_OFFSET_LEGACY_ENGINE + 234,
			value = "Invalid target types: %1$s"
					+ " These types are not indexed, nor is any of their subtypes."
	)
	SearchException invalidScopeTarget(Collection<PojoRawTypeIdentifier<?>> nonIndexedTypes);

	@Message(id = ID_OFFSET_LEGACY_ENGINE + 295, value = "Invalid value for type '$2%s': '$1%s'. %3$s")
	SearchException parseException(String text, @FormatWith(SimpleNameClassFormatter.class) Class<?> readerClass,
			String causeMessage, @Cause Exception e);

	@Message(id = ID_OFFSET_LEGACY_ENGINE + 297, value = "Unable to convert '%2$s' into type '%1$s': value is too large.")
	SearchException valueTooLargeForConversionException(@FormatWith(SimpleNameClassFormatter.class) Class<?> type,
			Object duration, @Cause Exception ae);

	// -----------------------------------
	// New messages from Search 6 onwards
	// -----------------------------------
	int ID_OFFSET = MessageConstants.MAPPER_POJO_ID_RANGE_MIN;

	@Message(id = ID_OFFSET + 1,
			value = "No default identifier bridge implementation for type '%1$s'. Use a custom bridge.")
	SearchException unableToResolveDefaultIdentifierBridgeFromSourceType(
			@FormatWith(PojoTypeModelFormatter.class) PojoTypeModel<?> sourceType);

	@Message(id = ID_OFFSET + 3,
			value = "Empty binder reference.")
	SearchException missingBinderReferenceInBinding();

	@Message(id = ID_OFFSET + 5,
			value = "Ambiguous value bridge reference: both 'valueBridge' and 'valueBinder' are set."
					+ " Only one can be set."
	)
	SearchException invalidFieldDefiningBothBridgeReferenceAndBinderReference();

	@Message(id = ID_OFFSET + 6,
			value = "Ambiguous identifier bridge reference: both 'identifierBridge' and 'identifierBinder' are set."
					+ " Only one can be set."
	)
	SearchException invalidDocumentIdDefiningBothBridgeReferenceAndBinderReference();

	@Message(id = ID_OFFSET + 7,
			value = "Empty scope."
					+ " If you want to target all indexes, pass 'Object.class' as the target type."
	)
	SearchException invalidEmptyTargetForScope();

	@Message(id = ID_OFFSET + 10,
			value = "Invalid bridge for input type '%2$s': '%1$s'. This bridge expects an input of type '%3$s'.")
	SearchException invalidInputTypeForBridge(Object bridge,
			@FormatWith(PojoTypeModelFormatter.class) PojoTypeModel<?> typeModel,
			@FormatWith(PojoTypeModelFormatter.class) PojoTypeModel<?> expectedTypeModel);

	@Message(id = ID_OFFSET + 11,
			value = "Missing field name for @GeoPointBinding on type %1$s."
					+ " The field name is mandatory when the bridge is applied to a type, optional when applied to a property.")
	SearchException missingFieldNameForGeoPointBridgeOnType(String typeName);

	@Message(id = ID_OFFSET + 15,
			value = "Unable to interpret the type arguments to the ContainerExtractor interface in "
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
	@Message(id = ID_OFFSET + 16,
			value = "Invalid container extractor for type '%3$s': '%1$s' (implementation class: '%2$s')")
	SearchException invalidContainerExtractorForType(
			String extractorName,
			@FormatWith(ClassFormatter.class) Class<? extends ContainerExtractor> extractorClass,
			@FormatWith(PojoTypeModelFormatter.class) PojoGenericTypeModel<?> extractedType);

	@LogMessage(level = Logger.Level.DEBUG)
	@Message(id = ID_OFFSET + 17,
			value = "Type manager for indexed type '%1$s': %2$s")
	void indexedTypeManager(
			@FormatWith(PojoTypeModelFormatter.class) PojoRawTypeModel<?> typeModel,
			@FormatWith(ToStringTreeAppendableMultilineFormatter.class) PojoIndexedTypeManager<?, ?> typeManager);

	@LogMessage(level = Logger.Level.DEBUG)
	@Message(id = ID_OFFSET + 18,
			value = "Detected entity types: %1$s, indexed types: %2$s")
	void detectedEntityTypes(Set<PojoRawTypeModel<?>> entityTypes, Set<PojoRawTypeModel<?>> indexedTypes);

	@LogMessage(level = Logger.Level.DEBUG)
	@Message(id = ID_OFFSET + 19,
			value = "Type manager for contained type '%1$s': %2$s")
	void containedTypeManager(
			@FormatWith(PojoTypeModelFormatter.class) PojoRawTypeModel<?> typeModel,
			@FormatWith(ToStringTreeAppendableMultilineFormatter.class) PojoContainedTypeManager<?> typeManager);

	@Message(id = ID_OFFSET + 20,
			value = "Unable to find the inverse side of the association on type '%2$s' at path '%3$s'."
					+ " Hibernate Search needs this information in order to reindex '%2$s' when '%1$s' is modified."
					+ " You can solve this error by defining the inverse side of this association, "
					+ " either with annotations specific to your integration (@OneToMany(mappedBy = ...) in Hibernate ORM) "
					+ " or with the Hibernate Search @AssociationInverseSide annotation."
					+ " Alternatively, if you do not need to reindex '%2$s' when '%1$s' is modified,"
					+ " you can disable automatic reindexing with @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)."
	)
	SearchException cannotInvertAssociationForReindexing(
			@FormatWith(PojoTypeModelFormatter.class) PojoRawTypeModel<?> inverseSideTypeModel,
			@FormatWith(PojoTypeModelFormatter.class) PojoRawTypeModel<?> typeModel,
			@FormatWith(PojoModelPathFormatter.class) PojoModelPathValueNode associationPath);

	@Message(id = ID_OFFSET + 21,
			value = "Unable to apply path '%2$s' to type '%1$s'."
					+ " This path was resolved as the inverse side of the association '%4$s' on type '%3$s'."
					+ " Hibernate Search needs to apply this path in order to reindex '%3$s' when '%1$s' is modified."
					+ " Nested exception: %5$s")
	SearchException cannotApplyImplicitInverseAssociationPath(
			@FormatWith(PojoTypeModelFormatter.class) PojoRawTypeModel<?> inverseSideTypeModel,
			@FormatWith(PojoModelPathFormatter.class) PojoModelPathValueNode inverseSideAssociationPath,
			@FormatWith(PojoTypeModelFormatter.class) PojoRawTypeModel<?> originalSideTypeModel,
			@FormatWith(PojoModelPathFormatter.class) PojoModelPathValueNode originalSideAssociationPath,
			String errorMessage,
			@Cause Exception cause);

	@Message(id = ID_OFFSET + 22,
			value = "The inverse association targets type '%1$s',"
					+ " but a supertype or subtype of '%2$s' was expected.")
	SearchException incorrectTargetTypeForInverseAssociation(
			@FormatWith(PojoTypeModelFormatter.class) PojoRawTypeModel<?> inverseAssociationTargetType,
			@FormatWith(PojoTypeModelFormatter.class) PojoRawTypeModel<?> entityType);

	@Message(id = ID_OFFSET + 23,
			value = "@AssociationInverseSide.inversePath is empty.")
	SearchException missingInversePathInAssociationInverseSideMapping();

	@Message(id = ID_OFFSET + 27,
			value = "Unable to index type '%1$s': this type is not an entity type."
					+ " If you only expect subtypes to be instantiated, make this type abstract."
					+ " If you expect this exact type to be instantiated and want it to be indexed, make it an entity type."
					+ " Otherwise, ensure this type and its subtypes are never indexed by removing the @Indexed annotation"
					+ " or by annotating the type with @Indexed(enabled = false).")
	SearchException missingEntityTypeMetadata(@FormatWith(PojoTypeModelFormatter.class) PojoRawTypeModel<?> typeModel);

	@Message(id = ID_OFFSET + 29,
			value = "@IndexingDependency.derivedFrom contains an empty path.")
	SearchException missingPathInIndexingDependencyDerivedFrom();

	@Message(id = ID_OFFSET + 30,
			value = "Unable to resolve dependencies of a derived property:"
					+ " there is a cyclic dependency involving path '%2$s' on type '%1$s'."
					+ " A derived properties cannot be marked as derived from itself, even indirectly through other "
					+ " derived properties."
					+ " If your model actually contains such cyclic dependency, "
					+ " you should consider disabling automatic reindexing, at least partially "
					+ " using @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.NO) on one of the properties in the cycle."
	)
	SearchException infiniteRecursionForDerivedFrom(
			@FormatWith(PojoTypeModelFormatter.class) PojoRawTypeModel<?> typeModel,
			@FormatWith(PojoModelPathFormatter.class) PojoModelPathValueNode path);

	@Message(id = ID_OFFSET + 31,
			value = "Unable to apply property mapping:"
					+ " this property mapping must target an index field of standard String type,"
					+ " but the resolved field type is non-standard or non-String."
					+ " This generally means you need to use a different field annotation"
					+ " or to convert property values using a custom ValueBridge or ValueBinder."
					+ " If you are already using a custom ValueBridge or ValueBinder, check its field type."
					+ " Details: encountered type DSL step '%1$s',"
					+ " which does not extend the expected interface '%2$s'."
	)
	SearchException invalidFieldEncodingForStringFieldMapping(IndexFieldTypeOptionsStep<?, ?> step,
			@FormatWith(ClassFormatter.class) Class<?> expectedContextType);

	@Message(id = ID_OFFSET + 37,
			value = "Invalid type '%1$s' in an indexing plan:"
					+ " this type is not indexed, neither directly nor as a contained entity in another indexed type.")
	SearchException nonIndexedNorContainedTypeInIndexingPlan(PojoRawTypeIdentifier<?> targetedType);

	@Message(id = ID_OFFSET + 38, value = "The entity identifier must not be null." )
	SearchException nullProvidedIdentifier();

	@Message(id = ID_OFFSET + 39, value = "'%1$s' cannot be assigned to '%2$s'")
	SearchException incompatibleRequestedType(@FormatWith(PojoModelPathFormatter.class) PojoModelPathValueNode accessor,
			@FormatWith(ClassFormatter.class) Class<?> requestedType);

	@Message(id = ID_OFFSET + 40,
			value = "Invalid type '%1$s' in an indexer: this type is not indexed.")
	SearchException nonIndexedTypeInIndexer(PojoRawTypeIdentifier<?> targetedType);

	@Message(id = ID_OFFSET + 41,
			value = "Invalid reference to default extractors:"
					+ " a chain of multiple container extractors must not include the default extractors."
					+ " Either use only the default extractors, or explicitly reference every single extractor to be applied."
	)
	SearchException cannotUseDefaultExtractorsInMultiExtractorChain();

	@Message(id = ID_OFFSET + 43, value = "Exception creating URL from String '%1$s'.")
	SearchException malformedURL(String value, @Cause MalformedURLException e);

	@Message(id = ID_OFFSET + 44, value = "Exception creating URI from String '%1$s'.")
	SearchException badURISyntax(String value, @Cause URISyntaxException e);

	@Message(id = ID_OFFSET + 45,
			value = "A PojoModelPath must include at least one property."
	)
	SearchException cannotDefinePojoModelPathWithoutProperty();

	@Message(id = ID_OFFSET + 46,
			value = "Unable to apply path '%2$s' to type '%1$s'."
					+ " This path was declared as a path to collect entities of type '%3$s' to be reindexed."
					+ " Hibernate Search needs to apply this path in order to reindex '%3$s' when '%1$s' is modified."
					+ " Nested exception: %4$s")
	SearchException cannotApplyExplicitInverseAssociationPath(
			@FormatWith(PojoTypeModelFormatter.class) PojoRawTypeModel<?> inverseSideTypeModel,
			@FormatWith(PojoModelPathFormatter.class) PojoModelPathValueNode inverseSideAssociationPath,
			@FormatWith(PojoTypeModelFormatter.class) PojoRawTypeModel<?> originalSideTypeModel,
			String errorMessage,
			@Cause Exception cause);

	@Message(id = ID_OFFSET + 47,
			value = "Invalid use of 'fromOtherEntity': this method can only be used when the bridged element has an entity type,"
					+ " but the bridged element has type '%1$s',"
					+ " which is not an entity type.")
	SearchException cannotDefineOtherEntityDependencyOnNonEntityBridgedType(
			@FormatWith(PojoTypeModelFormatter.class) PojoRawTypeModel<?> bridgedTypeModel);

	@Message(id = ID_OFFSET + 48,
			value = "Invalid type passed to 'fromOtherEntity': the type must be an entity type. Type '%1$s' is not an entity type.")
	SearchException cannotDefineOtherEntityDependencyFromNonEntityType(
			@FormatWith(PojoTypeModelFormatter.class) PojoRawTypeModel<?> otherType);

	@Message(id = ID_OFFSET + 49,
			value = "Incorrect binder implementation: the binder did not declare any dependency to the entity model during binding."
					+ " Declare dependencies using context.dependencies().use(...) or,"
					+ " if the bridge really does not depend on the entity model, context.dependencies().useRootOnly()."
	)
	SearchException missingBridgeDependencyDeclaration();

	@Message(id = ID_OFFSET + 50,
			value = "Incorrect binder implementation: the binder called context.dependencies().useRootOnly() during binding,"
					+ " but also declared extra dependencies to the entity model."
	)
	SearchException inconsistentBridgeDependencyDeclaration();

	@Message(id = ID_OFFSET + 51,
			value = "Unable to apply property mapping:"
					+ " this property mapping must target an index field"
					+ " of standard, scaled-number type (BigDecimal or BigInteger),"
					+ " but the resolved field type is non-standard or non-scaled."
					+ " This generally means you need to use a different field annotation"
					+ " or to convert property values using a custom ValueBridge or ValueBinder."
					+ " If you are already using a custom ValueBridge or ValueBinder, check its field type."
					+ " Details: encountered type DSL step '%1$s',"
					+ " which does not extend the expected interface '%2$s'."
	)
	SearchException invalidFieldEncodingForScaledNumberFieldMapping(IndexFieldTypeOptionsStep<?, ?> step,
			@FormatWith(ClassFormatter.class) Class<?> expectedContextType);

	@Message(id = ID_OFFSET + 52,
			value = "Unexpected extractor references:"
					+ " extractors cannot be defined explicitly when extract = ContainerExtract.NO."
					+ " Either leave 'extract' to its default value to define extractors explicitly"
					+ " or leave the 'extractor' list to its default, empty value to disable extraction."
	)
	SearchException cannotReferenceExtractorsWhenExtractionDisabled();

	@Message(id = ID_OFFSET + 53,
			value = "No container extractor with name '%1$s'."
					+ " Check that this name matches a container extractor,"
					+ " either a builtin one whose name is a constant in '%2$s'"
					+ " or a custom one that was properly registered.")
	SearchException cannotResolveContainerExtractorName(String extractorName,
			@FormatWith(ClassFormatter.class) Class<?> builtinExtractorConstantsClass);

	@Message(id = ID_OFFSET + 55,
			value = "Unable to purge entity of type '%1$s' with identifier '%2$s': "
					+ " this type is contained in an indexed type but is not itself indexed."
	)
	SearchException cannotPurgeNonIndexedContainedType(PojoRawTypeIdentifier<?> type, Object providedId);

	@Message(id = ID_OFFSET + 58,
			value = "Incorrect binder implementation: binder '%1$s' did not call context.bridge(...).")
	SearchException missingBridgeForBinder(Object binder);

	@Message(id = ID_OFFSET + 59,
			value = "Incorrect binder implementation: binder '%1$s' did not call context.marker(...).")
	SearchException missingMarkerForBinder(Object binder);

	@Message(id = ID_OFFSET + 60,
			value = "Unable to trigger entity processing while already processing entities."
					+ " Make sure you do not change entities within an entity getter or a custom bridge used for indexing,"
					+ " and avoid any event that could trigger entity processing."
					+ " Hibernate ORM flushes, in particular, must be avoided in entity getters and bridges.")
	SearchException recursiveIndexingPlanProcess();

	@Message(id = ID_OFFSET + 61,
			value = "Unable to index-embed type '%1$s': no index mapping"
					+ " (@GenericField, @FullTextField, custom bridges, ...) is defined for that type.")
	SearchException invalidIndexedEmbedded(@FormatWith(PojoTypeModelFormatter.class) PojoTypeModel<?> typeModel);

	@Message(id = ID_OFFSET + 64, value = "Multiple entity names assigned to the same type: '%1$s', '%2$s'.")
	SearchException multipleEntityNames(String entityName, String otherEntityName);

	@Message(id = ID_OFFSET + 65,
			value = "Unable to apply property mapping:"
					+ " this property mapping must target an index field of standard type,"
					+ " but the resolved field type is non-standard."
					+ " This generally means you need to use a different field annotation"
					+ " or to convert property values using a custom ValueBridge or ValueBinder."
					+ " If you are already using a custom ValueBridge or ValueBinder, check its field type."
					+ " Details: encountered type DSL step '%1$s',"
					+ " which does not extend the expected interface '%2$s'.")
	SearchException invalidFieldEncodingForStandardFieldMapping(IndexFieldTypeOptionsStep<?, ?> step,
			@FormatWith(ClassFormatter.class) Class<?> expectedContextType);

	@Message(id = ID_OFFSET + 66,
			value = "Unable to apply property mapping: this property mapping must target an index field of non-standard type,"
					+ " but the resolved field type is standard."
					+ " Switch to a standard field annotation such as @GenericField."
					+ " Details: encountered type DSL step '%1$s',"
					+ " which does extend the interface '%2$s'.")
	SearchException invalidFieldEncodingForNonStandardFieldMapping(IndexFieldTypeOptionsStep<?, ?> step,
			@FormatWith(ClassFormatter.class) Class<?> expectedContextType);

	@Message(id = ID_OFFSET + 67,
			value = "Invalid annotation processor: '%1$s'."
					+ " This processor expects annotations of a different type: '%2$s'.")
	SearchException invalidAnnotationTypeForAnnotationProcessor(Object annotationProcessor,
			@FormatWith(ClassFormatter.class) Class<?> processorAnnotationType);

	@Message(id = ID_OFFSET + 68,
			value = "Empty annotation processor reference in meta-annotation '%1$s'.")
	SearchException missingProcessorReferenceInMappingAnnotation(
			@FormatWith(ClassFormatter.class) Class<? extends Annotation> metaAnnotationType);

	@Message(id = ID_OFFSET + 69,
			value = "Ambiguous @IndexedEmbedded name: both 'name' and 'prefix' are set."
					+ " Only one can be set."
					+ " Name is '%1$s', prefix is '%2$s'.")
	SearchException cannotSetBothIndexedEmbeddedNameAndPrefix(String relativeFieldName, String prefix);

	@Message(id = ID_OFFSET + 70,
			value = "Invalid index field name '%1$s': field names cannot contain a dot ('.').")
	SearchException invalidFieldNameDotNotAllowed(String relativeFieldName);

	@Message(id = ID_OFFSET + 71,
			value = "No property annotated with @Alternative(id = %1$s)."
					+ " There must be exactly one such property in order to map property '%2$s' to multi-alternative fields.")
	SearchException cannotFindAlternativeDiscriminator(String alternativeId, String fieldValueSourcePropertyName);

	@Message(id = ID_OFFSET + 72,
			value = "Multiple properties annotated with @Alternative(id = %1$s)."
					+ " There must be exactly one such property in order to map property '%2$s' to multi-alternative fields.")
	SearchException conflictingAlternativeDiscriminators(String alternativeId, String fieldValueSourcePropertyName);

	@Message(id = ID_OFFSET + 73,
			value = "Invalid routing bridge for entity type '%2$s': '%1$s'"
					+ " This bridge expects an entity type extending '%3$s'.")
	SearchException invalidInputTypeForRoutingBridge(Object routingBridge,
			@FormatWith(PojoTypeModelFormatter.class) PojoTypeModel<?> typeModel,
			@FormatWith(PojoTypeModelFormatter.class) PojoTypeModel<?> expectedTypeModel);

	@Message(id = ID_OFFSET + 75,
			value = "Incorrect routing bridge implementation: routing bridge '%1$s' did not define any current route."
					+ " In the implementation of RoutingBridge.route(...),"
					+ " define exactly one current route by calling 'routes.addRoute()',"
					+ " or explicitly indicate indexing is not required by calling 'routes.notIndexed()'.")
	SearchException noCurrentRoute(Object routingBridge);

	@Message(id = ID_OFFSET + 76,
			value = "Incorrect routing bridge implementation: routing bridge '%1$s' defined multiple current routes."
					+ " In the implementation of RoutingBridge.route(...),"
					+ " define at most one current route by calling 'routes.addRoute()' at most once.")
	SearchException multipleCurrentRoutes(Object routingBridge);

	@Message(id = ID_OFFSET + 77,
			value = "Incorrect routing bridge implementation: routing bridge '%1$s' did not define any previous route."
					+ " In the implementation of RoutingBridge.previousRoutes(...),"
					+ " define at least one previous route by calling 'routes.addRoute()' at least once,"
					+ " or explicitly indicate no prior indexing was performed by calling 'routes.notIndexed()'.")
	SearchException noPreviousRoute(Object routingBridge);

	@Message(id = ID_OFFSET + 78,
			value = "No readable property named '%2$s' on type '%1$s'.")
	SearchException cannotFindReadableProperty(@FormatWith(PojoTypeModelFormatter.class) PojoRawTypeModel<?> typeModel,
			String propertyName);

	@Message(id = ID_OFFSET + 79, value = "Exception while retrieving property type model for '%1$s' on '%2$s'.")
	SearchException errorRetrievingPropertyTypeModel(String propertyModelName,
			@FormatWith(PojoTypeModelFormatter.class) PojoRawTypeModel<?> parentTypeModel, @Cause Exception cause);

	@Message(id = ID_OFFSET + 80,
			value = "Unable to infer index field type for value bridge '%1$s':"
					+ " this bridge implements ValueBridge<V, F>,"
					+ " but sets the generic type parameter F to '%2$s'."
					+ " The index field type can only be inferred automatically"
					+ " when this type parameter is set to a raw class."
					+ " Use a ValueBinder to set the index field type explicitly,"
					+ " or set the type parameter F to a definite, raw type.")
	SearchException invalidGenericParameterToInferFieldType(Object bridge, @FormatWith(TypeFormatter.class) Type type);

	@Message(id = ID_OFFSET + 81,
			value = "Unable to infer expected identifier type for identifier bridge '%1$s':"
					+ " this bridge implements IdentifierBridge<I>,"
					+ " but sets the generic type parameter I to '%2$s'."
					+ " The expected identifier type can only be inferred automatically"
					+ " when this type parameter is set to a raw class."
					+ " Use an IdentifierBinder to set the expected identifier type explicitly,"
					+ " or set the type parameter I to a definite, raw type.")
	SearchException invalidGenericParameterToInferIdentifierType(Object bridge, @FormatWith(TypeFormatter.class) Type type);

	@Message(id = ID_OFFSET + 82,
			value = "Unable to infer expected value type for value bridge '%1$s':"
					+ " this bridge implements ValueBridge<V, F>,"
					+ " but sets the generic type parameter V to '%2$s'."
					+ " The expected value type can only be inferred automatically"
					+ " when this type parameter is set to a raw class."
					+ " Use a ValueBinder to set the expected value type explicitly,"
					+ " or set the type parameter V to a definite, raw type.")
	SearchException invalidGenericParameterToInferValueType(Object bridge, @FormatWith(TypeFormatter.class) Type type);

	@Message(id = ID_OFFSET + 83,
			value = "Exception while building document for entity '%1$s': %2$s")
	SearchException errorBuildingDocument(Object entityReference,
			String message, @Cause Exception e);

	@Message(id = ID_OFFSET + 84,
			value = "Exception while resolving other entities to reindex as a result of changes on entity '%1$s': %2$s")
	SearchException errorResolvingEntitiesToReindex(Object entityReference,
			String message, @Cause Exception e);
}
