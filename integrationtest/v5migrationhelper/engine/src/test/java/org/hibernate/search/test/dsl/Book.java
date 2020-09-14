/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.dsl;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.SortableField;
import org.hibernate.search.testsupport.AnalysisNames;

/**
 * @author Guillaume Smet
 */
@Indexed
class Book {

	@DocumentId
	String id;

	@Field(analyzer = @Analyzer(definition = AnalysisNames.ANALYZER_WHITESPACE_LOWERCASE_ASCIIFOLDING))
	@Field(name = "title_sort", analyze = Analyze.NO)
	@SortableField(forField = "title_sort")
	private String title;

	@Field(analyzer = @Analyzer(definition = AnalysisNames.ANALYZER_WHITESPACE))
	private String author;

	public Book() {
	}

	public Book(String title, String author) {
		this.id = title;
		this.title = title;
		this.author = author;
	}

	public String getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public String getAuthor() {
		return author;
	}
}
