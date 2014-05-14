/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.analyzer;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.apache.lucene.analysis.pattern.PatternTokenizerFactory;
import org.apache.lucene.analysis.standard.StandardFilterFactory;

import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Parameter;
import org.hibernate.search.annotations.TokenFilterDef;
import org.hibernate.search.annotations.TokenizerDef;

/**
 * @author Hardy Ferentschik
 */
@Entity
@Indexed
@AnalyzerDef(name = "my-analyzer",
		tokenizer = @TokenizerDef(factory = PatternTokenizerFactory.class, params = {
				@Parameter(name = "pattern", value = "|")
		}),
		filters = {
				@TokenFilterDef(factory = StandardFilterFactory.class)
		})
public class Entity2 {
	@Id
	private long id;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}
}


