/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.analyzer.common;

import java.util.Set;

import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.AnalyzerDefs;
import org.hibernate.search.annotations.AnalyzerDiscriminator;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.annotations.TokenFilterDef;
import org.hibernate.search.annotations.TokenizerDef;

import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.de.GermanStemFilterFactory;
import org.apache.lucene.analysis.snowball.SnowballPorterFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;

/**
 * @author Hardy Ferentschik
 */
@Indexed
@AnalyzerDefs({
		@AnalyzerDef(name = "en",
				tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class),
				filters = {
						@TokenFilterDef(factory = LowerCaseFilterFactory.class),
						@TokenFilterDef(factory = SnowballPorterFilterFactory.class
						)
				}),
		@AnalyzerDef(name = "de",
				tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class),
				filters = {
						@TokenFilterDef(factory = LowerCaseFilterFactory.class),
						@TokenFilterDef(factory = GermanStemFilterFactory.class)
				})
})
@AnalyzerDiscriminator(impl = LanguageDiscriminator.class)
public class BlogEntry {

	private Integer id;
	private String language;
	private String text;
	private Set<BlogEntry> references;

	@DocumentId
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Field(store = Store.YES)
	@AnalyzerDiscriminator(impl = LanguageDiscriminator.class)
	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	@Field(store = Store.YES)
	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	@IndexedEmbedded(depth = 1)
	public Set<BlogEntry> getReferences() {
		return references;
	}

	public void setReferences(Set<BlogEntry> references) {
		this.references = references;
	}
}
