/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration.indexingStrategy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.Session;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.test.SearchTestBase;

import org.junit.jupiter.api.Test;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
class ManualIndexingStrategyTest extends SearchTestBase {

	@Test
	void testMultipleEntitiesPerIndex() {
		indexTestEntity();
		assertThat( getNumberOfDocumentsInIndex( "TestEntity" ) )
				.as( "Due to manual indexing being enabled no listener-triggered indexing should have occurred" )
				.isZero();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { TestEntity.class };
	}

	@Override
	public void configure(Map<String, Object> cfg) {
		cfg.put( HibernateOrmMapperSettings.INDEXING_LISTENERS_ENABLED, false );
	}

	private void indexTestEntity() {
		Session session = getSessionFactory().openSession();
		session.getTransaction().begin();

		session.persist( new TestEntity() );

		session.getTransaction().commit();
		session.close();
	}

	@Indexed(index = "TestEntity")
	@Entity
	@Table(name = "TestEntity")
	public static class TestEntity {

		@Id
		@GeneratedValue
		private int id;
	}
}
