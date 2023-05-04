/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.test.reflect;

import java.util.Iterator;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public final class RuntimeHelper {

	private static final CallerClassWalker CALLER_CLASS_WALKER;

	static {
		CALLER_CLASS_WALKER = new StackWalkerCallerClassWalker();
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
	 * Retrieves the caller stack using a StackWalker.
	 */
	private static class StackWalkerCallerClassWalker implements CallerClassWalker {
		private final StackWalker stackWalker;

		private StackWalkerCallerClassWalker() {
			this.stackWalker = StackWalker.getInstance( StackWalker.Option.RETAIN_CLASS_REFERENCE );
		}

		@Override
		public <T> T walk(Function<? super Stream<Class<?>>, ? extends T> function) {
			Function<Stream<StackWalker.StackFrame>, T> stackFrameExtractFunction =
					stream -> function.apply( stream
							.skip( 1 ) // Skip this method
							.map( StackWalker.StackFrame::getDeclaringClass ) );
			try {
				return stackWalker.walk( stackFrameExtractFunction );
			}
			catch (IllegalArgumentException e) {
				throw new IllegalStateException( "Unable to walk the stack using StackWalker", e );
			}
		}
	}
}
