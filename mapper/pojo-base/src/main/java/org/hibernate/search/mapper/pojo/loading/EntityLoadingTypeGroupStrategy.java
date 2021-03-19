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
public interface EntityLoadingTypeGroupStrategy {

	/**
	 * The value returned after the type comparison determines how the entity types are grouped. *
	 */
	enum GroupingType {
		/**
		 * Entities will not be grouped.
		 */
		NONE,
		/**
		 * The first type is the parent type of the second type.
		 * The loader of the first type will be selected as the target loader,
		 * and the loader of the second type will be set next.
		 */
		SUPER,
		/**
		 * The second type is the parent type of the first type.
		 * The first type will be set included.
		 * A loader of the second type will be selected as the target loader.
		 */
		INCLUDED
	}

	/**
	 * Compare the types of entities and define the method of grouping.
	 * @param entityName1 the name of the first entity.
	 * @param entityType1 the type of the first entity.
	 * @param entityName2 the name of the second entity.
	 * @param entityType2 the type of the second entity.
	 * @return e grouping type
	 * @see GroupingType
	 */
	GroupingType copare(String entityName1, Class<?> entityType1, String entityName2, Class<?> entityType2);

	/**
	 * Static comparator setting grouping by the java type hierarchy.
	 * @return a {@link EntityLoadingTypeGroupStrategy}
	 */
	static EntityLoadingTypeGroupStrategy byJavaTypeHierarchy() {

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

	/**
	 * A static comparator that always returns no grouping.
	 * @return a {@link EntityLoadingTypeGroupStrategy}
	 */
	static EntityLoadingTypeGroupStrategy none() {
		return (entityName1, entityType1, entityName2, entityType2) -> GroupingType.NONE;
	}
}
