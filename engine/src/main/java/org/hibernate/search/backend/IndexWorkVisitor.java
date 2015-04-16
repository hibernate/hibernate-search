/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend;

import org.hibernate.search.backend.spi.DeleteByQueryLuceneWork;

/**
 * Contract for visitors of {@link LuceneWork} types.
 * <p>
 * Implementations can receive a specific input parameter and return a specific output type. Use {@link Void} as
 * parameterization for each of the two not needed.
 *
 * @author Sanne Grinovero
 * @author Gunnar Morling
 * @param <P> Context parameter type expected by a specific visitor
 * @param <R> Return type provided by a specific visitor
 */
public interface IndexWorkVisitor<P, R> {

	R visitAddWork(AddLuceneWork work, P p);

	R visitDeleteWork(DeleteLuceneWork work, P p);

	R visitOptimizeWork(OptimizeLuceneWork work, P p);

	R visitPurgeAllWork(PurgeAllLuceneWork work, P p);

	R visitUpdateWork(UpdateLuceneWork work, P p);

	R visitFlushWork(FlushLuceneWork work, P p);

	R visitDeleteByQueryWork(DeleteByQueryLuceneWork work, P p);
}
