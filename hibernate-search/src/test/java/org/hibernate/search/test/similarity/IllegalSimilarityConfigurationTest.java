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
package org.hibernate.search.test.similarity;

import org.hibernate.search.test.util.FullTextSessionBuilder;
import org.junit.Test;
import static org.junit.Assert.*;

public class IllegalSimilarityConfigurationTest {

	@Test
	public void testValidConfiguration() {
		boolean configurationIsLegal = true;
		FullTextSessionBuilder builder = null;
		try {
			builder = new FullTextSessionBuilder()
					.addAnnotatedClass(Can.class)
					.addAnnotatedClass(Trash.class).build();
		} catch (Exception e) {
			configurationIsLegal = false;
		}
		finally {
			if (builder!=null)
				builder.close();
		}
		assertTrue( "A valid configuration could not be started.", configurationIsLegal );
	}
	
	@Test
	public void testInconsistentSimilarityInClassHierarchy() {
		boolean configurationIsLegal = true;
		FullTextSessionBuilder builder = null;
		try {
			builder = new FullTextSessionBuilder()
					.addAnnotatedClass( Trash.class )
					.addAnnotatedClass( LittleTrash.class ).build();
		} catch (Exception e) {
			configurationIsLegal = false;
		}
		finally {
			if (builder!=null)
				builder.close();
		}
		assertFalse( "Invalid Similarity declared, should have thrown an exception: same similarity"
				+ " must be used across class hierarchy", configurationIsLegal );
	}
	
	@Test
	public void testInconsistentSimilarityInClassSharingAnIndex() {
		boolean configurationIsLegal = true;
		FullTextSessionBuilder builder = null;
		try {
			builder = new FullTextSessionBuilder()
					.addAnnotatedClass( Trash.class )
					.addAnnotatedClass( Sink.class ).build();
		} catch (Exception e) {
			configurationIsLegal = false;
		}
		finally {
			if (builder!=null)
				builder.close();
		}
		assertFalse( "Invalid Similarity declared, should have thrown an exception: two entities"
				+ "sharing the same index are using a different similarity", configurationIsLegal );
	}
	
	@Test
	public void testImplicitSimilarityInheritanceIsValid() {
		boolean configurationIsLegal = true;
		FullTextSessionBuilder builder = null;
		try {
			builder = new FullTextSessionBuilder()
					.addAnnotatedClass( Trash.class )
					.addAnnotatedClass( ProperTrashExtension.class ).build();
		} catch (Exception e) {
			configurationIsLegal = false;
		}
		finally {
			if (builder!=null)
				builder.close();
		}
		assertTrue( "Valid configuration could not be built", configurationIsLegal );
	}
	
	@Test
	public void testInvalidToOverrideParentsSimilarity() {
		boolean configurationIsLegal = true;
		FullTextSessionBuilder builder = null;
		try {
			builder = new FullTextSessionBuilder()
					.addAnnotatedClass( Can.class )
					.addAnnotatedClass( SmallerCan.class ).build();
		} catch (Exception e) {
			configurationIsLegal = false;
		}
		finally {
			if (builder!=null)
				builder.close();
		}
		assertFalse( "Invalid Similarity declared, should have thrown an exception: child entity"
				+ " is overriding parent's Similarity", configurationIsLegal );
	}

}

