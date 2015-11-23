/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.List;

import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XMember;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.search.annotations.Factory;
import org.hibernate.search.util.StringHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public abstract class ReflectionHelper {
	private static final Log LOG = LoggerFactory.make();
	private static final Object[] EMPTY_ARRAY = new Object[0];

	private ReflectionHelper() {
	}

	/**
	 * Get attribute name out of member unless overridden by <code>name</code>.
	 *
	 * @param member <code>XMember</code> from which to extract the name.
	 * @param name Override value which will be returned in case it is not empty.
	 *
	 * @return attribute name out of member unless overridden by <code>name</code>.
	 */
	public static String getAttributeName(XMember member, String name) {
		return StringHelper.isNotEmpty( name ) ?
				name :
				member.getName(); //explicit field name
	}

	/**
	 * Always use this method to set accessibility regardless of the visibility.
	 * @param member the {@link XMember} to check
	 */
	public static void setAccessible(XMember member) {
		try {
			// always set accessible to true as it bypass the security model checks
			// at execution time and is faster.
			member.setAccessible( true );
		}
		catch (SecurityException se) {
			if ( !Modifier.isPublic( member.getModifiers() ) ) {
				throw se;
			}
		}
	}

	/**
	 * Always use this method to set accessibility regardless of the visibility.
	 * @param member the {@link AccessibleObject} to change
	 */
	public static void setAccessible(AccessibleObject member) {
		try {
			// always set accessible to true as it bypass the security model checks
			// at execution time and is faster.
			member.setAccessible( true );
		}
		catch (SecurityException se) {
			if ( !Modifier.isPublic( ( (Member) member ).getModifiers() ) ) {
				throw se;
			}
		}
	}

	public static Object getMemberValue(Object bean, XMember getter) {
		Object value;
		try {
			//EMPTY_ARRAY used to avoid excessive allocations of empty arrays as
			//the invoke method accepts varargs
			value = getter.invoke( bean, EMPTY_ARRAY );
		}
		catch (Exception e) {
			throw new IllegalStateException( "Could not get property value", e );
		}
		return value;
	}

	/**
	 * Creates the class hierarchy for a given {@code XClass}.
	 *
	 * @param clazz the class for which to create the hierarchy
	 *
	 * @return the list of classes in the hierarchy starting at {@code java.lang.Object}
	 */
	public static List<XClass> createXClassHierarchy(XClass clazz) {
		List<XClass> hierarchy = new LinkedList<>();
		XClass next;
		for ( XClass previousClass = clazz; previousClass != null; previousClass = next ) {
			next = previousClass.getSuperclass();
			if ( next != null ) {
				hierarchy.add( 0, previousClass ); // append to head to create a list in top-down iteration order
			}
		}
		return hierarchy;
	}

	/**
	 * Checks whether the specified class contains any Search specific annotations.
	 *
	 * @param mappedClass the {@code XClass} to check for Search annotations
	 *
	 * @return Returns {@code true} if the class contains at least one Search annotation, {@code false} otherwise
	 */
	public static boolean containsSearchAnnotations(XClass mappedClass) {
		// check the type annotations
		if ( containsSearchAnnotation( mappedClass.getAnnotations() ) ) {
			return true;
		}

		for ( XProperty method : mappedClass.getDeclaredProperties( XClass.ACCESS_PROPERTY ) ) {
			if ( containsSearchAnnotation( method.getAnnotations() ) ) {
				return true;
			}
		}

		for ( XProperty field : mappedClass.getDeclaredProperties( XClass.ACCESS_FIELD ) ) {
			if ( containsSearchAnnotation( field.getAnnotations() ) ) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Checks if the annotation is a Search annotation by comparing the package of the annotation.
	 *
	 * @param annotation the annotation to check
	 *
	 * @return Returns {@code true} if the annotation is a Search annotation, {@code false} otherwise
	 */
	public static boolean isSearchAnnotation(Annotation annotation) {
		return "org.hibernate.search.annotations".equals( annotation.annotationType().getPackage().getName() );
	}


	/**
	 * Creates an instance of the specified class and returns it. If {@code checkForFactoryAnnotation == true}, the created
	 * instance is checked for a {@code @Factory} annotated method. If one exists the factory method is invoked and the
	 * return value returns as expected instances.
	 *
	 * @param clazz the class to instantiate
	 * @param checkForFactoryAnnotation  whether to check for {@code @Factory} annotated methods
	 * @return the created instance
	 */
	public static Object createInstance(Class<?> clazz, boolean checkForFactoryAnnotation) {
		// create the instance
		Object instance;
		try {
			instance = clazz.newInstance();
		}
		catch (InstantiationException e) {
			throw LOG.noPublicNoArgConstructor( clazz.getName() );
		}
		catch (IllegalAccessException e) {
			throw LOG.unableToAccessClass( clazz.getName() );
		}

		if ( !checkForFactoryAnnotation ) {
			return instance;
		}

		// check for a factory annotation
		int numberOfFactoryMethodsFound = 0;
		for ( Method method : clazz.getMethods() ) {
			if ( method.isAnnotationPresent( Factory.class ) ) {
				if ( numberOfFactoryMethodsFound == 1 ) {
					throw LOG.multipleFactoryMethodsInClass( clazz.getName() );
				}
				if ( method.getReturnType() == void.class ) {
					throw LOG.factoryMethodsMustReturnAnObject( clazz.getName(), method.getName() );
				}
				try {
					instance = method.invoke( instance );
				}
				catch (IllegalAccessException e) {
					throw LOG.unableToAccessMethod( clazz.getName(), method.getName() );
				}
				catch (InvocationTargetException e) {
					throw LOG.exceptionDuringFactoryMethodExecution( e, clazz.getName(), method.getName() );
				}
				numberOfFactoryMethodsFound++;
			}
		}
		return instance;
	}

	/**
	 * Checks whether the specified type is a floating point type.
	 *
	 * @param type the type to check
	 * @return {@code true} if the specified type is an floating point type or a wrapper thereof. {@code false} otherwise.
	 */
	public static boolean isFloatingPointType(Class<?> type) {
		return float.class.equals( type )
				|| Float.class.equals( type )
				|| double.class.equals( type )
				|| Double.class.equals( type );
	}

	/**
	 * Checks whether the specified type is a integer type.
	 *
	 * @param type the type to check
	 * @return {@code true} if the specified type is an primitive integer type or a wrapper thereof. {@code false} otherwise.
	 */
	public static boolean isIntegerType(Class<?> type) {
		return byte.class.equals( type )
				|| Byte.class.equals( type )
				|| short.class.equals( type )
				|| Short.class.equals( type )
				|| int.class.equals( type )
				|| Integer.class.equals( type )
				|| long.class.equals( type )
				|| Long.class.equals( type );
	}

	private static boolean containsSearchAnnotation(Annotation[] annotations) {
		for ( Annotation annotation : annotations ) {
			if ( isSearchAnnotation( annotation ) ) {
				return true;
			}
		}
		return false;
	}
}
