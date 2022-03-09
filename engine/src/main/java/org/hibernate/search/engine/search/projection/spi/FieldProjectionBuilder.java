/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.spi;

import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.projection.SearchProjection;

public interface FieldProjectionBuilder<T> extends SearchProjectionBuilder<T> {

	interface TypeSelector {
		<T> FieldProjectionBuilder<T> type(Class<T> expectedType, ValueConvert convert);
	}

	@Override
	default SearchProjection<T> build() {
		return build( ProjectionAccumulator.single() );
	}

	<P> SearchProjection<P> build(ProjectionAccumulator.Provider<T, P> accumulatorProvider);

}
