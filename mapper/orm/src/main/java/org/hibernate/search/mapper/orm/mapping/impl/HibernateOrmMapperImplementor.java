/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import org.hibernate.search.mapper.orm.model.impl.HibernateOrmIntrospector;
import org.hibernate.search.mapper.orm.model.impl.HibernateOrmProxyIntrospector;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMapperImplementor;


/**
 * @author Yoann Rodiere
 */
public final class HibernateOrmMapperImplementor extends PojoMapperImplementor {

	private static final HibernateOrmMapperImplementor INSTANCE = new HibernateOrmMapperImplementor();

	private HibernateOrmMapperImplementor() {
		super( HibernateOrmIntrospector.get(), HibernateOrmProxyIntrospector.get(), false );
	}

	public static HibernateOrmMapperImplementor get() {
		return INSTANCE;
	}

}
