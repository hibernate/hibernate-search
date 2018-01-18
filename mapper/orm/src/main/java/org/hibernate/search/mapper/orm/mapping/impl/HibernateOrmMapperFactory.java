/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.mapper.orm.model.impl.HibernateOrmIntrospector;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMapperFactory;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;


/**
 * @author Yoann Rodiere
 */
public final class HibernateOrmMapperFactory extends PojoMapperFactory<HibernateOrmMappingImpl> {

	private final SessionFactoryImplementor sessionFactoryImplementor;

	public HibernateOrmMapperFactory(SessionFactoryImplementor sessionFactoryImplementor) {
		super( new HibernateOrmIntrospector( sessionFactoryImplementor ), false );
		this.sessionFactoryImplementor = sessionFactoryImplementor;
	}

	@Override
	protected HibernateOrmMappingImpl createMapping(ConfigurationPropertySource propertySource,
			PojoMappingDelegate mappingDelegate) {
		return new HibernateOrmMappingImpl( mappingDelegate, sessionFactoryImplementor );
	}
}
