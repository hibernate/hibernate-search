/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.serialization;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.apache.lucene.analysis.ngram.NGramFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;

import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.Boost;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.annotations.Parameter;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.annotations.TermVector;
import org.hibernate.search.annotations.TokenFilterDef;
import org.hibernate.search.annotations.TokenizerDef;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
@Entity
@Indexed
@AnalyzerDef(name = "ngram",
		tokenizer = @TokenizerDef( factory = StandardTokenizerFactory.class),
		filters = @TokenFilterDef( factory = NGramFilterFactory.class,
		params = {
		@Parameter(name = "minGramSize", value = "3"),
		@Parameter(name = "maxGramSize", value = "3") })
		)
public class RemoteEntity {
	@Id @DocumentId @GeneratedValue
	public Integer getId() { return id; }
	public void setId(Integer id) { this.id = id; }
	private Integer id;

	@Field(store = Store.YES, termVector = TermVector.WITH_POSITION_OFFSETS, boost = @Boost(23f) )
	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	private String name;

	@Field(analyzer = @Analyzer(definition = "ngram")) @NumericField(precisionStep = 2)
	public Float getApproximation() { return approximation; }
	public void setApproximation(Float approximation) { this.approximation = approximation; }
	private Float approximation;
}
