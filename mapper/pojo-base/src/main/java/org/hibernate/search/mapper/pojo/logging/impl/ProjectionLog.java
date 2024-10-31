/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.logging.impl;

import static org.hibernate.search.mapper.pojo.logging.impl.PojoMapperLog.ID_OFFSET;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.mapper.model.spi.MappingElement;
import org.hibernate.search.mapper.pojo.logging.spi.PojoTypeModelFormatter;
import org.hibernate.search.mapper.pojo.model.path.spi.ProjectionConstructorPath;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.mapper.pojo.search.definition.impl.ConstructorProjectionApplicationException;
import org.hibernate.search.mapper.pojo.search.definition.impl.PojoConstructorProjectionDefinition;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.CategorizedLogger;
import org.hibernate.search.util.common.logging.impl.ClassFormatter;
import org.hibernate.search.util.common.logging.impl.EventContextNoPrefixFormatter;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.logging.impl.MessageConstants;
import org.hibernate.search.util.common.logging.impl.ToStringTreeMultilineFormatter;
import org.hibernate.search.util.common.reporting.EventContext;

import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.FormatWith;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.Param;

@CategorizedLogger(
		category = ProjectionLog.CATEGORY_NAME
)
@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
public interface ProjectionLog {
	String CATEGORY_NAME = "org.hibernate.search.mapper.projection";

	ProjectionLog INSTANCE = LoggerFactory.make( ProjectionLog.class, CATEGORY_NAME, MethodHandles.lookup() );


	@Message(id = ID_OFFSET + 112,
			value = "Invalid object class for projection: %1$s."
					+ " Make sure that this class is mapped correctly,"
					+ " either through annotations (@ProjectionConstructor) or programmatic mapping."
					+ " If it is, make sure the class is included in a Jandex index made available to Hibernate Search.")
	SearchException invalidObjectClassForProjection(@FormatWith(ClassFormatter.class) Class<?> objectClass);

	@Message(id = ID_OFFSET + 113,
			value = "Invalid declaring type for projection constructor: type '%1$s' is abstract."
					+ " Projection constructors can only be declared on concrete types.")
	SearchException invalidAbstractTypeForProjectionConstructor(
			@FormatWith(PojoTypeModelFormatter.class) PojoRawTypeModel<?> typeModel);

	@Message(id = ID_OFFSET + 114,
			value = "Missing parameter names in Java metadata for projection constructor."
					+ " When inferring inner projections from constructor parameters, constructor parameter names must be known."
					+ " Either make sure this class was compiled with the '-parameters' compiler flag,"
					+ " or set the path explicitly with '@FieldProjection(path = ...)'"
					+ " or '@ObjectProjection(path = ...)'.")
	SearchException missingParameterNameForInferredProjection();

	@Message(id = ID_OFFSET + 115,
			value = "Invalid parameter type for projection constructor: %1$s."
					+ " When inferring the cardinality of inner projections from constructor parameters,"
					+ " multi-valued constructor parameters must be lists/sets (java.util.List<...>/java.util.Set<...>/java.util.SortedSet<...>)"
					+ ", their supertypes (java.lang.Iterable<...>, java.util.Collection<...>)"
					+ " or arrays")
	SearchException invalidMultiValuedParameterTypeForProjectionConstructor(
			@FormatWith(PojoTypeModelFormatter.class) PojoTypeModel<?> parentTypeModel);

	@Message(id = ID_OFFSET + 116,
			value = "Multiple projection constructor are mapped for type '%1$s'."
					+ " At most one projection constructor is allowed for each type.")
	SearchException multipleProjectionConstructorsForType(Class<?> instantiatedJavaClass);

	@LogMessage(level = Logger.Level.DEBUG)
	@Message(id = ID_OFFSET + 117,
			value = "Constructor projection for type '%1$s': %2$s")
	void constructorProjection(@FormatWith(PojoTypeModelFormatter.class) PojoRawTypeModel<?> typeModel,
			@FormatWith(ToStringTreeMultilineFormatter.class) PojoConstructorProjectionDefinition<?> projectionDefinition);

	@Message(id = ID_OFFSET + 118, value = "Cyclic recursion starting from '%1$s' on %2$s."
			+ " Index field path starting from that location and ending with a cycle: '%3$s'."
			+ " A projection constructor cannot declare an unrestricted @ObjectProjection to itself, even indirectly."
			+ " To break the cycle, you should consider adding filters to your @ObjectProjection: includePaths, includeDepth, excludePaths, ...")
	SearchException objectProjectionCyclicRecursion(MappingElement objectProjection,
			@FormatWith(EventContextNoPrefixFormatter.class) EventContext objectProjectionLocation,
			String cyclicRecursionIndexFieldPath);

