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
package org.hibernate.search.test.analyzer;

import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.queryParser.QueryParser;

import org.hibernate.Transaction;

import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.SearchException;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.cfg.impl.SearchConfigurationFromHibernateCore;
import org.hibernate.search.engine.metadata.impl.AnnotationMetadataProvider;
import org.hibernate.search.engine.metadata.impl.MetadataProvider;
import org.hibernate.search.impl.ConfigContext;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.test.TestConstants;
import org.hibernate.search.util.AnalyzerUtils;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import org.junit.Assert;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public class AnalyzerTest extends SearchTestCase {

	public static final Log log = LoggerFactory.make();

	public void testAnalyzerDiscriminator() throws Exception {
		Article germanArticle = new Article();
		germanArticle.setLanguage( "de" );
		germanArticle.setText( "aufeinanderschl\u00FCgen" );
		Set<Article> references = new HashSet<Article>();
		references.add( germanArticle );

		Article englishArticle = new Article();
		englishArticle.setLanguage( "en" );
		englishArticle.setText( "acknowledgment" );
		englishArticle.setReferences( references );

		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();
		s.persist( englishArticle );
		tx.commit();

		tx = s.beginTransaction();

		// at query time we use a standard analyzer. We explicitly search for tokens which can only be found if the
		// right language specific stemmer was used at index time
		QueryParser parser = new QueryParser(
				TestConstants.getTargetLuceneVersion(),
				"references.text",
				TestConstants.standardAnalyzer
		);
		org.apache.lucene.search.Query luceneQuery = parser.parse( "aufeinanderschlug" );
		FullTextQuery query = s.createFullTextQuery( luceneQuery );
		assertEquals( 1, query.getResultSize() );

		parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "text", TestConstants.standardAnalyzer );
		luceneQuery = parser.parse( "acknowledg" );
		query = s.createFullTextQuery( luceneQuery );
		assertEquals( 1, query.getResultSize() );

		tx.commit();
		s.close();
	}

	public void testMultipleAnalyzerDiscriminatorDefinitions() {
		SearchConfigurationFromHibernateCore searchConfig = new SearchConfigurationFromHibernateCore( getCfg() );
		ReflectionManager reflectionManager = searchConfig.getReflectionManager();
		XClass xclass = reflectionManager.toXClass( BlogEntry.class );
		ConfigContext context = new ConfigContext( searchConfig );
		MetadataProvider metadataProvider = new AnnotationMetadataProvider(
				searchConfig.getReflectionManager(),
				context
		);

		try {
			metadataProvider.getTypeMetadataFor( reflectionManager.toClass( xclass ) );
			fail();
		}
		catch (SearchException e) {
			assertTrue(
					"Wrong error message",
					e.getMessage().startsWith( "Multiple AnalyzerDiscriminator defined in the same class hierarchy" )
			);
		}
	}

	public void testScopedAnalyzers() throws Exception {
		MyEntity en = new MyEntity();
		en.setEntity( "Entity" );
		en.setField( "Field" );
		en.setProperty( "Property" );
		en.setComponent( new MyComponent() );
		en.getComponent().setComponentProperty( "component property" );
		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();
		s.persist( en );
		tx.commit();

		tx = s.beginTransaction();
		QueryParser parser = new QueryParser(
				TestConstants.getTargetLuceneVersion(),
				"id",
				TestConstants.standardAnalyzer
		);
		org.apache.lucene.search.Query luceneQuery = parser.parse( "entity:alarm" );
		FullTextQuery query = s.createFullTextQuery( luceneQuery, MyEntity.class );
		assertEquals( 1, query.getResultSize() );

		luceneQuery = parser.parse( "property:cat" );
		query = s.createFullTextQuery( luceneQuery, MyEntity.class );
		assertEquals( 1, query.getResultSize() );

		luceneQuery = parser.parse( "field:energy" );
		query = s.createFullTextQuery( luceneQuery, MyEntity.class );
		assertEquals( 1, query.getResultSize() );

		luceneQuery = parser.parse( "component.componentProperty:noise" );
		query = s.createFullTextQuery( luceneQuery, MyEntity.class );
		assertEquals( 1, query.getResultSize() );

		s.delete( query.uniqueResult() );
		tx.commit();

		s.close();
	}

	public void testScopedAnalyzersFromSearchFactory() throws Exception {
		FullTextSession session = Search.getFullTextSession( openSession() );
		SearchFactory searchFactory = session.getSearchFactory();
		Analyzer analyzer = searchFactory.getAnalyzer( MyEntity.class );

		// you can pass what so ever into the analysis since the used analyzers are
		// returning the same tokens all the time. We just want to make sure that
		// the right analyzers are used.
		Token[] tokens = AnalyzerUtils.tokensFromAnalysis( analyzer, "entity", "" );
		assertTokensEqual( tokens, new String[] { "alarm", "dog", "performance" } );

		tokens = AnalyzerUtils.tokensFromAnalysis( analyzer, "property", "" );
		assertTokensEqual( tokens, new String[] { "sound", "cat", "speed" } );

		tokens = AnalyzerUtils.tokensFromAnalysis( analyzer, "field", "" );
		assertTokensEqual( tokens, new String[] { "music", "elephant", "energy" } );

		tokens = AnalyzerUtils.tokensFromAnalysis( analyzer, "component.componentProperty", "" );
		assertTokensEqual( tokens, new String[] { "noise", "mouse", "light" } );

		// test border cases
		try {
			searchFactory.getAnalyzer( (Class) null );
		}
		catch (IllegalArgumentException iae) {
			log.debug( "success" );
		}

		try {
			searchFactory.getAnalyzer( String.class );
		}
		catch (IllegalArgumentException iae) {
			log.debug( "success" );
		}

		session.close();
	}

	public void testNotAnalyzedFieldAndScopedAnalyzer() throws Exception {
		FullTextSession session = Search.getFullTextSession( openSession() );
		SearchFactory searchFactory = session.getSearchFactory();
		Analyzer analyzer = searchFactory.getAnalyzer( MyEntity.class );

		// you can pass what so ever into the analysis since the used analyzers are
		// returning the same tokens all the time. We just want to make sure that
		// the right analyzers are used.
		Token[] tokens = AnalyzerUtils.tokensFromAnalysis( analyzer, "notAnalyzed", "pass through" );
		assertTokensEqual( tokens, new String[] { "pass through" } );

		session.close();
	}

	public static void assertTokensEqual(Token[] tokens, String[] strings) {
		Assert.assertEquals( strings.length, tokens.length );

		for ( int i = 0; i < tokens.length; i++ ) {
			Assert.assertEquals( "index " + i, strings[i], AnalyzerUtils.getTermText( tokens[i] ) );
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { MyEntity.class, Article.class };
	}
}
