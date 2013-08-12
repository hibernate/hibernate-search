/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.test.embedded.depth;

import java.util.List;

import junit.framework.Assert;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.test.util.LeakingLuceneBackend;

/**
 * @author Sanne Grinovero
 */
public class DocumentIdContainedInTest extends RecursiveGraphTest {

	@Override
	public void testCorrectDepthIndexed() {
		Session session = openSession();
		try {
			Transaction transaction = session.beginTransaction();
			session.persist( new PersonWithBrokenSocialSecurityNumber( 1L, "Mario Rossi" ) );
			session.persist( new PersonWithBrokenSocialSecurityNumber( 2L, "Bruno Rossi" ) );
			transaction.commit();
		}
		finally {
			session.close();
		}
		List<LuceneWork> processedQueue = LeakingLuceneBackend.getLastProcessedQueue();
		// as they resolve to the same Lucene id only one instance will make it to the backend.
		// (which one is undefined, nobody should use a constant as id)
		Assert.assertEquals( 1, processedQueue.size() );
		Assert.assertEquals( "100", processedQueue.get( 0 ).getId() );
		Assert.assertTrue( processedQueue.get( 0 ) instanceof AddLuceneWork );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { PersonWithBrokenSocialSecurityNumber.class };
	}

}
