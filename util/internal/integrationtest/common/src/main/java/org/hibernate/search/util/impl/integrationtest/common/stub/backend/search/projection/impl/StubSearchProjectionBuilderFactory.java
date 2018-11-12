/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl;

import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.DistanceToFieldSearchProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.DocumentReferenceSearchProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.FieldSearchProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ObjectSearchProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ReferenceSearchProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ScoreSearchProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilderFactory;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.impl.StubSearchTargetModel;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.types.converter.impl.StubFieldConverter;

public class StubSearchProjectionBuilderFactory implements SearchProjectionBuilderFactory {

	private final StubSearchTargetModel targetModel;

	public StubSearchProjectionBuilderFactory(StubSearchTargetModel targetModel) {
		this.targetModel = targetModel;
	}

	@Override
	public DocumentReferenceSearchProjectionBuilder documentReference() {
		return new DocumentReferenceSearchProjectionBuilder() {
			@Override
			public SearchProjection<DocumentReference> build() {
				return StubDefaultSearchProjection.get();
			}
		};
	}

	@Override
	public <T> FieldSearchProjectionBuilder<T> field(String absoluteFieldPath, Class<T> clazz) {
		StubFieldConverter<?> converter = targetModel.getFieldConverter( absoluteFieldPath );
		return new FieldSearchProjectionBuilder<T>() {
			@Override
			public SearchProjection<T> build() {
				return new StubFieldSearchProjection<>( clazz, converter );
			}
		};
	}

	@Override
	public ObjectSearchProjectionBuilder object() {
		return new ObjectSearchProjectionBuilder() {
			@Override
			public SearchProjection<Object> build() {
				return StubObjectSearchProjection.get();
			}
		};
	}

	@Override
	public ReferenceSearchProjectionBuilder reference() {
		return new ReferenceSearchProjectionBuilder() {
			@Override
			public SearchProjection<Object> build() {
				return StubReferenceSearchProjection.get();
			}
		};
	}

	@Override
	public ScoreSearchProjectionBuilder score() {
		return new ScoreSearchProjectionBuilder() {
			@Override
			public SearchProjection<Float> build() {
				return StubDefaultSearchProjection.get();
			}
		};
	}

	@Override
	public DistanceToFieldSearchProjectionBuilder distance(String absoluteFieldPath, GeoPoint center) {
		return new DistanceToFieldSearchProjectionBuilder() {
			@Override
			public DistanceToFieldSearchProjectionBuilder unit(DistanceUnit unit) {
				return this;
			}

			@Override
			public SearchProjection<Double> build() {
				return StubDefaultSearchProjection.get();
			}
		};
	}
}
