/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
