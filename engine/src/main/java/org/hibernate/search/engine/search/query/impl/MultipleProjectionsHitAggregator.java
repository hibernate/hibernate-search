/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.query.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Function;

import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.loading.spi.ObjectLoader;

public class MultipleProjectionsHitAggregator<R, T> extends AbstractProjectionHitAggregator<R, List<Object>, T> {

	private static final Object ELEMENT_TO_LOAD_MARKER = new Object();

	private final Function<List<?>, T> hitTransformer;
	private final int expectedHitSize;

	private final MultipleProjectionsHitCollectorImpl hitCollector = new MultipleProjectionsHitCollectorImpl();

	public MultipleProjectionsHitAggregator(
			Function<DocumentReference, R> documentReferenceTransformer,
			ObjectLoader<R, ?> objectLoader,
			Function<List<?>, T> hitTransformer, int expectedHitSize) {
		super( documentReferenceTransformer, objectLoader );
		this.hitTransformer = hitTransformer;
		this.expectedHitSize = expectedHitSize;
	}

	@Override
	protected T convertHit(List<Object> hit, Iterator<?> loadedObjectIterator) {
		ListIterator<Object> hitElementIterator = hit.listIterator();
		while ( hitElementIterator.hasNext() ) {
			Object value = hitElementIterator.next();
			if ( ELEMENT_TO_LOAD_MARKER == value ) {
				hitElementIterator.set( loadedObjectIterator.next() );
			}
		}
		return hitTransformer.apply( hit );
	}

	@Override
	protected AbstractProjectionHitCollector getHitCollector() {
		return hitCollector;
	}

	private class MultipleProjectionsHitCollectorImpl extends AbstractProjectionHitCollector {
		private List<Object> currentHitItems;

		@Override
		void initialize() {
			this.currentHitItems = new ArrayList<>( expectedHitSize );
		}

		@Override
		void doCollect(Object hitElement) {
			currentHitItems.add( hitElement );
		}

		@Override
		void doCollectLoadingMarker() {
			currentHitItems.add( ELEMENT_TO_LOAD_MARKER );
		}

		@Override
		List<Object> getCollectedHit() {
			return currentHitItems;
		}
	}

}
