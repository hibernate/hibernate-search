/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.search.engine.mapper.mapping.spi.MappingImplementor;
import org.hibernate.search.engine.mapper.mapping.spi.MappingPartialBuildState;
import org.hibernate.search.mapper.orm.mapping.spi.HibernateOrmMapping;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;

public class HibernateOrmMappingPartialBuildState implements MappingPartialBuildState {

	private final PojoMappingDelegate mappingDelegate;

	public HibernateOrmMappingPartialBuildState(PojoMappingDelegate mappingDelegate) {
		this.mappingDelegate = mappingDelegate;
	}

	public MappingImplementor<HibernateOrmMapping> bindToSessionFactory(SessionFactoryImplementor sessionFactoryImplementor) {
		return new HibernateOrmMappingImpl( mappingDelegate, sessionFactoryImplementor );
	}

	@Override
	public void closeOnFailure() {
		mappingDelegate.close();
	}
}
