/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene;

import java.util.Optional;

import org.hibernate.search.engine.backend.Backend;
import org.hibernate.search.engine.backend.metamodel.IndexValueFieldTypeDescriptor;

import org.apache.lucene.analysis.Analyzer;

public interface LuceneBackend extends Backend {

	/**
	 * @param name An analyzer name, e.g. a name returned by {@link IndexValueFieldTypeDescriptor#analyzerName()}
	 * or {@link IndexValueFieldTypeDescriptor#searchAnalyzerName()}.
	 * @return The corresponding analyzer, or {@link Optional#empty()} if it doesn't exist.
	 */
	Optional<? extends Analyzer> analyzer(String name);

	/**
	 * @param name A normalizer name, e.g. a name returned by {@link IndexValueFieldTypeDescriptor#normalizerName()}.
	 * @return The corresponding normalizer, or {@link Optional#empty()} if it doesn't exist.
	 */
	Optional<? extends Analyzer> normalizer(String name);

}
