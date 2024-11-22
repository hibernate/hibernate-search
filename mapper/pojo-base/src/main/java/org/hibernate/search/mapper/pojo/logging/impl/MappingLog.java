/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.logging.impl;

import static org.hibernate.search.mapper.pojo.logging.impl.PojoMapperLog.ID_OFFSET;
import static org.hibernate.search.mapper.pojo.logging.impl.PojoMapperLog.ID_OFFSET_LEGACY_ENGINE;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeOptionsStep;
import org.hibernate.search.engine.logging.spi.MappableTypeModelFormatter;
import org.hibernate.search.engine.mapper.model.spi.MappingElement;
import org.hibernate.search.mapper.pojo.automaticindexing.building.impl.DerivedDependencyWalkingInfo;
import org.hibernate.search.mapper.pojo.extractor.ContainerExtractor;
import org.hibernate.search.mapper.pojo.logging.spi.PojoConstructorModelFormatter;
import org.hibernate.search.mapper.pojo.logging.spi.PojoModelPathFormatter;
import org.hibernate.search.mapper.pojo.logging.spi.PojoTypeModelFormatter;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.impl.PojoContainedTypeManager;
import org.hibernate.search.mapper.pojo.mapping.impl.PojoIndexedTypeManager;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoConstructorModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.data.impl.LinkedNode;
import org.hibernate.search.util.common.logging.CategorizedLogger;
import org.hibernate.search.util.common.logging.impl.ClassFormatter;
import org.hibernate.search.util.common.logging.impl.CommaSeparatedClassesFormatter;
import org.hibernate.search.util.common.logging.impl.EventContextFormatter;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.logging.impl.MessageConstants;
import org.hibernate.search.util.common.logging.impl.ToStringTreeMultilineFormatter;
import org.hibernate.search.util.common.logging.impl.TypeFormatter;
import org.hibernate.search.util.common.reporting.EventContext;

import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.FormatWith;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.Param;

@CategorizedLogger(
		category = MappingLog.CATEGORY_NAME,
		description = """
				Logs related to creating Hibernate Search mapping.
				"""
)
@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
public interface MappingLog {
	String CATEGORY_NAME = "org.hibernate.search.mapping.mapper";

	MappingLog INSTANCE = LoggerFactory.make( MappingLog.class, CATEGORY_NAME, MethodHandles.lookup() );

	// -----------------------------------
	// Pre-existing messages from Search 5 (engine module)
	// DO NOT ADD ANY NEW MESSAGES HERE
	// -----------------------------------
	@Message(id = ID_OFFSET + 126,
			value = "Target path '%1$s' already exists and is not an empty directory. Use a path to an empty or non-existing directory.")
	SearchException schemaExporterTargetIsNotEmptyDirectory(Path targetDirectory);

	@Message(id = ID_OFFSET + 127, value = "Unable to export the schema: %1$s")
	SearchException unableToExportSchema(String cause, @Cause Exception e, @Param EventContext context);

	@Message(id = ID_OFFSET_LEGACY_ENGINE + 135,
			value = "No default value bridge implementation for type '%1$s'."
					+ " Use a custom bridge.")
	SearchException unableToResolveDefaultValueBridgeFromSourceType(
			@FormatWith(PojoTypeModelFormatter.class) PojoTypeModel<?> sourceType);

	// see also MigrationHelperLog.unableToFindLongitudeOrLatitudeProperty(...)
	@Message(id = ID_OFFSET_LEGACY_ENGINE + 159,
			value = "No property annotated with %1$s(markerSet = \"%3$s\")."
					+ " There must be exactly one such property in order to map it to geo-point field '%2$s'.")
	SearchException unableToFindLongitudeOrLatitudeProperty(String annotation, String fieldName, String markerSet);

