package org.hibernate.search.test.configuration;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;

import org.apache.solr.analysis.StandardTokenizerFactory;
import org.apache.solr.analysis.LowerCaseFilterFactory;
import org.apache.solr.analysis.EnglishPorterFilterFactory;
import org.apache.solr.analysis.GermanStemFilterFactory;

import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.AnalyzerDefs;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.TokenizerDef;
import org.hibernate.search.annotations.TokenFilterDef;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.AnalyzerDiscriminator;
import org.hibernate.search.analyzer.Discriminator;

/**
 * @author Emmanuel Bernard
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
public class BlogEntry {
	private Long id;
	private String language;
	private String title;
	private String description;

	@Id @GeneratedValue
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public static class BlogLangDiscriminator implements Discriminator {

		public String getAnalyzerDefinitionName(Object value, Object entity, String field) {
			if ( value == null ) return null;
			if ( !( value instanceof String ) )
				throw new IllegalArgumentException( "expecte string as value in language discriminator");
			if ( "description".equals( field ) ) {
				return (String) value;
			}
			else {
				//"title" is not affected
				return null;
			}

		}
	}
}
