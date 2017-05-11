/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.analyzer.definition;

import org.hibernate.search.analyzer.definition.spi.LuceneAnalyzerDefinitionSourceService;
import org.hibernate.search.annotations.Factory;
import org.hibernate.search.cfg.Environment;

/**
 * A provider of analyzer definitions that can be referenced from the mapping,
 * e.g. with {@literal @Analyzer(definition = "some-name")}.
 * <p>
 * Implementors should define a concrete class with either a public default constructor
 * or a public static method annotated with {@link Factory}.
 * <p>
 * Users can select a definition provider through the
 * {@link Environment#ANALYZER_DEFINITION_PROVIDER configuration properties}, while
 * framework integrators also have the option to inject an alternative implementation
 * of a {@link LuceneAnalyzerDefinitionSourceService}.
 *
 * @author Yoann Rodiere
 */
public interface LuceneAnalyzerDefinitionProvider {

	void register(LuceneAnalyzerDefinitionRegistryBuilder builder);

}
