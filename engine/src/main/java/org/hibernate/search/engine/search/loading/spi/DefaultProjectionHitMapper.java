/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.loading.spi;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.common.spi.DocumentReferenceConverter;
import org.hibernate.search.util.common.impl.CollectionHelper;

public final class DefaultProjectionHitMapper<R, E> implements ProjectionHitMapper<R, E> {

	private final DocumentReferenceConverter<R> documentReferenceConverter;
	private final EntityLoader<R, ? extends E> objectLoader;

	private final List<DocumentReference> referencesToLoad = new ArrayList<>();

	public DefaultProjectionHitMapper(DocumentReferenceConverter<R> documentReferenceConverter,
			EntityLoader<R, ? extends E> objectLoader) {
		this.documentReferenceConverter = documentReferenceConverter;
		this.objectLoader = objectLoader;
	}

	@Override
	public Object planLoading(DocumentReference reference) {
		referencesToLoad.add( reference );
		return referencesToLoad.size() - 1;
	}

	@Override
	public LoadingResult<R, E> loadBlocking(Integer timeout) {
		List<R> converted = referencesToLoad.stream().map( documentReferenceConverter::fromDocumentReference )
				.collect( Collectors.toList() );
		return new DefaultLoadingResult<>( objectLoader.loadBlocking( converted, timeout ),
				documentReferenceConverter );
	}

	private static class DefaultLoadingResult<R, E> implements LoadingResult<R, E> {

		private final List<? extends E> loadedObjects;
		private final DocumentReferenceConverter<R> documentReferenceConverter;

		private DefaultLoadingResult(List<? extends E> loadedObjects, DocumentReferenceConverter<R> documentReferenceConverter) {
			this.loadedObjects = CollectionHelper.toImmutableList( loadedObjects );
			this.documentReferenceConverter = documentReferenceConverter;
		}

		@Override
		public E get(Object key) {
			return loadedObjects.get( (int) key );
		}

		@Override
		public R convertReference(DocumentReference reference) {
			return documentReferenceConverter.fromDocumentReference( reference );
		}
	}
}
