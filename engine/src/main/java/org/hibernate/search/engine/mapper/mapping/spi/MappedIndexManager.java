/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.spi;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.engine.backend.index.spi.IndexDocumentWorkExecutor;
import org.hibernate.search.engine.backend.index.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.index.spi.IndexWorkExecutor;
import org.hibernate.search.engine.backend.index.spi.IndexWorkPlan;
import org.hibernate.search.engine.mapper.mapping.context.spi.MappingContextImplementor;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;

/**
 * The object responsible for applying works and searches to a full-text index.
 * <p>
 * This is the interface provided to mappers to access the index manager.
 */
public interface MappedIndexManager<D extends DocumentElement> {

	IndexManager toAPI();

	IndexWorkPlan<D> createWorkPlan(SessionContextImplementor sessionContext, DocumentRefreshStrategy refreshStrategy);

	IndexDocumentWorkExecutor<D> createDocumentWorkExecutor(SessionContextImplementor sessionContext);

	IndexWorkExecutor createWorkExecutor();

	<R, E> MappedIndexSearchScopeBuilder<R, E> createSearchScopeBuilder(MappingContextImplementor mappingContext);

	void addTo(MappedIndexSearchScopeBuilder<?, ?> builder);
}
