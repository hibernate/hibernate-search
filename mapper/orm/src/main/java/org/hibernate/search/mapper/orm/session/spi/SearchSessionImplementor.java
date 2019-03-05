/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.session.spi;

import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.work.spi.PojoSessionWorkExecutor;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkPlan;

public interface SearchSessionImplementor extends AutoCloseable, SearchSession {

	@Override
	void close();

	/**
	 * @return A new work plan for this session, maintaining its state (list of works) independently from the session.
	 * Calling {@link PojoWorkPlan#execute()} is required to actually execute works,
	 * the session will <strong>not</strong> do it automatically upon closing.
	 */
	PojoWorkPlan createWorkPlan();

	PojoSessionWorkExecutor createSessionWorkExecutor();

}
