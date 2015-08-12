/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.impl;

import org.hibernate.annotations.common.annotationfactory.AnnotationDescriptor;
import org.hibernate.annotations.common.annotationfactory.AnnotationProxy;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;

/**
 * Creates live annotations (actually <code>AnnotationProxies</code>) from <code>AnnotationDescriptors</code>.
 *
 * @author Paolo Perrotta
 * @author Davide Marchignoli
 * @author Emmanuel Bernard
 *
 * @see org.hibernate.annotations.common.annotationfactory.AnnotationProxy
 */
//FIXME remove when HSEARCH-1085 is fixed
class AnnotationFactory {

	private AnnotationFactory() {
		//now allowed
	}

	@SuppressWarnings("unchecked")
	public static <T extends Annotation> T create(AnnotationDescriptor descriptor, ClassLoader classLoader) {
		//TODO round 34ms to generate the proxy, hug! is Javassist Faster?
		//TODO prebuild the javax.persistence and org.hibernate.annotations classes?
		Class<T> proxyClass = (Class<T>) Proxy.getProxyClass( classLoader, descriptor.type() );
		InvocationHandler handler = new AnnotationProxy( descriptor );
		try {
			return getProxyInstance( proxyClass, handler );
		}
		catch (RuntimeException e) {
			throw e;
		}
		catch (Exception e) {
			throw new RuntimeException( e );
		}
	}

	private static <T extends Annotation> T getProxyInstance(Class<T> proxyClass, InvocationHandler handler) throws
			NoSuchMethodException, InstantiationException,
			IllegalAccessException, InvocationTargetException {
		Constructor<T> constructor = proxyClass.getConstructor( new Class[]{InvocationHandler.class} );
		return constructor.newInstance( new Object[]{handler} );
	}
}
