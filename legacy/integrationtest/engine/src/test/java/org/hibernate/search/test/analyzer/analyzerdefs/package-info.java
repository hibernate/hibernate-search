/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
@AnalyzerDefs({
	@AnalyzerDef(
		name = "package-analyzer-1",
		tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class),
		filters = { @TokenFilterDef(factory = StandardFilterFactory.class) } ),
	@AnalyzerDef(
		name = "package-analyzer-2",
		tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class),
		filters = { @TokenFilterDef(factory = StandardFilterFactory.class) } )
})
package org.hibernate.search.test.analyzer.analyzerdefs;

import org.apache.lucene.analysis.standard.StandardFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.AnalyzerDefs;
import org.hibernate.search.annotations.TokenFilterDef;
import org.hibernate.search.annotations.TokenizerDef;

