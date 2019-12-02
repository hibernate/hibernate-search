/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.common.impl;

import java.lang.invoke.MethodHandles;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class HibernateOrmUtils {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private HibernateOrmUtils() {
	}

	public static SessionFactoryImplementor toSessionFactoryImplementor(EntityManagerFactory entityManagerFactory) {
		try {
			return entityManagerFactory.unwrap( SessionFactoryImplementor.class );
		}
		catch (IllegalStateException e) {
			throw log.hibernateSessionFactoryAccessError( e );
		}
	}

	public static SessionImplementor toSessionImplementor(EntityManager entityManager) {
		try {
			return entityManager.unwrap( SessionImplementor.class );
		}
		catch (IllegalStateException e) {
			throw log.hibernateSessionAccessError( e );
		}
	}

	public static boolean isSuperTypeOf(EntityPersister type1, EntityPersister type2) {
		return type1.isSubclassEntityName( type2.getEntityName() );
	}
}