	@Message(id = ID_OFFSET + 123,
			value = "Could not apply projection constructor: %1$s")
	ConstructorProjectionApplicationException errorApplyingProjectionConstructor(
			String causeMessage,
			@Cause Exception cause,
			@Param ProjectionConstructorPath path);

	@Message(id = ID_OFFSET + 134,
			value = "Multiple projections are mapped for this parameter."
					+ " At most one projection is allowed for each parameter.")
	SearchException multipleProjectionMappingsForParameter();

	@Message(id = ID_OFFSET + 135,
			value = "Incorrect binder implementation: binder '%1$s' did not call context.definition(...).")
	SearchException missingProjectionDefinitionForBinder(Object binder);

	@Message(id = ID_OFFSET + 136,
			value = "Invalid projection definition for constructor parameter type '%2$s': '%1$s'. This projection results in values of type '%3$s'.")
	SearchException invalidOutputTypeForProjectionDefinition(Object definition,
			@FormatWith(PojoTypeModelFormatter.class) PojoTypeModel<?> typeModel,
			@FormatWith(PojoTypeModelFormatter.class) PojoTypeModel<?> expectedValueModel);

	@Message(id = ID_OFFSET + 137,
			value = "Invalid multi-valued projection definition for constructor parameter type '%2$s': '%1$s'. This projection results in values of type '%3$s'.")
	SearchException invalidOutputTypeForMultiValuedProjectionDefinition(Object definition,
			@FormatWith(PojoTypeModelFormatter.class) PojoTypeModel<?> typeModel,
			@FormatWith(PojoTypeModelFormatter.class) PojoTypeModel<?> expectedValueModel);

	@Message(id = ID_OFFSET + 138,
			value = "Missing parameter names in Java metadata for projection constructor."
					+ " When mapping a projection constructor parameter to a field projection without providing a field path,"
					+ " constructor parameter names must be known."
					+ " Either make sure this class was compiled with the '-parameters' compiler flag,"
					+ " or set the path explicitly with '@FieldProjection(path = ...)'.")
	SearchException missingParameterNameForFieldProjectionInProjectionConstructor();

	@Message(id = ID_OFFSET + 139,
			value = "Missing parameter names in Java metadata for projection constructor."
					+ " When mapping a projection constructor parameter to an object projection without providing a field path,"
					+ " constructor parameter names must be known."
					+ " Either make sure this class was compiled with the '-parameters' compiler flag,"
					+ " or set the path explicitly with '@ObjectProjection(path = ...)'.")
	SearchException missingParameterNameForObjectProjectionInProjectionConstructor();

	@Message(id = ID_OFFSET + 140,
			value = "Missing parameter names in Java metadata for projection constructor."
					+ " When mapping a projection constructor parameter to a highlight projection without providing a field path,"
					+ " constructor parameter names must be known."
					+ " Either make sure this class was compiled with the '-parameters' compiler flag,"
					+ " or set the path explicitly with '@HighlightProjection(path = ...)'.")
	SearchException missingParameterNameForHighlightProjectionInProjectionConstructor();

	@Message(id = ID_OFFSET + 141,
			value = "Invalid constructor parameter type: '%1$s'. The highlight projection results in values of type '%2$s'.")
	SearchException invalidParameterTypeForHighlightProjectionInProjectionConstructor(
			@FormatWith(ClassFormatter.class) Class<?> rawClass, String expectedClass);

	@Message(id = ID_OFFSET + 159,
			value = "Invalid constructor parameter type: '%1$s'. The distance projection results in values of type '%2$s'.")
	SearchException invalidParameterTypeForDistanceProjectionInProjectionConstructor(
			@FormatWith(ClassFormatter.class) Class<?> rawClass, String expectedClass);

	@Message(id = ID_OFFSET + 171,
			value = "Implicit binding of a java.util.SortedSet<%1$s> constructor parameter is not possible since %1$s is not implementing java.lang.Comparable."
					+ " Either make %1$s implement java.lang.Comparable or create a custom @ProjectionBinding and use the ProjectionCollector.sortedSet(comparator) collector provider in it.")
	SearchException cannotBindSortedSetWithNonComparableElements(@FormatWith(ClassFormatter.class) Class<?> elementType,
			@Param EventContext eventContext);

}
