/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.engine;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.DisjunctionMaxQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.MultiPhraseQuery;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.RegexpQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.search.query.engine.impl.FieldNameCollector;

import static org.junit.Assert.fail;

/**
 * @author Hardy Ferentschik
 */
public class FieldNameCollectorTest {
	DirectoryReader indexReader;

	@Before
	public void setUp() throws Exception {
		Directory directory = new RAMDirectory();
		indexTestDocuments( directory );
		indexReader = DirectoryReader.open( directory );
	}

	@After
	public void tearDown() throws Exception {
		indexReader.close();
	}

	@Test
	public void testExtractFieldNameFromTermQuery() {
		TermQuery query = new TermQuery( new Term( "stringField", "foobar" ) );
		assertFieldNames( query, FieldType.STRING, "stringField" );
	}

	@Test
	public void testExtractFieldNameFromWildcardQuery() {
		WildcardQuery query = new WildcardQuery( new Term( "stringField", "foo*" ) );
		assertFieldNames( query, FieldType.STRING, "stringField" );
	}

	@Test
	public void testExtractFieldNameFromFuzzyQuery() {
		FuzzyQuery query = new FuzzyQuery( new Term( "stringField", "foo*" ) );
		assertFieldNames( query, FieldType.STRING, "stringField" );
	}

	@Test
	public void testExtractFieldNameFromRegexpQuery() {
		RegexpQuery query = new RegexpQuery( new Term( "stringField", ".foo?" ) );
		assertFieldNames( query, FieldType.STRING, "stringField" );
	}

	@Test
	public void testExtractFieldNameFromPrefixQuery() {
		PrefixQuery query = new PrefixQuery( new Term( "stringField", "foo*" ) );
		assertFieldNames( query, FieldType.STRING, "stringField" );
	}

	@Test
	public void testExtractFieldNameFromMultiPhraseQuery() {
		MultiPhraseQuery phraseQuery = new MultiPhraseQuery();
		phraseQuery.add( new Term( "stringField1", "hello world" ) );
		assertFieldNames( phraseQuery, FieldType.STRING, "stringField1" );
	}

	@Test
	public void testExtractFieldNameFromPhraseQuery() {
		PhraseQuery.Builder phraseQBuilder = new PhraseQuery.Builder();
		phraseQBuilder.add( new Term( "stringField", "hello world" ) );
		PhraseQuery phraseQuery = phraseQBuilder.build();
		assertFieldNames( phraseQuery, FieldType.STRING, "stringField" );
	}

	@Test
	public void testExtractFieldNameFromTermRangeQuery() {
		TermRangeQuery query = TermRangeQuery.newStringRange( "stringField", "A", "Z", true, true );
		assertFieldNames( query, FieldType.STRING, "stringField" );
	}

	@Test
	public void testExtractFieldNameFromNumericRangeQuery() {
		NumericRangeQuery query = NumericRangeQuery.newIntRange( "intField", 0, 0, true, true );
		assertFieldNames( query, FieldType.NUMBER, "intField" );
	}

	@Test
	public void testBooleanQuery() {
		BooleanQuery.Builder booleanQueryBuilder = new BooleanQuery.Builder();

		TermQuery termQuery = new TermQuery( new Term( "stringField", "foobar" ) );
		booleanQueryBuilder.add( termQuery, BooleanClause.Occur.MUST );

		NumericRangeQuery numericRangeQuery = NumericRangeQuery.newIntRange( "intField", 0, 0, true, true );
		booleanQueryBuilder.add( numericRangeQuery, BooleanClause.Occur.SHOULD );

		BooleanQuery booleanQuery = booleanQueryBuilder.build();

		assertFieldNames( booleanQuery, FieldType.NUMBER, "intField" );
		assertFieldNames( booleanQuery, FieldType.STRING, "stringField" );
	}

	@Test
	public void testNestedBooleanQuery() {
		BooleanQuery.Builder booleanQueryBuilder = new BooleanQuery.Builder();

		TermQuery termQuery = new TermQuery( new Term( "stringField", "foobar" ) );
		booleanQueryBuilder.add( termQuery, BooleanClause.Occur.MUST );

		BooleanQuery.Builder nestedBuilder = new BooleanQuery.Builder();
		NumericRangeQuery numericRangeQuery = NumericRangeQuery.newIntRange( "intField", 0, 0, true, true );
		nestedBuilder.add( numericRangeQuery, BooleanClause.Occur.SHOULD );
		BooleanQuery nestedBooleanQuery = nestedBuilder.build();
		booleanQueryBuilder.add( nestedBooleanQuery, BooleanClause.Occur.MUST );

		BooleanQuery booleanQuery = booleanQueryBuilder.build();

		assertFieldNames( booleanQuery, FieldType.NUMBER, "intField" );
		assertFieldNames( booleanQuery, FieldType.STRING, "stringField" );
	}

	@Test
	public void testDisjunctionMaxQuery() {
		Set<Query> queriesToCombine = new HashSet<>();

		TermQuery termQuery = new TermQuery( new Term( "stringField", "foobar" ) );
		queriesToCombine.add( termQuery );

		NumericRangeQuery numericRangeQuery = NumericRangeQuery.newIntRange( "intField", 0, 0, true, true );
		queriesToCombine.add( numericRangeQuery );

		DisjunctionMaxQuery disjunctionMaxQuery = new DisjunctionMaxQuery( queriesToCombine, 0.0f );

		assertFieldNames( disjunctionMaxQuery, FieldType.NUMBER, "intField" );
		assertFieldNames( disjunctionMaxQuery, FieldType.STRING, "stringField" );
	}

	private void assertFieldNames(Query query, FieldType fieldType, String... expectedFields) {
		FieldNameCollector.FieldCollection fieldCollection = FieldNameCollector.extractFieldNames( query );
		Set<String> actualFieldNames = new HashSet<>();
		if ( FieldType.STRING.equals( fieldType ) ) {
			actualFieldNames.addAll( fieldCollection.getStringFieldNames() );
		}
		else {
			actualFieldNames.addAll( fieldCollection.getNumericFieldNames() );
		}

		for ( String expectedFieldName : expectedFields ) {
			if ( !actualFieldNames.contains( expectedFieldName ) ) {
				fail( "The expected field name " + expectedFieldName + " was not found in the actual field names: " + actualFieldNames );
			}
			actualFieldNames.remove( expectedFieldName );
		}

		if ( !actualFieldNames.isEmpty() ) {
			fail( "There were field names which were unexpected: " + actualFieldNames );
		}
	}

	private void indexTestDocuments(Directory directory) throws IOException {
		IndexWriterConfig indexWriterConfig = new IndexWriterConfig( new StandardAnalyzer() );
		indexWriterConfig.setOpenMode( IndexWriterConfig.OpenMode.CREATE );
		IndexWriter indexWriter = new IndexWriter( directory, indexWriterConfig );
		Document document = new Document();
		document.add( new StringField( "stringField", "test", Field.Store.NO ) );
		document.add( new IntField( "intField", 0, Field.Store.NO ) );
		indexWriter.addDocument( document );
		indexWriter.commit();
		indexWriter.close();
	}

	enum FieldType {
		STRING,
		NUMBER
	}
}


