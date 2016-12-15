/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.analyzer;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.analyzer.Discriminator;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.AnalyzerDefs;
import org.hibernate.search.annotations.AnalyzerDiscriminator;
import org.hibernate.search.annotations.ClassBridge;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.TokenizerDef;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.MetadataProvidingFieldBridge;
import org.hibernate.search.bridge.spi.FieldMetadataBuilder;
import org.hibernate.search.bridge.spi.FieldType;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.junit.SkipOnElasticsearch;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.assertEquals;

/**
 * Add test to the various ways to customize the analyzer used for
 * fields created by a ClassBridge
 *
 * Test bed of the following blog entry
 * http://in.relation.to/Bloggers/CustomAnalyzersForFieldsDefinedInClassOrFieldBridges
 *
 * @author Hardy Ferentschik
 */
@Category(SkipOnElasticsearch.class) // Custom analyzer implementations cannot be used with Elasticsearch
public class CustomAnalyzerImplementationInClassBridgeTest extends SearchTestBase {

	public static final Log log = LoggerFactory.make();

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { Foo.class, Bar.class };
	}

	@Test
	public void testCustomAnalyzersAppliedForFieldsAddedInClassBridge() throws Exception {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();
		fullTextSession.persist( new Foo() );
		tx.commit();
		fullTextSession.clear();

		String[] searchTokens = new String[] { "dog", "cat", "mouse" };
		for ( int i = 0; i < searchTokens.length; i++ ) {
			TermQuery termQuery = new TermQuery( new Term( FooBridge.fieldNames[i], searchTokens[i] ) );
			FullTextQuery query = fullTextSession.createFullTextQuery( termQuery );

			assertEquals(
					"custom analyzer should have inserted search token",
					1,
					query.getResultSize()
			);
		}

		fullTextSession.close();
	}

	@Test
	public void testClassBridgeWithSingleField() throws Exception {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();
		fullTextSession.persist( new Bar() );
		tx.commit();
		fullTextSession.clear();

		TermQuery termQuery = new TermQuery( new Term( "classField", "dog" ) );
		FullTextQuery query = fullTextSession.createFullTextQuery( termQuery );

		assertEquals(
				"custom analyzer should have inserted search token",
				1,
				query.getResultSize()
		);

		fullTextSession.close();
	}

	@Entity
	@Table(name = "JUSTFOO")
	@Indexed
	@ClassBridge(impl = FooBridge.class)
	@AnalyzerDiscriminator(impl = FooBridge.class)
	@AnalyzerDefs({
			@AnalyzerDef(name = "analyzer1", tokenizer = @TokenizerDef(factory = TestTokenizer.TestTokenizer1.class)),
			@AnalyzerDef(name = "analyzer2", tokenizer = @TokenizerDef(factory = TestTokenizer.TestTokenizer2.class)),
			@AnalyzerDef(name = "analyzer3", tokenizer = @TokenizerDef(factory = TestTokenizer.TestTokenizer3.class))
	})
	public static class Foo {
		@Id
		@GeneratedValue
		private Integer id;
	}

	public static class FooBridge implements Discriminator, MetadataProvidingFieldBridge {

		public static final String[] fieldNames = new String[] { "field1", "field2", "field3" };
		public static final String[] analyzerNames = new String[] { "analyzer1", "analyzer2", "analyzer3" };

		@Override
		public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
			for ( String fieldName : fieldNames ) {
				luceneOptions.addFieldToDocument( fieldName, "This text will be replaced by the test analyzers", document );
			}
		}

		@Override
		public String getAnalyzerDefinitionName(Object value, Object entity, String field) {
			for ( int i = 0; i < fieldNames.length; i++ ) {
				if ( fieldNames[i].equals( field ) ) {
					return analyzerNames[i];
				}
			}
			return null;
		}

		@Override
		public void configureFieldMetadata(String name, FieldMetadataBuilder builder) {
			for ( String field : fieldNames ) {
				builder.field( field, FieldType.STRING );
			}
		}
	}

	@Entity
	@Table(name = "JUSTBAR")
	@Indexed
	@ClassBridge(name = "classField", impl = BarBridge.class, analyzer = @Analyzer(impl = AnalyzerForTests1.class))
	public static class Bar {
		@Id
		@GeneratedValue
		private Integer id;
	}

	public static class BarBridge implements FieldBridge {

		@Override
		public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
			luceneOptions.addFieldToDocument( name, "This text will be replaced by the test analyzers", document );
		}
	}

}
