/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.mapper.orm.logging.impl;

import java.util.Collection;
import java.util.Set;

import org.hibernate.SessionFactory;
import org.hibernate.mapping.Value;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.search.mapper.pojo.extractor.ContainerValueExtractor;
import org.hibernate.search.mapper.pojo.logging.spi.PojoTypeModelFormatter;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPath;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.util.SearchException;
import org.hibernate.search.util.impl.common.MessageConstants;
import org.hibernate.search.util.impl.common.logging.ClassFormatter;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.FormatWith;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;
import org.jboss.logging.annotations.ValidIdRanges;

@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
@ValidIdRanges({
		@ValidIdRange(min = MessageConstants.ORM_ID_RANGE_MIN, max = MessageConstants.ORM_ID_RANGE_MAX)
		// Exceptions for legacy messages from Search 5
		// TODO HSEARCH-3308 add exceptions here for legacy messages from Search 5. See the Lucene logger for examples.
})
public interface Log extends BasicLogger {

	// -----------------------------------
	// Pre-existing messages from Search 5
	// DO NOT ADD ANY NEW MESSAGES HERE
	// -----------------------------------
	int ID_OFFSET_1 = MessageConstants.ENGINE_ID_RANGE_MIN;

	// TODO HSEARCH-3308 migrate relevant messages from Search 5 here

	// -----------------------------------
	// New messages from Search 6 onwards
	// -----------------------------------
	int ID_OFFSET_2 = MessageConstants.ORM_ID_RANGE_MIN;

	@Message(id = ID_OFFSET_2 + 1,
			value = "Hibernate Search was not initialized.")
	SearchException hibernateSearchNotInitialized();

	@Message(id = ID_OFFSET_2 + 2,
			value = "Unexpected entity type for a query hit: %1$s. Expected one of %2$s.")
	SearchException unexpectedSearchHitType(Class<?> entityType, Collection<? extends Class<?>> expectedTypes);

	@Message(id = ID_OFFSET_2 + 3,
			value = "Unknown indexing mode: %1$s")
	SearchException unknownIndexingMode(String indexingMode);

	@Message(id = ID_OFFSET_2 + 4,
			value = "Could not retrieve metadata for type %1$s, property '%2$s' accessed through getter '%3$s'")
	SearchException unknownPropertyForGetter(Class<?> entityType, String propertyName, Getter getter);

	@LogMessage(level = Logger.Level.INFO)
	@Message(id = ID_OFFSET_2 + 5,
			value = "Configuration property tracking is disabled; unused properties will not be logged.")
	void configurationPropertyTrackingDisabled();

	@LogMessage(level = Logger.Level.WARN)
	@Message(id = ID_OFFSET_2 + 6,
			value = "Some properties in the Hibernate Search configuration were not used;"
					+ " there might be misspelled property keys in your configuration. Unused properties were: %1$s."
					+ " To disable this warning, set the property '%2$s' to false.")
	void configurationPropertyTrackingUnusedProperties(Set<String> propertyKeys, String disableWarningKey);

	@Message(id = ID_OFFSET_2 + 7,
			value = "Path '%2$s' on entity type '%1$s' cannot be resolved using Hibernate ORM metadata."
					+ " Please check that this path points to a persisted value.")
	SearchException unknownPathForDirtyChecking(Class<?> entityType, PojoModelPath path, @Cause Exception e);

	@Message(id = ID_OFFSET_2 + 8,
			value = "Path '%2$s' on entity type '%1$s' can be resolved using Hibernate ORM metadata,"
					+ " but points to value '%3$s' that will never be reported as dirty by Hibernate ORM."
					+ " Please check that this path points to a persisted value, and in particular not an embedded property.")
	SearchException unreportedPathForDirtyChecking(Class<?> entityType, PojoModelPath path, Value value);

	@Message(id = ID_OFFSET_2 + 9,
			value = "Container value extractor of type '%2$s' cannot be applied to"
					+ " Hibernate ORM metadata node of type '%1$s'.")
	SearchException invalidContainerValueExtractorForDirtyChecking(Class<?> ormMappingClass,
			Class<? extends ContainerValueExtractor> extractorClass);

	@Message(id = ID_OFFSET_2 + 10,
			value = "Unable to find a readable property '%2$s' on type '%1$s'.")
	SearchException cannotFindReadableProperty(@FormatWith(PojoTypeModelFormatter.class) PojoRawTypeModel<?> typeModel,
			String propertyName);

	@Message(id = ID_OFFSET_2 + 11, value = "Mapping service cannot create a search manager using a different session factory. Expected: '%1$s'. In use: '%2$s'.")
	SearchException usingDifferentSessionFactories(SessionFactory expectedSessionFactory, SessionFactory usedSessionFactory);

	@Message(id = ID_OFFSET_2 + 12, value = "Exception while retrieving property type model for '%1$s' on '%2$s'.")
	SearchException errorRetrievingPropertyTypeModel(String propertyModelName, @FormatWith(PojoTypeModelFormatter.class) PojoRawTypeModel<?> parentTypeModel, @Cause Exception cause);

	@Message(id = ID_OFFSET_2 + 13,
			value = "A Hibernate ORM session context ('%2$s') cannot be unwrapped to '%1$s'.")
	SearchException sessionContextUnwrappingWithUnknownType(@FormatWith(ClassFormatter.class) Class<?> requestedClass,
			@FormatWith(ClassFormatter.class) Class<?> actualClass);
}
