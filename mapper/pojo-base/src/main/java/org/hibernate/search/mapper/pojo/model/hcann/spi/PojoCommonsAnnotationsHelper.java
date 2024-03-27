/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.hcann.spi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.annotations.common.reflection.java.JavaXMember;
import org.hibernate.search.util.common.AssertionFailure;

/**
 * Allows to access to the {@link XProperty} private field {@link Member}.
 * <p>
 * Copied and adapted from {@code org.hibernate.cfg.annotations.HCANNHelper}
 * of <a href="https://github.com/hibernate/hibernate-orm">Hibernate ORM project</a>.
 */
public class PojoCommonsAnnotationsHelper {

	private PojoCommonsAnnotationsHelper() {
	}

	private static final Method getMemberMethod;

	static {
		// The following is in a static block to avoid problems lazy-initializing
		// and making accessible in a multi-threaded context. See HHH-11289.
		final Class<?> javaXMemberClass = JavaXMember.class;
		try {
			getMemberMethod = javaXMemberClass.getDeclaredMethod( "getMember" );
			// NOTE : no need to check accessibility here - we know it is protected
			getMemberMethod.setAccessible( true );
		}
		catch (Exception e) {
			throw new AssertionFailure(
					"Could not resolve JavaXMember#getMember method in order to extract Java Member from XProperty",
					e
			);
		}
	}

	public static Member extractUnderlyingMember(XProperty xProperty) {
		try {
			return (Member) getMemberMethod.invoke( xProperty );
		}
		catch (IllegalAccessException e) {
			throw new AssertionFailure(
					"Could not resolve member signature from XProperty reference",
					e
			);
		}
		catch (InvocationTargetException e) {
			throw new AssertionFailure(
					"Could not resolve member signature from XProperty reference",
					e.getCause()
			);
		}
	}
}
