/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.query.spi;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.util.common.annotation.Incubating;

@Incubating
@FunctionalInterface
public interface QueryParametersValueProvider<T> {

	T provide(QueryParametersContext context);

	static <F> QueryParametersValueProvider<F> simple(F value) {
		return new SimpleValueProvider<>( value );
	}

	static <F> QueryParametersValueProvider<F> parameter(String parameterName, Class<F> parameterType) {
		return new ParameterValueProvider<>( parameterName, parameterType, Function.identity() );
	}

	static <F, R> QueryParametersValueProvider<F> parameter(String parameterName, Class<? extends R> parameterType,
			Function<? super R, ? extends F> mapping) {
		return new ParameterValueProvider<>( parameterName, parameterType, mapping );
	}

	static <F> QueryParametersValueProvider<F> parameterCollectionOrSingle(String parameterName,
			Function<Collection<?>, ? extends F> mapping) {
		return new ParameterCollectionOrSingleValueProvider<>( parameterName, mapping );
	}

	static QueryParametersValueProvider<Double> distanceInMeters(String radius, String unit) {
		return new DistanceInMetersProvider( radius, unit );
	}

	class SimpleValueProvider<F> implements QueryParametersValueProvider<F> {
		private final F value;

		public SimpleValueProvider(F value) {
			this.value = value;
		}

		@Override
		public F provide(QueryParametersContext context) {
			return value;
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + "{" + "value=" + value + '}';
		}
	}

	class ParameterValueProvider<F, R> implements QueryParametersValueProvider<F> {
		private final String parameterName;
		private final Class<? extends R> parameterType;
		private final Function<? super R, ? extends F> mapping;

		public ParameterValueProvider(String parameterName, Class<? extends R> parameterType,
				Function<? super R, ? extends F> mapping) {
			this.parameterName = parameterName;
			this.parameterType = parameterType;
			this.mapping = mapping;
		}

		@Override
		public F provide(QueryParametersContext context) {
			return mapping.apply( context.parameter( parameterName, parameterType ) );
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + "{"
					+ "parameterName='" + parameterName
					+ "', parameterType=" + parameterType + '}';
		}
	}

	class DistanceInMetersProvider implements QueryParametersValueProvider<Double> {

		private final String radius;
		private final String unit;

		public DistanceInMetersProvider(String radius, String unit) {
			this.radius = radius;
			this.unit = unit;
		}

		@Override
		public Double provide(QueryParametersContext context) {
			DistanceUnit unitValue = context.parameter( unit, DistanceUnit.class );
			Double radiusValue = context.parameter( radius, Double.class );
			return unitValue.toMeters( radiusValue );
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + "{" +
					"radius='" + radius + '\'' +
					", unit='" + unit + '\'' +
					'}';
		}
	}

	class ParameterCollectionOrSingleValueProvider<F> implements QueryParametersValueProvider<F> {
		private final String parameterName;
		private final Function<Collection<?>, ? extends F> mapping;

		public ParameterCollectionOrSingleValueProvider(String parameterName, Function<Collection<?>, ? extends F> mapping) {
			this.parameterName = parameterName;
			this.mapping = mapping;
		}

		@Override
		public F provide(QueryParametersContext context) {
			Object parameter = context.parameter( parameterName );
			return mapping.apply( parameter instanceof Collection ? (Collection<?>) parameter : List.of( parameter ) );
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + "{" +
					"parameterName='" + parameterName + '\'' +
					'}';
		}
	}
}
