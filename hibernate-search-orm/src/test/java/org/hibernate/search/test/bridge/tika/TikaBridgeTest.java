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

package org.hibernate.search.test.bridge.tika;

import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.SearchException;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.TikaBridge;
import org.hibernate.search.bridge.BridgeException;
import org.hibernate.search.test.SearchTestCase;

/**
 * @author Hardy Ferentschik
 */
public class TikaBridgeTest extends SearchTestCase {

	public void testUnsupportedTypeForTikaBridge() throws Exception {
		Session session = openSession();

		try {
			Transaction tx = session.beginTransaction();
			session.save( new Foo() );
			tx.commit();
			fail();
		} catch ( HibernateException e ) {
			// hmm, a lot of exception wrapping going on
			assertTrue( e.getCause() instanceof BridgeException);
			BridgeException bridgeException = (BridgeException) e.getCause();
			assertTrue( e.getCause() instanceof SearchException );
			SearchException searchException = (SearchException) bridgeException.getCause();
			assertTrue( "Wrong root cause", searchException.getMessage().startsWith( "HSEARCH000151" ) );
		}
		finally {

			session.close();
		}
	}

	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Foo.class
		};
	}

	@Entity
	@Indexed
	public static class Foo {
		@Id
		@GeneratedValue
		long id;

		@Field
		@TikaBridge
		Date now = new Date();

		public long getId() {
			return id;
		}

		public Date getNow() {
			return now;
		}
	}
}
