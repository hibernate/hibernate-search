/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.extractor.impl;

import java.util.stream.Stream;

import org.hibernate.search.mapper.pojo.extractor.ContainerExtractor;

class ChainingContainerExtractor<C, U, V> implements ContainerExtractor<C, V> {

	private final ContainerExtractor<C, U> parent;
	private final ContainerExtractor<? super U, V> chained;

	ChainingContainerExtractor(ContainerExtractor<C, U> parent,
			ContainerExtractor<? super U, V> chained) {
		this.parent = parent;
		this.chained = chained;
	}

	@Override
	public Stream<V> extract(C container) {
		return parent.extract( container ).flatMap( chained::extract );
	}

	@Override
	public boolean multiValued() {
		return parent.multiValued() || chained.multiValued();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder( "[" );
		appendToString( builder, this, true );
		builder.append( "]" );
		return builder.toString();
	}

	private void appendToString(StringBuilder builder, ContainerExtractor<?, ?> extractor, boolean first) {
		if ( extractor instanceof ChainingContainerExtractor ) {
			ChainingContainerExtractor<?, ?, ?> chaining = (ChainingContainerExtractor<?, ?, ?>) extractor;
			appendToString( builder, chaining.parent, first );
			appendToString( builder, chaining.chained, false );
		}
		else {
			if ( !first ) {
				builder.append( ", " );
			}
			builder.append( extractor );
		}
	}
}
