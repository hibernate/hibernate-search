/*
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
package org.hibernate.search.test.configuration.norms;

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Norms;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.test.util.LeakingLuceneBackend;

/**
 * Test storing and omitting index time norms
 *
 * @author Hardy Ferentschik
 */
public class StoreNormsTest extends SearchTestCase {

	public void testStoreAndOmitNorms() throws Exception {
		Session session = openSession();
		FullTextSession fullTextSession = Search.getFullTextSession( session );
		Transaction tx = fullTextSession.beginTransaction();
		NormsTestEntity test = new NormsTestEntity();
		test.setWithNormsImplicit( "hello" );
		test.setWithNormsExplicit( "world" );
		test.setWithoutNorms( "how are you?" );
		fullTextSession.save( test );
		tx.commit();

		List<LuceneWork> processedQueue = LeakingLuceneBackend.getLastProcessedQueue();
		assertTrue( processedQueue.size() == 1 );
		AddLuceneWork addLuceneWork = (AddLuceneWork) processedQueue.get( 0 );
		Document doc = addLuceneWork.getDocument();

		Fieldable implicitNormField = doc.getFieldable( "withNormsImplicit" );
		assertFalse( "norms should be stored for this field", implicitNormField.getOmitNorms() );

		Fieldable explicitNormField = doc.getFieldable( "withNormsExplicit" );
		assertFalse( "norms should be stored for this field", explicitNormField.getOmitNorms() );

		Fieldable withoutNormField = doc.getFieldable( "withoutNorms" );
		assertTrue( "norms should not be stored for this field", withoutNormField.getOmitNorms() );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { NormsTestEntity.class };
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( "hibernate.search.default.worker.backend", LeakingLuceneBackend.class.getName() );
	}

	@Entity
	@Indexed(index = "test")
	public class NormsTestEntity {
		@Id
		@GeneratedValue
		private int id;

		@Field(store = Store.YES)
		private String withNormsImplicit;

		@Field(norms = Norms.YES, store = Store.YES)
		private String withNormsExplicit;

		@Field(norms = Norms.NO, store = Store.YES)
		private String withoutNorms;

		public String getWithNormsImplicit() {
			return withNormsImplicit;
		}

		public void setWithNormsImplicit(String withNormsImplicit) {
			this.withNormsImplicit = withNormsImplicit;
		}

		public String getWithNormsExplicit() {
			return withNormsExplicit;
		}

		public void setWithNormsExplicit(String withNormsExplicit) {
			this.withNormsExplicit = withNormsExplicit;
		}

		public String getWithoutNorms() {
			return withoutNorms;
		}

		public void setWithoutNorms(String withoutNorms) {
			this.withoutNorms = withoutNorms;
		}
	}
}
