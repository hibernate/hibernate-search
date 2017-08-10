/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl;

import java.io.Closeable;
import java.io.IOException;

import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author Yoann Rodiere
 */
public final class Closeables {

	private static final Log log = LoggerFactory.make();

	private Closeables() {
		// Private constructor
	}

	/**
	 * Close multiple resources, ensuring that all resources get closed
	 * even if one of them throws a Throwable.
	 * <p>
	 * The first caught throwable will be re-thrown, and any additional
	 * throwable will be {@link Throwable#addSuppressed(Throwable) suppressed}.
	 *
	 * @param closeables The resources to close
	 * @throws E if a closeable throws such an exception
	 */
	public static <E extends Exception> void close(Iterable<? extends GenericCloseable<? extends E>> closeables) throws E {
		try ( Closer<E> closer = new Closer<>() ) {
			closer.pushAll( GenericCloseable::close, closeables );
		}
	}

	/**
	 * Close a resource without throwing an exception.
	 *
	 * @param resource the resource to close
	 */
	public static void closeQuietly(Closeable resource) {
		if ( resource != null ) {
			try {
				resource.close();
			}
			catch (IOException | RuntimeException e) {
				//we don't really care if we can't close
				log.couldNotCloseResource( e );
			}
		}
	}

	/**
	 * Close resources without throwing an exception.
	 *
	 * @param resourceIterables the iterables containing the resources to close
	 */
	@SafeVarargs
	public static void closeQuietly(Iterable<? extends Closeable> ... resourceIterables) {
		for ( Iterable<? extends Closeable> resourceIterable : resourceIterables ) {
			for ( Closeable resource : resourceIterable ) {
				closeQuietly( resource );
			}
		}
	}

}
