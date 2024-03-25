/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.util.function.Function;

import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.engine.search.common.NamedValues;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.ProjectionFinalStep;

public class LuceneWithParametersProjection<P> extends AbstractLuceneProjection<P> {

	private final LuceneSearchIndexScope<?> scope;
	private final Function<? super NamedValues,
			? extends ProjectionFinalStep<P>> projectionCreator;

	public LuceneWithParametersProjection(LuceneSearchIndexScope<?> scope,
			Function<? super NamedValues, ? extends ProjectionFinalStep<P>> projectionCreator) {
		super( scope );
		this.scope = scope;
		this.projectionCreator = projectionCreator;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "["
				+ "projectionCreator=" + projectionCreator
				+ "]";
	}

	@Override
	public Extractor<?, P> request(ProjectionRequestContext context) {
		SearchProjection<P> delegate = projectionCreator.apply( context.queryParameters() ).toProjection();

		return LuceneSearchProjection.from( scope, delegate ).request( context );
	}
}
