/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.test.id;

import java.lang.annotation.ElementType;
import java.util.List;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.search.Environment;
import org.hibernate.search.Search;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.test.SearchTestCase;


/**
 * Copied from EmbeddedIdTest, but using programmatic mapping instead.
 *
 * @author Emmanuel Bernard
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 */
public class ProgrammaticEmbeddedItTest extends SearchTestCase {

	public void testFieldBridge() throws Exception {
		PersonPK emmanuelPk = new PersonPK();
		emmanuelPk.setFirstName( "Emmanuel" );
		emmanuelPk.setLastName( "Bernard" );
		PlainPerson emmanuel = new PlainPerson();
		emmanuel.setFavoriteColor( "Blue" );
		emmanuel.setId( emmanuelPk );

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.save( emmanuel );
		tx.commit();
		s.clear();

		tx = s.beginTransaction();
		List results = Search.getFullTextSession( s ).createFullTextQuery(
				new TermQuery( new Term( "id.lastName", "Bernard" ) )
		).list();
		assertEquals( 1, results.size() );
		emmanuel = (PlainPerson) results.get( 0 );
		emmanuel.setFavoriteColor( "Red" );
		tx.commit();
		s.clear();

		tx = s.beginTransaction();
		results = Search.getFullTextSession( s ).createFullTextQuery(
				new TermQuery( new Term( "id.lastName", "Bernard" ) )
		).list();
		assertEquals( 1, results.size() );
		emmanuel = (PlainPerson) results.get( 0 );
		assertEquals( "Red", emmanuel.getFavoriteColor() );
		s.delete( results.get( 0 ) );
		tx.commit();
		s.close();
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		SearchMapping mapping = new SearchMapping();
		mapping
			.entity( PlainPerson.class )
				.indexed()
			.property( "id", ElementType.FIELD )
				.documentId()
				.bridge( PersonPKBridge.class )
			.property( "", ElementType.FIELD )
				.field();
		cfg.getProperties().put( Environment.MODEL_MAPPING, mapping );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{ PlainPerson.class };
	}

}
