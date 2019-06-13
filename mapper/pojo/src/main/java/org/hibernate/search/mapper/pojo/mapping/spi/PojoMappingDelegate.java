/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.spi;

import org.hibernate.search.mapper.pojo.work.spi.PojoWorkPlan;
import org.hibernate.search.mapper.pojo.session.context.spi.AbstractPojoSessionContextImplementor;
import org.hibernate.search.mapper.pojo.session.spi.PojoSearchSessionDelegate;

public interface PojoMappingDelegate extends AutoCloseable {

	@Override
	void close();

	PojoSearchSessionDelegate createSearchSessionDelegate(AbstractPojoSessionContextImplementor sessionContext);

	/**
	 * @param type A Java type.
	 * @return The metadata for the given type if this type can be the subject of a work (i.e. it can be passed to
	 * {@link PojoWorkPlan#add(Object)} for instance), {@code null} otherwise.
	 */
	PojoMappingTypeMetadata getMappingTypeMetadata(Class<?> type);

}
