/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.engine.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.SearchException;
import org.hibernate.search.annotations.FilterCacheModeType;
import org.hibernate.search.annotations.FullTextFilterDef;
import org.hibernate.search.util.impl.ReflectionHelper;

/**
 * A wrapper class which encapsulates all required information to create a defined filter.
 *
 * @author Emmanuel Bernard
 */
//TODO serialization
public class FilterDef {
	private Method factoryMethod;
	private Method keyMethod;
	private Map<String, Method> setters = new HashMap<String, Method>();
	private final FilterCacheModeType cacheMode;
	private final Class<?> impl;
	private final String name;

	public FilterDef(FullTextFilterDef def) {
		this.name = def.name();
		this.impl = def.impl();
		this.cacheMode = def.cache();
	}

	public String getName() {
		return name;
	}

	public FilterCacheModeType getCacheMode() {
		return cacheMode;
	}

	public Class<?> getImpl() {
		return impl;
	}

	public Method getFactoryMethod() {
		return factoryMethod;
	}

	public void setFactoryMethod(Method factoryMethod) {
		this.factoryMethod = factoryMethod;
	}

	public Method getKeyMethod() {
		return keyMethod;
	}

	public void setKeyMethod(Method keyMethod) {
		this.keyMethod = keyMethod;
	}

	public void addSetter(String name, Method method) {
		ReflectionHelper.setAccessible( method );
		setters.put( name, method );
	}

	public void invoke(String parameterName, Object filter, Object parameterValue) {
		Method method = setters.get( parameterName );
		if ( method == null ) {
			throw new SearchException( "No setter " + parameterName + " found in " + this.impl );
		}
		try {
			method.invoke( filter, parameterValue );
		}
		catch (IllegalAccessException e) {
			throw new SearchException( "Unable to set Filter parameter: " + parameterName + " on filter class: " + this.impl, e );
		}
		catch (InvocationTargetException e) {
			throw new SearchException( "Unable to set Filter parameter: " + parameterName + " on filter class: " + this.impl, e );
		}
		catch (IllegalArgumentException e) {
			throw new SearchException( "Unable to set Filter parameter: " + parameterName + " on filter class: "
					+ this.impl + " : " + e.getMessage(), e );
		}
	}
}
