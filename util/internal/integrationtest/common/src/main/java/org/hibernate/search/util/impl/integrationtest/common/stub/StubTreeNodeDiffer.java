/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.stub;

import java.lang.reflect.Array;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.hibernate.search.util.common.impl.ToStringTreeBuilder;

public final class StubTreeNodeDiffer<N extends StubTreeNode<N>> {

	private static final Object NO_VALUE = new Object() {
		@Override
		public String toString() {
			return "<no value>";
		}
	};

	public static <N extends StubTreeNode<N>> Builder<N> builder() {
		return new Builder<>();
	}

	public static class Builder<N extends StubTreeNode<N>> {
		private final Set<String> missingEquivalentToEmptyPaths = new HashSet<>();

		private Builder() {
		}

		public Builder<N> missingEquivalentToEmptyForPath(String path) {
			missingEquivalentToEmptyPaths.add( path );
			return this;
		}

		public StubTreeNodeDiffer<N> build() {
			return new StubTreeNodeDiffer<>( this );
		}
	}

	private final Set<String> missingEquivalentToEmptyPaths;

	private StubTreeNodeDiffer(Builder<N> builder) {
		this.missingEquivalentToEmptyPaths = new HashSet<>( builder.missingEquivalentToEmptyPaths );
	}

	public Map<String, StubTreeNodeMismatch> diff(N expected, N actual) {
		Map<String, StubTreeNodeMismatch> mismatchesByPath = new LinkedHashMap<>();
		addMismatchesRecursively( mismatchesByPath, null, expected, actual );
		return mismatchesByPath;
	}

	public static void appendTo(ToStringTreeBuilder treeBuilder, Map<String, StubTreeNodeMismatch> mismatchesByPath) {
		for ( Map.Entry<String, StubTreeNodeMismatch> entry : mismatchesByPath.entrySet() ) {
			String path = entry.getKey();
			treeBuilder.startObject( path );
			StubTreeNodeMismatch mismatch = entry.getValue();
			treeBuilder.attribute( "expected", mismatch.expected );
			treeBuilder.attribute( "actual", mismatch.actual );
			treeBuilder.endObject();
		}
	}

	private void addMismatchesRecursively(Map<String, StubTreeNodeMismatch> mismatchesByPath, String path,
			N expectedNode, N actualNode) {
		if ( expectedNode == null && actualNode == null ) {
			return;
		}
		else if ( expectedNode == null || actualNode == null ) {
			// One is null, the other is not
			addExtraOrMissingNodeMismatch( mismatchesByPath, path, expectedNode, actualNode );
			return;
		}
		Set<String> attributeKeys = new LinkedHashSet<>( expectedNode.getAttributes().keySet() );
		attributeKeys.addAll( actualNode.getAttributes().keySet() );
		for ( String key : attributeKeys ) {
			List<Object> expected = expectedNode.getAttributes().get( key );
			List<Object> actual = actualNode.getAttributes().get( key );
			if ( !equalLists( expected, actual ) ) {
				StubTreeNodeMismatch mismatch = new StubTreeNodeMismatch( expected == null ? NO_VALUE : expected,
						actual == null ? NO_VALUE : actual );
				mismatchesByPath.put( attributePath( path, key ), mismatch );
			}
		}
		Set<String> childKeys = new LinkedHashSet<>( expectedNode.getChildren().keySet() );
		childKeys.addAll( actualNode.getChildren().keySet() );
		for ( String key : childKeys ) {
			List<N> expectedChildren = expectedNode.getChildren().get( key );
			List<N> actualChildren = actualNode.getChildren().get( key );
			addChildrenMismatchesRecursively( mismatchesByPath, path, key, expectedChildren, actualChildren );
		}
	}

	private boolean equalLists(List<Object> listA, List<Object> listB) {
		if ( listA == null && listB == null ) {
			return true;
		}
		if ( listA == null || listB == null ) {
			return false;
		}
		if ( listA.size() != listB.size() ) {
			return false;
		}
		Iterator<Object> ai = listA.iterator();
		Iterator<Object> bi = listB.iterator();
		while ( ai.hasNext() ) {
			if ( !objectsEquals( ai.next(), bi.next() ) ) {
				return false;
			}
		}
		return true;
	}

	private boolean objectsEquals(Object a, Object b) {
		if ( a == null && b == null ) {
			return true;
		}
		if ( a == null || b == null ) {
			return false;
		}
		if ( a.getClass().isArray() ) {
			return arraysEquals( a, b );
		}
		return Objects.equals( a, b );
	}

	private boolean arraysEquals(Object a, Object b) {
		assert a != null;
		assert b != null;
		int length = Array.getLength( a );
		if ( length != Array.getLength( b ) ) {
			return false;
		}
		for ( int i = 0; i < length; i++ ) {
			if ( !Objects.equals( Array.get( a, i ), Array.get( b, i ) ) ) {
				return false;
			}
		}
		return true;
	}

	private void addChildrenMismatchesRecursively(Map<String, StubTreeNodeMismatch> mismatchesByPath,
			String path, String childrenKey, List<N> expectedChildren, List<N> actualChildren) {
		int expectedChildrenSize = expectedChildren == null ? 0 : expectedChildren.size();
		int actualChildrenSize = actualChildren == null ? 0 : actualChildren.size();
		int minSize = Integer.min( expectedChildrenSize, actualChildrenSize );
		int maxSize = Integer.max( expectedChildrenSize, actualChildrenSize );
		for ( int i = 0; i < minSize; ++i ) {
			String childPath = childPath( path, childrenKey, i, maxSize );
			N expectedChild = expectedChildren.get( i );
			N actualChild = actualChildren.get( i );
			addMismatchesRecursively( mismatchesByPath, childPath, expectedChild, actualChild );
		}
		/*
		 * The remaining indexes contain mismatches: they can only exist
		 * if there is a different number of children in each node.
		 */
		for ( int i = minSize; i < expectedChildrenSize; ++i ) {
			String childPath = childPath( path, childrenKey, i, maxSize );
			N expectedChild = expectedChildren.get( i );
			addExtraOrMissingNodeMismatch( mismatchesByPath, childPath, expectedChild, null );
		}
		for ( int i = minSize; i < actualChildrenSize; ++i ) {
			String childPath = childPath( path, childrenKey, i, maxSize );
			N actualChild = actualChildren.get( i );
			addExtraOrMissingNodeMismatch( mismatchesByPath, childPath, null, actualChild );
		}
	}

	private void addExtraOrMissingNodeMismatch(Map<String, StubTreeNodeMismatch> mismatchesByPath,
			String path, N expectedNode, N actualNode) {
		String pathWithoutArrayIndices = path == null ? null : path.replaceAll( "\\[[^]]*]", "" );
		if ( missingEquivalentToEmptyPaths.contains( pathWithoutArrayIndices )
				&& ( expectedNode == null || expectedNode.getChildren().isEmpty() )
				&& ( actualNode == null || actualNode.getChildren().isEmpty() ) ) {
			// One is null (missing), the other has no children (empty),
			// and we were told to consider this as equivalent.
			return;
		}
		StubTreeNodeMismatch mismatch = new StubTreeNodeMismatch( expectedNode == null ? NO_VALUE : expectedNode,
				actualNode == null ? NO_VALUE : actualNode );
		mismatchesByPath.put( path, mismatch );
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

}
