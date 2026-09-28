package org.hibernate.search.mapper.orm.mapping.context;

import org.hibernate.SessionFactory;

public interface HibernateOrmMappingContext {

	/**
	 * @return The Hibernate ORM {@link SessionFactory}.
	 */
	SessionFactory sessionFactory();

}
