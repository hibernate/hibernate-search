/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.extractor.impl;

import java.util.stream.Stream;

import org.hibernate.search.mapper.pojo.extractor.ContainerValueExtractor;

class ChainingContainerValueExtractor<C, T, U> implements ContainerValueExtractor<C, U> {

	private final ContainerValueExtractor<C, T> parent;
	private final ContainerValueExtractor<? super T, U> chained;

	ChainingContainerValueExtractor(ContainerValueExtractor<C, T> parent,
			ContainerValueExtractor<? super T, U> chained) {
		this.parent = parent;
		this.chained = chained;
	}

	@Override
	public Stream<U> extract(C container) {
		return parent.extract( container ).flatMap( chained::extract );
	}

	public ContainerValueExtractor<C, T> getParent() {
		return parent;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder( "[" );
		appendToString( builder, this, true );
		builder.append( "]" );
		return builder.toString();
	}

	private void appendToString(StringBuilder builder, ContainerValueExtractor<?, ?> extractor, boolean first) {
		if ( extractor instanceof ChainingContainerValueExtractor ) {
			ChainingContainerValueExtractor<?, ?, ?> chaining = (ChainingContainerValueExtractor<?, ?, ?>) extractor;
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
