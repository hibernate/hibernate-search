/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping;

public interface PojoMapping {

	/**
	 * @param type a Java type
	 * @return {@code true} if this type can be the subject of a work (i.e. it can be passed to
	 * {@link PojoWorkPlan#add(Object)} for instance), {@code false} if it cannot.
	 * Workable types include both indexable types and contained entity types.
	 */
	boolean isWorkable(Class<?> type);

	/**
	 * @param type a Java type
	 * @return {@code true} if this type is indexable, {@code false} if it is not.
	 */
	boolean isIndexable(Class<?> type);

	/**
	 * @param type a Java type
	 * @return {@code true} if this type is searchable (i.e. it can be passed to {@link PojoSearchManager#search(Class)}),
	 * {@code false} if it is not.
	 */
	boolean isSearchable(Class<?> type);

}
