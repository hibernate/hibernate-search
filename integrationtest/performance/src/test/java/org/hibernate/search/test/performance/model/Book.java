/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.performance.model;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.snowball.SnowballPorterFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.Boost;
import org.hibernate.search.annotations.DateBridge;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Parameter;
import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.annotations.TokenFilterDef;
import org.hibernate.search.annotations.TokenizerDef;

/**
 * @author Tomas Hradec
 */
@Entity
@Table(name = "book")
@Indexed
@AnalyzerDef(name = "textAnalyzer", tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class), filters = {
		@TokenFilterDef(factory = LowerCaseFilterFactory.class),
		@TokenFilterDef(factory = SnowballPorterFilterFactory.class, params = { @Parameter(name = "language", value = "English") }) })
public class Book {

	@Id
	private Long id;

	@Field(store = Store.YES)
	@Boost(2.0f)
	@Analyzer(definition = "textAnalyzer")
	private String title;

	@Field
	@Analyzer(definition = "textAnalyzer")
	@Column(columnDefinition = "text")
	private String summary;

	@Field(analyze = Analyze.NO, store = Store.YES)
	@DateBridge(resolution = Resolution.DAY)
	private Date publicationDate;

	@Field(analyze = Analyze.NO, store = Store.YES)
	private Float rating;

	@Field(analyze = Analyze.NO, store = Store.YES)
	private Long totalSold;

	@IndexedEmbedded
	@ManyToMany(cascade = CascadeType.ALL)
	private Set<Author> authors = new HashSet<Author>();

	public Book() {
	}

	public Book(Long id, String title) {
		this.id = id;
		this.title = title;
	}

	public Book(Long id, String title, String summary, Date publicationDate, Float rating, Author... authors) {
		this.id = id;
		this.title = title;
		this.summary = summary;
		this.publicationDate = publicationDate;
		this.rating = rating;
		if ( authors != null ) {
			this.authors.addAll( Arrays.asList( authors ) );
		}
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	public Date getPublicationDate() {
		return publicationDate;
	}

	public void setPublicationDate(Date publicationDate) {
		this.publicationDate = publicationDate;
	}

	public Float getRating() {
		return rating;
	}

	public void setRating(Float rating) {
		this.rating = rating;
	}

	public Long getTotalSold() {
		return totalSold;
	}

	public void setTotalSold(Long totalSold) {
		this.totalSold = totalSold;
	}

	public Set<Author> getAuthors() {
		return authors;
	}

	public void setAuthors(Set<Author> authors) {
		this.authors = authors;
	}

	@Override
	public String toString() {
		return new ToStringBuilder( this )
				.append( "id", id )
				.append( "title", title )
				.append( "rating", rating )
				.append( "totalSold", totalSold )
				.append( "publicationDate", publicationDate )
				.append( "authors", authors )
				.toString();
	}

}
