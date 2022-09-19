/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.util;

import java.util.function.Consumer;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;

import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;

/**
 * Some helpers to allow using lamda blocks to ensure that
 * resources which get opened are also being closed correctly.
 *
 * @author Sanne Grinovero (C) 2016 Red Hat Inc.
 */
public final class ResourceCleanupFunctions {

	private ResourceCleanupFunctions() {
		//Utility class: not to be constructed.
	}

	/**
	 * Given an EntityManagerFactory, we'll open an EntityManager, allow some
	 * code block to use it as a FullTextEntityManager, and finally close
	 * the EntityManager.
	 * @param emf The EntityManagerFactory which we'll use to create a new EntityManager
	 * @param consumer A block of code which uses the FullTextEntityManager
	 */
	public static void withinEntityManager(EntityManagerFactory emf, Consumer<FullTextEntityManager> consumer) {
		EntityManager entityManager = emf.createEntityManager();
		try {
			FullTextEntityManager fem = Search.getFullTextEntityManager( entityManager );
			consumer.accept( fem );
		}
		catch (Throwable mainError) {
			try {
				entityManager.close();
			}
			catch (Throwable errorOnClose) {
				mainError.addSuppressed( errorOnClose );
			}
			throw mainError;
		}
		entityManager.close();
	}

	/**
	 * Given an EntityManager, we can being a transaction on it, run the passed
	 * code block in the transaction context, and then commit the transaction.
	 * Leaking transactions introduce hard to find problems in the tests; use
	 * this helper to make sure all started transactions are ended.
	 * @param em The EntityManager on which we'll start the transaction
	 * @param codeblock The Runnable to run within the transaction
	 */
	public static void withinTransaction(EntityManager em, Runnable codeblock) {
		EntityTransaction transaction = em.getTransaction();
		transaction.begin();
		try {
			codeblock.run();
			transaction.commit();
		}
		catch (Throwable mainError) {
			try {
				if ( transaction.isActive() ) {
					transaction.rollback();
				}
			}
			catch (Throwable errorOnRollBack) {
				mainError.addSuppressed( errorOnRollBack );
			}
			throw mainError;
		}
	}

}
