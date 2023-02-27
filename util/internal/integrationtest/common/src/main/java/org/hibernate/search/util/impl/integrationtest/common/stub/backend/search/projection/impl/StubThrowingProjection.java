/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl;

import java.util.Iterator;
import java.util.function.Supplier;

import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.util.common.SearchException;

class StubThrowingProjection<T> extends StubSearchProjection<T> {

	private final Supplier<SearchException> exceptionSupplier;

	StubThrowingProjection(Supplier<SearchException> exceptionSupplier) {
		this.exceptionSupplier = exceptionSupplier;
	}

	@Override
	public Object extract(ProjectionHitMapper<?> projectionHitMapper, Iterator<?> projectionFromIndex,
			StubSearchProjectionContext context) {
		throw exceptionSupplier.get();
	}

	@Override
	public T transform(LoadingResult<?> loadingResult, Object extractedData,
			StubSearchProjectionContext context) {
		throw exceptionSupplier.get();
	}

	@Override
	protected String typeName() {
		return "throwing";
	}

	@Override
	public void toNode(StubProjectionNode.Builder self) {
		// Nothing to do
	}
}
