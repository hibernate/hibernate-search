/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.model.impl;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathDefinition;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathDefinitionProvider;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;

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

	private final PojoRawTypeModel<?> typeModel;
	private final PersistentClass persistentClass;
	private final List<String> propertyStringRepresentationByOrdinal;
	private final HibernateOrmPathInterpreter interpreter = new HibernateOrmPathInterpreter();

	@SuppressWarnings("unchecked")
	public HibernateOrmPathDefinitionProvider(PojoRawTypeModel<?> typeModel, PersistentClass persistentClass) {
		this.typeModel = typeModel;
		this.persistentClass = persistentClass;
		this.propertyStringRepresentationByOrdinal = new ArrayList<>();
		for ( Property property : persistentClass.getPropertyClosure() ) {
			propertyStringRepresentationByOrdinal.add( property.getName() );
		}
	}

	@Override
	public List<String> preDefinedOrdinals() {
		return propertyStringRepresentationByOrdinal;
	}

	@Override
	public PojoPathDefinition interpretPath(PojoModelPathValueNode source) {
		return interpreter.interpretPath( typeModel, persistentClass, propertyStringRepresentationByOrdinal, source );
	}

}
