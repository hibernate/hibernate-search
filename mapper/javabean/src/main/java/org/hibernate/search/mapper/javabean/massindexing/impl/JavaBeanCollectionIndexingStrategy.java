/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.massindexing.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.hibernate.search.mapper.javabean.log.impl.Log;
import org.hibernate.search.mapper.javabean.massindexing.loader.JavaBeanIndexingOptions;
import org.hibernate.search.mapper.pojo.loading.EntityIdentifierScroll;
import org.hibernate.search.mapper.pojo.loading.EntityLoader;
import org.hibernate.search.mapper.pojo.loading.EntityLoadingTypeGroup;
import org.hibernate.search.mapper.pojo.massindexing.loader.MassIndexingEntityLoadingStrategy;
import org.hibernate.search.mapper.pojo.massindexing.loader.MassIndexingThreadContext;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class JavaBeanCollectionIndexingStrategy<E> implements MassIndexingEntityLoadingStrategy<E, JavaBeanIndexingOptions> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Collection<E> source;
	private final Map<Object, E> map = new ConcurrentHashMap<>();
	private final EntityLoadingTypeGroup typeGroup;

	public JavaBeanCollectionIndexingStrategy(Collection<E> source, EntityLoadingTypeGroup typeGroup) {
		this.source = source;
		this.typeGroup = typeGroup;
	}

	@Override
	public EntityLoadingTypeGroup assignGroup() {
		return typeGroup;
	}

	@Override
	public EntityIdentifierScroll createIdentifierScroll(MassIndexingThreadContext<JavaBeanIndexingOptions> context, Set<Class<? extends E>> includedTypes) throws InterruptedException {
		Iterator<E> iterator = source.iterator();

		return new EntityIdentifierScroll() {
			@Override
			public long totalCount() {
				return source.size();
			}

			@Override
			public List<?> next() {
				int batchSize = context.options().batchSize();

				List<Object> destination = new ArrayList<>( batchSize );
				while ( iterator.hasNext() ) {
					E entity = iterator.next();
					Object identifier = context.entityIdentifier( iterator.next() );
					map.put( identifier, entity );

					destination.add( identifier );
					if ( !context.active() ) {
						throw log.contextNotActiveWhileProducingIdsForBatchIndexing( context.includedEntityNames() );
					}

					if ( destination.size() == batchSize ) {
						return destination;
					}

				}
				return destination;
			}
		};
	}

	@Override
	public EntityLoader<E> createLoader(MassIndexingThreadContext<JavaBeanIndexingOptions> context, Set<Class<? extends E>> includedTypes) throws InterruptedException {
		return (identifiers) -> {
			List<E> found = identifiers.stream().map( map::get ).collect( Collectors.toList() );
			identifiers.forEach( map::remove );
			return found;
		};
	}
}
