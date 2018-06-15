/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.mapper.pojo.logging.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.extractor.ContainerValueExtractor;
import org.hibernate.search.mapper.pojo.mapping.impl.PojoContainedTypeManager;
import org.hibernate.search.mapper.pojo.mapping.impl.PojoIndexedTypeManager;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoGenericTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.util.impl.common.logging.ToStringTreeAppendableMultilineFormatter;
import org.hibernate.search.util.SearchException;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.FormatWith;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

@MessageLogger(projectCode = "HSEARCH-POJO")
public interface Log extends BasicLogger {

	@Message(id = 1, value = "Unable to find a default identifier bridge implementation for type '%1$s'")
	SearchException unableToResolveDefaultIdentifierBridgeFromSourceType(Class<?> sourceType);

	@Message(id = 2, value = "Unable to find a default value bridge implementation for type '%1$s'")
	SearchException unableToResolveDefaultValueBridgeFromSourceType(Class<?> sourceType);

	@Message(id = 3, value = "Annotation type '%2$s' is annotated with '%1$s',"
			+ " but neither a bridge reference nor a bridge builder reference was provided.")
	SearchException missingBridgeReferenceInBridgeMapping(Class<? extends Annotation> metaAnnotationType,
			Class<? extends Annotation> annotationType);

	@Message(id = 4, value = "Annotation type '%1$s' is annotated with @MarkerMapping,"
			+ " but the marker builder reference is empty.")
	SearchException missingBuilderReferenceInMarkerMapping(Class<? extends Annotation> annotationType);

	@Message(id = 5, value = "Annotation @Field on property '%1$s' defines both valueBridge and valueBridgeBuilder."
			+ " Only one of those can be defined, not both."
	)
	SearchException invalidFieldDefiningBothBridgeReferenceAndBridgeBuilderReference(String property);

	@Message(id = 6, value = "Annotation @DocumentId on property '%1$s' defines both identifierBridge and identifierBridgeBuilder."
			+ " Only one of those can be defined, not both."
	)
	SearchException invalidDocumentIdDefiningBothBridgeReferenceAndBridgeBuilderReference(String property);

	@Message(id = 7, value = "Cannot query on an empty target."
			+ " If you want to target all indexes, put Object.class in the collection of target types,"
			+ " or use the method of the same name, but without Class<?> parameters."
	)
	SearchException cannotSearchOnEmptyTarget();

	@Message(id = 8, value = "Could not auto-detect the input type for value bridge %1$s"
			+ "; make sure the bridge uses generics.")
	SearchException unableToInferValueBridgeInputType(ValueBridge<?, ?> bridge);

	@Message(id = 9, value = "Could not auto-detect the return type for value bridge %1$s"
			+ "; make sure the bridge uses generics or configure the field explicitly in the bridge's bind() method.")
	SearchException unableToInferValueBridgeIndexFieldType(ValueBridge<?, ?> bridge);

	@Message(id = 10, value = "Value bridge %1$s cannot be applied to input type %2$s.")
	SearchException invalidInputTypeForValueBridge(ValueBridge<?, ?> bridge, PojoTypeModel<?> typeModel);

	@Message(id = 11, value = "Missing field name for GeoPointBridge on type %1$s."
			+ " The field name is mandatory when the bridge is applied on an type, optional when applied on a property.")
	SearchException missingFieldNameForGeoPointBridgeOnType(String typeName);

	@Message(id = 12, value = "Requested type argument %3$s to type %2$s"
			+ " in implementing type %1$s, but %2$s doesn't declare any type parameter")
	SearchException cannotRequestTypeParameterOfUnparameterizedType(Type type, Class<?> rawSuperType,
			int typeArgumentIndex);

	@Message(id = 13, value = "Requested type argument %3$s to type %2$s"
			+ " in implementing type %1$s, but %2$s only declares %4$s type parameter(s)")
	SearchException typeParameterIndexOutOfBound(Type type, Class<?> rawSuperType, int typeArgumentIndex,
			int typeParametersLength);

	@Message(id = 14, value = "Requested type argument index %3$s to type %2$s"
			+ " in implementing type %1$s should be 0 or greater")
	SearchException invalidTypeParameterIndex(Type type, Class<?> rawSuperType, int typeArgumentIndex);

