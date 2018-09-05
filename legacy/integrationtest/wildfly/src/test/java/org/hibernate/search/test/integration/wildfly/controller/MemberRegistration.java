/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.wildfly.controller;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.ejb.Stateful;
import javax.enterprise.inject.Model;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.lucene.search.Query;
import org.hibernate.search.engine.Version;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.test.integration.wildfly.model.Member;

@Stateful
@Model
public class MemberRegistration {

	@Inject
	private FullTextEntityManager em;

	private Member newMember;

	@Produces
	@Named
	public Member getNewMember() {
		return newMember;
	}

	public void register() throws Exception {
		register( newMember );
	}

	public void register(Member member) throws Exception {
		em.persist( member );
		initNewMember();
	}

	public String getHibernateSearchVersionString() {
		return Version.getVersionString();
	}

	@SuppressWarnings("unchecked")
	public List<Member> search(String name) {
		Query luceneQuery = em.getSearchFactory().buildQueryBuilder()
				.forEntity( Member.class ).get().keyword()
				.onField( "name" ).matching( name )
				.createQuery();

		return em.createFullTextQuery( luceneQuery ).getResultList();
	}

	@PostConstruct
	public void initNewMember() {
		newMember = new Member();
	}

}
