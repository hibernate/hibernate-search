/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.query.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.loading.spi.ObjectLoader;
import org.hibernate.search.engine.search.query.spi.HitAggregator;
import org.hibernate.search.engine.search.query.spi.LoadingHitCollector;
import org.hibernate.search.util.AssertionFailure;

public final class ObjectHitAggregator<R, O> implements HitAggregator<LoadingHitCollector, List<O>> {

	private final Function<DocumentReference, R> documentReferenceTransformer;
	private final ObjectLoader<R, O> objectLoader;
	private final HitCollectorImpl hitCollector = new HitCollectorImpl();

	private final ArrayList<R> referencesToLoad = new ArrayList<>();

	public ObjectHitAggregator(Function<DocumentReference, R> documentReferenceTransformer,
			ObjectLoader<R, O> objectLoader) {
		this.documentReferenceTransformer = documentReferenceTransformer;
		this.objectLoader = objectLoader;
	}

	@Override
	public void init(int expectedHitCount) {
		referencesToLoad.clear();
		referencesToLoad.ensureCapacity( expectedHitCount );
	}

	@Override
	public LoadingHitCollector nextCollector() {
		hitCollector.reset();
		return hitCollector;
	}

	@Override
	public List<O> build() {
		return objectLoader.load( referencesToLoad );
	}

	private class HitCollectorImpl implements LoadingHitCollector {

		private boolean currentHitCollected = false;

		@Override
		public void collectForLoading(DocumentReference reference) {
			checkNotAlreadyCollected();
			currentHitCollected = true;
			referencesToLoad.add( documentReferenceTransformer.apply( reference ) );
		}

		public void reset() {
			currentHitCollected = false;
		}

		private void checkNotAlreadyCollected() {
			if ( currentHitCollected ) {
				throw new AssertionFailure( "Received multiple values for a single hit" );
			}
		}

	}
}
