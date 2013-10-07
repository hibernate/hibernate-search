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
package org.hibernate.search.test.configuration.indexingStrategy;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.lucene.index.IndexReader;

import org.hibernate.Session;

import org.hibernate.cfg.Configuration;
import org.hibernate.search.Environment;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.test.SearchTestCaseJUnit4;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public class ManualIndexingStrategyTest extends SearchTestCaseJUnit4 {

	@Test
	public void testMultipleEntitiesPerIndex() throws Exception {
		indexTestEntity();
		assertEquals(
				"Due to manual indexing being enabled no automatic indexing should have occurred",
				0,
				getDocumentNbr()
		);
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { TestEntity.class };
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.INDEXING_STRATEGY, "manual" );
	}

	private void indexTestEntity() {
		Session session = getSessionFactory().openSession();
		session.getTransaction().begin();

		session.persist( new TestEntity() );

		session.getTransaction().commit();
		session.close();
	}

	private int getDocumentNbr() throws Exception {
		// we directly access the index to verify the document count
		IndexReader reader = IndexReader.open( getDirectory( TestEntity.class ) );
		try {
			return reader.numDocs();
		}
		finally {
			reader.close();
		}
	}

	@Indexed
	@Entity
	@Table(name = "TestEntity")
	public static class TestEntity {

		@Id
		@GeneratedValue
		private int id;
	}
}
