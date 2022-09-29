/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.model.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathDefinition;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathDefinitionProvider;

/**
 * A {@link PojoPathDefinitionProvider} suitable for use with Hibernate ORM,
 * in particular with its event system.
 * <p>
 * Paths passed to this factory are assigned a string representation so as to match the property names
 * and collection roles from Hibernate ORM.
 *
 * @see HibernateOrmPathInterpreter
 */
public class HibernateOrmPathDefinitionProvider implements PojoPathDefinitionProvider {

	private final PersistentClass persistentClass;
	private final HibernateOrmPathInterpreter interpreter = new HibernateOrmPathInterpreter();

	public HibernateOrmPathDefinitionProvider(PersistentClass persistentClass) {
		this.persistentClass = persistentClass;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<String> preDefinedOrdinals() {
		List<String> preDefinedOrdinals = new ArrayList<>();
		for ( Iterator<Property> iterator = persistentClass.getPropertyClosureIterator(); iterator.hasNext(); ) {
			Property property = iterator.next();
			preDefinedOrdinals.add( property.getName() );
		}
		return preDefinedOrdinals;
	}

	@Override
	public PojoPathDefinition interpretPath(PojoModelPathValueNode source) {
		return interpreter.interpretPath( persistentClass, source );
	}

}
