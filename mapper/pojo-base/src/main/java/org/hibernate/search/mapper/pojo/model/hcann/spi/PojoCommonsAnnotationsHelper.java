/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.hcann.spi;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.annotations.common.reflection.java.JavaXMember;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * Allows to access to the {@link XProperty} private field {@link Member}.
 * <p>
 * Copied and adapted from {@code org.hibernate.cfg.annotations.HCANNHelper}
 * of <a href="https://github.com/hibernate/hibernate-orm">Hibernate ORM project</a>.
 */
public class PojoCommonsAnnotationsHelper {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private PojoCommonsAnnotationsHelper() {
	}

	private static Method getMemberMethod;

	static {
		// The following is in a static block to avoid problems lazy-initializing
		// and making accessible in a multi-threaded context. See HHH-11289.
		final Class<?> javaXMemberClass = JavaXMember.class;
		try {
			getMemberMethod = javaXMemberClass.getDeclaredMethod( "getMember" );
			// NOTE : no need to check accessibility here - we know it is protected
			getMemberMethod.setAccessible( true );
		}
		catch (NoSuchMethodException e) {
			throw new AssertionFailure(
					"Could not resolve JavaXMember#getMember method in order to access XProperty member signature",
					e
			);
		}
		catch (Exception e) {
			throw log.cannotAccessPropertyMember( e );
		}
	}

	public static Member getUnderlyingMember(XProperty xProperty) {
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

	public static Method getUnderlyingMethod(XProperty xProperty) {
		Member member = getUnderlyingMember( xProperty );
		if ( !( member instanceof Method ) ) {
			throw log.cannotAccessPropertyMethod( xProperty.getName() );
		}

		return (Method) member;
	}
}
