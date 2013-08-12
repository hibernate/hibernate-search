/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat, Inc. and/or its affiliates or third-party contributors as
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

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Fieldable;
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
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Add test to the various ways to customize the analyzer used for
 * fields created by a ClassBridge
 *
 * Test bed of the following blog entry
 * http://in.relation.to/Bloggers/CustomAnalyzersForFieldsDefinedInClassOrFieldBridges
 *
 * @author Hardy Ferentschik
 */
public class CustomAnalyzerInClassBridgeTest extends SearchTestCase {

	public static final Log log = LoggerFactory.make();

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Foo.class, Bar.class };
	}

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

	public static class FooBridge implements Discriminator, FieldBridge {

		public static final String[] fieldNames = new String[] { "field1", "field2", "field3" };
		public static final String[] analyzerNames = new String[] { "analyzer1", "analyzer2", "analyzer3" };

		@Override
		public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
			for ( String fieldName : fieldNames ) {
				Fieldable field = new Field(
						fieldName,
						"This text will be replaced by the test analyzers",
						luceneOptions.getStore(),
						luceneOptions.getIndex(),
						luceneOptions.getTermVector()
				);
				field.setBoost( luceneOptions.getBoost() );
				document.add( field );
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
			Fieldable field = new Field(
					name,
					"This text will be replaced by the test analyzers",
					luceneOptions.getStore(),
					luceneOptions.getIndex(),
					luceneOptions.getTermVector()
			);
			field.setBoost( luceneOptions.getBoost() );
			document.add( field );
		}
	}
}
