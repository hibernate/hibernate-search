/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.environment.thread.impl;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ThreadFactory;
import java.util.function.Supplier;

import org.hibernate.search.engine.environment.thread.spi.ThreadProvider;
import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.util.common.annotation.impl.SuppressForbiddenApis;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class EmbeddedThreadProvider implements ThreadProvider {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public static final String NAME = "embedded";
	private static final Supplier<ThreadGroup> THREAD_GROUP_PROVIDER;

	static {
		Supplier<ThreadGroup> provider = null;
		try {
			// if the SM is loaded then it means it is still around and we'll try getting the thread group out of it if it is configured:
			Class<?> smClass = Thread.currentThread().getContextClassLoader().loadClass( "java.lang.SecurityManager" );
			Method getSecurityManager = System.class.getDeclaredMethod( "getSecurityManager" );
			Method getThreadGroup = smClass.getDeclaredMethod( "getThreadGroup" );

			provider = () -> {
				Object sm;
				try {
					sm = getSecurityManager.invoke( null );
					if ( sm != null ) {
						return (ThreadGroup) getThreadGroup.invoke( sm );
					}
				}
				catch (InvocationTargetException | IllegalAccessException e) {
					// shouldn't really happen, but just in case:
					throw log.securityManagerInvocationProblem( e.getMessage(), e );
				}

				return Thread.currentThread().getThreadGroup();
			};
		}
		catch (ClassNotFoundException | NoSuchMethodException e) {
			provider = () -> Thread.currentThread().getThreadGroup();
		}
		THREAD_GROUP_PROVIDER = provider;
	}

	private final String commonThreadNamePrefix;

	public EmbeddedThreadProvider() {
		this( "Hibernate Search - " );
	}

	public EmbeddedThreadProvider(String commonThreadNamePrefix) {
		this.commonThreadNamePrefix = commonThreadNamePrefix;
	}

	@Override
	public String createThreadName(String prefix, int threadNumber) {
		return createFullThreadNamePrefix( prefix ) + threadNumber;
	}

	@Override
	@SuppressForbiddenApis(reason = "It's unclear how we will handle this without the security manager;"
			+ " we'll see when the security manager actually gets removed from the JDK")
	public ThreadFactory createThreadFactory(String prefix) {
		ThreadGroup group = THREAD_GROUP_PROVIDER.get();
		String namePrefix = createFullThreadNamePrefix( prefix );
		return new SimpleThreadFactory( group, namePrefix );
	}

	private String createFullThreadNamePrefix(String prefix) {
		return commonThreadNamePrefix + prefix + " - ";
	}
}
