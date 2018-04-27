/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.mapper.orm.logging.impl;

import java.util.Collection;
import java.util.Set;

import org.hibernate.property.access.spi.Getter;
import org.hibernate.search.mapper.orm.cfg.SearchOrmSettings;
import org.hibernate.search.util.SearchException;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

@MessageLogger(projectCode = "HSEARCH-ORM")
public interface Log extends BasicLogger {

	@Message(id = 1, value = "Hibernate Search was not initialized.")
	SearchException hibernateSearchNotInitialized();

	@Message(id = 2, value = "Unexpected entity type for a query hit: %1$s. Expected one of %2$s.")
	SearchException unexpectedSearchHitType(Class<?> entityType, Collection<? extends Class<?>> expectedTypes);

	@Message(id = 3, value = "Unknown indexing mode: %1$s")
	SearchException unknownIndexingMode(String indexingMode);

	@Message(id = 4, value = "Could not retrieve metadata for type %1$s, property '%2$s' accessed through getter '%3$s'")
	SearchException unknownPropertyForGetter(Class<?> entityType, String propertyName, Getter getter);

	@LogMessage(level = Logger.Level.INFO)
	@Message(id = 5, value = "Configuration property tracking is disabled; unused properties will not be logged.")
	void configurationPropertyTrackingDisabled();

	@LogMessage(level = Logger.Level.WARN)
	@Message(id = 6, value = "Some properties in the Hibernate Search configuration were not used;"
			+ " there might be misspelled property keys in your configuration. Unused properties were: %1$s."
			+ " To disable this warning, set '"
			+ SearchOrmSettings.ENABLE_CONFIGURATION_PROPERTY_TRACKING + "' to false.")
	void configurationPropertyTrackingUnusedProperties(Set<String> propertyKeys);
}
