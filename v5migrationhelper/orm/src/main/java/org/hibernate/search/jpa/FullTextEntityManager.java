/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jpa;

import java.io.Serializable;

import jakarta.persistence.EntityManager;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.hibernate.search.MassIndexer;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.hibernate.search.mapper.orm.scope.SearchScope;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.orm.work.SearchIndexingPlan;
import org.hibernate.search.mapper.orm.work.SearchWorkspace;

/**
 * Extends an EntityManager with Full-Text operations
 *
 * @author Emmanuel Bernard
 * @deprecated Instead of using Hibernate Search 5 APIs, get a {@link SearchSession}
 * using {@link org.hibernate.search.mapper.orm.Search#session(EntityManager)}.
 * Refer to the <a href="https://hibernate.org/search/documentation/migrate/6.0/">migration guide</a> for more information.
 */
@Deprecated
public interface FullTextEntityManager extends EntityManager {

	/**
	 * Create a fulltext query on top of a native Lucene query returning the matching objects
	 * of type <code>entities</code> and their respective subclasses.
	 *
	 * @param luceneQuery The native Lucene query to be rn against the Lucene index.
	 * @param entities List of classes for type filtering. The query result will only return entities of
	 * the specified types and their respective subtype. If no class is specified no type filtering will take place.
	 *
	 * @return A <code>FullTextQuery</code> wrapping around the native Lucene wuery.
	 *
	 * @throws IllegalArgumentException if entityType is <code>null</code> or not a class or superclass annotated with <code>@Indexed</code>.
	 * @deprecated Instead of using Hibernate Search 5 APIs, get a {@link SearchSession}
	 * using {@link org.hibernate.search.mapper.orm.Search#session(EntityManager)},
	 * then create a {@link SearchQuery} with {@link SearchSession#search(Class)}.
	 * If you really need an adapter to Hibernate ORM's {@link Query} type,
	 * convert that {@link SearchQuery} using {@link org.hibernate.search.mapper.orm.Search#toOrmQuery(SearchQuery)},
	 * but be aware that only part of the contract is implemented.
	 * Refer to the <a href="https://hibernate.org/search/documentation/migrate/6.0/">migration guide</a> for more information.
	 */
	@Deprecated
	FullTextQuery createFullTextQuery(org.apache.lucene.search.Query luceneQuery, Class<?>... entities);

	/**
	 * Force the (re)indexing of a given <b>managed</b> object.
	 * Indexation is batched per transaction: if a transaction is active, the operation
	 * will not affect the index at least until commit.
	 *
	 * @param <T> the type of the entity to index
	 * @param entity The entity to index - must not be <code>null</code>.
	 *
	 * @throws IllegalArgumentException if entity is null or not an @Indexed entity
	 * @deprecated Instead of using Hibernate Search 5 APIs, get a {@link SearchSession}
	 * using {@link org.hibernate.search.mapper.orm.Search#session(EntityManager)},
	 * then get the indexing plan for that session using {@link SearchSession#indexingPlan()},
	 * then add/update/remove entities to/from the index using
	 * {@link SearchIndexingPlan#addOrUpdate(Object)},
	 * {@link SearchIndexingPlan#delete(Object)},
	 * or {@link SearchIndexingPlan#purge(Class, Object, String)}.
	 */
	@Deprecated
	<T> void index(T entity);

	/**
	 * @return the <code>SearchFactory</code> instance.
	 * @deprecated See the deprecation note on {@link SearchFactory}.
	 */
	@Deprecated
	SearchFactory getSearchFactory();

	/**
	 * Remove the entity with the type <code>entityType</code> and the identifier <code>id</code> from the index.
	 * If <code>id == null</code> all indexed entities of this type and its indexed subclasses are deleted. In this
	 * case this method behaves like {@link #purgeAll(Class)}.
	 *
	 * @param <T> the type of the entity to purge
	 * @param entityType The type of the entity to delete.
	 * @param id The id of the entity to delete.
	 *
	 * @throws IllegalArgumentException if entityType is <code>null</code> or not a class or superclass annotated with <code>@Indexed</code>.
	 *
	 * @deprecated To purge all instances of a given type, see the deprecation note on {@link #purgeAll(Class)}.
	 * To purge a specific instance, instead of using Hibernate Search 5 APIs, get a {@link SearchSession}
	 * using {@link org.hibernate.search.mapper.orm.Search#session(EntityManager)},
	 * then get the indexing plan for that session using {@link SearchSession#indexingPlan()},
	 * then add/update/remove entities to/from the index using
	 * {@link SearchIndexingPlan#addOrUpdate(Object)},
	 * {@link SearchIndexingPlan#delete(Object)},
	 * or {@link SearchIndexingPlan#purge(Class, Object, String)}.
	 */
	@Deprecated
	<T> void purge(Class<T> entityType, Serializable id);

	/**
	 * Remove all entities from of particular class and all its subclasses from the index.
	 *
	 * @param <T> the type of the entity to purge
	 * @param entityType The class of the entities to remove.
	 *
	 * @throws IllegalArgumentException if entityType is <code>null</code> or not a class or superclass annotated with <code>@Indexed</code>.
	 *
	 * @deprecated Instead of using Hibernate Search 5 APIs, get a {@link SearchScope}
	 * using {@link SearchSession#scope(Class)} or {@link SearchMapping#scope(Class)},
	 * then a {@link SearchWorkspace} using {@link SearchScope#workspace()},
	 * then call {@link SearchWorkspace#purge()} to purge all indexes in scope.
	 */
	@Deprecated
	<T> void purgeAll(Class<T> entityType);

	/**
	 * Flush all index changes forcing Hibernate Search to apply all changes to the index not waiting for the batch limit.
	 * @deprecated Instead of using Hibernate Search 5 APIs, get a {@link SearchSession}
	 * using {@link org.hibernate.search.mapper.orm.Search#session(EntityManager)},
	 * then get the indexing plan for that session using {@link SearchSession#indexingPlan()},
	 * then force the immediate execution of indexing operations
	 * using {@link SearchIndexingPlan#execute()}.
	 */
	@Deprecated
	void flushToIndexes();

	/**
	 * Creates a MassIndexer to rebuild the indexes of some
	 * or all indexed entity types.
	 * Instances cannot be reused.
	 *
	 * @param types optionally restrict the operation to selected types
	 * @return a new MassIndexer
	 * @deprecated Instead of using Hibernate Search 5 APIs, get a {@link SearchSession}
	 * using {@link org.hibernate.search.mapper.orm.Search#session(Session)},
	 * then create a mass indexer with {@link SearchSession#massIndexer(Class[])}.
	 * Refer to the <a href="https://hibernate.org/search/documentation/migrate/6.0/">migration guide</a> for more information.
	 */
	@Deprecated
	MassIndexer createIndexer(Class<?>... types);

}
