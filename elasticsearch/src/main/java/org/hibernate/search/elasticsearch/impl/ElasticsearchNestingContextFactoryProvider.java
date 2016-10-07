/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.impl;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.TextField;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.metadata.impl.EmbeddedTypeMetadata.Container;
import org.hibernate.search.engine.nesting.impl.NestingContext;
import org.hibernate.search.engine.nesting.impl.NestingContextFactory;
import org.hibernate.search.engine.nesting.impl.NestingContextFactoryProvider;
import org.hibernate.search.engine.nesting.impl.NoOpNestingContext;
import org.hibernate.search.engine.service.spi.Startable;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.util.StringHelper;

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
		public void push(String fieldName, Container containerType) {
			path.push(
					new NestingStackElement(
							fieldName,
							containerType == Container.OBJECT ? null : 0
					)
			);
		}

		@Override
		public void mark(Document document) {
			String currentPath = StringHelper.join( path.descendingIterator(), "." );
			document.add( new TextField( ElasticsearchIndexWorkVisitor.NESTING_MARKER, currentPath, Store.NO ) );
		}

		@Override
		public void incrementCollectionIndex() {
			path.peek().increment();
		}

		@Override
		public void pop() {
			path.pop();
		}
	}

	private static class NestingStackElement {
		String field;
		Integer index;

		public NestingStackElement(String fieldName, Integer index) {
			this.field = fieldName;
			this.index = index;
		}

		@Override
		public String toString() {
			if ( index == null ) {
				return field;
			}
			else {
				return field + "[" + index + "]";
			}
		}

		void increment() {
			index = index + 1;
		}
	}
}
