/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.showcase.library.service;

import java.util.Collections;
import java.util.List;
import javax.persistence.EntityManager;

import org.hibernate.search.integrationtest.showcase.library.model.Account;
import org.hibernate.search.integrationtest.showcase.library.model.Borrowal;
import org.hibernate.search.integrationtest.showcase.library.model.BorrowalType;
import org.hibernate.search.integrationtest.showcase.library.model.Document;
import org.hibernate.search.integrationtest.showcase.library.model.DocumentCopy;
import org.hibernate.search.integrationtest.showcase.library.model.Library;
import org.hibernate.search.integrationtest.showcase.library.model.Person;
import org.hibernate.search.integrationtest.showcase.library.repository.AccountRepository;
import org.hibernate.search.integrationtest.showcase.library.repository.BorrowalRepository;
import org.hibernate.search.integrationtest.showcase.library.repository.PersonRepository;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.jpa.FullTextQuery;
import org.hibernate.search.mapper.orm.jpa.FullTextSearchTarget;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BorrowalService {

	@Autowired
	private BorrowalRepository borrowalRepo;

	@Autowired
	private AccountRepository accountRepo;

	@Autowired
	private PersonRepository personRepo;

	@Autowired
	private EntityManager entityManager;

	public Person create(int id, String firstName, String lastName) {
		return personRepo.save( new Person( id, firstName, lastName ) );
	}

	public Account createAccount(Person person) {
		Account account = new Account( person );
		person.setAccount( account );
		return accountRepo.save( account );
	}

	public <D extends Document<C>, C extends DocumentCopy<D>> Borrowal borrow(Person user, Library library, Document document, BorrowalType type) {
		return borrow( user, library, document, 0, type );
	}

	public <D extends Document<C>, C extends DocumentCopy<D>> Borrowal borrow(Person user, Library library, Document document, int copyIndex, BorrowalType type) {
		return borrowalRepo.save( new Borrowal( user.getAccount(), document.getCopy( library, copyIndex ), type ) );
	}

	public List<Person> listTopBorrowers(int offset, int limit) {
		return listTopBorrowers( "account.borrowals.totalCount", offset, limit );
	}

	public List<Person> listTopShortTermBorrowers(int offset, int limit) {
		return listTopBorrowers( "account.borrowals.shortTermCount", offset, limit );
	}

	public List<Person> listTopLongTermBorrowers(int offset, int limit) {
		return listTopBorrowers( "account.borrowals.longTermCount", offset, limit );
	}

	public List<Person> searchPerson(String terms, int offset, int limit) {
		if ( terms == null || terms.isEmpty() ) {
			return Collections.emptyList();
		}

		FullTextSearchTarget<Person> target = Search.getFullTextEntityManager( entityManager ).search( Person.class );

		FullTextQuery<Person> query = target.query()
				.asEntity()
				.predicate( f -> f.match().onFields( "firstName", "lastName" ).matching( terms ) )
				.sort( c -> {
					c.byField( "lastName_sort" );
					c.byField( "firstName_sort" );
				} )
				.build();

		query.setFirstResult( offset );
		query.setMaxResults( limit );

		return query.getResultList();
	}

	private List<Person> listTopBorrowers(String borrowalsCountField, int offset, int limit) {
		FullTextSearchTarget<Person> target = Search.getFullTextEntityManager( entityManager ).search( Person.class );

		FullTextQuery<Person> query = target.query()
				.asEntity()
				.predicate( f -> f.matchAll() )
				.sort( c -> c.byField( borrowalsCountField ).desc() )
				.build();

		query.setFirstResult( offset );
		query.setMaxResults( limit );

		return query.getResultList();
	}
}
