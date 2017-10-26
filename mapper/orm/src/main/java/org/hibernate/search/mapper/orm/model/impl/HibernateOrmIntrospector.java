/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.model.impl;

import javax.persistence.EntityManagerFactory;

import org.hibernate.search.mapper.pojo.model.spi.PojoIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.TypeModel;

/**
 * @author Yoann Rodiere
 */
public class HibernateOrmIntrospector implements PojoIntrospector {

	public HibernateOrmIntrospector(EntityManagerFactory entityManagerFactory) {
	}

	@Override
	public <T> TypeModel<T> getEntityTypeModel(Class<T> type) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException( "Not implemented yet" );
	}

}
