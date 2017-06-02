/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.analyzer.definition;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.apache.lucene.analysis.charfilter.HTMLStripCharFilterFactory;
import org.apache.lucene.analysis.charfilter.MappingCharFilterFactory;
import org.apache.lucene.analysis.core.KeywordTokenizerFactory;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.StopFilterFactory;
import org.apache.lucene.analysis.en.PorterStemFilterFactory;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilterFactory;
import org.apache.lucene.analysis.miscellaneous.LengthFilterFactory;
import org.apache.lucene.analysis.miscellaneous.StemmerOverrideFilterFactory;
import org.apache.lucene.analysis.miscellaneous.TrimFilterFactory;
import org.apache.lucene.analysis.miscellaneous.WordDelimiterFilterFactory;
import org.apache.lucene.analysis.pattern.PatternTokenizerFactory;
import org.apache.lucene.analysis.phonetic.PhoneticFilterFactory;
import org.apache.lucene.analysis.shingle.ShingleFilterFactory;
import org.apache.lucene.analysis.snowball.SnowballPorterFilterFactory;
import org.apache.lucene.analysis.standard.StandardFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.apache.lucene.analysis.synonym.SynonymFilterFactory;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.AnalyzerDefs;
import org.hibernate.search.annotations.CharFilterDef;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Normalizer;
import org.hibernate.search.annotations.NormalizerDef;
import org.hibernate.search.annotations.NormalizerDefs;
import org.hibernate.search.annotations.Parameter;
import org.hibernate.search.annotations.TokenFilterDef;
import org.hibernate.search.annotations.TokenizerDef;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Indexed
@AnalyzerDefs({
		@AnalyzerDef(name = "customanalyzer",
				tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class),
				filters = {
						@TokenFilterDef(factory = ASCIIFoldingFilterFactory.class),
						@TokenFilterDef(factory = LowerCaseFilterFactory.class),
						@TokenFilterDef(factory = StopFilterFactory.class, params = {
								@Parameter(name = "words",
										value = "org/hibernate/search/test/analyzer/stoplist.properties"),
								@Parameter(name = "ignoreCase", value = "true")
						}),
						@TokenFilterDef(factory = SnowballPorterFilterFactory.class, params = {
								@Parameter(name = "language", value = "English")
						})
				}),

		@AnalyzerDef(name = "pattern_analyzer",
				tokenizer = @TokenizerDef(factory = PatternTokenizerFactory.class, params = {
						@Parameter(name = "pattern", value = ",")
				})),

		@AnalyzerDef(name = "standard_analyzer",
				tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class),
				filters = {
						@TokenFilterDef(factory = StandardFilterFactory.class)
				}),
		@AnalyzerDef(name = "html_standard_analyzer",
				charFilters = {
						@CharFilterDef(factory = HTMLStripCharFilterFactory.class)
				},
				tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class),
				filters = {
						@TokenFilterDef(factory = StandardFilterFactory.class)
				}),

		@AnalyzerDef(name = "html_whitespace_analyzer",
				tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class),
				charFilters = {
						@CharFilterDef(factory = HTMLStripCharFilterFactory.class)
				}),

		@AnalyzerDef(name = "trim_analyzer",
				tokenizer = @TokenizerDef(factory = KeywordTokenizerFactory.class),
				filters = {
						@TokenFilterDef(factory = TrimFilterFactory.class)
				}),

		@AnalyzerDef(name = "length_analyzer",
				tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class),
				filters = {
						@TokenFilterDef(factory = LengthFilterFactory.class, params = {
								@Parameter(name = "min", value = "3"),
								@Parameter(name = "max", value = "5")
						})
				}),

		@AnalyzerDef(name = "porter_analyzer",
				tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class),
				filters = {
						@TokenFilterDef(factory = PorterStemFilterFactory.class)
				}),

		@AnalyzerDef(name = "word_analyzer",
				charFilters = {
						@CharFilterDef(factory = HTMLStripCharFilterFactory.class)
				},
				tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class),
				filters = {
						@TokenFilterDef(factory = WordDelimiterFilterFactory.class, params = {
								@Parameter(name = "splitOnCaseChange", value = "1")
						})
				}),

		@AnalyzerDef(name = "synonym_analyzer",
				charFilters = {
						@CharFilterDef(factory = HTMLStripCharFilterFactory.class)
				},
				tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class),
				filters = {
						@TokenFilterDef(factory = SynonymFilterFactory.class, params = {
								@Parameter(
										name = "synonyms",
										value = "org/hibernate/search/test/analyzer/synonyms.properties"
								),
								@Parameter(name = "expand", value = "false")
						})
				}),

		@AnalyzerDef(name = "shingle_analyzer",
				charFilters = {
						@CharFilterDef(factory = HTMLStripCharFilterFactory.class)
				},
				tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class),
				filters = {
						@TokenFilterDef(factory = ShingleFilterFactory.class)
				}),

		@AnalyzerDef(name = "phonetic_analyzer",
				tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class),
				filters = {
						@TokenFilterDef(factory = PhoneticFilterFactory.class, params = {
								@Parameter(name = "encoder", value = "Metaphone"),
								@Parameter(name = "inject", value = "false")
						})
				}),

		@AnalyzerDef(name = "html_char_analyzer",
				charFilters = {
						@CharFilterDef(factory = HTMLStripCharFilterFactory.class)
				},
				tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class)
				),

		@AnalyzerDef(name = "mapping_char_analyzer",
				charFilters = {
						@CharFilterDef(factory = MappingCharFilterFactory.class, params = {
								@Parameter(name = "mapping", value = "org/hibernate/search/test/analyzer/mapping-chars.properties")
						})
				},
				tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class)
		),

		@AnalyzerDef(name = "stemmer_override_analyzer",
				tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class),
				filters = {
						@TokenFilterDef(factory = ASCIIFoldingFilterFactory.class),
						@TokenFilterDef(factory = LowerCaseFilterFactory.class),
						@TokenFilterDef(factory = StemmerOverrideFilterFactory.class, params = {
								@Parameter(name = "dictionary",
										value = "org/hibernate/search/test/analyzer/stemmer-override.properties")
						}),
						@TokenFilterDef(factory = SnowballPorterFilterFactory.class, params = {
								@Parameter(name = "language", value = "English")
						})
		})
})
@NormalizerDefs({
		@NormalizerDef(name = "custom_normalizer",
				filters = {
						@TokenFilterDef(factory = ASCIIFoldingFilterFactory.class),
						@TokenFilterDef(factory = LowerCaseFilterFactory.class),
				}
		)
})
public class Team {
	@Id
	@DocumentId
	@GeneratedValue
	private Integer id;

