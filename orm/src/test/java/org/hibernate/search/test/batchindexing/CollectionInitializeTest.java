/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
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

package org.hibernate.search.test.batchindexing;

import java.util.List;

import junit.framework.Assert;

import org.apache.lucene.search.MatchAllDocsQuery;

import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestCase;

/**
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class CollectionInitializeTest extends SearchTestCase {

	public void testMassIndexing() throws InterruptedException {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		initializeData( fullTextSession );
		try {
			List list = fullTextSession.createCriteria( LegacyCarPlant.class ).list();
			Assert.assertEquals( 1, list.size() );
			fullTextSession.createIndexer( LegacyCarPlant.class ).startAndWait();
			int resultSize = fullTextSession.createFullTextQuery( new MatchAllDocsQuery(), LegacyCarPlant.class ).getResultSize();
			Assert.assertEquals( 1, resultSize );
		}
		finally {
			clearData( fullTextSession );
			fullTextSession.close();
		}
	}

	private void clearData(FullTextSession fullTextSession) {
		final Transaction transaction = fullTextSession.beginTransaction();
		final LegacyCarPlantPK id = new LegacyCarPlantPK();
		id.setCarId( "1" );
		id.setPlantId( "2" );
		fullTextSession.delete( fullTextSession.get( LegacyCarPlant.class, id ) );
		for ( int i = 1; i < 4; i++ ) {
			fullTextSession.delete( fullTextSession.get( LegacyCar.class, "" + i ) );
		}
		transaction.commit();
	}

	private void initializeData(FullTextSession fullTextSession) {
		final Transaction transaction = fullTextSession.beginTransaction();
		LegacyCar[] cars = new LegacyCar[3];
		for ( int i = 1; i < 4; i++ ) {
			cars[i - 1] = new LegacyCar();
			cars[i - 1].setId( "" + i );
			cars[i - 1].setModel( "model" + i );
			fullTextSession.persist( cars[i - 1] );
		}
		LegacyCarPlant plant = new LegacyCarPlant();
		plant.setCar( cars[0] );
		plant.setName( "plant12" );
		plant.setId( new LegacyCarPlantPK() );
		plant.getId().setCarId( cars[0].getId() );
		plant.getId().setPlantId( "2" );
		fullTextSession.persist( plant );
		transaction.commit();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { LegacyCarPlant.class, LegacyCar.class, LegacyTire.class };
	}
}
