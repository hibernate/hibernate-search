/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.extractor.builtin.impl;

import java.util.OptionalDouble;

import org.hibernate.search.mapper.pojo.extractor.ContainerExtractionContext;
import org.hibernate.search.mapper.pojo.extractor.ContainerExtractor;
import org.hibernate.search.mapper.pojo.extractor.ValueProcessor;
import org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractors;

public class OptionalDoubleValueExtractor implements ContainerExtractor<OptionalDouble, Double> {
	@Override
	public String toString() {
		return BuiltinContainerExtractors.OPTIONAL_DOUBLE;
	}

	@Override
	public <T, C2> void extract(OptionalDouble container, ValueProcessor<T, ? super Double, C2> perValueProcessor, T target,
			C2 context, ContainerExtractionContext extractionContext) {
		if ( container == null ) {
			return;
		}
		if ( container.isPresent() ) {
			perValueProcessor.process( target, container.getAsDouble(), context, extractionContext );
		}
	}

	@Override
	public boolean multiValued() {
		return false;
	}
}
