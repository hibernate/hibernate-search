/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.dsl;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.WhitespaceTokenizerFactory;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilterFactory;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.AnalyzerDefs;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.SortableField;
import org.hibernate.search.annotations.TokenFilterDef;
import org.hibernate.search.annotations.TokenizerDef;

/**
 * @author Guillaume Smet
 */
@Indexed
@Entity
@AnalyzerDefs({
		@AnalyzerDef(name = "titleAnalyzer",
				tokenizer = @TokenizerDef(factory = WhitespaceTokenizerFactory.class),
				filters = {
						@TokenFilterDef(factory = LowerCaseFilterFactory.class),
						@TokenFilterDef(factory = ASCIIFoldingFilterFactory.class)
				}
		),
		@AnalyzerDef(name = "authorAnalyzer",
				tokenizer = @TokenizerDef(factory = WhitespaceTokenizerFactory.class)
		)
})
public class Book {

	@DocumentId
	@Id
	Long id;

	@Field(analyzer = @Analyzer(definition = "titleAnalyzer"))
	@SortableField
	private String title;

	@Field(analyzer = @Analyzer(definition = "authorAnalyzer"))
	private String author;

	public Book() {
	}

	public Book(Long id, String title, String author) {
		this.id = id;
		this.title = title;
		this.author = author;
	}

	public Long getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public String getAuthor() {
		return author;
	}
}
