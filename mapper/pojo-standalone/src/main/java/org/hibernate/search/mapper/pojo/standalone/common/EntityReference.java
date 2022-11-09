/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.standalone.common;

import org.hibernate.search.mapper.pojo.standalone.mapping.StandalonePojoMappingConfigurationContext;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * A reference to an indexed entity.
 */
@Incubating
public interface EntityReference {

	/**
	 * @return The type of the referenced entity.
	 */
	Class<?> type();

	/**
	 * @return The name of the referenced entity in the Hibernate Search mapping.
	 * @see StandalonePojoMappingConfigurationContext#addEntityType(Class, String)
	 */
	String name();

	/**
	 * @return The identifier of the referenced entity,
	 * i.e. the value of the property marked as {@code @DocumentId}.
	 */
	Object id();

}
