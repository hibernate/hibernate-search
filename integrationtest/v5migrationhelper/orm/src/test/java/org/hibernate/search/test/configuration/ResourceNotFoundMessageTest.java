/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.configuration;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.apache.lucene.analysis.core.StopFilterFactory;

import org.hibernate.search.cfg.Environment;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.annotations.Factory;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.test.util.FullTextSessionBuilder;
import org.hibernate.search.testsupport.junit.SkipOnElasticsearch;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Verifies a proper message is thrown when a resource is not found
 *
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
@Category(SkipOnElasticsearch.class) // This test is specific to Lucene
public class ResourceNotFoundMessageTest {

	@Test
	public void testIllegalAnalyzerDefinition() {
		try {
			new FullTextSessionBuilder()
					.addAnnotatedClass( SomeHibernateEntity.class )
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
				.param( "words", "non-existent-resourcename.file" )
				// We must mark at least one entity as indexed, otherwise analyzer definitions are not initialized (no need to)
				.entity( SomeHibernateEntity.class ).indexed();
		return mapping;
	}

	@Entity
	private static class SomeHibernateEntity {
		@Id
		private Long id;
	}
}
