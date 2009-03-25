// $Id$
package org.hibernate.search.test.analyzer;

import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.apache.solr.analysis.EnglishPorterFilterFactory;
import org.apache.solr.analysis.GermanStemFilterFactory;
import org.apache.solr.analysis.LowerCaseFilterFactory;
import org.apache.solr.analysis.StandardTokenizerFactory;

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

/**
 * @author Hardy Ferentschik
 */
@Entity
@Indexed
@AnalyzerDefs({
		@AnalyzerDef(name = "en",
				tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class),
				filters = {
						@TokenFilterDef(factory = LowerCaseFilterFactory.class),
						@TokenFilterDef(factory = EnglishPorterFilterFactory.class
						)
				}),
		@AnalyzerDef(name = "de",
				tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class),
				filters = {
						@TokenFilterDef(factory = LowerCaseFilterFactory.class),
						@TokenFilterDef(factory = GermanStemFilterFactory.class)
				})
})
public class Article {

	private Integer id;
	private String language;
	private String text;
	private Set<Article> references;

	@Id
	@GeneratedValue
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

	@OneToMany(cascade = CascadeType.ALL)
	@IndexedEmbedded(depth = 1)
	public Set<Article> getReferences() {
		return references;
	}

	public void setReferences(Set<Article> references) {
		this.references = references;
	}
}

