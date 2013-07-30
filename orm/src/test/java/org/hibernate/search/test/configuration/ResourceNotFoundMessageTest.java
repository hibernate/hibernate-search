/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat, Inc. and/or its affiliates or third-party contributors as
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

package org.hibernate.search.test.configuration;

import org.apache.solr.analysis.LowerCaseFilterFactory;
import org.apache.solr.analysis.StandardTokenizerFactory;
import org.apache.solr.analysis.StopFilterFactory;

import org.hibernate.search.Environment;
import org.hibernate.search.SearchException;
import org.hibernate.search.annotations.Factory;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.test.util.FullTextSessionBuilder;

import org.junit.Assert;
import org.junit.Test;

/**
 * Verifies a proper message is thrown when a resource is not found
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class ResourceNotFoundMessageTest {

	@Test
	public void testIllegalAnalyzerDefinition() {
		try {
			new FullTextSessionBuilder()
					.addAnnotatedClass( User.class )
					.setProperty( Environment.MODEL_MAPPING, ResourceNotFoundMessageTest.class.getName() )
					.build();
			Assert.fail( "should not reach this" );
		}
		catch (SearchException initException) {
			String message = initException.getMessage();
			Assert.assertEquals( "Resource not found: non-existent-resourcename.file", message );
		}
	}

	@Factory
	public SearchMapping build() {
		SearchMapping mapping = new SearchMapping();
		mapping
				.analyzerDef( "ngram", StandardTokenizerFactory.class )
				.filter( LowerCaseFilterFactory.class )
				.filter( StopFilterFactory.class )
				.param( "words", "non-existent-resourcename.file" );
		return mapping;
	}
}
