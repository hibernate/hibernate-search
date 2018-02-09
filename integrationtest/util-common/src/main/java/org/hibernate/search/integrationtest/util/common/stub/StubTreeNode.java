/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.util.common.stub;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class StubTreeNode<N extends StubTreeNode<N>> {

	private final Map<String, List<Object>> attributes;
	private final Map<String, List<N>> children;

	protected StubTreeNode(Builder<N> builder) {
		this.attributes = Collections.unmodifiableMap(
				builder.attributes.entrySet().stream()
						.collect( Collectors.toMap(
								Map.Entry::getKey,
								e -> Collections.unmodifiableList( new ArrayList<>( e.getValue() ) ),
								(u, v) -> {
									throw new IllegalStateException( String.format( "Duplicate key %s", u ) );
								},
								LinkedHashMap::new
						) )
		);
		this.children = Collections.unmodifiableMap(
				builder.children.entrySet().stream()
						.collect( Collectors.toMap(
								Map.Entry::getKey,
								e -> Collections.unmodifiableList(
										e.getValue().stream()
												.map( b -> b == null ? null : b.build() )
												.collect( Collectors.toList() )
								),
								(u, v) -> {
									throw new IllegalStateException( String.format( "Duplicate key %s", u ) );
								},
								LinkedHashMap::new
						) )
		);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		appendTo( builder, "", "", ", " );
		return builder.toString();
	}

	public void appendTo(StringBuilder builder, String newline, String indent, String separator) {
		boolean first = true;
		builder.append( "<" );
		for ( Map.Entry<String, List<Object>> entry : attributes.entrySet() ) {
			builder.append( newline );
			if ( first ) {
				first = false;
			}
			else {
				builder.append( separator );
			}
			builder.append( entry.getKey() );
			builder.append( "=" );
			builder.append( entry.getValue() );
		}
		builder.append( newline );
		if ( !first ) {
			builder.append( separator );
		}
		builder.append( "children={" );
		String childrenNewline = newline + indent;
		String childrenItemNewline = childrenNewline + indent;
		first = true;
		for ( Map.Entry<String, List<N>> entry : children.entrySet() ) {
			builder.append( childrenNewline );
			if ( first ) {
				first = false;
			}
			else {
				builder.append( separator );
			}
			builder.append( entry.getKey() );
			builder.append( "=" );
			boolean firstItem = true;
			for ( N child : entry.getValue() ) {
				if ( firstItem ) {
					firstItem = false;
				}
				else {
					builder.append( separator );
				}
				child.appendTo( builder, childrenItemNewline, indent, separator );
			}
		}
		if ( !first ) {
			builder.append( newline );
		}
		builder.append( "}" );
		builder.append( newline ).append( ">" );
	}

	public Map<String, List<Object>> getAttributes() {
		return attributes;
	}

	public Map<String, List<N>> getChildren() {
		return children;
	}

	public abstract static class Builder<N> {

		private final Map<String, List<Object>> attributes = new LinkedHashMap<>();
		private final Map<String, List<Builder<N>>> children = new LinkedHashMap<>();

		protected void attribute(String name, Object... values) {
			List<Object> attributeValues = attributes.computeIfAbsent( name, ignored -> new ArrayList<>() );
			Collections.addAll( attributeValues, values );
		}

		protected void child(String relativeName, Builder<N> nodeBuilder) {
			children.computeIfAbsent( relativeName, ignored -> new ArrayList<>() )
					.add( nodeBuilder );
		}

		public abstract N build();
	}

}
