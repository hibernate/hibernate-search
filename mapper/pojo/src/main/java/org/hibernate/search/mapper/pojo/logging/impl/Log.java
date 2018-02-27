/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.mapper.pojo.logging.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import org.hibernate.search.mapper.pojo.bridge.FunctionBridge;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.util.SearchException;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

@MessageLogger(projectCode = "HSEARCH-POJO")
public interface Log extends BasicLogger {

	@Message(id = 1, value = "Unable to find a default identifier bridge implementation for type '%1$s'")
	SearchException unableToResolveDefaultIdentifierBridgeFromSourceType(Class<?> sourceType);

	@Message(id = 2, value = "Unable to find a default function bridge implementation for type '%1$s'")
	SearchException unableToResolveDefaultFunctionBridgeFromSourceType(Class<?> sourceType);

	@Message(id = 3, value = "Annotation type '%1$s' is annotated with @PropertyBridgeMapping,"
			+ " but the bridge builder reference is empty.")
	SearchException missingBuilderReferenceInBridgeMapping(Class<? extends Annotation> annotationType);

	@Message(id = 4, value = "Annotation type '%1$s' is annotated with @MarkerMapping,"
			+ " but the marker builder reference is empty.")
	SearchException missingBuilderReferenceInMarkerMapping(Class<? extends Annotation> annotationType);

	@Message(id = 5, value = "Annotation @Field on property '%1$s' defines both functionBridge and functionBridgeBuilder."
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

	@Message(id = 8, value = "Could not auto-detect the input type for function bridge %1$s"
			+ "; make sure the bridge uses generics.")
	SearchException unableToInferFunctionBridgeInputType(FunctionBridge<?, ?> bridge);

	@Message(id = 9, value = "Could not auto-detect the return type for function bridge %1$s"
			+ "; make sure the bridge uses generics or configure the field explicitly in the bridge's bind() method.")
	SearchException unableToInferFunctionBridgeIndexFieldType(FunctionBridge<?, ?> bridge);

	@Message(id = 10, value = "Function bridge %1$s cannot be applied to input type %2$s.")
	SearchException invalidInputTypeForFunctionBridge(FunctionBridge<?, ?> bridge, PojoTypeModel<?> typeModel);

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
}
