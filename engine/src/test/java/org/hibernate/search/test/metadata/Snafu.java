/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.metadata;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Boost;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Norms;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.annotations.TermVector;
import org.hibernate.search.testsupport.analyzer.FooAnalyzer;

/**
 * @author Hardy Ferentschik
 */
@Indexed
public class Snafu {
	@DocumentId
	private long id;

	@Field(name = "my-snafu",
			index = Index.NO,
			store = Store.YES,
			analyze = Analyze.NO,
			norms = Norms.NO,
			termVector = TermVector.WITH_POSITIONS,
			boost = @Boost(10.0f))
	private String snafu;

	@Field
	@NumericField(precisionStep = 16)
	private int numericField;

	@Field
	@NumericField(precisionStep = 8)
	private short numericShortField;

	@Field
	@NumericField(precisionStep = 4)
	private byte numericByteField;


	@Field(indexNullAs = "snafu")
	private String nullValue;

	@Field
	@org.hibernate.search.annotations.Analyzer(impl = FooAnalyzer.class)
	private String custom;
}
