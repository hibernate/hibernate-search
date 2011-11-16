/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
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
package org.hibernate.search.spi;

import java.util.Collection;
import java.util.Map;

import org.hibernate.search.backend.spi.Work;

/**
 * When using the Hibernate Core integration (for example) we need to make sure that the
 * entities and collections we're working on are initialized.
 * Initialization strategies might vary according to the integrating framework;
 * when integrating with Infinispan (as Infinispan Query) no initialization is needed.
 *
 * @see org.hibernate.search.impl.SimpleInitializer
 * @author Sanne Grinovero &lt;sanne@hibernate.org&gt; (C) 2011 Red Hat Inc.
 */
public interface InstanceInitializer {

	<T> Class<T> getClassFromWork(Work<T> work);

	/**
	 * @param entity an instance or proxy of T
	 * @return the class from the instance, or the underlying class from a proxy.
	 */
	public <T> Class<T> getClass(T entity);

	/**
	 * @param value the object to unproxy
	 * @return if value is a proxy, unwraps it, otherwise works as a pass-through function.
	 */
	public Object unproxy(Object value);

	/**
	 * @param value the collection to initialize
	 * @return the initialized Collection, to be used on lazily-loading collections
	 */
	public <T> Collection<T> initializeCollection(Collection<T> value);

	/**
	 * @param value the map to initialize
	 * @return the initialized Map, to be used on lazily-loading maps
	 */
	public <K,V> Map<K,V> initializeMap(Map<K,V> value);

	/**
	 * @param value the array to initialize
	 * @return the initialized array, to be used on lazily-loading arrays
	 */
	public Object[] initializeArray(Object[] value);

}
