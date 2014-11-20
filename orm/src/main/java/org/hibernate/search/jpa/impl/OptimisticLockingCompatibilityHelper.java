/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jpa.impl;

import javax.persistence.OptimisticLockException;

import org.hibernate.HibernateException;
import org.hibernate.Session;

/**
 * Helper class for {@code FullTextQueryImpl} to extract information out of an
 * Hibernate {@code OptimisticLockException} and create the JPA counterpart.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
final class OptimisticLockingCompatibilityHelper {

	//This is the new class name, and is extended by the deprecated one so it is compatible with both types
	private static final String parentOptimisticLockExceptionClassName = "org.hibernate.dialect.lock.OptimisticEntityLockException";
	private static final String deprecatedOptimisticLockExceptionClassName = "org.hibernate.OptimisticLockException";
	private static final Class optimisticLockExceptionClass = determineCompatibleOptimisticLockExceptionClass();

	private OptimisticLockingCompatibilityHelper() {
		//not allowed
	}

	/**
	 * Looks for the new Hibernate class name, or falls back to the older one.
	 *
	 * @return the type of optimistic lock exceptions which Hibernate is going to throw
	 */
	private static Class determineCompatibleOptimisticLockExceptionClass() {
		try {
			return Class.forName( parentOptimisticLockExceptionClassName, true, Session.class.getClassLoader() );
		}
		catch (ClassNotFoundException e) {
			// the failing class was introduced in Hibernate Core 4.0.0.CR7 only; fall back to old name when it's not found:
			try {
				return Class.forName(
						deprecatedOptimisticLockExceptionClassName,
						true,
						Session.class.getClassLoader()
				);
			}
			catch (ClassNotFoundException e1) {
				// this is fatal, will need to check for null at runtime
				return null;
			}
		}
	}

	/**
	 * We might need different ways to extract the error message according to what is available at runtime
	 *
	 * @param e the Hibernate exception
	 *
	 * @return Returns the entity passed as part of the exception context. {@code null} is returned if no entity is
	 *         provided within the exception
	 */
	private static Object extractEntityOufOfException(HibernateException e) {
		if ( parentOptimisticLockExceptionClassName.equals( optimisticLockExceptionClass.getName() ) ) {
			org.hibernate.dialect.lock.OptimisticEntityLockException oele = (org.hibernate.dialect.lock.OptimisticEntityLockException) e;
			return oele.getEntity();
		}
		else if ( deprecatedOptimisticLockExceptionClassName.equals( optimisticLockExceptionClass.getName() ) ) {
			org.hibernate.OptimisticLockException oele = (org.hibernate.OptimisticLockException) e;
			return oele.getEntity();
		}
		return null;
	}

	/**
	 * @param e an Hibernate runtime exception
	 *
	 * @return true if it's definitely an optimistic locking exception, false if we can't tell
	 */
	public static boolean isOptimisticLockException(HibernateException e) {
		return optimisticLockExceptionClass != null && optimisticLockExceptionClass.isInstance( e );
	}

	/**
	 * Convert a provided Hibernate exception to a JPA exception. If possible entity information is added.
	 *
	 * @param e the Hibernate exception to convert
	 *
	 * @return A JPA optimistic lock exception
	 */
	public static OptimisticLockException convertToLockException(HibernateException e) {
		Object entity = extractEntityOufOfException( e );
		if ( entity != null ) {
			return new OptimisticLockException( e.getMessage(), e, entity );
		}
		else {
			return new OptimisticLockException( e.getMessage(), e );
		}
	}

}
