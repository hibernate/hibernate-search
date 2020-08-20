/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.apache.lucene.analysis.de.GermanStemFilterFactory;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.snowball.SnowballPorterFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.hibernate.search.analyzer.Discriminator;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.AnalyzerDefs;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.TokenFilterDef;
import org.hibernate.search.annotations.TokenizerDef;
import org.hibernate.search.exception.AssertionFailure;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Indexed
@AnalyzerDefs({
		@AnalyzerDef(name = BlogEntry.EN_ANALYZER_NAME,
				tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class),
				filters = {
						@TokenFilterDef(factory = LowerCaseFilterFactory.class),
						@TokenFilterDef(factory = SnowballPorterFilterFactory.class
						)
				}),
		@AnalyzerDef(name = BlogEntry.DE_ANALYZER_NAME,
				tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class),
				filters = {
						@TokenFilterDef(factory = LowerCaseFilterFactory.class),
						@TokenFilterDef(factory = GermanStemFilterFactory.class)
				})
})
public class BlogEntry {

	public static final String EN_ANALYZER_NAME = "org_hibernate_search_test_configuration_BlogEntry" + "_en";
	public static final String DE_ANALYZER_NAME = "org_hibernate_search_test_configuration_BlogEntry" + "_de";

	private Long id;
	private String language;
	private String title;
	private String description;
	private Date dateCreated;

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

	public Date getDateCreated() {
		return dateCreated;
	}

	public void setDateCreated(Date dateCreated) {
		this.dateCreated = dateCreated;
	}
	public static class BlogLangDiscriminator implements Discriminator {

		@Override
		public String getAnalyzerDefinitionName(Object language, Object entity, String field) {
			if ( language == null ) {
				return null;
			}
			if ( !( language instanceof String ) ) {
				throw new IllegalArgumentException( "expected string as value in language discriminator" );
			}
			if ( "description".equals( field ) ) {
				return toAnalyzerName( (String) language );
			}
			else {
				//"title" is not affected
				return null;
			}

		}

		private String toAnalyzerName(String language) {
			if ( language == null ) {
				return null;
			}
			switch ( language ) {
				case "en":
					return EN_ANALYZER_NAME;
				case "de":
					return DE_ANALYZER_NAME;
				default:
					throw new AssertionFailure( "Unexpected language:" + language );
			}
		}
	}
}
