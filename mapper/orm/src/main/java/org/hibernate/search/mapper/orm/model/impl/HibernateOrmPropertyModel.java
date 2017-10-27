/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.model.impl;

import org.hibernate.property.access.spi.Getter;
import org.hibernate.search.mapper.pojo.model.spi.PropertyHandle;
import org.hibernate.search.mapper.pojo.model.spi.PropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.TypeModel;
import org.hibernate.search.util.SearchException;

public class HibernateOrmPropertyModel<T> implements PropertyModel<T> {

	private final HibernateOrmIntrospector introspector;

	private final TypeModel<?> holderTypeModel;

	private final String name;

	private final Getter getter;

	private PropertyHandle handle;

	private TypeModel<T> typeModel;

	public HibernateOrmPropertyModel(HibernateOrmIntrospector introspector,
			TypeModel<?> holderTypeModel, String name, Getter getter) {
		this.introspector = introspector;
		this.holderTypeModel = holderTypeModel;
		this.name = name;
		this.getter = getter;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Class<T> getJavaType() {
		return getter.getReturnType();
	}

	@Override
	public TypeModel<T> getTypeModel() {
		if ( typeModel == null ) {
			try {
				typeModel = introspector.createUnknownTypeModel( getJavaType() );
			}
			catch (RuntimeException e) {
				throw new SearchException( "Exception while retrieving property type model for '"
						+ getName() + "' on '" + holderTypeModel.getJavaType() + "'", e );
			}
		}
		return typeModel;
	}

	@Override
	public PropertyHandle getHandle() {
		if ( handle == null ) {
			handle = new GetterPropertyHandle( getter );
		}
		return handle;
	}
}
