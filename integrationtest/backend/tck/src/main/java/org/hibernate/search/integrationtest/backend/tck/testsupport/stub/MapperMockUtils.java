/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.stub;

import static org.hibernate.search.util.impl.integrationtest.common.MockUtils.referenceMatcher;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.common.spi.DocumentReferenceConverter;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContext;
import org.hibernate.search.engine.search.loading.spi.DefaultProjectionHitMapper;
import org.hibernate.search.engine.search.loading.spi.EntityLoader;
import org.hibernate.search.util.impl.integrationtest.common.MockUtils;

public final class MapperMockUtils {

	private MapperMockUtils() {
	}

	/**
	 * @param loadingContextMock The mock for the loading context.
	 * @param referenceTransformerMock The mock for the reference transformer.
	 * @param objectLoaderMock The mock for the entity loader.
	 * @param hitMappingDefinition A definition of the reference -> entity mapping.
	 * @param <R> The reference type.
	 * @param <E> The entity type.
	 */
	public static <R, E> void expectHitMapping(
			LoadingContext<R, E> loadingContextMock,
			DocumentReferenceConverter<R> referenceTransformerMock,
			EntityLoader<R, E> objectLoaderMock,
			Consumer<HitMappingDefinitionContext<R, E>> hitMappingDefinition) {
		reset( loadingContextMock, referenceTransformerMock, objectLoaderMock );

		/*
		 * We expect getProjectionHitMapper to be called *every time* a load is performed,
		 * so that the mapper can check its state (session is open in ORM, for example).
		 */
		when( loadingContextMock.createProjectionHitMapper() )
				.thenReturn( new DefaultProjectionHitMapper<>(
						referenceTransformerMock,
						objectLoaderMock
				) );

		HitMappingDefinitionContext<R, E> context = new HitMappingDefinitionContext<>();
		hitMappingDefinition.accept( context );

		for ( Map.Entry<DocumentReference, Set<R>> entry : context.referenceMap.entrySet() ) {
			for ( R transformedReference : entry.getValue() ) {
				when( referenceTransformerMock.fromDocumentReference( referenceMatcher( entry.getKey() ) ) )
						.thenReturn( transformedReference );
			}
		}

		Set<R> keysToLoad = context.loadingMap.keySet();
		if ( !keysToLoad.isEmpty() ) {
			when( objectLoaderMock.loadBlocking(
					MockUtils.collectionAnyOrderMatcher( new ArrayList<>( keysToLoad ) ), notNull() ) )
					.thenAnswer( invocationOnMock -> invocationOnMock.<List<R>>getArgument( 0 ).stream()
							.map( context.loadingMap::get )
							.collect( Collectors.toList() ) );
		}
	}

	public static class HitMappingDefinitionContext<R, E> {
		private final Map<DocumentReference, Set<R>> referenceMap = new HashMap<>();
		private final Map<R, E> loadingMap = new HashMap<>();

		public HitMappingDefinitionContext<R, E> entityReference(DocumentReference documentReference, R transformedReference) {
			referenceMap.computeIfAbsent( documentReference, ignored -> new LinkedHashSet<>() )
					.add( transformedReference );
			return this;
		}

		public HitMappingDefinitionContext<R, E> load(DocumentReference documentReference, R transformedReference, E loadedObject) {
			// For each load, the backend must first transform the reference
			entityReference( documentReference, transformedReference );
			// Then it will need to trigger loading
			loadingMap.put( transformedReference, loadedObject );
			return this;
		}
	}
}
