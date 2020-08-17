/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.stub;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isNull;
import static org.easymock.EasyMock.notNull;
import static org.hibernate.search.util.impl.integrationtest.common.EasyMockUtils.referenceMatcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.common.spi.DocumentReferenceConverter;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContext;
import org.hibernate.search.engine.search.loading.spi.DefaultProjectionHitMapper;
import org.hibernate.search.engine.search.loading.spi.EntityLoader;
import org.hibernate.search.util.impl.integrationtest.common.EasyMockUtils;

import org.easymock.EasyMock;

public final class MapperEasyMockUtils {

	private MapperEasyMockUtils() {
	}

	/**
	 * @param loadingContextMock The EasyMock mock for the loading context.
	 * @param referenceTransformerMock The EasyMock mock for the reference transformer.
	 * @param objectLoaderMock The EasyMock mock for the entity loader.
	 * @param hitMappingDefinition A definition of the reference -> entity mapping.
	 * @param <R> The reference type.
	 * @param <E> The entity type.
	 */
	public static <R, E> void expectHitMapping(
			LoadingContext<R, E> loadingContextMock,
			DocumentReferenceConverter<R> referenceTransformerMock,
			EntityLoader<R, E> objectLoaderMock,
			Consumer<HitMappingDefinitionContext<R, E>> hitMappingDefinition) {
		expectHitMapping( loadingContextMock, referenceTransformerMock, objectLoaderMock, hitMappingDefinition, false );
	}

	@SuppressWarnings({"unchecked"})
	public static <R, E> void expectHitMapping(
			LoadingContext<R, E> loadingContextMock,
			DocumentReferenceConverter<R> referenceTransformerMock,
			EntityLoader<R, E> objectLoaderMock,
			Consumer<HitMappingDefinitionContext<R, E>> hitMappingDefinition,
			boolean entityLoadingTimeout) {
		/*
		 * We expect getProjectionHitMapper to be called *every time* a load is performed,
		 * so that the mapper can check its state (session is open in ORM, for example).
		 */
		expect( loadingContextMock.createProjectionHitMapper() )
				.andReturn( new DefaultProjectionHitMapper<>(
						referenceTransformerMock,
						objectLoaderMock
				) );

		HitMappingDefinitionContext<R, E> context = new HitMappingDefinitionContext<>();
		hitMappingDefinition.accept( context );

		for ( Map.Entry<DocumentReference, List<R>> entry : context.referenceMap.entrySet() ) {
			for ( R transformedReference : entry.getValue() ) {
				expect( referenceTransformerMock.fromDocumentReference( referenceMatcher( entry.getKey() ) ) )
						.andReturn( transformedReference );
			}
		}

		if ( entityLoadingTimeout ) {
			loadWithTimeout( objectLoaderMock, context );
		}
		else {
			loadWithoutTimeout( objectLoaderMock, context );
		}
	}

	public static class HitMappingDefinitionContext<R, E> {
		private final Map<DocumentReference, List<R>> referenceMap = new HashMap<>();
		private final Map<R, E> loadingMap = new HashMap<>();

		public HitMappingDefinitionContext<R, E> entityReference(DocumentReference documentReference, R transformedReference) {
			referenceMap.computeIfAbsent( documentReference, ignored -> new ArrayList<>() )
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

	private static <R, E> void loadWithoutTimeout(EntityLoader<R, E> objectLoaderMock,
			HitMappingDefinitionContext<R, E> context) {
		expect( objectLoaderMock.loadBlocking(
				EasyMockUtils.collectionAnyOrderMatcher( new ArrayList<>( context.loadingMap.keySet() ) ), isNull() ) )
				.andAnswer( () -> ( (List<R>) EasyMock.getCurrentArguments()[0] ).stream()
						.map( context.loadingMap::get )
						.collect( Collectors.toList() ) );
	}

	private static <R, E> void loadWithTimeout(EntityLoader<R, E> objectLoaderMock,
			HitMappingDefinitionContext<R, E> context) {
		expect( objectLoaderMock.loadBlocking(
				EasyMockUtils.collectionAnyOrderMatcher( new ArrayList<>( context.loadingMap.keySet() ) ), notNull() ) )
				.andAnswer( () -> ( (List<R>) EasyMock.getCurrentArguments()[0] ).stream()
						.map( context.loadingMap::get )
						.collect( Collectors.toList() ) );
	}
}
