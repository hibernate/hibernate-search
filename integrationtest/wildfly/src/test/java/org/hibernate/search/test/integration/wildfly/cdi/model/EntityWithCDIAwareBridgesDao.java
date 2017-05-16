/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.wildfly.cdi.model;

import java.util.List;

import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaDelete;
import javax.transaction.Transactional;

import org.apache.lucene.search.Query;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.dsl.QueryBuilder;

/**
 * @author Yoann Rodiere
 */
@Singleton
public class EntityWithCDIAwareBridgesDao {
	@PersistenceContext
	private EntityManager entityManager;

	@Transactional
	public void create(EntityWithCDIAwareBridges entity) {
		entityManager.persist( entity );
	}

	@Transactional
	public void update(EntityWithCDIAwareBridges entity) {
		entityManager.merge( entity );
	}

	@Transactional
	public void delete(EntityWithCDIAwareBridges entity) {
		entity = entityManager.merge( entity );
		entityManager.remove( entity );
	}

	@Transactional
	public void deleteAll() {
		CriteriaDelete<EntityWithCDIAwareBridges> delete = entityManager.getCriteriaBuilder()
				.createCriteriaDelete( EntityWithCDIAwareBridges.class );
		delete.from( EntityWithCDIAwareBridges.class );
		entityManager.createQuery( delete ).executeUpdate();
	}

	@Transactional
	@SuppressWarnings("unchecked")
	public List<EntityWithCDIAwareBridges> searchFieldBridge(String terms) {
		FullTextEntityManager ftEntityManager = Search.getFullTextEntityManager( entityManager );
		QueryBuilder queryBuilder = ftEntityManager.getSearchFactory().buildQueryBuilder()
				.forEntity( EntityWithCDIAwareBridges.class ).get();
		Query luceneQuery = queryBuilder.keyword()
				.onField( "internationalizedValue" )
				.ignoreFieldBridge()
				.matching( terms )
				.createQuery();
		FullTextQuery query = ftEntityManager.createFullTextQuery( luceneQuery, EntityWithCDIAwareBridges.class );
		return query.getResultList();
	}

	@Transactional
	@SuppressWarnings("unchecked")
	public List<EntityWithCDIAwareBridges> searchClassBridge(String terms) {
		FullTextEntityManager ftEntityManager = Search.getFullTextEntityManager( entityManager );
		QueryBuilder queryBuilder = ftEntityManager.getSearchFactory().buildQueryBuilder()
				.forEntity( EntityWithCDIAwareBridges.class ).get();
		Query luceneQuery = queryBuilder.keyword()
				.onField( EntityWithCDIAwareBridges.CLASS_BRIDGE_FIELD_NAME )
				.ignoreFieldBridge()
				.matching( terms )
				.createQuery();
		FullTextQuery query = ftEntityManager.createFullTextQuery( luceneQuery, EntityWithCDIAwareBridges.class );
		return query.getResultList();
	}
}