/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hibernate.search.util.common.impl.StreamHelper;
import org.hibernate.search.util.common.spi.ToStringTreeAppendable;
import org.hibernate.search.util.common.spi.ToStringTreeAppender;

public abstract class StubTreeNode<N extends StubTreeNode<N>> implements ToStringTreeAppendable {

	private final String absolutePath;
	private final Map<String, List<Object>> attributes;
	private final Map<String, List<N>> children;

	protected StubTreeNode(AbstractBuilder<N> builder) {
		this.absolutePath = builder.absolutePath;
		this.attributes = Collections.unmodifiableMap(
				builder.attributes.entrySet().stream()
						.collect( StreamHelper.toMap(
								Map.Entry::getKey,
								e -> Collections.unmodifiableList( new ArrayList<>( e.getValue() ) ),
								LinkedHashMap::new
						) )
		);
		this.children = Collections.unmodifiableMap(
				builder.children.entrySet().stream()
						.collect( StreamHelper.toMap(
								Map.Entry::getKey,
								e -> Collections.unmodifiableList(
										e.getValue().stream()
												.map( b -> b == null ? null : b.build() )
												.collect( Collectors.toList() )
								),
								LinkedHashMap::new
						) )
		);
	}

	@Override
	public String toString() {
		return toStringTree();
	}

	@Override
	public void appendTo(ToStringTreeAppender appender) {
		for ( Map.Entry<String, List<Object>> entry : attributes.entrySet() ) {
			appender.attribute( entry.getKey(), entry.getValue() );
		}
		for ( Map.Entry<String, List<N>> entry : children.entrySet() ) {
			List<N> list = entry.getValue();
			if ( list.size() == 1 ) {
				appender.startObject( entry.getKey() );
				list.get( 0 ).appendTo( appender );
				appender.endObject();
			}
			else {
				appender.startList( entry.getKey() );
				for ( N child : entry.getValue() ) {
					appender.startObject();
					child.appendTo( appender );
					appender.endObject();
				}
				appender.endList();
			}
		}
	}

	public String absolutePath() {
		return absolutePath;
	}

	public Map<String, List<Object>> getAttributes() {
		return attributes;
	}

	public Map<String, List<N>> getChildren() {
		return children;
	}

	public abstract static class AbstractBuilder<N> {

		private final AbstractBuilder<?> parent;
		private final String relativeFieldName;
		private final String absolutePath;

		private final Map<String, List<Object>> attributes = new LinkedHashMap<>();
		private final Map<String, List<AbstractBuilder<N>>> children = new LinkedHashMap<>();

		protected AbstractBuilder(AbstractBuilder<?> parent, String relativeFieldName) {
			this.parent = parent;
			this.relativeFieldName = relativeFieldName;
			if ( parent == null ) {
				this.absolutePath = "<ROOT>";
			}
			else if ( parent.parent == null ) {
				this.absolutePath = relativeFieldName;
			}
			else {
				this.absolutePath = parent.getAbsolutePath() + "." + relativeFieldName;
			}
		}

		public String getRelativeName() {
			return relativeFieldName;
		}

		public String getAbsolutePath() {
			return absolutePath;
		}

		protected void attribute(String name, Object... values) {
			List<Object> attributeValues = attributes.computeIfAbsent( name, ignored -> new ArrayList<>() );
			Collections.addAll( attributeValues, values );
		}

		protected void missingChild(String relativeFieldName) {
			children.computeIfAbsent( relativeFieldName, ignored -> new ArrayList<>() )
					.add( null );
		}

		protected void child(AbstractBuilder<N> nodeBuilder) {
			children.computeIfAbsent( nodeBuilder.relativeFieldName, ignored -> new ArrayList<>() )
					.add( nodeBuilder );
		}

		public abstract N build();
	}

}
