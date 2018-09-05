/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.massindexing;

import java.util.ArrayList;
import java.util.List;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;

import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.jsr352.massindexing.test.entity.Company;
import org.hibernate.search.jsr352.massindexing.test.entity.Person;
import org.hibernate.search.jsr352.massindexing.test.entity.WhoAmI;
import org.hibernate.search.jsr352.test.util.PersistenceUnitTestUtil;

import org.junit.After;
import org.junit.Before;

/**
 * @author Mincong Huang
 */
public abstract class AbstractBatchIndexingIT {

	private static final String PERSISTENCE_UNIT_NAME = PersistenceUnitTestUtil.getPersistenceUnitName();

	protected static final int INSTANCES_PER_DATA_TEMPLATE = 100;

	// We have three data templates per entity type (see setup)
	protected static final int INSTANCE_PER_ENTITY_TYPE = INSTANCES_PER_DATA_TEMPLATE * 3;

	protected JobOperator jobOperator = BatchRuntime.getJobOperator();
	protected EntityManagerFactory emf;

	@Before
	public void setup() {
		List<Company> companies = new ArrayList();
		List<Person> people = new ArrayList();
		List<WhoAmI> whos = new ArrayList();
		for ( int i = 0 ; i < INSTANCE_PER_ENTITY_TYPE; i += 3 ) {
			int index1 = i;
			int index2 = i + 1;
			int index3 = i + 2;
			companies.add( new Company( "Google " + index1 ) );
			companies.add( new Company( "Red Hat " + index2 ) );
			companies.add( new Company( "Microsoft " + index3 ) );
			people.add( new Person( "BG " + index1, "Bill", "Gates" ) );
			people.add( new Person( "LT " + index2, "Linus", "Torvalds" ) );
			people.add( new Person( "SJ " + index3, "Steven", "Jobs" ) );
			whos.add( new WhoAmI( "cid01 " + index1, "id01 " + index1, "uid01 " + index1 ) );
			whos.add( new WhoAmI( "cid02 " + index2, "id02 " + index2, "uid02 " + index2 ) );
			whos.add( new WhoAmI( "cid03 " + index3, "id03 " + index3, "uid03 " + index3 ) );
		}
		EntityManager em = null;

		try {
			emf = Persistence.createEntityManagerFactory( getPersistenceUnitName() );
			em = emf.createEntityManager();
			em.getTransaction().begin();
			companies.forEach( em::persist );
			people.forEach( em::persist );
			whos.forEach( em::persist );
			em.getTransaction().commit();
		}
		finally {
			if ( em != null ) {
				em.close();
			}
		}
	}

	@After
	public void shutdown() {
		emf.close();
	}

	protected String getPersistenceUnitName() {
		return PERSISTENCE_UNIT_NAME;
	}

	protected final void indexSomeCompanies(int count) {
		EntityManager em = null;
		try {
			em = emf.createEntityManager();
			em.getTransaction().begin();
			CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
			CriteriaQuery<Company> criteria = criteriaBuilder.createQuery( Company.class );
			Root<Company> root = criteria.from( Company.class );
			Path<Integer> id = root.get( root.getModel().getId( int.class ) );
			criteria.orderBy( criteriaBuilder.asc( id ) );
			List<Company> companies = em.createQuery( criteria ).setMaxResults( count ).getResultList();
			FullTextEntityManager ftEm = Search.getFullTextEntityManager( em );
			companies.forEach( ftEm::index );
			em.getTransaction().commit();
		}
		finally {
			if ( em != null ) {
				em.close();
			}
		}
	}
}
