/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.loading;

/**
 * A type group for grouping types during mass indexing.
 */
public interface EntityLoadingTypeGroup {

	enum GroupingType {
		NONE, SUPER, INCLUDED
	}

	GroupingType copare(String entityName1, Class<?> entityType1, String entityName2, Class<?> entityType2);

	static EntityLoadingTypeGroup asIstanceOf() {

		return (entityName1, entityType1, entityName2, entityType2) -> {

			if ( entityType1.isAssignableFrom( entityType2 ) ) {
				return GroupingType.SUPER;
			}
			if ( entityType2.isAssignableFrom( entityType1 ) ) {
				return GroupingType.INCLUDED;
			}
			return GroupingType.NONE;
		};
	}

	static EntityLoadingTypeGroup asNone() {
		return (entityName1, entityType1, entityName2, entityType2) -> GroupingType.NONE;
	}
}