	@Message(id = 15, value = "Could not interpret the type arguments to the ContainerValueExtractor interface in "
			+ " implementation '%1$s'. Only the following implementations of ContainerValueExtractor are valid: "
			+ " 1) implementations setting both type parameters to *raw* types,"
			+ " e.g. class MyExtractor implements ContainerValueExtractor<MyBean, String>;"
			+ " 2) implementations setting the first type parameter to an array of an unbounded type variable,"
			+ " and setting the second parameter to the same type variable,"
			+ " e.g. MyExtractor<T> implements ContainerValueExtractor<T[], T>"
			+ " 3) implementations setting the first type parameter to a parameterized type"
			+ " with one argument set to an unbounded type variable and the other to unbounded wildcards,"
			+ " and setting the second type parameter to the same type variable,"
			+ " e.g. MyExtractor<T> implements ContainerValueExtractor<MyParameterizedBean<?, T, ?>, T>")
	SearchException couldNotInferContainerValueExtractorClassTypePattern(Class<?> extractorClass);

	@Message(id = 16, value = "Could not apply the requested container value extractor '%1$s' to type '%2$s'")
	SearchException invalidContainerValueExtractorForType(Class<? extends ContainerValueExtractor> extractorClass,
			PojoGenericTypeModel<?> extractedType);

	@LogMessage(level = Logger.Level.DEBUG)
	@Message(id = 17, value = "Created POJO indexed type manager: %1$s")
	void createdPojoIndexedTypeManager(
			@FormatWith(ToStringTreeAppendableMultilineFormatter.class) PojoIndexedTypeManager<?, ?, ?> typeManager);

	@LogMessage(level = Logger.Level.DEBUG)
	@Message(id = 18, value = "Detected entity types: %1$s")
	void detectedEntityTypes(Set<PojoRawTypeModel<?>> entityTypes);

	@LogMessage(level = Logger.Level.DEBUG)
	@Message(id = 19, value = "Created POJO contained type manager: %1$s")
	void createdPojoContainedTypeManager(
			@FormatWith(ToStringTreeAppendableMultilineFormatter.class) PojoContainedTypeManager<?> typeManager);

	@Message(id = 20, value = "Could not find the inverse side of the association '%3$s' from type '%2$s' on type '%1$s'")
	SearchException cannotInvertAssociation(PojoRawTypeModel<?> inverseSideTypeModel, PojoRawTypeModel<?> typeModel,
			PojoModelPathValueNode associationPath);

	@Message(id = 21, value = "Could not apply the path of the inverse association '%2$s' to type '%1$s'."
			+ " Association on the original side (which was inverted) was '%4$s' on type '%3$s'."
			+ " Error was: '%5$s'")
	SearchException cannotApplyInvertAssociationPath(
			PojoRawTypeModel<?> inverseSideTypeModel, PojoModelPathValueNode inverseSideAssociationPath,
			PojoRawTypeModel<?> originalSideTypeModel, PojoModelPathValueNode originalSideAssociationPath,
			String errorMessage,
			@Cause Exception cause);

	@Message(id = 22, value = "The inverse association targets type '%1$s',"
			+ " but a supertype or subtype of '%2$s' was expected.")
	SearchException incorrectTargetTypeForInverseAssociation(PojoRawTypeModel<?> inverseAssociationTargetType,
			PojoRawTypeModel<?> entityType);

	@Message(id = 23, value = "Property '%2$s' from type '%1$s' is annotated with @AnnotationInverseSide,"
			+ " but the inverse path is empty.")
	SearchException missingInversePathInAssociationInverseSideMapping(PojoRawTypeModel<?> typeModel, String propertyName);

	@Message(id = 24, value = "Found an infinite embedded recursion involving path '%2$s' on type '%1$s'")
	SearchException infiniteRecursionForAssociationEmbeddeds(PojoRawTypeModel<?> typeModel,
			PojoModelPathValueNode path);

	@LogMessage(level = Logger.Level.INFO)
	@Message(id = 25, value = "Cannot access the value of containing annotation '%1$s'."
			+ " Ignoring annotation.")
	void cannotAccessRepeateableContainingAnnotationValue(Class<?> containingAnnotationType, @Cause Throwable e);

	@Message(id = 26, value = "Annotation type '%2$s' is annotated with '%1$s',"
			+ " but both a bridge reference and a bridge builder reference were provided."
			+ " Only one can be provided.")
	SearchException conflictingBridgeReferenceInBridgeMapping(Class<? extends Annotation> metaAnnotationType,
			Class<? extends Annotation> annotationType);

	@Message(id = 27, value = "Type '%1$s' is not marked as an entity type, yet it is indexed or targeted"
			+ " by an association from an indexed type. Please check your configuration.")
	SearchException missingEntityTypeMetadata(PojoRawTypeModel<?> typeModel);

	@Message(id = 28, value = "There isn't any explicit document ID mapping for indexed type '%1$s',"
			+ " and the entity ID cannot be used as a default because it is unknown.")
	SearchException missingIdentifierMapping(PojoRawTypeModel<?> typeModel);
}
