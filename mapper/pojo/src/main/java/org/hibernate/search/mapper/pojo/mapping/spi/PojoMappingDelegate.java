/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.spi;

import java.util.Collection;
import java.util.Set;

import org.hibernate.search.mapper.pojo.work.spi.PojoWorkPlan;
import org.hibernate.search.mapper.pojo.session.context.spi.AbstractPojoSessionContextImplementor;
import org.hibernate.search.mapper.pojo.session.spi.PojoSearchSessionDelegate;

public interface PojoMappingDelegate extends AutoCloseable {

	@Override
	void close();

	PojoSearchSessionDelegate createSearchSessionDelegate(AbstractPojoSessionContextImplementor sessionContext);

	/**
	 * @param type A Java type.
	 * @return {@code true} if this type can be the subject of a work (i.e. it can be passed to
	 * {@link PojoWorkPlan#add(Object)} for instance), {@code false} if it cannot.
	 * Workable types include both indexable types and contained entity types.
	 */
	boolean isWorkable(Class<?> type);

	/**
	 * @param type A Java type.
	 * @return {@code true} if this type is indexable, {@code false} if it is not.
	 */
	boolean isIndexable(Class<?> type);

	/**
	 * @param type A Java type.
	 * @return {@code true} if this type is searchable
	 * (i.e. it can be passed to {@link PojoSearchSessionDelegate#createPojoScope(Collection)}),
	 * {@code false} if it is not.
	 */
	boolean isSearchable(Class<?> type);

	/**
	 * Given a target entity type, return the set of configured subtypes that are indexed.
	 *
	 * @param entityType A {@link Class} representing the target entity type.
	 * @param <E> The target entity type.
	 * @return The set of configured subtypes that are indexed.
	 */
	<E> Set<Class<? extends E>> getIndexedTypesPolymorphic(Class<E> entityType);

}
