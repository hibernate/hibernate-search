/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.analyzer.common;

import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.ClassBridge;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.TokenFilterDef;
import org.hibernate.search.annotations.TokenizerDef;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.junit.SearchITHelper;

import org.junit.Rule;
import org.junit.Test;

import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.WhitespaceTokenizerFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;

/**
 * Equivalent of {@link CustomAnalyzerImplementationInClassBridgeTest}, but using analyzer
 * definitions instead of custom analyzer implementations.
 *
 * @author Hardy Ferentschik
 */
public class CustomAnalyzerDefinitionInClassBridgeTest {

	@Rule
	public final SearchFactoryHolder sfHolder = new SearchFactoryHolder( FooBar.class );

	private final SearchITHelper helper = new SearchITHelper( sfHolder );

	@Test
	public void testClassBridgeWithSingleField() throws Exception {
		helper.index( new FooBar( 1 ) );

		TermQuery termQuery = new TermQuery( new Term( "classField", "dog" ) );

		helper.assertThat( termQuery )
				.as( "custom analyzer should have inserted search token" )
				.matchesExactlyIds( 1 );
	}

	@Indexed
	@AnalyzerDef(
			name = "foobarAnalyzer",
			tokenizer = @TokenizerDef(factory = WhitespaceTokenizerFactory.class),
			filters = @TokenFilterDef(factory = LowerCaseFilterFactory.class)
	)
	@ClassBridge(name = "classField", impl = FooBarBridge.class, analyzer = @Analyzer(definition = "foobarAnalyzer"))
	public static class FooBar {
		@DocumentId
		private Integer id;

		public FooBar(Integer id) {
			this.id = id;
		}
	}

	public static class FooBarBridge implements FieldBridge {

		@Override
		public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
			luceneOptions.addFieldToDocument( name, "alarm Dog performance", document );
		}
	}
}
