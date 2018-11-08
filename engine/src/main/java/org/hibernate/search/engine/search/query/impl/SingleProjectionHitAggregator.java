/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.query.impl;

import java.util.Iterator;
import java.util.function.Function;

import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.loading.spi.ObjectLoader;

public class SingleProjectionHitAggregator<R, T> extends AbstractProjectionHitAggregator<R, Object, T> {

	private static final Object ELEMENT_TO_LOAD_MARKER = new Object();

	private final Function<Object, T> hitTransformer;

	private final SingleProjectionHitCollectorImpl hitCollector = new SingleProjectionHitCollectorImpl();

	public SingleProjectionHitAggregator(
			Function<DocumentReference, R> documentReferenceTransformer,
			ObjectLoader<R, ?> objectLoader,
			Function<Object, T> hitTransformer) {
		super( documentReferenceTransformer, objectLoader );
		this.hitTransformer = hitTransformer;
	}

	@Override
	protected T convertHit(Object hit, Iterator<?> loadedObjectIterator) {
		Object loadedHit;
		if ( ELEMENT_TO_LOAD_MARKER == hit ) {
			loadedHit = loadedObjectIterator.next();
		}
		else {
			loadedHit = hit;
		}
		return hitTransformer.apply( loadedHit );
	}

	@Override
	protected AbstractProjectionHitCollector getHitCollector() {
		return hitCollector;
	}

	private class SingleProjectionHitCollectorImpl extends AbstractProjectionHitCollector {
		private Object hit;

		@Override
		void initialize() {
			this.hit = null;
		}

		@Override
		void doCollect(Object hitElement) {
			hit = hitElement;
		}

		@Override
		void doCollectLoadingMarker() {
			hit = ELEMENT_TO_LOAD_MARKER;
		}

		@Override
		Object getCollectedHit() {
			return hit;
		}
	}

}
