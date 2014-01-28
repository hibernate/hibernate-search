/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.test.configuration.bootstrapfailure;

import java.util.Set;

import org.hibernate.search.test.SearchTestCaseJUnit4;
import org.hibernate.search.test.util.TestForIssue;
import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Hardy Ferentschik
 */
@TestForIssue(jiraKey = "HSEARCH-1447")
public class BootstrapTest extends SearchTestCaseJUnit4 {

	@Test
	public void testCreateIndexSearchEntityWithLobField() {
		Set<Class<?>> indexedTypes = getSearchFactory().getIndexedTypes();

		assertTrue( "There should only be one indexed entity", indexedTypes.size() == 1 );
		assertTrue(
				"Unexpected indexed type: " + getSearchFactory().getIndexedTypes(),
				getSearchFactory().getIndexedTypes().contains( IndexedEntity.class )
		);

		assertNull(
				"NoSearchEntity should not have a DocumentBuilderContainedEntity",
				getSearchFactoryImpl().getDocumentBuilderContainedEntity( NoSearchEntity.class )
		);
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				// just adding NoSearchEntity causes an exception, even though it is not used from a Search perspective
				IndexedEntity.class, EmbeddedEntity.class, NoSearchEntity.class
		};
	}
}


