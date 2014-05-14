/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.configuration;

import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.apache.lucene.analysis.core.StopFilterFactory;

import org.hibernate.search.cfg.Environment;
import org.hibernate.search.exception.SearchException;
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
			Assert.assertEquals( "HSEARCH000114: Could not load resource: 'non-existent-resourcename.file'", message );
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