	// see also MigrationHelperLog.multipleLatitudeOrLongitudeProperties(...)
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
			value = "%1$s defines includePaths filters that do not match anything."
					+ " Non-matching includePaths filters: %2$s."
					+ " Encountered field paths: %3$s."
					+ " Check the filters for typos, or remove them if they are not useful."
	)
	SearchException uselessIncludePathFilters(MappingElement mappingElement,
			Set<String> nonMatchingIncludePaths, Set<String> encounteredFieldPaths,
			@Param EventContext eventContext);

	@Message(id = ID_OFFSET_LEGACY_ENGINE + 221,
			value = "Infinite embedded recursion involving path '%2$s' on type '%1$s'")
	SearchException infiniteRecursionForAssociationEmbeddeds(
			@FormatWith(PojoTypeModelFormatter.class) PojoRawTypeModel<?> typeModel,
			@FormatWith(PojoModelPathFormatter.class) PojoModelPathValueNode path);

	@Message(id = ID_OFFSET_LEGACY_ENGINE + 234,
			value = "No matching indexed entity types for types %1$s"
					+ " These types are not indexed entity types, nor is any of their subtypes."
					+ " Valid indexed entity classes, superclasses and superinterfaces are: %2$s."
	)
	SearchException invalidIndexedSuperTypes(Collection<PojoRawTypeIdentifier<?>> nonIndexedTypes,
			Collection<PojoRawTypeIdentifier<?>> validSuperTypes);

	@Message(id = ID_OFFSET_LEGACY_ENGINE + 337, value = "Conflicting usage of @Param annotation for parameter name:" +
			" '%1$s'. Can't assign both value '%2$s' and '%3$s'")
	SearchException conflictingParameterDefined(String name, Object value1, Object value2);

	// -----------------------------------
	// New messages from Search 6 onwards
	// -----------------------------------
	@Message(id = ID_OFFSET + 1,
			value = "No default identifier bridge implementation for type '%1$s'." +
					" Implement a custom bridge and assign it to the identifier property with @DocumentId(identifierBridge = ...)."
					+
					" See the reference documentation for more information about bridges.")
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
			@FormatWith(PojoTypeModelFormatter.class) PojoTypeModel<?> extractedType);

	@LogMessage(level = Logger.Level.DEBUG)
	@Message(id = ID_OFFSET + 17,
			value = "Type manager for indexed type '%1$s': %2$s")
	void indexedTypeManager(
			@FormatWith(PojoTypeModelFormatter.class) PojoRawTypeModel<?> typeModel,
			@FormatWith(ToStringTreeMultilineFormatter.class) PojoIndexedTypeManager<?, ?> typeManager);

	@LogMessage(level = Logger.Level.DEBUG)
	@Message(id = ID_OFFSET + 18,
			value = "Detected entity types: %1$s, indexed types: %2$s, initial mapped types: %3$s.")
	void detectedMappedTypes(Set<PojoRawTypeModel<?>> entityTypes, Set<PojoRawTypeModel<?>> indexedTypes,
			Set<PojoRawTypeModel<?>> initialMappedTypes);

	@LogMessage(level = Logger.Level.DEBUG)
	@Message(id = ID_OFFSET + 19,
			value = "Type manager for contained type '%1$s': %2$s")
	void containedTypeManager(
			@FormatWith(PojoTypeModelFormatter.class) PojoRawTypeModel<?> typeModel,
			@FormatWith(ToStringTreeMultilineFormatter.class) PojoContainedTypeManager<?, ?> typeManager);

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
					+ " there is a cyclic dependency starting from type '%1$s'.\n"
					+ "Derivation chain starting from that type and ending with a cycle:%2$s\n"
					+ " A derived property cannot be marked as derived from itself, even indirectly through other "
					+ " derived properties."
					+ " If your model actually contains such cyclic dependency, "
					+ " you should consider disabling automatic reindexing, at least partially "
					+ " using @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.NO) on one of the properties in the cycle."
	)
	SearchException infiniteRecursionForDerivedFrom(
			@FormatWith(PojoTypeModelFormatter.class) PojoRawTypeModel<?> typeModel,
			@FormatWith(ToStringTreeMultilineFormatter.class) LinkedNode<DerivedDependencyWalkingInfo> cycle);

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
			value = "No matching entity type for type identifier '%1$s'."
					+ " Either this type is not an entity type, or the entity type is not mapped in Hibernate Search."
					+ " Valid identifiers for mapped entity types are: %2$s")
	SearchException unknownTypeIdentifierForMappedEntityType(PojoRawTypeIdentifier<?> invalidTypeId,
			Collection<PojoRawTypeIdentifier<?>> validTypeIds);

	@Message(id = ID_OFFSET + 39, value = "'%1$s' cannot be assigned to '%2$s'")
	SearchException incompatibleRequestedType(@FormatWith(PojoModelPathFormatter.class) PojoModelPathValueNode accessor,
			@FormatWith(ClassFormatter.class) Class<?> requestedType);

	@Message(id = ID_OFFSET + 40,
			value = "No matching indexed entity type for type identifier '%1$s'."
					+ " Either this type is not an entity type, or the entity type is not indexed in Hibernate Search."
					+ " Valid identifiers for indexed entity types are: %2$s")
	SearchException unknownTypeIdentifierForIndexedEntityType(PojoRawTypeIdentifier<?> invalidTypeId,
			Collection<PojoRawTypeIdentifier<?>> validTypeIds);

	@Message(id = ID_OFFSET + 41,
			value = "Invalid reference to default extractors:"
					+ " a chain of multiple container extractors must not include the default extractors."
					+ " Either use only the default extractors, or explicitly reference every single extractor to be applied."
	)
	SearchException cannotUseDefaultExtractorsInMultiExtractorChain();

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

	@Message(id = ID_OFFSET + 58,
			value = "Incorrect binder implementation: binder '%1$s' did not call context.bridge(...).")
	SearchException missingBridgeForBinder(Object binder);

	@Message(id = ID_OFFSET + 59,
			value = "Incorrect binder implementation: binder '%1$s' did not call context.marker(...).")
	SearchException missingMarkerForBinder(Object binder);

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

	@LogMessage(level = Logger.Level.WARN)
	@Message(id = ID_OFFSET + 85,
			value = "Multiple getters exist for property named '%2$s' on type '%1$s'."
					+ " Hibernate Search will use '%3$s' and ignore %4$s."
					+ " The selected getter may change from one startup to the next."
					+ " To get rid of this warning, either remove the extra getters"
					+ " or configure the access type for this property to 'FIELD'.")
	void arbitraryMemberSelection(@FormatWith(PojoTypeModelFormatter.class) PojoRawTypeModel<?> typeModel,
			String propertyName, Member selectedMember, List<Member> otherMembers);

	@Message(id = ID_OFFSET + 89,
			value = "No matching entity type for name '%1$s'."
					+ " Either this is not the name of an entity type, or the entity type is not mapped in Hibernate Search."
					+ " Valid names for mapped entity types are: %2$s")
	SearchException unknownEntityNameForMappedEntityType(String invalidName, Collection<String> validNames);

	@Message(id = ID_OFFSET + 110, value = "Exception while retrieving parameter type model for parameter #%1$s of '%2$s'.")
	SearchException errorRetrievingConstructorParameterTypeModel(int parameterIndex,
			PojoConstructorModel<?> constructorModel, @Cause Exception cause);

	@Message(id = ID_OFFSET + 105, value = "Cannot work with the identifier of entities of type '%1$s':"
			+ " identifier mapping (@DocumentId, ...) is not configured for this type.")
	SearchException cannotWorkWithIdentifierBecauseUnconfiguredIdentifierMapping(PojoRawTypeIdentifier<?> typeIdentifier);

	@Message(id = ID_OFFSET + 107,
			value = "No main constructor for type '%1$s': this type does not declare exactly one constructor.")
	SearchException cannotFindMainConstructorNotExactlyOneConstructor(
			@FormatWith(PojoTypeModelFormatter.class) PojoRawTypeModel<?> typeModel);

	@Message(id = ID_OFFSET + 109,
			value = "No constructor with parameter types %2$s on type '%1$s'. Available constructors: %3$s")
	SearchException cannotFindConstructorWithParameterTypes(
			@FormatWith(PojoTypeModelFormatter.class) PojoRawTypeModel<?> typeModel,
			@FormatWith(CommaSeparatedClassesFormatter.class) Class<?>[] parameterTypes,
			Collection<? extends PojoConstructorModel<?>> constructors);

	@Message(id = ID_OFFSET + 111, value = "Exception while retrieving constructor handle for '%1$s' on '%2$s'.")
	SearchException errorRetrievingConstructorHandle(Constructor<?> constructor,
			@FormatWith(PojoTypeModelFormatter.class) PojoRawTypeModel<?> parentTypeModel, @Cause Exception cause);

	@Message(id = ID_OFFSET + 121,
			value = "Invalid ObjectPath encountered '%1$s': %2$s")
	SearchException invalidObjectPath(ObjectPath path, String causeMessage, @Cause Exception cause);

	@Message(id = ID_OFFSET + 122,
			value = "An unexpected failure occurred while configuring resolution of association inverse side for reindexing."
					+ " This may lead to incomplete reindexing and thus out-of-sync indexes."
					+ " The exception is being ignored to preserve backwards compatibility with earlier versions of Hibernate Search."
					+ " Failure: %3$s"
					+ " %2$s" // Context
					+ " Association inverse side: %1$s.")
	SearchException failedToCreateImplicitReindexingAssociationInverseSideResolverNode(
			Map<PojoRawTypeModel<?>, PojoModelPathValueNode> inversePathByInverseType,
			@FormatWith(EventContextFormatter.class) EventContext context,
			String causeMessage, @Cause Exception cause);

	@Message(id = ID_OFFSET + 130,
			value = "No matching entity type for class '%1$s'."
					+ " Neither this class nor any of its subclasses is mapped in Hibernate Search."
					+ " Note interfaces are not considered superclasses and are not permitted here."
					+ " Valid classes are: %2$s")
	SearchException unknownClassForNonInterfaceSuperType(@FormatWith(ClassFormatter.class) Class<?> invalidClass,
			@FormatWith(CommaSeparatedClassesFormatter.class) Collection<Class<?>> validClasses);

	@Message(id = ID_OFFSET + 131,
			value = "No matching entity type for name '%1$s'."
					+ " This is not the name of an entity type in Hibernate Search."
					+ " Valid entity names are: %2$s")
	SearchException unknownEntityName(String invalidName, Collection<String> validNames);

	@Message(id = ID_OFFSET + 132,
			value = "No matching entity type for type '%1$s'."
					+ " Neither this type nor any of its subclasses is mapped in Hibernate Search."
					+ " Note interfaces are not considered superclasses and are not permitted here."
					+ " Valid types are: %2$s")
	SearchException unknownNonInterfaceSuperTypeIdentifier(PojoRawTypeIdentifier<?> typeIdentifier,
			Set<PojoRawTypeIdentifier<?>> availableTypeIdentifiers);

	@Message(id = ID_OFFSET + 133,
			value = "No parameter at index '%2$s' for constructor '%1$s'.")
	SearchException cannotFindConstructorParameter(
			@FormatWith(PojoConstructorModelFormatter.class) PojoConstructorModel<?> constructorModel, int index);

	@Message(id = ID_OFFSET + 142,
			value = "%1$s defines excludePaths filters that do not match anything."
					+ " Non-matching excludePaths filters: %2$s."
					+ " Encountered field paths: %3$s."
					+ " Check the filters for typos, or remove them if they are not useful."
	)
	SearchException uselessExcludePathFilters(MappingElement mappingElement,
			Set<String> nonMatchingExcludePaths, Set<String> encounteredFieldPaths,
			@Param EventContext eventContext);

	@Message(id = ID_OFFSET + 143, value = "Cyclic recursion when applying the default container extractors to type '%1$s'."
			+ " Container extractors applied to that type and resulting in the same type: %2$s."
			+ " To break the cycle, you should consider configuring container extraction explicitly,"
			+ " possibly disabling it for this part of your mapping."
			+ " See the reference documentation for more information.")
	SearchException defaultContainerExtractorCyclicRecursion(
			@FormatWith(PojoTypeModelFormatter.class) PojoTypeModel<?> typeModel,
			List<String> extractorNames);

	@Message(id = ID_OFFSET + 144,
			value = "Unable to apply property mapping:"
					+ " this property mapping must target an index field of vector type,"
					+ " but the resolved field type is non-vector."
					+ " This generally means you need to use a different field annotation"
					+ " or to convert property values using a custom ValueBridge or ValueBinder."
					+ " If you are already using a custom ValueBridge or ValueBinder, check its field type."
					+ " Details: encountered type DSL step '%1$s',"
					+ " which does not extend the expected interface '%2$s'.")
	SearchException invalidFieldEncodingForVectorFieldMapping(IndexFieldTypeOptionsStep<?, ?> step,
			@FormatWith(ClassFormatter.class) Class<?> expectedContextType);

	@Message(id = ID_OFFSET + 145, value = "Vector dimension is a required property. "
			+ "Either specify it as an annotation property (@VectorField(dimension = somePositiveInteger)), "
			+ "or define a value binder (@VectorField(valueBinder = @ValueBinderRef(..))) that explicitly declares a vector field specifying the dimension.")
	SearchException vectorDimensionNotSpecified();

	@Message(id = ID_OFFSET + 146, value = "Vector fields require an explicit extraction path being specified, "
			+ "i.e. extraction must be set to DEFAULT and a nonempty array of container value extractor names provided, "
			+ "e.g. @ContainerExtraction(extract = ContainerExtract.DEFAULT, value = { ... }).")
	SearchException vectorFieldMustUseExplicitExtractorPath();

	@Message(id = ID_OFFSET + 147,
			value = "No matching entity type for entity name '%1$s'."
					+ " Either this is not the name of an entity type,"
					+ " or neither the entity type nor any of its subclasses is mapped in Hibernate Search."
					+ " Note interfaces are not considered superclasses and are not permitted here."
					+ " Valid entity names are: %2$s")
	SearchException unknownEntityNameForNonInterfaceSuperType(String invalidEntityName, Collection<String> validEntityNames);

	@Message(id = ID_OFFSET + 148,
			value = "Multiple entity types configured with the same name '%1$s': '%2$s', '%3$s'")
	SearchException multipleEntityTypesWithSameName(String entityName,
			@FormatWith(MappableTypeModelFormatter.class) PojoRawTypeModel<?> previousType,
			@FormatWith(MappableTypeModelFormatter.class) PojoRawTypeModel<?> type);

	@Message(id = ID_OFFSET + 149,
			value = "Multiple secondary entity names assigned to the same type: '%1$s', '%2$s'.")
	SearchException multipleSecondaryEntityNames(String entityName, String otherEntityName);

	@Message(id = ID_OFFSET + 150,
			value = "Multiple entity types configured with the same secondary name '%1$s': '%2$s', '%3$s'")
	SearchException multipleEntityTypesWithSameSecondaryName(String entityName,
			@FormatWith(MappableTypeModelFormatter.class) PojoRawTypeModel<?> previousType,
			@FormatWith(MappableTypeModelFormatter.class) PojoRawTypeModel<?> type);

	@Message(id = ID_OFFSET + 151,
			value = "Invalid type for '%1$s': the entity type must extend '%2$s'," +
					" but entity type '%3$s' does not."
	)
	SearchException invalidEntitySuperType(String entityName,
			@FormatWith(ClassFormatter.class) Class<?> expectedSuperType,
			@FormatWith(ClassFormatter.class) Class<?> actualJavaType);

	@Message(id = ID_OFFSET + 152,
			value = "No matching indexed entity types for entity names %1$s."
					+ " Either these are not the names of entity types,"
					+ " or neither the entity types nor any of their subclasses are indexed in Hibernate Search."
					+ " Valid entity names are: %2$s."
	)
	SearchException invalidIndexedSuperTypeEntityNames(Collection<String> nonIndexedTypes,
			Collection<String> validSuperTypes);

	@Message(id = ID_OFFSET + 153,
			value = "No matching indexed entity types for classes %1$s."
					+ " Neither these classes nor any of their subclasses are indexed in Hibernate Search."
					+ " Valid classes are: %2$s."
	)
	SearchException invalidIndexedSuperTypeClasses(
			@FormatWith(CommaSeparatedClassesFormatter.class) Collection<Class<?>> invalidClasses,
			@FormatWith(CommaSeparatedClassesFormatter.class) Collection<Class<?>> validClasses);

	@Message(id = ID_OFFSET + 154,
			value = "No matching indexed entity type for name '%1$s'."
					+ " Either this is not the name of an entity type, or the entity type is not indexed in Hibernate Search."
					+ " Valid names for indexed entity types are: %2$s")
	SearchException unknownEntityNameForIndexedEntityType(String invalidName, Collection<String> validNames);

	@Message(id = ID_OFFSET + 162,
			value = "Using non-default `valueModel=ValueModel.%1$s` and `convert=ValueConvert.%2$s` at the same time is not allowed. "
					+ "Remove the `convert` attribute and keep only the `valueModel=ValueModel.%1$s`.")
	SearchException usingNonDefaultValueConvertAndValueModelNotAllowed(String model,
			String convert, @Param EventContext eventContext);

	@Message(id = ID_OFFSET + 170, value = "Property name '%1$s' cannot contain dots.")
	IllegalArgumentException propertyNameCannotContainDots(String propertyName);
}
