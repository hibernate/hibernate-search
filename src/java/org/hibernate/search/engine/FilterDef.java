// $Id$
package org.hibernate.search.engine;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.SearchException;
import org.hibernate.search.annotations.CacheBitResults;

/**
 * A wrapper class which encapsualtes all required information to create a defined filter.
 * 
 * @author Emmanuel Bernard
 */
//TODO serialization
@SuppressWarnings("unchecked")
public class FilterDef {
	private Class impl;
	private Method factoryMethod;
	private Method keyMethod;
	private Map<String, Method> setters = new HashMap<String, Method>();
	private boolean cache;
	private CacheBitResults useCachingWrapperFilter;

	public CacheBitResults getUseCachingWrapperFilter() {
		return useCachingWrapperFilter;
	}

	public void setUseCachingWrapperFilter(
			CacheBitResults useCachingWrapperFilter) {
		this.useCachingWrapperFilter = useCachingWrapperFilter;
	}

	public Class getImpl() {
		return impl;
	}

	public void setImpl(Class impl) {
		this.impl = impl;
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
		if ( method.isAccessible() ) method.setAccessible( true );
		setters.put( name, method );
	}

	public void invoke(String parameterName, Object filter, Object parameterValue) {
		Method method = setters.get( parameterName );
		if ( method == null ) throw new SearchException( "No setter " + parameterName + " found in " + this.impl );
		try {
			method.invoke( filter, parameterValue );
		}
		catch (IllegalAccessException e) {
			throw new SearchException( "Unable to set Filter parameter: " + parameterName + " on filter class: " + this.impl, e );
		}
		catch (InvocationTargetException e) {
			throw new SearchException( "Unable to set Filter parameter: " + parameterName + " on filter class: " + this.impl, e );
		}
	}

	public void setCache(boolean cache) {
		this.cache = cache;
	}

	public boolean isCache() {
		return cache;
	}
}
