/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.showcase.library.service;

import java.util.List;

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

	public Person create(int id, String firstName, String lastName) {
		return personRepo.save( new Person( id, firstName, lastName ) );
	}

	public Account createAccount(Person person) {
		Account account = new Account( person );
		person.setAccount( account );
		return accountRepo.save( account );
	}

	public <D extends Document<C>, C extends DocumentCopy<D>> Borrowal borrow(Person user, Library library,
			Document<?> document, BorrowalType type) {
		return borrow( user, library, document, 0, type );
	}

	public <D extends Document<C>, C extends DocumentCopy<D>> Borrowal borrow(Person user, Library library,
			Document<?> document, int copyIndex, BorrowalType type) {
		DocumentCopy<?> copy = getCopy( document, library, copyIndex );
		Borrowal borrowal = new Borrowal( user.getAccount(), copy, type );
		user.getAccount().getBorrowals().add( borrowal );
		copy.getBorrowals().add( borrowal );
		return borrowalRepo.save( borrowal );
	}

	public List<Person> listTopBorrowers(int offset, int limit) {
		return personRepo.listTopBorrowers( offset, limit );
	}

	public List<Person> listTopShortTermBorrowers(int offset, int limit) {
		return personRepo.listTopShortTermBorrowers( offset, limit );
	}

	public List<Person> listTopLongTermBorrowers(int offset, int limit) {
		return personRepo.listTopLongTermBorrowers( offset, limit );
	}

	public List<Person> searchPerson(String terms, int offset, int limit) {
		return personRepo.searchPerson( terms, offset, limit );
	}

	private DocumentCopy<?> getCopy(Document<?> document, Library library, int copyIndex) {
		return document.getCopies().stream()
				.filter( c -> c.getLibrary().equals( library ) )
				.skip( copyIndex )
				.findFirst()
				.orElseThrow( () -> new IllegalStateException(
						"The test setup is incorrect; could not find copy #" + copyIndex
								+ " of document " + this
								+ " for library " + library
				) );
	}
}
