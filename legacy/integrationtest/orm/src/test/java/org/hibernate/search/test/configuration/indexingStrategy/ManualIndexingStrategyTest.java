/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration.indexingStrategy;

import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.Session;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.test.SearchTestBase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public class ManualIndexingStrategyTest extends SearchTestBase {

	@Test
	public void testMultipleEntitiesPerIndex() throws Exception {
		indexTestEntity();
		assertEquals(
				"Due to manual indexing being enabled no automatic indexing should have occurred",
				0,
				getNumberOfDocumentsInIndex( TestEntity.class )
		);
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { TestEntity.class };
	}

	@Override
	public void configure(Map<String,Object> cfg) {
		cfg.put( Environment.INDEXING_STRATEGY, "manual" );
	}

	private void indexTestEntity() {
		Session session = getSessionFactory().openSession();
		session.getTransaction().begin();

		session.persist( new TestEntity() );

		session.getTransaction().commit();
		session.close();
	}

	@Indexed
	@Entity
	@Table(name = "TestEntity")
	public static class TestEntity {

		@Id
		@GeneratedValue
		private int id;
	}
}