	@Field
	@Field(name = "name_customanalyzer", analyzer = @Analyzer(definition = "customanalyzer"))
	@Field(name = "name_pattern_analyzer", analyzer = @Analyzer(definition = "pattern_analyzer"))
	@Field(name = "name_standard_analyzer", analyzer = @Analyzer(definition = "standard_analyzer"))
	@Field(name = "name_html_standard_analyzer", analyzer = @Analyzer(definition = "html_standard_analyzer"))
	@Field(name = "name_html_whitespace_analyzer", analyzer = @Analyzer(definition = "html_whitespace_analyzer"))
	@Field(name = "name_trim_analyzer", analyzer = @Analyzer(definition = "trim_analyzer"))
	@Field(name = "name_length_analyzer", analyzer = @Analyzer(definition = "length_analyzer"))
	@Field(name = "name_porter_analyzer", analyzer = @Analyzer(definition = "porter_analyzer"))
	@Field(name = "name_word_analyzer", analyzer = @Analyzer(definition = "word_analyzer"))
	@Field(name = "name_synonym_analyzer", analyzer = @Analyzer(definition = "synonym_analyzer"))
	@Field(name = "name_shingle_analyzer", analyzer = @Analyzer(definition = "shingle_analyzer"))
	@Field(name = "name_html_char_analyzer", analyzer = @Analyzer(definition = "html_char_analyzer"))
	@Field(name = "name_mapping_char_analyzer", analyzer = @Analyzer(definition = "mapping_char_analyzer"))
	@Field(name = "name_stemmer_override_analyzer", analyzer = @Analyzer(definition = "stemmer_override_analyzer"))
	@Field(name = "name_custom_normalizer", normalizer = @Normalizer(definition = "custom_normalizer"))
	private String name;

	@Field
	private String location;

	@Field
	@Analyzer(definition = "customanalyzer")
	private String description;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
