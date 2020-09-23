/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.extractor.builtin.impl;

import java.util.function.Consumer;

import org.hibernate.search.mapper.pojo.extractor.ContainerExtractor;
import org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractors;

public class CharArrayElementExtractor implements ContainerExtractor<char[], Character> {
	@Override
	public String toString() {
		return BuiltinContainerExtractors.ARRAY_CHAR;
	}

	@Override
	public void extract(char[] container, Consumer<Character> consumer) {
		if ( container == null ) {
			return;
		}
		for ( char element : container ) {
			consumer.accept( element );
		}
	}
}
