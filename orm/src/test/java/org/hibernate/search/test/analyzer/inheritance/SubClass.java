/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.analyzer.inheritance;

import javax.persistence.Entity;

import org.apache.lucene.analysis.tr.TurkishAnalyzer;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.Indexed;

/**
 * @author Hardy Ferentschik
 */
@Entity
@Indexed
@Analyzer(impl = TurkishAnalyzer.class)
public class SubClass extends BaseClass {

}
