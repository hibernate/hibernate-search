/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.mapping.programmatic;

import org.hibernate.search.engine.environment.bean.BeanResolver;

public interface MarkerBuildContext {

	/**
	 * @return A bean provider, allowing the retrieval of beans,
	 * including CDI/Spring DI beans when in the appropriate environment.
	 */
	BeanResolver getBeanResolver();

}
