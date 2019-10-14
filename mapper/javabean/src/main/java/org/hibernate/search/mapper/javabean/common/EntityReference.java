/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.common;

/**
 * A reference to an indexed entity.
 */
public interface EntityReference {

	/**
	 * @return The type of the referenced entity.
	 */
	Class<?> getType();

	/**
	 * @return The name of the referenced entity in the Hibernate Search mapping.
	 * @see org.hibernate.search.mapper.javabean.mapping.SearchMappingBuilder#addEntityType(Class, String)
	 */
	String getName();

	/**
	 * @return The identifier of the referenced entity,
	 * i.e. the value of the property marked as {@code @DocumentId}.
	 */
	Object getId();

}
