/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.spring.injection.model;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.lucene.search.Query;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Yoann Rodiere
 */
@Repository
public class EntityWithSpringAwareBridgesDao {
	@PersistenceContext
	private EntityManager entityManager;

	@Transactional
	public void create(EntityWithSpringAwareBridges entity) {
		entityManager.persist( entity );
	}

	@Transactional
	public void update(EntityWithSpringAwareBridges entity) {
		entityManager.merge( entity );
	}

	@Transactional
	public void delete(EntityWithSpringAwareBridges entity) {
		entity = entityManager.merge( entity );
		entityManager.remove( entity );
	}

	@Transactional
	@SuppressWarnings("unchecked")
	public List<EntityWithSpringAwareBridges> searchFieldBridge(String terms) {
		FullTextEntityManager ftEntityManager = Search.getFullTextEntityManager( entityManager );
		QueryBuilder queryBuilder = ftEntityManager.getSearchFactory().buildQueryBuilder()
				.forEntity( EntityWithSpringAwareBridges.class ).get();
		Query luceneQuery = queryBuilder.keyword()
				.onField( "internationalizedValue" )
				.ignoreFieldBridge()
				.matching( terms )
				.createQuery();
		FullTextQuery query = ftEntityManager.createFullTextQuery( luceneQuery, EntityWithSpringAwareBridges.class );
		return query.getResultList();
	}

	@Transactional
	@SuppressWarnings("unchecked")
	public List<EntityWithSpringAwareBridges> searchClassBridge(String terms) {
		FullTextEntityManager ftEntityManager = Search.getFullTextEntityManager( entityManager );
		QueryBuilder queryBuilder = ftEntityManager.getSearchFactory().buildQueryBuilder()
				.forEntity( EntityWithSpringAwareBridges.class ).get();
		Query luceneQuery = queryBuilder.keyword()
				.onField( EntityWithSpringAwareBridges.CLASS_BRIDGE_FIELD_NAME )
				.ignoreFieldBridge()
				.matching( terms )
				.createQuery();
		FullTextQuery query = ftEntityManager.createFullTextQuery( luceneQuery, EntityWithSpringAwareBridges.class );
		return query.getResultList();
	}

	@Transactional
	@SuppressWarnings("unchecked")
	public List<EntityWithSpringAwareBridges> searchNonSpringBridge(String terms) {
		FullTextEntityManager ftEntityManager = Search.getFullTextEntityManager( entityManager );
		QueryBuilder queryBuilder = ftEntityManager.getSearchFactory().buildQueryBuilder()
				.forEntity( EntityWithSpringAwareBridges.class ).get();
		Query luceneQuery = queryBuilder.keyword()
				.onField( EntityWithSpringAwareBridges.NON_SPRING_BRIDGE_FIELD_NAME )
				.ignoreFieldBridge()
				.matching( terms )
				.createQuery();
		FullTextQuery query = ftEntityManager.createFullTextQuery( luceneQuery, EntityWithSpringAwareBridges.class );
		return query.getResultList();
	}
}