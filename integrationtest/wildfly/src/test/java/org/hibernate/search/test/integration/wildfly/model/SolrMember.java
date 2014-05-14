/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.wildfly.model;

import static javax.persistence.InheritanceType.TABLE_PER_CLASS;

import javax.persistence.Entity;
import javax.persistence.Inheritance;

import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.TokenFilterDef;
import org.hibernate.search.annotations.TokenizerDef;

@Entity
@Indexed
@Inheritance(strategy = TABLE_PER_CLASS)
@AnalyzerDef(
	name = "customanalyzer",
	tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class),
	filters = { @TokenFilterDef(factory = LowerCaseFilterFactory.class) })
public class SolrMember extends Member {

	/** Default value included to remove warning. Remove or modify at will. **/
	private static final long serialVersionUID = 1L;
}
