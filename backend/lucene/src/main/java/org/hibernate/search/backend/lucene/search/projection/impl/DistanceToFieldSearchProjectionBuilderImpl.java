/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.DistanceToFieldSearchProjectionBuilder;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoPoint;

public class DistanceToFieldSearchProjectionBuilderImpl implements DistanceToFieldSearchProjectionBuilder {

	private final String absoluteFieldPath;

	private final GeoPoint center;

	private DistanceUnit unit = DistanceUnit.METERS;

	public DistanceToFieldSearchProjectionBuilderImpl(String absoluteFieldPath, GeoPoint center) {
		this.absoluteFieldPath = absoluteFieldPath;
		this.center = center;
	}

	@Override
	public DistanceToFieldSearchProjectionBuilder unit(DistanceUnit unit) {
		this.unit = unit;
		return this;
	}

	@Override
	public SearchProjection<Double> build() {
		return new DistanceToFieldSearchProjectionImpl( absoluteFieldPath, center, unit );
	}
}
