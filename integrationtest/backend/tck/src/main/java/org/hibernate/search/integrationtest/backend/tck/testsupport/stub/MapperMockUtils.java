/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.stub;

import static org.hibernate.search.util.impl.integrationtest.common.MockUtils.referenceMatcher;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContext;

import org.mockito.Mockito;

public final class MapperMockUtils {

	private MapperMockUtils() {
	}

	/**
	 * @param loadingContextMock The mock for the loading context.
	 * @param hitMappingDefinition A definition of the reference -> entity mapping.
	 * @param <R> The reference type.
	 * @param <E> The entity type.
	 */
	public static <R, E> void expectHitMapping(
			SearchLoadingContext<R, E> loadingContextMock,
			Consumer<HitMappingDefinitionContext<R, E>> hitMappingDefinition) {
		reset( (Object) loadingContextMock );

		@SuppressWarnings("unchecked")
		ProjectionHitMapper<R, E> projectionHitMapperMock = Mockito.mock( ProjectionHitMapper.class );
		@SuppressWarnings("unchecked")
		LoadingResult<R, E> loadingResultMock = Mockito.mock( LoadingResult.class );

		/*
		 * We expect getProjectionHitMapper to be called *every time* a load is performed,
		 * because it may not reset its internal state,
		 * and also so that the mapper can check the session state (session is still open in ORM, for example).
		 */
		when( loadingContextMock.createProjectionHitMapper() )
				.thenReturn( projectionHitMapperMock );
		when( projectionHitMapperMock.loadBlocking( any() ) )
				.thenReturn( loadingResultMock );

		HitMappingDefinitionContext<R, E> context = new HitMappingDefinitionContext<>();
		hitMappingDefinition.accept( context );

		List<StubLoadingKey> loadingKeys = new ArrayList<>();
		for ( int i = 0; i < context.referencesToLoad.size(); i++ ) {
			DocumentReference documentReference = context.referencesToLoad.get( i );
			StubLoadingKey loadingKey = new StubLoadingKey( i );
			loadingKeys.add( loadingKey );
			when( projectionHitMapperMock.planLoading( referenceMatcher( documentReference ) ) )
					.thenReturn( loadingKey );
		}

		when( projectionHitMapperMock.loadBlocking( any() ) )
				.thenReturn( loadingResultMock );

		for ( int i = 0; i < context.loadedObjects.size(); i++ ) {
			when( loadingResultMock.get( loadingKeys.get( i ) ) )
					.thenReturn( context.loadedObjects.get( i ) );
		}

		for ( Map.Entry<DocumentReference, Set<R>> entry : context.referenceMap.entrySet() ) {
			for ( R transformedReference : entry.getValue() ) {
				when( loadingResultMock.convertReference( referenceMatcher( entry.getKey() ) ) )
						.thenReturn( transformedReference );
			}
		}
	}

	public static class HitMappingDefinitionContext<R, E> {
		private final Map<DocumentReference, Set<R>> referenceMap = new HashMap<>();
		private final List<DocumentReference> referencesToLoad = new ArrayList<>();
		private final List<E> loadedObjects = new ArrayList<>();

		public HitMappingDefinitionContext<R, E> entityReference(DocumentReference documentReference, R transformedReference) {
			referenceMap.computeIfAbsent( documentReference, ignored -> new LinkedHashSet<>() )
					.add( transformedReference );
			return this;
		}

		public HitMappingDefinitionContext<R, E> load(DocumentReference documentReference, E loadedObject) {
			referencesToLoad.add( documentReference );
			loadedObjects.add( loadedObject );
			return this;
		}
	}

	private static class StubLoadingKey {
		private final int ordinal;
		public StubLoadingKey(int ordinal) {
			this.ordinal = ordinal;
		}
	}
}
