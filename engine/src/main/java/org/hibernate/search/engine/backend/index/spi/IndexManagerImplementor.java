/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.index.spi;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.engine.mapper.mapping.context.spi.MappingContextImplementor;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;

/**
 * The object responsible for applying works and searches to a full-text index.
 * <p>
 * This is the interface implemented by backends and provided to the engine.
 */
public interface IndexManagerImplementor<D extends DocumentElement> extends AutoCloseable {

	/**
	 * @return The object that should be exposed as API to users.
	 */
	IndexManager toAPI();

	IndexWorkPlan<D> createWorkPlan(SessionContextImplementor sessionContext);

	IndexSearchTargetBuilder createSearchTargetBuilder(MappingContextImplementor mappingContext);

	void addToSearchTarget(IndexSearchTargetBuilder searchTargetBuilder);

	@Override
	void close();

}
