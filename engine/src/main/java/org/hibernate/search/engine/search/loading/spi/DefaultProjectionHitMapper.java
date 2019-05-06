/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.loading.spi;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.util.common.impl.CollectionHelper;

public final class DefaultProjectionHitMapper<R, O> implements ProjectionHitMapper<R, O> {

	private final Function<DocumentReference, R> documentReferenceTransformer;
	private final ObjectLoader<R, O> objectLoader;

	private final List<R> referencesToLoad = new ArrayList<>();

	public DefaultProjectionHitMapper(Function<DocumentReference, R> documentReferenceTransformer,
			ObjectLoader<R, O> objectLoader) {
		this.documentReferenceTransformer = documentReferenceTransformer;
		this.objectLoader = objectLoader;
	}

	@Override
	public R convertReference(DocumentReference reference) {
		return documentReferenceTransformer.apply( reference );
	}

	@Override
	public Object planLoading(DocumentReference reference) {
		referencesToLoad.add( documentReferenceTransformer.apply( reference ) );
		return referencesToLoad.size() - 1;
	}

	@Override
	public LoadingResult<O> loadBlocking() {
		return new DefaultLoadingResult<>( objectLoader.loadBlocking( referencesToLoad ) );
	}

	private static class DefaultLoadingResult<O> implements LoadingResult<O> {

		private final List<O> loadedObjects;

		private DefaultLoadingResult(List<O> loadedObjects) {
			this.loadedObjects = CollectionHelper.toImmutableList( loadedObjects );
		}

		@Override
		public O getLoaded(Object key) {
			return loadedObjects.get( (int) key );
		}
	}
}
