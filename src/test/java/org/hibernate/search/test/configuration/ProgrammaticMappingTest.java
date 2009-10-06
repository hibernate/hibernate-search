/* $Id$
 * 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * Copyright (c) 2009, Red Hat, Inc. and/or its affiliates or third-party contributors as
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

import java.lang.annotation.ElementType;
import java.util.List;

import org.apache.solr.analysis.StandardTokenizerFactory;
import org.apache.solr.analysis.SnowballPorterFilterFactory;
import org.apache.solr.analysis.LowerCaseFilterFactory;
import org.apache.solr.analysis.NGramFilterFactory;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.search.DefaultSimilarity;

import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.cfg.ConcatStringBridge;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.test.analyzer.inheritance.ISOLatin1Analyzer;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.FullTextQuery;
import org.hibernate.cfg.Configuration;
import org.hibernate.Transaction;

/**
 * @author Emmanuel Bernard
 */
public class ProgrammaticMappingTest extends SearchTestCase {

	public void testMapping() throws Exception{
		Address address = new Address();
		address.setStreet1( "3340 Peachtree Rd NE" );
		address.setStreet2( "JBoss" );

		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();
		s.persist( address );
		tx.commit();

		s.clear();

		tx = s.beginTransaction();

		QueryParser parser = new QueryParser( "id", new StandardAnalyzer( ) );
		org.apache.lucene.search.Query luceneQuery = parser.parse( "" + address.getAddressId() );
		System.out.println(luceneQuery.toString(  ));
		FullTextQuery query = s.createFullTextQuery( luceneQuery );
		assertEquals( "documenId does not work properly", 1, query.getResultSize() );

		luceneQuery = parser.parse( "street1:peachtree" );
		query = s.createFullTextQuery( luceneQuery ).setProjection( "idx_street2", FullTextQuery.THIS );
		assertEquals( "Not properly indexed", 1, query.getResultSize() );
		Object[] firstResult = (Object[]) query.list().get( 0 );
		assertEquals( "@Field.store not respected", "JBoss", firstResult[0] );

		s.delete( firstResult[1] );
		tx.commit();
		s.close();
	}

	public void testAnalyzerDef() throws Exception{
		Address address = new Address();
		address.setStreet1( "3340 Peachtree Rd NE" );
		address.setStreet2( "JBoss" );

		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();
		s.persist( address );
		tx.commit();

		s.clear();

		tx = s.beginTransaction();

		QueryParser parser = new QueryParser( "id", new StandardAnalyzer( ) );
		org.apache.lucene.search.Query luceneQuery =  parser.parse( "street1_ngram:pea" );

		final FullTextQuery query = s.createFullTextQuery( luceneQuery );
		assertEquals( "Analyzer inoperant", 1, query.getResultSize() );

		s.delete( query.list().get( 0 ));
		tx.commit();
		s.close();

	}

	public void testBridgeMapping() throws Exception{
		Address address = new Address();
		address.setStreet1( "Peachtree Rd NE" );
		address.setStreet2( "JBoss" );

		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();
		s.persist( address );
		tx.commit();

		s.clear();

		tx = s.beginTransaction();

		QueryParser parser = new QueryParser( "id", new StandardAnalyzer( ) );
		org.apache.lucene.search.Query luceneQuery = parser.parse( "street1:peac" );
		FullTextQuery query = s.createFullTextQuery( luceneQuery );
		assertEquals( "PrefixQuery should not be on", 0, query.getResultSize() );

		luceneQuery = parser.parse( "street1_abridged:peac" );
		query = s.createFullTextQuery( luceneQuery );
		assertEquals( "Bridge not used", 1, query.getResultSize() );

		s.delete( query.list().get( 0 ) );
		tx.commit();
		s.close();
	}

	public void testBoost() throws Exception{
		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();

		Address address = new Address();
		address.setStreet1( "Peachtree Rd NE" );
		address.setStreet2( "Peachtnot Rd NE" );
		s.persist( address );

		address = new Address();
		address.setStreet1( "Peachtnot Rd NE" );
		address.setStreet2( "Peachtree Rd NE" );
		s.persist( address );

		tx.commit();

		s.clear();

		tx = s.beginTransaction();

		QueryParser parser = new QueryParser( "id", new StandardAnalyzer( ) );
		org.apache.lucene.search.Query luceneQuery = parser.parse( "street1:peachtree OR idx_street2:peachtree" );
		FullTextQuery query = s.createFullTextQuery( luceneQuery ).setProjection( FullTextQuery.THIS, FullTextQuery.SCORE );
		assertEquals( "expecting two results", 2, query.getResultSize() );

		@SuppressWarnings( "unchecked" )
		List<Object[]> results = query.list();

		assertTrue( "first result should be strictly higher", (Float) results.get( 0 )[1] > (Float) results.get( 1 )[1]*1.9f );
		assertEquals( "Wrong result ordered", address.getStreet1(), ( (Address) results.get( 0 )[0] ).getStreet1() );
		for( Object[] result : results ) {
			s.delete( result[0] );
		}
		tx.commit();
		s.close();
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		//cfg.setProperty( "hibernate.search.default.directory_provider", FSDirectoryProvider.class.getName() );
		SearchMapping mapping = new SearchMapping();
		mapping.analyzerDef( "ngram", StandardTokenizerFactory.class )
					.filter( LowerCaseFilterFactory.class )
					.filter( NGramFilterFactory.class )
						.param( "minGramSize", "3" )
						.param( "maxGramSize", "3" )

				.indexedClass( Address.class )
					.similarity( DefaultSimilarity.class )
					.boost( 2 )
					.property( "addressId", ElementType.FIELD ).documentId().name( "id" )
					.property( "street1", ElementType.FIELD )
						.field()
						.field().name( "street1_ngram" ).analyzer( "ngram" )
						.field()
							.name( "street1_abridged" )
							.bridge( ConcatStringBridge.class ).param( ConcatStringBridge.SIZE, "4" )
					.property( "street2", ElementType.METHOD )
						.field().name( "idx_street2" ).store( Store.YES ).boost( 2 );
		cfg.getProperties().put( "hibernate.search.mapping_model", mapping );
	}

	public void NotUseddefineMapping() {
		SearchMapping mapping = new SearchMapping();
		mapping.analyzerDef( "stem", StandardTokenizerFactory.class )
					.tokenizerParam( "name", "value" )
					.tokenizerParam(  "name2", "value2" )
					.filter( LowerCaseFilterFactory.class )
					.filter( SnowballPorterFilterFactory.class)
						.param("language", "English")
				.analyzerDef( "ngram", StandardTokenizerFactory.class )
					.tokenizerParam( "name", "value" )
					.tokenizerParam(  "name2", "value2" )
					.filter( LowerCaseFilterFactory.class )
					.filter( NGramFilterFactory.class)
						.param("minGramSize", "3")
						.param("maxGramSize", "3")
				.indexedClass(Address.class, "Address_Index")
					.property("street1", ElementType.FIELD)
						.field()
						.field()
							.name("street1_iso")
							.store( Store.YES )
							.index( Index.TOKENIZED )
							.analyzer( ISOLatin1Analyzer.class)
						.field()
							.name("street1_ngram")
							.analyzer("ngram")
				.indexedClass(User.class)
					.property("name", ElementType.METHOD)
						.field()
				.analyzerDef( "minimal", StandardTokenizerFactory.class  );

	}

	protected Class[] getMappings() {
		return new Class[] {
				Address.class,
				Country.class
		};
	}
}
