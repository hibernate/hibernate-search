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
package org.hibernate.search.engine;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import org.hibernate.search.SearchException;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.PurgeAllLuceneWork;
import org.hibernate.search.backend.Work;
import org.hibernate.search.backend.WorkType;
import org.hibernate.search.util.HibernateHelper;

/**
 * Represents the set of changes going to be applied to the index for the entities. A stream of Work is feed as input, a
 * list of LuceneWork is output, and in the process we try to reduce the number of output operations to the minimum
 * needed to reach the same final state.
 * 
 * @since 3.3
 * @author Sanne Grinovero
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class WorkPlan {
	
	private HashMap<Class<?>, PerClassWork<?>> byClass = new HashMap<Class<?>, PerClassWork<?>>();
	
	public <T> void addWork(Work<T> work) {
		Class<T> entityClass = HibernateHelper.getClassFromWork( work );
		PerClassWork classWork = getClassWork( entityClass );
		classWork.addWork( work );
	}
	
	private <T> PerClassWork getClassWork(Class<T> entityClass) {
		PerClassWork classWork = byClass.get( entityClass );
		if ( classWork == null ) {
			classWork = new PerClassWork( entityClass );
			byClass.put( entityClass, classWork );
		}
		return classWork;
	}
	
	public void processContainedIn(SearchFactoryImplementor searchFactoryImplementor) {
		for ( PerClassWork perClassWork : byClass.values() ) {
			perClassWork.processContainedIn( searchFactoryImplementor );
		}
	}
	
	<T> void recurseContainedIn(SearchFactoryImplementor searchFactoryImplementor, T value) {
		Class<T> entityClass = HibernateHelper.getClass( value );
		PerClassWork classWork = getClassWork( entityClass );
		classWork.recurseContainedIn( searchFactoryImplementor, value );
	}
	
	/**
	 * @param searchFactoryImplementor
	 * @return
	 */
	public List<LuceneWork> getPlannedLuceneWork(SearchFactoryImplementor searchFactoryImplementor) {
		List<LuceneWork> luceneQueue = new ArrayList<LuceneWork>();
		for ( PerClassWork perClassWork : byClass.values() ) {
			perClassWork.enqueueLuceneWork( searchFactoryImplementor, luceneQueue );
		}
		return luceneQueue;
	}
	
	class PerClassWork<T> {
		
		private HashMap<Serializable, PerEntityWork<T>> byEntityId = new HashMap<Serializable, PerEntityWork<T>>();
		private boolean purgeAll = false;
		private final Class<T> entityClass;
		private AbstractDocumentBuilder<T> entityBuilder;
		
		PerClassWork(Class<T> clazz) {
			this.entityClass = clazz;
		}
		
		public void addWork(Work<T> work) {
			if ( work.getType() == WorkType.PURGE_ALL ) {
				byEntityId.clear();
				purgeAll = true;
			}
			else {
				Serializable id = work.getId();
				PerEntityWork<T> entityWork = byEntityId.get( id );
				if ( entityWork == null ) {
					entityWork = new PerEntityWork<T>( work );
					byEntityId.put( id, entityWork );
				}
				entityWork.addWork( work );
			}
		}
		
		public void enqueueLuceneWork(SearchFactoryImplementor searchFactoryImplementor, List<LuceneWork> luceneQueue) {
			final AbstractDocumentBuilder<T> builder = getEntityBuilder( searchFactoryImplementor );
			final Set<Entry<Serializable, PerEntityWork<T>>> entityInstances = byEntityId.entrySet();
			if ( purgeAll ) {
				luceneQueue.add( new PurgeAllLuceneWork( entityClass ) );
			}
			for ( Entry<Serializable, PerEntityWork<T>> entry : entityInstances ) {
				Serializable id = entry.getKey();
				PerEntityWork<T> perEntityWork = entry.getValue();
				perEntityWork.enqueueLuceneWork( entityClass, id, builder, luceneQueue, searchFactoryImplementor );
			}
		}
		
		public void processContainedIn(SearchFactoryImplementor searchFactoryImplementor) {
			AbstractDocumentBuilder<T> builder = getEntityBuilder( searchFactoryImplementor );
			final Set<Entry<Serializable, PerEntityWork<T>>> entityInstances = byEntityId.entrySet();
			for ( Entry<Serializable, PerEntityWork<T>> entry : entityInstances ) {
				Serializable id = entry.getKey();
				PerEntityWork<T> perEntityWork = entry.getValue();
				perEntityWork.processContainedIn( entityClass, id, builder, WorkPlan.this, searchFactoryImplementor );
			}
		}
		
		void recurseContainedIn(SearchFactoryImplementor searchFactoryImplementor, T value) {
			AbstractDocumentBuilder<T> documentBuilder = getEntityBuilder( searchFactoryImplementor );
			Serializable indexingId = documentBuilder.getIndexingId( value );
			if ( indexingId != null ) {
				PerEntityWork<T> entityWork = byEntityId.get( indexingId );
				if ( entityWork == null ) {
					entityWork = new PerEntityWork( value );
					byEntityId.put( indexingId, entityWork );
					// recursion starts:
					documentBuilder.processContainedInInstances( entityClass, value, WorkPlan.this, searchFactoryImplementor );
				}
				// else nothing to do as it's being processed already
			}
			else {
				documentBuilder.processContainedInInstances( entityClass, value, WorkPlan.this, searchFactoryImplementor );
			}
		}
		
		private AbstractDocumentBuilder<T> getEntityBuilder(SearchFactoryImplementor searchFactoryImplementor) {
			if ( entityBuilder != null ) {
				return entityBuilder;
			}
			entityBuilder = searchFactoryImplementor.getDocumentBuilderIndexedEntity( entityClass );
			if ( entityBuilder == null ) {
				entityBuilder = searchFactoryImplementor.getDocumentBuilderContainedEntity( entityClass );
				if ( entityBuilder == null ) {
					// should never happen but better be safe than sorry
					throw new SearchException( "Unable to perform work. Entity Class is not @Indexed nor hosts @ContainedIn: " + entityClass );
				}
			}
			return entityBuilder;
		}
	}
	
	private static class PerEntityWork<T> {
		
		private T entity;
		private boolean delete = false; // needs to generate a Lucene delete work
		private boolean add = false; // needs to generate a Lucene add work
		private boolean batch = false;
		private boolean containedInProcessed = false;
		
		public PerEntityWork(T entity) {
			// for updates only
			this.entity = entity;
			this.delete = true;
			this.add = true;
			this.containedInProcessed = true;
		}
		
		public PerEntityWork(Work<T> work) {
			entity = work.getEntity();
			WorkType type = work.getType();
			// sets the initial state:
			switch ( type ) {
			case ADD:
				add = true;
				break;
			case DELETE:
			case PURGE:
				delete = true;
				break;
			case COLLECTION:
			case UPDATE:
				delete = true;
				add = true;
				break;
			case INDEX:
				add = true;
				delete = true;
				batch = true;
				break;
			case PURGE_ALL:
				// not breaking intentionally: PURGE_ALL should not reach this
				// class
			default:
				throw new SearchException( "unexpected state:" + type );
			}
		}
		
		public void addWork(Work<T> work) {
			entity = work.getEntity();
			WorkType type = work.getType();
			switch ( type ) {
			case INDEX:
				batch = true;
				// not breaking intentionally
			case UPDATE:
				if ( add & !delete ) {
					// noop: the entity was newly created in this same unit of work
					// so it needs to be added no need to delete
				}
				else {
					add = true;
					delete = true;
				}
				break;
			case ADD: // Is the only operation which doesn't imply a delete-before-add
				add = true;
				// leave delete flag as-is
				break;
			case DELETE:
			case PURGE:
				if ( add & !delete ) {
					// the entity was was newly created in this same unit of
					// work so works counter each other
					add = false;
				}
				else {
					add = false;
					delete = true;
				}
			case COLLECTION:
				if ( !add && !delete ) {
					add = true;
					delete = true;
				}
				// nothing to do, as something else was done
				break;
			case PURGE_ALL:
			default:
				throw new SearchException( "unexpected state:" + type );
			}
		}
		
		public void enqueueLuceneWork(Class<T> entityClass, Serializable id, AbstractDocumentBuilder<T> entityBuilder, List<LuceneWork> luceneQueue,
				SearchFactoryImplementor searchFactoryImplementor) {
			if ( add || delete ) {
				entityBuilder.addWorkToQueue( entityClass, entity, id, delete, add, batch, luceneQueue );
			}
		}
		
		public void processContainedIn(Class<T> entityClass, Serializable id, AbstractDocumentBuilder<T> entityBuilder, WorkPlan workplan,
				SearchFactoryImplementor searchFactoryImplementor) {
			if ( !containedInProcessed ) {
				containedInProcessed = true;
				entityBuilder.processContainedInInstances( entityClass, entity, workplan, searchFactoryImplementor );
			}
		}
	}
	
}