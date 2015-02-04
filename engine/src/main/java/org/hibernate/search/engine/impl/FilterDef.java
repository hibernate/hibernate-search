/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.annotations.FilterCacheModeType;
import org.hibernate.search.annotations.FullTextFilterDef;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.util.impl.ReflectionHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * A wrapper class which encapsulates all required information to create a defined filter.
 *
 * @author Emmanuel Bernard
 */
//TODO serialization
public class FilterDef {

	private static final Log LOG = LoggerFactory.make();

	private Method factoryMethod;
	private Method keyMethod;
	private final Map<String, Method> setters = new HashMap<String, Method>();
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
		catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
			throw LOG.unableToSetFilterParameter( impl, parameterName, e );
		}
	}

	@Override
	public String toString() {
		return "FilterDef [name=" + name + ", impl=" + impl + ", cacheMode=" + cacheMode + ", factoryMethod="
				+ ( factoryMethod != null ? factoryMethod.getName() : null ) + ", keyMethod=" + ( keyMethod != null ? keyMethod.getName() : null )
				+ ", setters=" + setters.keySet() + "]";
	}
}
