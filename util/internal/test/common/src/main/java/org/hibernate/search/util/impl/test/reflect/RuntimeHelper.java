/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.test.reflect;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public final class RuntimeHelper {

	private static final CallerClassWalker CALLER_CLASS_WALKER;

	static {
		Class<?> stackWalkerClass = null;
		try {
			stackWalkerClass = Class.forName( "java.lang.StackWalker" );
		}
		catch (ClassNotFoundException ignored) {
		}
		if ( stackWalkerClass != null ) {
			// JDK 9+, where StackWalker is available
			try {
				CALLER_CLASS_WALKER = new StackWalkerCallerClassWalker( stackWalkerClass );
			}
			catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException | RuntimeException e) {
				throw new IllegalStateException( "Unable to initialize ClassWalker based on java.lang.StackWaker", e );
			}
		}
		else {
			// JDK 8
			CALLER_CLASS_WALKER = new SecurityManagerCallerClassWalker();
		}
	}

	private RuntimeHelper() {
	}

	public static CallerClassWalker callerClassWalker() {
		return CALLER_CLASS_WALKER;
	}

	public static Optional<Class<?>> firstNonSelfNonJdkCaller() {
		return firstNonSelfNonJdkCaller( 1L ); // Skip this method
	}

	public static Optional<Class<?>> firstNonSelfNonJdkCaller(long skip) {
		return callerClassWalker().walk( stream -> {
			Iterator<Class<?>> iterator = stream
					.skip( 1L + skip ) // Skip this method and whatever was requested
					.iterator();
			if ( !iterator.hasNext() ) {
				// No called class... ?
				return Optional.empty();
			}
			Class<?> calledClass = iterator.next();
			if ( !iterator.hasNext() ) {
				// No caller class... ?
				return Optional.empty();
			}
			Class<?> callerClass = iterator.next();
			// Skip self-calls
			while ( callerClass.equals( calledClass ) && iterator.hasNext() ) {
				callerClass = iterator.next();
			}
			if ( callerClass.equals( calledClass ) ) {
				// Only self calls.
				return Optional.empty();
			}
			// Skip JDK calls
			while ( isJdk( callerClass ) && iterator.hasNext() ) {
				callerClass = iterator.next();
			}
			if ( isJdk( callerClass ) ) {
				// Only calls from JDK.
				return Optional.empty();
			}
			return Optional.of( callerClass );
		} );
	}

	private static boolean isJdk(Class<?> clazz) {
		Package pakkgage = clazz.getPackage();
		return pakkgage != null && pakkgage.getName().startsWith( "java." );
	}

	public static boolean isHibernateSearch(Class<?> clazz) {
		Package pakkgage = clazz.getPackage();
		return pakkgage != null && pakkgage.getName().startsWith( "org.hibernate.search." );
	}

	public interface CallerClassWalker {
		<T> T walk(Function<? super Stream<Class<?>>, ? extends T> function);
	}

	/**
	 * Retrieves the caller stack from the security manager class context.
	 */
	@SuppressWarnings("removal")
	private static class SecurityManagerCallerClassWalker extends SecurityManager
			implements CallerClassWalker {
		@Override
		public <T> T walk(Function<? super Stream<Class<?>>, ? extends T> function) {
			// The type arguments *are* necessary for some reason.
			// Without them, we get strange compilation errors when targeting JDK 8.
			return function.apply( Arrays.<Class<?>>stream( getClassContext() )
					.skip( 1 ) // Skip this method
					.filter( c -> !c.isSynthetic() ) // Skip lambdas, like StackWalker does
			);
		}
	}

	/**
	 * Retrieves the caller stack using a StackWalker.
	 */
	private static class StackWalkerCallerClassWalker
			implements CallerClassWalker {
		private final Object stackWalker;
		private final Method stackWalkerWalkMethod;
		private final Function<Object, Class<?>> stackFrameGetDeclaringClassFunction;

		private StackWalkerCallerClassWalker(Class<?> stackWalkerClass)
				throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
			Class<?> optionClass = Class.forName( "java.lang.StackWalker$Option" );
			this.stackWalker = stackWalkerClass.getMethod( "getInstance", optionClass )
					.invoke( null, optionClass.getEnumConstants()[0] );

			this.stackWalkerWalkMethod = stackWalkerClass.getMethod( "walk", Function.class );
			Method stackFrameGetDeclaringClass = Class.forName( "java.lang.StackWalker$StackFrame" )
					.getMethod( "getDeclaringClass" );
			stackFrameGetDeclaringClassFunction = frame -> {
				try {
					return (Class<?>) stackFrameGetDeclaringClass.invoke( frame );
				}
				catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					throw new IllegalStateException( "Unable to get stack frame declaring class", e );
				}
			};
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T> T walk(Function<? super Stream<Class<?>>, ? extends T> function) {
			Function<Stream<?>, T> stackFrameExtractFunction =
					stream -> function.apply( stream
							.skip( 1 ) // Skip this method
							.map( stackFrameGetDeclaringClassFunction ) );
			try {
				return (T) stackWalkerWalkMethod.invoke( stackWalker, stackFrameExtractFunction );
			}
			catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new IllegalStateException( "Unable to walk the stack using StackWalker", e );
			}
		}
	}
}
