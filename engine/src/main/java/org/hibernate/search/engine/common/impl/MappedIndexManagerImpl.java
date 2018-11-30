/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.impl;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.engine.backend.index.spi.IndexManagerImplementor;
import org.hibernate.search.engine.backend.index.spi.IndexWorkExecutor;
import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexSearchTargetBuilder;
import org.hibernate.search.engine.backend.index.spi.IndexWorkPlan;
import org.hibernate.search.engine.mapper.mapping.context.spi.MappingContextImplementor;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexManager;
import org.hibernate.search.engine.search.DocumentReference;

class MappedIndexManagerImpl<D extends DocumentElement> implements MappedIndexManager<D> {

	private final IndexManagerImplementor<D> implementor;

	MappedIndexManagerImpl(IndexManagerImplementor<D> implementor) {
		this.implementor = implementor;
	}

	@Override
	public IndexManager toAPI() {
		return implementor.toAPI();
	}

	@Override
	public IndexWorkPlan<D> createWorkPlan(SessionContextImplementor sessionContext) {
		return implementor.createWorkPlan( sessionContext );
	}

	@Override
	public IndexWorkExecutor<D> createWorkExecutor(SessionContextImplementor sessionContext) {
		return implementor.createWorkExecutor( sessionContext );
	}

	@Override
	public CompletableFuture<?> optimize() {
		return implementor.optimize();
	}

	@Override
	public CompletableFuture<?> purge() {
		return implementor.purge();
	}

	@Override
	public CompletableFuture<?> flush() {
		return implementor.flush();
	}

	@Override
	public <R, O> MappedIndexSearchTargetBuilder<R, O> createSearchTargetBuilder(MappingContextImplementor mappingContext,
			Function<DocumentReference, R> documentReferenceTransformer) {
		return new MappedIndexSearchTargetBuilderImpl<>(
				implementor, mappingContext,
				documentReferenceTransformer
		);
	}

	@Override
	public void addToSearchTarget(MappedIndexSearchTargetBuilder<?, ?> builder) {
		((MappedIndexSearchTargetBuilderImpl<?, ?>) builder).add( implementor );
	}
}
