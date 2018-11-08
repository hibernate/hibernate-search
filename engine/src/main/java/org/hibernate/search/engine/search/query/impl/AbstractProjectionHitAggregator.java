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
import org.hibernate.search.engine.search.query.spi.HitAggregator;
import org.hibernate.search.engine.search.query.spi.ProjectionHitCollector;

/**
 * @param <R> The type of references.
 * @param <H> The type of "raw" hits, before they are {@link #convertHit(Object, Iterator) converted}
 * @param <T> The type of "final" hits, after they are {@link #convertHit(Object, Iterator) converted}
 */
abstract class AbstractProjectionHitAggregator<R, H, T> implements HitAggregator<ProjectionHitCollector, List<T>> {

	private final Function<DocumentReference, R> documentReferenceTransformer;
	private final ObjectLoader<R, ?> objectLoader;

	private final ArrayList<R> referencesToLoad = new ArrayList<>();
	private ArrayList<Object> hits;
	private int collectorRequestCount = 0;

	AbstractProjectionHitAggregator(
			Function<DocumentReference, R> documentReferenceTransformer,
			ObjectLoader<R, ?> objectLoader) {
		this.documentReferenceTransformer = documentReferenceTransformer;
		this.objectLoader = objectLoader;
	}

	@Override
	public void init(int expectedHitCount) {
		referencesToLoad.clear();
		hits = new ArrayList<>( expectedHitCount );
		collectorRequestCount = 0;
	}

	@Override
	public ProjectionHitCollector nextCollector() {
		AbstractProjectionHitCollector hitCollector = getHitCollector();
		if ( collectorRequestCount > 0 ) {
			// Don't forget to add the result of the previous collector to the hits
			hits.add( hitCollector.getCollectedHit() );
		}
		hitCollector.initialize();
		++collectorRequestCount;
		return hitCollector;
	}

	@Override
	@SuppressWarnings("unchecked") // These casts are just fine, and allow us to use one List instance instead of two
	public List<T> build() {
		if ( collectorRequestCount > 0 ) {
			// Don't forget to add the result of the last collector to the hits
			hits.add( getHitCollector().getCollectedHit() );
		}

		if ( !referencesToLoad.isEmpty() ) {
			List<?> loadedObjects = objectLoader.load( referencesToLoad );

			// Replace markers with the appropriate values
			Iterator<?> loadedObjectIterator = loadedObjects.iterator();
			ListIterator<Object> hitIterator = hits.listIterator();
			while ( hitIterator.hasNext() ) {
				T convertedHit = convertHit( (H) hitIterator.next(), loadedObjectIterator );
				hitIterator.set( convertedHit );
			}
		}

		return (List<T>) hits;
	}

	/**
	 * Convert collected hits to the final type and replace
	 * {@link AbstractProjectionHitCollector#doCollectLoadingMarker() loading markers}
	 * with the corresponding loaded object.
	 *
	 * @param hit The hit to convert
	 * @param loadedObjectIterator A source of loaded objects to use as a replacement for loading markers,
	 * in the correct order.
	 * @return The converted hit
	 */
	protected abstract T convertHit(H hit, Iterator<?> loadedObjectIterator);

	protected abstract AbstractProjectionHitCollector getHitCollector();

	abstract class AbstractProjectionHitCollector implements ProjectionHitCollector {
		@Override
		public final void collectProjection(Object hit) {
			doCollect( hit );
		}

		@Override
		public final void collectReference(DocumentReference reference) {
			doCollect( documentReferenceTransformer.apply( reference ) );
		}

		@Override
		public final void collectForLoading(DocumentReference reference) {
			doCollectLoadingMarker();
			referencesToLoad.add( documentReferenceTransformer.apply( reference ) );
		}

		abstract void doCollect(Object hitElement);

		abstract void doCollectLoadingMarker();

		abstract void initialize();

		abstract H getCollectedHit();
	}
}
