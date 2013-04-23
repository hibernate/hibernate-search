/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2008-2012 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.search.impl;

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
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 *
 * @see org.hibernate.annotations.common.annotationfactory.AnnotationProxy
 */
//FIXME remove when HSEARCH-1085 is fixed
class AnnotationFactory {
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
			SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException,
			IllegalAccessException, InvocationTargetException {
		Constructor<T> constructor = proxyClass.getConstructor( new Class[]{InvocationHandler.class} );
		return constructor.newInstance( new Object[]{handler} );
	}
}
