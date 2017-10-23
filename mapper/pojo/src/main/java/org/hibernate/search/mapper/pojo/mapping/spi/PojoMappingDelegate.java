/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.spi;

import org.hibernate.search.mapper.pojo.mapping.ChangesetPojoWorker;
import org.hibernate.search.mapper.pojo.mapping.PojoMapping;
import org.hibernate.search.mapper.pojo.mapping.StreamPojoWorker;

public interface PojoMappingDelegate extends PojoMapping, AutoCloseable {

	@Override
	void close();

	ChangesetPojoWorker createWorker(PojoSessionContext sessionContext);

	StreamPojoWorker createStreamWorker(PojoSessionContext sessionContext);

	PojoSearchTarget createPojoSearchTarget(Class<?>... targetedTypes);

}
