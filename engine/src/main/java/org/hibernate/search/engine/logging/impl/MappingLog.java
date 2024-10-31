/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.logging.impl;

import static org.hibernate.search.engine.logging.impl.EngineLog.ID_OFFSET;

import java.lang.invoke.MethodHandles;
import java.util.Set;

import org.hibernate.search.engine.mapper.model.spi.MappingElement;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.CategorizedLogger;
import org.hibernate.search.util.common.logging.impl.EventContextNoPrefixFormatter;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.logging.impl.MessageConstants;
import org.hibernate.search.util.common.reporting.EventContext;

import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.FormatWith;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.Param;

@CategorizedLogger(
		category = MappingLog.CATEGORY_NAME
)
@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
public interface MappingLog {
	String CATEGORY_NAME = "org.hibernate.search.mapping";

	MappingLog INSTANCE = LoggerFactory.make( MappingLog.class, CATEGORY_NAME, MethodHandles.lookup() );

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

	@Message(id = ID_OFFSET + 46, value = "Cyclic recursion starting from '%1$s' on %2$s."
			+ " Index field path starting from that location and ending with a cycle: '%3$s'."
			+ " A type cannot declare an unrestricted @IndexedEmbedded to itself, even indirectly."
			+ " To break the cycle, you should consider adding filters to your @IndexedEmbedded: includePaths, includeDepth, excludePaths, ...")
	SearchException indexedEmbeddedCyclicRecursion(MappingElement indexedEmbedded,
			@FormatWith(EventContextNoPrefixFormatter.class) EventContext indexedEmbeddedLocation,
			String cyclicRecursionPath);

	@Message(id = ID_OFFSET + 70,
			value = "Invalid index field template name '%1$s': field template names cannot be null or empty.")
	SearchException fieldTemplateNameCannotBeNullOrEmpty(String templateName, @Param EventContext context);

	@Message(id = ID_OFFSET + 71,
			value = "Invalid index field template name '%1$s': field template names cannot contain a dot ('.').")
	SearchException fieldTemplateNameCannotContainDot(String relativeFieldName,
			@Param EventContext context);

	@Message(id = ID_OFFSET + 88,
			value = "Multiple entity types mapped to index '%1$s': '%2$s', '%3$s'."
					+ " Each indexed type must be mapped to its own, dedicated index.")
	SearchException twoTypesTargetSameIndex(String indexName, String mappedTypeName, String anotherMappedTypeName);

	@Message(id = ID_OFFSET + 93,
			value = "Named predicate name '%1$s' is invalid: field names cannot be null or empty.")
	SearchException relativeNamedPredicateNameCannotBeNullOrEmpty(String relativeNamedPredicateName,
			@Param EventContext context);

	@Message(id = ID_OFFSET + 94,
			value = "Named predicate name '%1$s' is invalid: field names cannot contain a dot ('.')."
					+ " Remove the dot from your named predicate name.")
	SearchException relativeNamedPredicateNameCannotContainDot(String relativeNamedPredicateName,
			@Param EventContext context);

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

	@Message(id = ID_OFFSET + 115,
			value = "Unable to resolve field '%1$s': %2$s")
	SearchException unableToResolveField(String absolutePath, String causeMessage, @Cause SearchException e,
			@Param EventContext context);

	@Message(id = ID_OFFSET + 119,
			value = "'includePaths' and 'excludePaths' cannot be used together in the same filter. "
					+ "Use either `includePaths` or `excludePaths` leaving the other one empty. "
					+ "Included paths are: '%1$s', excluded paths are: '%2$s'.")
	SearchException cannotIncludeAndExcludePathsWithinSameFilter(Set<String> includePaths,
			Set<String> excludePaths);
}
