/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.common.spi.SearchIndexIdentifierContext;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.CompositeProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.DocumentReferenceProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.EntityProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.EntityReferenceProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.IdProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ScoreProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilderFactory;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.StubSearchIndexScope;

public class StubSearchProjectionBuilderFactory implements SearchProjectionBuilderFactory {

	private final StubSearchIndexScope scope;

	public StubSearchProjectionBuilderFactory(StubSearchIndexScope scope) {
		this.scope = scope;
	}

	@Override
	public DocumentReferenceProjectionBuilder documentReference() {
		return new DocumentReferenceProjectionBuilder() {
			@Override
			public SearchProjection<DocumentReference> build() {
				return StubDefaultProjection.get();
			}
		};
	}

	@Override
	public <E> EntityProjectionBuilder<E> entity() {
		return StubEntityProjection::get;
	}

	@Override
	public <R> EntityReferenceProjectionBuilder<R> entityReference() {
		return StubReferenceProjection::get;
	}

	@Override
	public <I> IdProjectionBuilder<I> id(Class<I> identifierType) {
		SearchIndexIdentifierContext identifier = scope.identifier();
		return new StubIdProjection.Builder<>(
				identifier.projectionConverter().withConvertedType( identifierType, identifier ) );
	}

	@Override
	public ScoreProjectionBuilder score() {
		return new ScoreProjectionBuilder() {
			@Override
			public SearchProjection<Float> build() {
				return StubDefaultProjection.get();
			}
		};
	}

	@Override
	public CompositeProjectionBuilder composite() {
		return new StubCompositeProjection.Builder();
	}

}
