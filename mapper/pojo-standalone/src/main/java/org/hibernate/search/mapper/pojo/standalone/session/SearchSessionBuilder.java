/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.standalone.session;

import java.util.function.Consumer;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.mapper.pojo.standalone.loading.dsl.SelectionLoadingOptionsStep;
import org.hibernate.search.util.common.annotation.Incubating;

@Incubating
public interface SearchSessionBuilder {

	/**
	 * @param tenantId The tenant ID to use when performing index-related operations (indexing, searching, ...)
	 * in the resulting session.
	 * @return {@code this} for method chaining.
	 */
	SearchSessionBuilder tenantId(String tenantId);

	/**
	 * @param commitStrategy The commit strategy for indexing works added to the {@link SearchSession#indexingPlan() indexing plan}.
	 * @return {@code this} for method chaining.
	 */
	SearchSessionBuilder commitStrategy(DocumentCommitStrategy commitStrategy);

	/**
	 * @param refreshStrategy The refresh strategy for indexing works added to the {@link SearchSession#indexingPlan() indexing plan}.
	 * @return {@code this} for method chaining.
	 */
	SearchSessionBuilder refreshStrategy(DocumentRefreshStrategy refreshStrategy);

	/**
	 * @param loadingOptionsContributor The default loading options.
	 * @return {@code this} for method chaining.
	 */
	SearchSessionBuilder loading(Consumer<SelectionLoadingOptionsStep> loadingOptionsContributor);

	/**
	 * @return The resulting session.
	 */
	SearchSession build();

}
