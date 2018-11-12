/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl;

import org.hibernate.search.engine.backend.document.converter.runtime.FromIndexFieldValueConvertContext;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.query.spi.ProjectionHitCollector;

class StubObjectSearchProjection<T> implements StubSearchProjection<T> {

	@SuppressWarnings("rawtypes")
	private static final StubSearchProjection INSTANCE = new StubObjectSearchProjection();

	@SuppressWarnings("unchecked")
	static <T> StubObjectSearchProjection<T> get() {
		return (StubObjectSearchProjection<T>) INSTANCE;
	}

	private StubObjectSearchProjection() {
	}

	@Override
	public void extract(ProjectionHitCollector collector, Object projectionFromIndex,
			FromIndexFieldValueConvertContext context) {
		collector.collectForLoading( (DocumentReference) projectionFromIndex );
	}
}
