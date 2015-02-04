/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.jms.controller;

import java.util.List;
import javax.annotation.PostConstruct;
import javax.ejb.Stateful;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.lucene.search.Query;

import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.test.integration.jms.model.RegisteredMember;

@Stateful
public class RegistrationController {

	@PersistenceContext
	private EntityManager em;

	private RegisteredMember newMember;

	@Named
	public RegisteredMember getNewMember() {
		return newMember;
	}

	public void register() throws Exception {
		em.persist( newMember );
		resetNewMember();
	}

	public int deleteAllMembers() throws Exception {
		return em.createQuery( "DELETE FROM RegisteredMember" ).executeUpdate();
	}

	public RegisteredMember findById(Long id) {
		return em.find( RegisteredMember.class, id );
	}

	@SuppressWarnings("unchecked")
	public List<RegisteredMember> search(String name) {
		FullTextEntityManager fullTextEm = Search.getFullTextEntityManager( em );
		Query luceneQuery = fullTextEm.getSearchFactory().buildQueryBuilder()
				.forEntity( RegisteredMember.class ).get()
				.keyword().onField( "name" ).matching( name ).createQuery();

		return fullTextEm.createFullTextQuery( luceneQuery ).getResultList();
	}

	@PostConstruct
	public void resetNewMember() {
		newMember = new RegisteredMember();
	}

}
