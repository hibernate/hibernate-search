/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.definition.spi;

import java.util.List;

import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.definition.ProjectionDefinitionContext;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.annotation.Incubating;
import org.hibernate.search.util.common.spi.ToStringTreeAppender;

@Incubating
public abstract class DistanceProjectionDefinition<F> extends AbstractProjectionDefinition<F> {

	protected final String fieldPath;
	protected final String parameterName;
	protected final DistanceUnit unit;

	private DistanceProjectionDefinition(String fieldPath, String parameterName, DistanceUnit unit) {
		this.fieldPath = fieldPath;
		this.parameterName = parameterName;
		this.unit = unit;
	}

	@Override
	protected String type() {
		return "distance";
	}

	@Override
	public void appendTo(ToStringTreeAppender appender) {
		super.appendTo( appender );
		appender.attribute( "fieldPath", fieldPath )
				.attribute( "multi", multi() )
				.attribute( "parameterName", parameterName );
	}

	protected abstract boolean multi();

	@Incubating
	public static final class SingleValued extends DistanceProjectionDefinition<Double> {
		public SingleValued(String fieldPath, String parameterName, DistanceUnit unit) {
			super( fieldPath, parameterName, unit );
		}

		@Override
		protected boolean multi() {
			return false;
		}

		@Override
		public SearchProjection<Double> create(SearchProjectionFactory<?, ?> factory, ProjectionDefinitionContext context) {
			return factory.withParameters( params -> factory
					.distance( fieldPath, params.get( parameterName, GeoPoint.class ) )
					.unit( unit )
			).toProjection();
		}
	}

	@Incubating
	public static final class MultiValued extends DistanceProjectionDefinition<List<Double>> {
		public MultiValued(String fieldPath, String parameterName, DistanceUnit unit) {
			super( fieldPath, parameterName, unit );
		}

		@Override
		protected boolean multi() {
			return true;
		}

		@Override
		public SearchProjection<List<Double>> create(SearchProjectionFactory<?, ?> factory,
				ProjectionDefinitionContext context) {
			return factory.withParameters( params -> factory
					.distance( fieldPath, params.get( parameterName, GeoPoint.class ) )
					.multi()
					.unit( unit )
			).toProjection();
		}
	}
}
