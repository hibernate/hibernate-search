/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.loading.spi;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.common.spi.DocumentReferenceConverter;
import org.hibernate.search.util.common.impl.CollectionHelper;

public final class DefaultProjectionHitMapper<R, E> implements ProjectionHitMapper<R, E> {

	private final DocumentReferenceConverter<R> documentReferenceConverter;
	private final EntityLoader<R, ? extends E> objectLoader;

	private final List<R> referencesToLoad = new ArrayList<>();

	public DefaultProjectionHitMapper(DocumentReferenceConverter<R> documentReferenceConverter,
			EntityLoader<R, ? extends E> objectLoader) {
		this.documentReferenceConverter = documentReferenceConverter;
		this.objectLoader = objectLoader;
	}

	@Override
	public R convertReference(DocumentReference reference) {
		return documentReferenceConverter.fromDocumentReference( reference );
	}

	@Override
	public Object planLoading(DocumentReference reference) {
		referencesToLoad.add( documentReferenceConverter.fromDocumentReference( reference ) );
		return referencesToLoad.size() - 1;
	}

	@Override
	public LoadingResult<E> loadBlocking() {
		return new DefaultLoadingResult<>( objectLoader.loadBlocking( referencesToLoad ) );
	}

	private static class DefaultLoadingResult<E> implements LoadingResult<E> {

		private final List<? extends E> loadedObjects;

		private DefaultLoadingResult(List<? extends E> loadedObjects) {
			this.loadedObjects = CollectionHelper.toImmutableList( loadedObjects );
		}

		@Override
		public E get(Object key) {
			return loadedObjects.get( (int) key );
		}
	}
}
