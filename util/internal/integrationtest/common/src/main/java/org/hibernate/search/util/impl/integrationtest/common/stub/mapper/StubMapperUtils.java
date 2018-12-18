/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.mapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.hibernate.search.engine.backend.index.spi.DocumentReferenceProvider;
import org.hibernate.search.engine.search.loading.spi.ObjectLoader;
import org.hibernate.search.util.impl.integrationtest.common.EasyMockUtils;

import org.easymock.EasyMock;

public final class StubMapperUtils {

	private StubMapperUtils() {
	}

	public static DocumentReferenceProvider referenceProvider(String identifier) {
		return referenceProvider( identifier, null );
	}

	public static DocumentReferenceProvider referenceProvider(String identifier, String routingKey) {
		return new StubDocumentReferenceProvider( identifier, routingKey );
	}

	/**
	 * @param objectLoaderMock The EasyMock mock for the object loader.
	 * @param loadingDefinition A definition of the reference -> loaded object mapping.
	 * @param <R> The reference type.
	 * @param <O> The loaded object type.
	 */
	public static <R, O> void expectLoad(ObjectLoader<R, O> objectLoaderMock,
			Consumer<LoadingDefinitionContext<R, O>> loadingDefinition) {
		LoadingDefinitionContext<R, O> context = new LoadingDefinitionContext<>();
		loadingDefinition.accept( context );

		EasyMock.expect( objectLoaderMock.loadBlocking(
				EasyMockUtils.collectionAnyOrderMatcher( new ArrayList<>( context.loadingMap.keySet() ) )
		) )
				.andAnswer(
						() -> ( (List<R>) EasyMock.getCurrentArguments()[0] ).stream()
						.map( context.loadingMap::get )
						.collect( Collectors.toList() )
				);
	}

	public static class LoadingDefinitionContext<R, O> {
		private Map<R, O> loadingMap = new HashMap<>();

		public LoadingDefinitionContext<R, O> load(R reference, O loadedObject) {
			loadingMap.put( reference, loadedObject );
			return this;
		}
	}
}
