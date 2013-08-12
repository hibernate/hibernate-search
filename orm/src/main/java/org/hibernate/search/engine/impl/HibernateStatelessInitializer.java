/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

package org.hibernate.search.engine.impl;

import java.util.Collection;
import java.util.Map;

import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.spi.InstanceInitializer;
import org.hibernate.search.util.impl.HibernateHelper;

/**
 * To be used for Hibernate initializations which don't need a specific Session.
 * {@link #initializeCollection(Collection)} and {@link #initializeMap(Map)}
 * are not supported.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class HibernateStatelessInitializer implements InstanceInitializer {

	public static final HibernateStatelessInitializer INSTANCE = new HibernateStatelessInitializer();

	protected HibernateStatelessInitializer() {
		// should not create instances, but allow for extension
	}

	@Override
	public <T> Class<T> getClass(T entity) {
		return HibernateHelper.getClass( entity );
	}

	@Override
	public Object unproxy(Object instance) {
		return HibernateHelper.unproxy( instance );
	}

	@Override
	public <T> Collection<T> initializeCollection(Collection<T> value) {
		// supports pass-through only, would need HibernateSessionLoadingInitializer
		return value;
	}

	@Override
	public <K, V> Map<K, V> initializeMap(Map<K, V> value) {
		// supports pass-through only, would need HibernateSessionLoadingInitializer
		return value;
	}

	@Override
	public Object[] initializeArray(Object[] value) {
		// hibernate doesn't allow lazy initialization of arrays,
		// so this must be initialized already.
		return value;
	}

	@Override
	public <T> Class<T> getClassFromWork(Work<T> work) {
		return HibernateHelper.getClassFromWork( work );
	}

}
