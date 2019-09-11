/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.util.Set;

import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.DistanceToFieldProjectionBuilder;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoPoint;

public class LuceneDistanceToFieldProjectionBuilder implements DistanceToFieldProjectionBuilder {

	private final Set<String> indexNames;
	private final String absoluteFieldPath;
	private final String nestedDocumentPath;

	private final GeoPoint center;

	private DistanceUnit unit = DistanceUnit.METERS;

	public LuceneDistanceToFieldProjectionBuilder(Set<String> indexNames, String absoluteFieldPath, String nestedDocumentPath, GeoPoint center) {
		this.indexNames = indexNames;
		this.absoluteFieldPath = absoluteFieldPath;
		this.nestedDocumentPath = nestedDocumentPath;
		this.center = center;
	}

	@Override
	public DistanceToFieldProjectionBuilder unit(DistanceUnit unit) {
		this.unit = unit;
		return this;
	}

	@Override
	public SearchProjection<Double> build() {
		return new LuceneDistanceToFieldProjection( indexNames, absoluteFieldPath, nestedDocumentPath, center, unit );
	}
}
