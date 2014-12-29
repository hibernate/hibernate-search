/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend;

import org.apache.lucene.search.Query;
import org.hibernate.search.util.impl.ScopedAnalyzer;

/**
 * @author Martin Braun
 */
public interface CustomBehaviour {

	Query toLuceneQuery(CustomBehaviourQuery query, ScopedAnalyzer analyzerForEntity);

	String[] toString(CustomBehaviourQuery query);

	CustomBehaviourQuery fromString(String[] string);

}
