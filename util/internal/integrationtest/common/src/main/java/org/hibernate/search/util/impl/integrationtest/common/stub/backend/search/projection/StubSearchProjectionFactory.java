/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection;

import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.DistanceFieldSearchProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.DocumentReferenceSearchProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.FieldSearchProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ObjectSearchProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ReferenceSearchProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ScoreSearchProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionFactory;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoPoint;

public class StubSearchProjectionFactory implements SearchProjectionFactory<StubSearchProjection<?>> {

	@SuppressWarnings("rawtypes")
	private static final StubSearchProjection PROJECTION = new StubSearchProjection<>();

	@Override
	public DocumentReferenceSearchProjectionBuilder documentReference() {
		return new DocumentReferenceSearchProjectionBuilder() {

			@SuppressWarnings("unchecked")
			@Override
			public SearchProjection<DocumentReference> build() {
				return (SearchProjection<DocumentReference>) PROJECTION;
			}
		};
	}

	@Override
	public <T> FieldSearchProjectionBuilder<T> field(String absoluteFieldPath, Class<T> clazz) {
		return new FieldSearchProjectionBuilder<T>() {

			@SuppressWarnings("unchecked")
			@Override
			public SearchProjection<T> build() {
				return (SearchProjection<T>) PROJECTION;
			}
		};
	}

	@Override
	public ObjectSearchProjectionBuilder object() {
		return new ObjectSearchProjectionBuilder() {

			@SuppressWarnings("unchecked")
			@Override
			public SearchProjection<Object> build() {
				return (SearchProjection<Object>) PROJECTION;
			}
		};
	}

	@Override
	public ReferenceSearchProjectionBuilder reference() {
		return new ReferenceSearchProjectionBuilder() {

			@SuppressWarnings("unchecked")
			@Override
			public SearchProjection<Object> build() {
				return (SearchProjection<Object>) PROJECTION;
			}
		};
	}

	@Override
	public ScoreSearchProjectionBuilder score() {
		return new ScoreSearchProjectionBuilder() {

			@SuppressWarnings("unchecked")
			@Override
			public SearchProjection<Float> build() {
				return (SearchProjection<Float>) PROJECTION;
			}
		};
	}

	@Override
	public DistanceFieldSearchProjectionBuilder distance(String absoluteFieldPath, GeoPoint center) {
		return new DistanceFieldSearchProjectionBuilder() {

			@Override
			public DistanceFieldSearchProjectionBuilder unit(DistanceUnit unit) {
				return this;
			}

			@SuppressWarnings("unchecked")
			@Override
			public SearchProjection<Double> build() {
				return (SearchProjection<Double>) PROJECTION;
			}
		};
	}

	@Override
	public StubSearchProjection<?> toImplementation(SearchProjection<?> projection) {
		return (StubSearchProjection<?>) projection;
	}
}
