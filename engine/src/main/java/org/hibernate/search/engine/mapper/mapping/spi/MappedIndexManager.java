/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.spi;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.engine.backend.index.spi.IndexDocumentWorkExecutor;
import org.hibernate.search.engine.backend.index.spi.IndexWorkPlan;
import org.hibernate.search.engine.mapper.mapping.context.spi.MappingContextImplementor;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.engine.search.DocumentReference;

/**
 * The object responsible for applying works and searches to a full-text index.
 * <p>
 * This is the interface provided to mappers to access the index manager.
 */
public interface MappedIndexManager<D extends DocumentElement> {

	IndexManager toAPI();

	IndexWorkPlan<D> createWorkPlan(SessionContextImplementor sessionContext);

	IndexDocumentWorkExecutor<D> createDocumentWorkExecutor(SessionContextImplementor sessionContext);

	CompletableFuture<?> optimize();

	CompletableFuture<?> purge();

	CompletableFuture<?> flush();

	<R, O> MappedIndexSearchTargetBuilder<R, O> createSearchTargetBuilder(MappingContextImplementor mappingContext,
			Function<DocumentReference, R> documentReferenceTransformer);

	void addToSearchTarget(MappedIndexSearchTargetBuilder<?, ?> builder);
}
