/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.fullstack.library.repository;

import java.util.List;
import javax.persistence.EntityManager;

import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.jpa.FullTextEntityManager;
import org.hibernate.search.integrationtest.fullstack.library.model.Account;
import org.hibernate.search.integrationtest.fullstack.library.model.Borrowal;
import org.hibernate.search.integrationtest.fullstack.library.model.BorrowalType;
import org.hibernate.search.integrationtest.fullstack.library.model.DocumentCopy;
import org.hibernate.search.integrationtest.fullstack.library.model.Person;

public abstract class PersonRepository {

	protected final FullTextEntityManager entityManager;

	public PersonRepository(EntityManager entityManager) {
		this.entityManager = Search.getFullTextEntityManager( entityManager );
	}

	public Person create(int id, String firstName, String lastName) {
		Person person = new Person();
		person.setId( id );
		person.setFirstName( firstName );
		person.setLastName( lastName );
		entityManager.persist( person );
		return person;
	}

	public Account createAccount(Person user) {
		Account account = new Account();
		account.setUser( user );
		user.setAccount( account );
		entityManager.persist( account );
		return account;
	}

	public Borrowal createBorrowal(Account account, DocumentCopy<?> copy, BorrowalType type) {
		Borrowal borrowal = new Borrowal();
		borrowal.setAccount( account );
		borrowal.setCopy( copy );
		borrowal.setType( type );
		account.getBorrowals().add( borrowal );
		copy.getBorrowals().add( borrowal );
		entityManager.persist( borrowal );
		return borrowal;
	}

	public abstract List<Person> search(String terms, int offset, int limit);

	public final List<Person> listTopBorrowers(int offset, int limit) {
		return listTopBorrowers( "account.borrowals.totalCount", offset, limit );
	}

	public final List<Person> listTopShortTermBorrowers(int offset, int limit) {
		return listTopBorrowers( "account.borrowals.shortTermCount", offset, limit );
	}

	public final List<Person> listTopLongTermBorrowers(int offset, int limit) {
		return listTopBorrowers( "account.borrowals.longTermCount", offset, limit );
	}

	protected abstract List<Person> listTopBorrowers(String borrowalsCountField, int offset, int limit);
}
