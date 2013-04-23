/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.sandbox.standalone;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.SearchException;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.spi.EntityIndexBinder;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;

import java.io.Serializable;
import java.util.Set;
import java.util.List;

/**
 * Implements a standalone full text service.
 * Data is stored in Lucene
 *
 * @author Emmanuel Bernard
 */
public class LuceneFullTextManager implements FullTextManager {
	private final SearchFactoryImplementor searchFactory;
	private final InstanceTransactionContext transactionContext;

	LuceneFullTextManager(SearchFactoryImplementor sfi) {
		this.searchFactory = sfi;
		this.transactionContext = new InstanceTransactionContext();
		transactionContext.beginTransaction();
	}

	public <T> T get(Class<T> entityType, Serializable id) {
		final EntityIndexBinder entityIndexBinding = searchFactory.getIndexBindingForEntity( entityType );
		if ( entityIndexBinding == null ) {
			String msg = "Entity to retrueve is not an @Indexed entity: " + entityType.getClass().getName();
			throw new IllegalArgumentException( msg );
		}
		if (id == null) {
			throw new IllegalArgumentException( "Identifier cannot be null" );
		}
		DocumentBuilderIndexedEntity<?> docBuilder = entityIndexBinding.getDocumentBuilder();
		Query luceneQuery = new TermQuery( docBuilder.getTerm( id ) );
		FullTextQuery searchQuery = createFullTextQuery( luceneQuery, entityType );
		List<?> results = searchQuery.list();
		if (results.size() > 1) {
			//TODO find correct exception
			throw new SearchException("Several entities with he same id found: " + entityType + "#" + id);
		}
		@SuppressWarnings( "unchecked" )
		final T result = (T) ( results.size() == 0 ? null : results.get( 0 ) );
		return result;
	}

	public FullTextQuery createFullTextQuery(Query luceneQuery, Class<?>... entities) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}



	/**
	 * (Re-)index an entity.
	 * The entity must be associated with the session and non indexable entities are ignored.
	 *
	 * @param entity The entity to index - must not be <code>null</code>.
	 *
	 * @throws IllegalArgumentException if entity is null or not an @Indexed entity
	 */
	public <T> void index(T entity) {
		if ( entity == null ) {
			throw new IllegalArgumentException( "Entity to index should not be null" );
		}

		Class<?> clazz = getClass( entity );
		//TODO cache that at the FTSession level
		//not strictly necessary but a small optimization
		final EntityIndexBinder entityIndexBinding = searchFactory.getIndexBindingForEntity( clazz );
		if ( entityIndexBinding == null ) {
			String msg = "Entity to index is not an @Indexed entity: " + entity.getClass().getName();
			throw new IllegalArgumentException( msg );
		}
		Serializable id = entityIndexBinding.getDocumentBuilder().getId( entity );
		Work<T> work = new Work<T>( entity, id, WorkType.INDEX );
		searchFactory.getWorker().performWork( work, transactionContext );

		//TODO
		//need to add elements in a queue kept at the Session level
		//the queue will be processed by a Lucene(Auto)FlushEventListener
		//note that we could keep this queue somewhere in the event listener in the mean time but that requires
		//a synchronized hashmap holding this queue on a per session basis plus some session house keeping (yuk)
		//another solution would be to subclass SessionImpl instead of having this LuceneSession delegation model
		//this is an open discussion
	}

	private Class<?> getClass(Object entity) {
		return entity.getClass();
	}

	public SearchFactory getSearchFactory() {
		return searchFactory;
	}

	public <T> void purge(Class<T> entityType, Serializable id) {
		if ( entityType == null ) {
			return;
		}

		Set<Class<?>> targetedClasses = searchFactory.getIndexedTypesPolymorphic( new Class[] {entityType} );
		if ( targetedClasses.isEmpty() ) {
			String msg = entityType.getName() + " is not an indexed entity or a subclass of an indexed entity";
			throw new IllegalArgumentException( msg );
		}

		for ( Class<?> clazz : targetedClasses ) {
			if ( id == null ) {
				createAndPerformWork( clazz, null, WorkType.PURGE_ALL );
			}
			else {
				createAndPerformWork( clazz, id, WorkType.PURGE );
			}
		}
	}

	private <T> void createAndPerformWork(Class<T> clazz, Serializable id, WorkType workType) {
		Work<T> work;
		work = new Work<T>( clazz, id, workType );
		searchFactory.getWorker().performWork( work, transactionContext );
	}

	public <T> void purgeAll(Class<T> entityType) {
		purge( entityType, null );
	}

	public void flushToIndexes() {
		searchFactory.getWorker().flushWorks( transactionContext );
	}
}
