/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.session;

import org.hibernate.search.engine.backend.index.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.index.DocumentRefreshStrategy;

public interface SearchSessionBuilder {

	/**
	 * @param tenantId The tenant ID to use when performing index-related operatiosn (indexing, searching, ...)
	 * in the resulting session.
	 * @return {@code this} for method chaining.
	 */
	SearchSessionBuilder tenantId(String tenantId);

	/**
	 * @param commitStrategy The commit strategy for indexing works added to the {@link SearchSession#getMainWorkPlan() main work plan}.
	 * @return {@code this} for method chaining.
	 */
	SearchSessionBuilder commitStrategy(DocumentCommitStrategy commitStrategy);

	/**
	 * @param refreshStrategy The refresh strategy for indexing works added to the {@link SearchSession#getMainWorkPlan() main work plan}.
	 * @return {@code this} for method chaining.
	 */
	SearchSessionBuilder refreshStrategy(DocumentRefreshStrategy refreshStrategy);

	/**
	 * @return The resulting session.
	 */
	SearchSession build();

}
