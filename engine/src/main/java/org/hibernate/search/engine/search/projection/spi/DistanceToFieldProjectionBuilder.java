/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.spi;

import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.spatial.DistanceUnit;

public interface DistanceToFieldProjectionBuilder extends SearchProjectionBuilder<Double> {

	DistanceToFieldProjectionBuilder unit(DistanceUnit unit);

	@Override
	default SearchProjection<Double> build() {
		return build( SingleValuedProjectionAccumulator.provider() );
	}

	<P> SearchProjection<P> build(ProjectionAccumulator.Provider<Double, P> accumulatorProvider);

}
