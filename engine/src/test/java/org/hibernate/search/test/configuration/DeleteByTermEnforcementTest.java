/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2015 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.search.test.configuration;


import org.hibernate.search.impl.MutableSearchFactory;
import org.hibernate.search.test.util.ManualConfiguration;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test to force deleteByTerm on backend
 *
 * @author gustavonalle
 */
public class DeleteByTermEnforcementTest extends BaseConfigurationTest {

	@Test
	public void testEnforcement() {
		ManualConfiguration cfg = new ManualConfiguration();
		cfg.setDeleteByTermEnforced( true );

		verifyDeleteByTerm( true, cfg );
	}

	@Test
	public void testDefaults() {
		ManualConfiguration cfg = new ManualConfiguration();

		verifyDeleteByTerm( false, cfg );
	}

	@Test
	public void testWithMetadataComplete() {
		ManualConfiguration cfg = new ManualConfiguration();
		cfg.setIndexMetadataComplete( true );

		verifyDeleteByTerm( false, cfg );
	}

	private void verifyDeleteByTerm(boolean enforced, ManualConfiguration cfg) {
		MutableSearchFactory sf = getMutableSearchFactoryWithSingleEntity( cfg );
		try {
			assertEquals( enforced, extractWorkspace( sf, Document.class ).isDeleteByTermEnforced() );

			// trigger a SearchFactory rebuild:
			sf.addClasses( Dvd.class, Book.class );

			assertEquals( enforced, extractWorkspace( sf, Book.class ).isDeleteByTermEnforced() );
			assertEquals( enforced, extractWorkspace( sf, Dvd.class ).isDeleteByTermEnforced() );
			assertEquals( enforced, extractWorkspace( sf, Document.class ).isDeleteByTermEnforced() );
		}
		finally {
			sf.close();

		}
	}
}
