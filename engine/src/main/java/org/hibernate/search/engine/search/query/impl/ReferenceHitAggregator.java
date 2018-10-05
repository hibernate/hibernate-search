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
import org.hibernate.search.engine.search.query.spi.HitAggregator;
import org.hibernate.search.engine.search.query.spi.ReferenceHitCollector;
import org.hibernate.search.util.AssertionFailure;

public final class ReferenceHitAggregator<T> implements HitAggregator<ReferenceHitCollector, List<T>> {

	private final Function<DocumentReference, T> documentReferenceTransformer;
	private final HitCollectorImpl hitCollector = new HitCollectorImpl();

	private List<T> hits;

	public ReferenceHitAggregator(Function<DocumentReference, T> documentReferenceTransformer) {
		this.documentReferenceTransformer = documentReferenceTransformer;
	}

	@Override
	public void init(int expectedHitCount) {
		hits = new ArrayList<>( expectedHitCount );
	}

	@Override
	public ReferenceHitCollector nextCollector() {
		hitCollector.reset();
		return hitCollector;
	}

	@Override
	public List<T> build() {
		List<T> result = hits;
		hits = null;
		return result;
	}

	private class HitCollectorImpl implements ReferenceHitCollector {

		private boolean currentHitCollected = false;

		@Override
		public void collectReference(DocumentReference reference) {
			checkNotAlreadyCollected();
			currentHitCollected = true;
			hits.add( documentReferenceTransformer.apply( reference ) );
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
