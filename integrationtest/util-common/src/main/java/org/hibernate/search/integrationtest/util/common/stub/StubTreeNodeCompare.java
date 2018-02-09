/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.util.common.stub;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class StubTreeNodeCompare {

	private static final Object NO_VALUE = new Object() {
		@Override
		public String toString() {
			return "<no value>";
		}
	};

	private StubTreeNodeCompare() {
	}

	public static <N extends StubTreeNode<N>> Map<String, StubTreeNodeMismatch> compare(N expected, N actual) {
		Map<String, StubTreeNodeMismatch> mismatchesByPath = new LinkedHashMap<>();
		addMismatchesRecursively( mismatchesByPath, null, expected, actual );
		return mismatchesByPath;
	}

	public static void appendTo(
			StringBuilder builder, Map<String, StubTreeNodeMismatch> mismatchesByPath,
			String newline, String indent) {
		String secondLevelNewline = newline + indent;
		String thirdLevelNewline = secondLevelNewline + indent;
		for ( Map.Entry<String, StubTreeNodeMismatch> entry : mismatchesByPath.entrySet() ) {
			String path = entry.getKey();
			if ( path != null ) {
				builder.append( newline ).append( path ).append( ":" );
			}
			StubTreeNodeMismatch mismatch = entry.getValue();
			builder.append( secondLevelNewline ).append( "expected: " );
			appendTo( builder, mismatch.expected, thirdLevelNewline, indent );
			builder.append( secondLevelNewline ).append( "actual: " );
			appendTo( builder, mismatch.actual, thirdLevelNewline, indent );
		}
	}

	private static <N extends StubTreeNode<N>> void addMismatchesRecursively(
			Map<String, StubTreeNodeMismatch> mismatchesByPath, String path, N expectedNode, N actualNode) {
		if ( expectedNode == null && actualNode == null ) {
			return;
		}
		else if ( expectedNode == null || actualNode == null ) {
			// One is null, the other is not
			StubTreeNodeMismatch mismatch = new StubTreeNodeMismatch( expectedNode, actualNode );
			mismatchesByPath.put( path, mismatch );
		}
		Set<String> attributeKeys = new LinkedHashSet<>( expectedNode.getAttributes().keySet() );
		attributeKeys.addAll( actualNode.getAttributes().keySet() );
		for ( String key : attributeKeys ) {
			List<Object> expected = expectedNode.getAttributes().get( key );
			List<Object> actual = actualNode.getAttributes().get( key );
			if ( !Objects.equals( expected, actual ) ) {
				StubTreeNodeMismatch mismatch = new StubTreeNodeMismatch( expected == null ? NO_VALUE : expected,
						actual == null ? NO_VALUE : actual );
				mismatchesByPath.put( attributePath( path, key ), mismatch );
			}
		}
		Set<String> childKeys = new LinkedHashSet<>( expectedNode.getChildren().keySet() );
		attributeKeys.addAll( actualNode.getChildren().keySet() );
		for ( String key : childKeys ) {
			List<N> expectedChildren = expectedNode.getChildren().get( key );
			List<N> actualChildren = actualNode.getChildren().get( key );
			int expectedChildrenSize = expectedChildren == null ? 0 : expectedChildren.size();
			int actualChildrenSize = actualChildren == null ? 0 : actualChildren.size();
			int minSize = Integer.min( expectedChildrenSize, actualChildrenSize );
			int maxSize = Integer.max( expectedChildrenSize, actualChildrenSize );
			for ( int i = 0; i < minSize; ++i ) {
				String childPath = childPath( path, key, i, maxSize );
				N expectedChild = expectedChildren.get( i );
				N actualChild = actualChildren.get( i );
				addMismatchesRecursively( mismatchesByPath, childPath, expectedChild, actualChild );
			}
			/*
			 * The remaining indexes contain mismatches: they can only exist
			 * if there is a different number of children in each node.
			 */
			for ( int i = minSize; i < expectedChildrenSize; ++i ) {
				String childPath = childPath( path, key, i, maxSize );
				Object expectedChild = expectedChildren.get( i );
				StubTreeNodeMismatch mismatch = new StubTreeNodeMismatch( expectedChild, NO_VALUE );
				mismatchesByPath.put( childPath, mismatch );
			}
			for ( int i = minSize; i < actualChildrenSize; ++i ) {
				String childPath = childPath( path, key, i, maxSize );
				Object actualChild = actualChildren.get( i );
				StubTreeNodeMismatch mismatch = new StubTreeNodeMismatch( NO_VALUE, actualChild );
				mismatchesByPath.put( childPath, mismatch );
			}
		}
	}

	private static String attributePath(String parentPath, String key) {
		StringBuilder builder = new StringBuilder();
		if ( parentPath != null ) {
			builder.append( parentPath );
		}
		builder.append( "#" ).append( key );
		return builder.toString();
	}

	private static String childPath(String parentPath, String key, int i, int maxSize) {
		StringBuilder builder = new StringBuilder();
		if ( parentPath != null ) {
			builder.append( parentPath ).append( "." );
		}
		builder.append( key );
		if ( maxSize > 1 ) {
			builder.append( "[" ).append( i ).append( "]" );
		}
		return builder.toString();
	}

	private static void appendTo(StringBuilder builder, Object value, String newline, String indent) {
		if ( value instanceof StubTreeNode ) {
			((StubTreeNode<?>)value).appendTo( builder, newline, indent, "" );
		}
		else {
			builder.append( value );
		}
	}

}
