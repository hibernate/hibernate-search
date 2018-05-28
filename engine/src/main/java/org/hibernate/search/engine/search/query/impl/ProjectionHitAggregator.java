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
import java.util.stream.Collectors;

import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.ObjectLoader;
import org.hibernate.search.engine.search.query.spi.HitAggregator;
import org.hibernate.search.engine.search.query.spi.ProjectionHitCollector;

public final class ProjectionHitAggregator<R, T> implements HitAggregator<ProjectionHitCollector, List<T>> {

	private static final Object ELEMENT_TO_LOAD_MARKER = new Object();

	private final Function<DocumentReference, R> documentReferenceTransformer;
	private final ObjectLoader<R, ?> objectLoader;
	private final Function<List<?>, T> hitTransformer;
	private final int expectedHitSize;
	private final int expectedLoadCountPerHit;
	private final HitCollectorImpl hitCollector = new HitCollectorImpl();

	private final ArrayList<R> referencesToLoad = new ArrayList<>();
	private final ArrayList<List<Object>> hits = new ArrayList<>();

	public ProjectionHitAggregator(
			Function<DocumentReference, R> documentReferenceTransformer,
			ObjectLoader<R, ?> objectLoader, Function<List<?>, T> hitTransformer,
			int expectedHitSize, int expectedLoadCountPerHit) {
		this.documentReferenceTransformer = documentReferenceTransformer;
		this.objectLoader = objectLoader;
		this.hitTransformer = hitTransformer;
		this.expectedHitSize = expectedHitSize;
		this.expectedLoadCountPerHit = expectedLoadCountPerHit;
	}

	@Override
	public void init(int expectedHitCount) {
		referencesToLoad.clear();
		hits.clear();
		referencesToLoad.ensureCapacity( expectedHitCount * expectedLoadCountPerHit );
		hits.ensureCapacity( expectedHitCount );
	}

	@Override
	public ProjectionHitCollector nextCollector() {
		List<Object> currentHitItems = new ArrayList<>( expectedHitSize );
		hits.add( currentHitItems );
		hitCollector.reset( currentHitItems );
		return hitCollector;
	}

	@Override
	public List<T> build() {
		if ( !referencesToLoad.isEmpty() ) {
			List<?> loadedObjects = objectLoader.load( referencesToLoad );

			// Replace markers with the appropriate values
			Iterator<?> loadedObjectIterator = loadedObjects.iterator();
			for ( List<Object> hit : hits ) {
				ListIterator<Object> hitElementIterator = hit.listIterator();
				while ( hitElementIterator.hasNext() ) {
					Object value = hitElementIterator.next();
					if ( ELEMENT_TO_LOAD_MARKER == value ) {
						hitElementIterator.set( loadedObjectIterator.next() );
					}
				}
			}
		}

		// TODO avoid creating this list when the transformer is the identity; maybe tranform in-place?
		// (if we transform in-place, make sure to reset the "hits" reference during init)
		return hits.stream().map( hitTransformer ).collect( Collectors.toList() );
	}

	private class HitCollectorImpl implements ProjectionHitCollector {

		private List<Object> currentHitItems;

		public void reset(List<Object> currentHitItems) {
			this.currentHitItems = currentHitItems;
		}

		@Override
		public void collectProjection(Object hit) {
			currentHitItems.add( hit );
		}

		@Override
		public void collectReference(DocumentReference reference) {
			currentHitItems.add( documentReferenceTransformer.apply( reference ) );
		}

		@Override
		public void collectForLoading(DocumentReference reference) {
			currentHitItems.add( ELEMENT_TO_LOAD_MARKER );
			referencesToLoad.add( documentReferenceTransformer.apply( reference ) );
		}

	}
}
