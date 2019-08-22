/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.showcase.library.service;

import javax.persistence.EntityManager;

import org.hibernate.search.integrationtest.showcase.library.model.Book;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InnerTransactionRollbackService {

	@Autowired
	private InnerTransactionRollbackService self;

	@Autowired
	private EntityManager entityManager;

	/**
	 * Persists a new book within an independent transaction.
	 * Flushes and finally rollback throwing a {@link MakeRollbackException}.
	 * <p>
	 * When the nested transaction rollbacks, this is the case here,
	 * {@code Propagation.REQUIRES_NEW} is equivalent to a {@code Propagation.NESTED},
	 * what does really matter is that:
	 * <b>inner transaction rollback does not affect outer transaction</b>.
	 * <p>
	 * {@code Propagation.REQUIRES_NEW} differs from {@code Propagation.NESTED}
	 * when outer transaction rollbacks, this is NOT the case here,
	 * with {@code Propagation.REQUIRES_NEW} the inner transaction can commit,
	 * with {@code Propagation.NESTED} the inner transaction cannot commit.
	 *
	 * @param isbn the isbn code for the book that has to be created within the inner transaction
	 * @throws MakeRollbackException
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void doInner(String isbn) {
		// This data is supposed to be NOT pushed to the index:
		Book book = new Book( 731, isbn, "The Prince", "Niccolò Machiavelli", "Describes the human people behaviour...", "politics" );
		entityManager.persist( book );
		entityManager.flush();
		throw new MakeRollbackException( "Rollback the inner transaction" );
	}

	public void doOuter(String outerIsbn, String nestedIsbn) {
		// This data is supposed to be pushed to the index:
		Book book = new Book( 739, outerIsbn, "The Late Mattia Pascal", "Luigi Pirandello", "Describes the human life conditions...", "misfortune" );
		entityManager.persist( book );
		try {
			self.doInner( nestedIsbn );
		}
		catch (MakeRollbackException e) {
			// Do not rollback the outer transaction
		}
		entityManager.flush();
	}

	public void doOuterFlushBeforeInner(String outerIsbn, String nestedIsbn) {
		// This data is supposed to be pushed to the index:
		Book book = new Book( 739, outerIsbn, "The Late Mattia Pascal", "Luigi Pirandello", "Describes the human life conditions...", "misfortune" );
		entityManager.persist( book );

		// flush before the execution of the inner transaction
		entityManager.flush();

		try {
			self.doInner( nestedIsbn );
		}
		catch (MakeRollbackException e) {
			// Do not rollback the outer transaction
		}
		entityManager.flush();
	}

	private static class MakeRollbackException extends RuntimeException {
		public MakeRollbackException(String message) {
			super( message );
		}
	}
}
