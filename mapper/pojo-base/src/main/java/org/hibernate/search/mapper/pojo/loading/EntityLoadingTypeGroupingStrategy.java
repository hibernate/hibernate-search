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
public interface EntityLoadingTypeGroupingStrategy {

	/**
	 * The value returned after the type comparison determines how the entity types are grouped.
	 */
	enum GroupingType {
		/**
		 * Entity types will not be grouped.
		 */
		NONE,
		/**
		 * The first type is the parent type of the second type.
		 * <p>
		 * The loader of the first type will be selected as the target loader,
		 * and the second type will be marked as included in the group.
		 */
		SUPER,
		/**
		 * The second type is the parent type of the first type.
		 * <p>
		 * The loader of the second type will be selected as the target loader,
		 * and the first type will be marked as included in the group.
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
	GroupingType get(String entityName1, Class<?> entityType1, String entityName2, Class<?> entityType2);

	/**
	 * A strategy that groups entity types according to the java type hierarchy.
	 * @return a {@link EntityLoadingTypeGroupingStrategy}
	 */
	static EntityLoadingTypeGroupingStrategy byJavaTypeHierarchy() {
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
	 * A strategy that never groups entity types.
	 * @return a {@link EntityLoadingTypeGroupingStrategy}
	 */
	static EntityLoadingTypeGroupingStrategy none() {
		return (entityName1, entityType1, entityName2, entityType2) -> GroupingType.NONE;
	}
}
