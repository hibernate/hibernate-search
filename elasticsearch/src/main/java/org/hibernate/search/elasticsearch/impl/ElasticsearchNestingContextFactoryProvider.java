/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.impl;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.hibernate.search.elasticsearch.impl.NestingMarker.NestingPathComponent;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.metadata.impl.EmbeddedTypeMetadata;
import org.hibernate.search.engine.metadata.impl.EmbeddedTypeMetadata.Container;
import org.hibernate.search.engine.nesting.impl.NestingContext;
import org.hibernate.search.engine.nesting.impl.NestingContextFactory;
import org.hibernate.search.engine.nesting.impl.NestingContextFactoryProvider;
import org.hibernate.search.engine.nesting.impl.NoOpNestingContext;
import org.hibernate.search.engine.service.spi.Startable;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.util.impl.CollectionHelper;

public class ElasticsearchNestingContextFactoryProvider implements NestingContextFactoryProvider, Startable {

	private ElasticsearchNestingContextFactory factory;

	@Override
	public void start(Properties properties, BuildContext context) {
		factory = new ElasticsearchNestingContextFactory( context.getUninitializedSearchIntegrator() );
	}

	@Override
	public NestingContextFactory getNestingContextFactory() {
		return factory;
	}

	private static class ElasticsearchNestingContextFactory implements NestingContextFactory {

		private final ConcurrentMap<String, ContextCreationStrategy> strategies = new ConcurrentHashMap<>();
		private ExtendedSearchIntegrator searchIntegrator;

		public ElasticsearchNestingContextFactory(ExtendedSearchIntegrator searchIntegrator) {
			this.searchIntegrator = searchIntegrator;
		}

		@Override
		public NestingContext createNestingContext(Class<?> indexedEntityType) {
			ContextCreationStrategy strategy = strategies.get( indexedEntityType.getName() );

			if ( strategy == null ) {
				strategy = isMappedToElasticsearch( indexedEntityType ) ? ContextCreationStrategy.ES : ContextCreationStrategy.NO_OP;
				strategies.putIfAbsent( indexedEntityType.getName(), strategy );
			}

			return strategy.create();
		}

		private boolean isMappedToElasticsearch(Class<?> entityType) {
			Set<Class<?>> queriedEntityTypesWithSubTypes = searchIntegrator.getIndexedTypesPolymorphic( new Class<?>[] { entityType } );

			for ( Class<?> queriedEntityType : queriedEntityTypesWithSubTypes ) {
				EntityIndexBinding binding = searchIntegrator.getIndexBinding( queriedEntityType );
				IndexManager[] indexManagers = binding.getIndexManagers();

				for ( IndexManager indexManager : indexManagers ) {
					if ( indexManager instanceof ElasticsearchIndexManager ) {
						return true;
					}
				}
			}

			return false;
		}
	}

	private enum ContextCreationStrategy {
		NO_OP {
			@Override
			NestingContext create() {
				return NoOpNestingContext.INSTANCE;
			}
		},
		ES {
			@Override
			NestingContext create() {
				return new ElasticsearchNestingContext();
			}
		};

		abstract NestingContext create();
	}

	private static class ElasticsearchNestingContext implements NestingContext {

		private Deque<NestingStackElement> path = new ArrayDeque<>();

		@Override
		public void push(EmbeddedTypeMetadata embeddedTypeMetadata) {
			path.addLast(
					new NestingStackElement(
							embeddedTypeMetadata,
							embeddedTypeMetadata.getEmbeddedContainer() == Container.OBJECT ? null : 0
					)
			);
		}

		@Override
		public void mark(Document document) {
			List<NestingPathComponent> currentPath = CollectionHelper.<NestingPathComponent>toImmutableList( path );
			document.add( new NestingMarkerField( currentPath ) );
		}

		@Override
		public void incrementCollectionIndex() {
			NestingStackElement current = path.removeLast();
			path.addLast( current.createNext() );
		}

		@Override
		public void pop() {
			path.removeLast();
		}
	}

	private static class NestingStackElement implements NestingMarker.NestingPathComponent, Cloneable {
		private final EmbeddedTypeMetadata embeddedTypeMetadata;
		private final Integer index;

		public NestingStackElement(EmbeddedTypeMetadata embeddedTypeMetadata, Integer index) {
			this.embeddedTypeMetadata = embeddedTypeMetadata;
			this.index = index;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder( embeddedTypeMetadata.getEmbeddedPropertyName() );
			if ( index != null ) {
				builder.append( "[" ).append( index ).append( "]" );
			}
			builder.append( " => " ).append( embeddedTypeMetadata.getEmbeddedFieldPrefix() );
			return builder.toString();
		}

		public NestingStackElement createNext() {
			return new NestingStackElement( embeddedTypeMetadata, index + 1 );
		}

		@Override
		public EmbeddedTypeMetadata getEmbeddedTypeMetadata() {
			return embeddedTypeMetadata;
		}

		@Override
		public Integer getIndex() {
			return index;
		}
	}

	private abstract static class MarkerField extends Field {

		public MarkerField() {
			super( "", new FieldType() );
		}

	}

	private static final class NestingMarkerField extends MarkerField implements NestingMarker {

		private final List<NestingPathComponent> nestingPath;

		public NestingMarkerField(List<NestingPathComponent> nestingPath) {
			super();
			this.nestingPath = nestingPath;
		}

		@Override
		public List<NestingPathComponent> getPath() {
			return nestingPath;
		}

		@Override
		public String toString() {
			return "<ES nesting marker: " + nestingPath.toString() + ">";
		}
	}

}
