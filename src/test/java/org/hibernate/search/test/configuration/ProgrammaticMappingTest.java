package org.hibernate.search.test.configuration;

import java.lang.annotation.ElementType;

import org.apache.solr.analysis.StandardTokenizerFactory;
import org.apache.solr.analysis.SnowballPorterFilterFactory;
import org.apache.solr.analysis.LowerCaseFilterFactory;
import org.apache.solr.analysis.NGramFilterFactory;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.test.analyzer.inheritance.ISOLatin1Analyzer;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.store.RAMDirectoryProvider;
import org.hibernate.search.store.FSDirectoryProvider;
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
		org.apache.lucene.search.Query luceneQuery = parser.parse( "street1:peachtree" );
		FullTextQuery query = s.createFullTextQuery( luceneQuery ).setProjection( "idx_street2", FullTextQuery.THIS );
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
		System.out.print( luceneQuery.toString() );
		final FullTextQuery query = s.createFullTextQuery( luceneQuery );
		assertEquals( "Analyzer inoperant", 1, query.getResultSize() );

		s.delete( query.list().get( 0 ));
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
					.property( "street1", ElementType.FIELD )
						.field()
						.field().name( "street1_ngram" ).analyzer( "ngram" )
					.property( "street2", ElementType.METHOD )
						.field().name( "idx_street2" ).store( Store.YES );
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