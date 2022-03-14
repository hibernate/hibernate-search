/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.projection;

import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.GeoPointFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.IndexableGeoPointWithDistanceFromCenterValues;

public class DistanceProjectionTestValues extends AbstractProjectionTestValues<GeoPoint, Double> {
	protected DistanceProjectionTestValues() {
		super( GeoPointFieldTypeDescriptor.INSTANCE );
	}

	@Override
	public GeoPoint fieldValue(int ordinal) {
		return IndexableGeoPointWithDistanceFromCenterValues.INSTANCE.getSingle().get( ordinal );
	}

	@Override
	public Double projectedValue(int ordinal) {
		return IndexableGeoPointWithDistanceFromCenterValues.INSTANCE
				.getSingleDistancesFromCenterPoint1().get( ordinal );
	}

	public GeoPoint projectionCenterPoint() {
		return IndexableGeoPointWithDistanceFromCenterValues.CENTER_POINT_1;
	}
}
