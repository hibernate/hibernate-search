/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.common.impl;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

import org.hibernate.search.util.common.AssertionFailure;

public class ToStringTreeBuilder {

	private final ToStringStyle style;
	private final StringBuilder builder = new StringBuilder();

	private final Deque<StructureType> structureTypeStack = new ArrayDeque<>();
	private boolean first = true;

	public ToStringTreeBuilder() {
		this( ToStringStyle.inlineDelimiterStructure() );
	}

	public ToStringTreeBuilder(ToStringStyle style) {
		this.style = style;
	}

	@Override
	public String toString() {
		return builder.toString();
	}

	public ToStringTreeBuilder attribute(String name, Object value) {
		if ( value instanceof ToStringTreeAppendable ) {
			ToStringTreeAppendable appendable = ( (ToStringTreeAppendable) value );
			startEntry( name, StructureType.OBJECT );
			startStructure( StructureType.OBJECT, style.startObject );
			appendable.appendTo( this );
			endStructure( StructureType.OBJECT, style.endObject );
			endEntry();
		}
		else if ( value instanceof Iterable ) {
			startList( name );
			for ( Object element : (Iterable<?>) value ) {
				value( element );
			}
			endList();
		}
		else if ( value instanceof Map ) {
			startObject( name );
			for ( Map.Entry<?, ?> entry : ( (Map<?, ?>) value ).entrySet() ) {
				attribute( Objects.toString( entry.getKey() ), entry.getValue() );
			}
			endObject();
		}
		else {
			startEntry( name, null );
			if ( value == null ) {
				builder.append( value );
			}
			else {
				String[] lines = value.toString().split( "\n" );
				for ( int i = 0; i < lines.length; i++ ) {
					if ( i != 0 ) {
						appendNewline();
						appendIndentIfNecessary();
					}
					builder.append( lines[i] );
				}
			}
			endEntry();
		}
		return this;
	}

	public ToStringTreeBuilder value(Object value) {
		return attribute( null, value );
	}

	public ToStringTreeBuilder startObject() {
		return startObject( null );
	}

	public ToStringTreeBuilder startObject(String name) {
		startEntry( name, StructureType.OBJECT );
		startStructure( StructureType.OBJECT, style.startObject );
		return this;
	}

	public ToStringTreeBuilder endObject() {
		endStructure( StructureType.OBJECT, style.endObject );
		endEntry();
		return this;
	}

	public ToStringTreeBuilder startList() {
		return startList( null );
	}

	public ToStringTreeBuilder startList(String name) {
		startEntry( name, StructureType.LIST );
		startStructure( StructureType.LIST, style.startList );
		return this;
	}

	public ToStringTreeBuilder endList() {
		endStructure( StructureType.LIST, style.endList );
		endEntry();
		return this;
	}

	private void startEntry(String name, StructureType containedStructureType) {
		if ( !first ) {
			builder.append( style.entrySeparator );
		}

		StructureType entryType =
				StringHelper.isEmpty( name ) ? StructureType.UNNAMED_ENTRY : StructureType.NAMED_ENTRY;

		// Add a new line
		if (
				// ... except for the very first element at the root
				!( first && structureTypeStack.isEmpty() )
				// ... or for entries containing a squeezed structure
				&& !shouldSqueeze( containedStructureType, entryType, structureTypeStack.peek() )
				// ... or for structures without a name nor a start delimiter
				&& !(
						StructureType.UNNAMED_ENTRY.equals( entryType )
						&& StructureType.OBJECT.equals( containedStructureType )
						&& StringHelper.isEmpty( style.startObject )
				)
				&& !(
						StructureType.UNNAMED_ENTRY.equals( entryType )
						&& StructureType.LIST.equals( containedStructureType )
						&& StringHelper.isEmpty( style.startList )
				)
		) {
			appendNewline();
			appendIndentIfNecessary();
		}

		if ( StringHelper.isNotEmpty( name ) ) {
			builder.append( name );
			builder.append( style.nameValueSeparator );
		}

		structureTypeStack.push( entryType );
	}

	private void endEntry() {
		StructureType lastType = structureTypeStack.peek();
		if ( lastType == null ) {
			throw new AssertionFailure( "Cannot pop, already at root" );
		}
		else if ( !StructureType.UNNAMED_ENTRY.equals( lastType )
				&& !StructureType.NAMED_ENTRY.equals( lastType ) ) {
			throw new AssertionFailure( "Cannot pop, not inside an entry" );
		}
		structureTypeStack.pop();
		first = false;
	}

	private void startStructure(StructureType structureType, String startDelimiter) {
		if ( StringHelper.isNotEmpty( startDelimiter ) ) {
			builder.append( startDelimiter );
		}

		structureTypeStack.push( structureType );
		first = true;
	}

	private void endStructure(StructureType structureType, String endDelimiter) {
		StructureType lastType = structureTypeStack.peek();
		if ( lastType == null ) {
			throw new AssertionFailure( "Cannot pop, already at root" );
		}
		else if ( lastType != structureType ) {
			throw new AssertionFailure( "Cannot pop, not inside a " + structureType );
		}
		structureTypeStack.pop();

		if ( StringHelper.isNotEmpty( endDelimiter ) ) {
			appendNewline();
			appendIndentIfNecessary();
			builder.append( endDelimiter );
		}
		first = false;
	}

	private void appendNewline() {
		builder.append( style.newline );
	}

	private void appendIndentIfNecessary() {
		if ( structureTypeStack.isEmpty() ) {
			return;
		}

		Iterator<StructureType> iterator = structureTypeStack.descendingIterator();
		StructureType grandParent = null;
		StructureType parent = null;
		StructureType current = iterator.next();
		StructureType child = iterator.hasNext() ? iterator.next() : null;
		StructureType grandChild;
		while ( current != null ) {
			grandChild = iterator.hasNext() ? iterator.next() : null;
			if ( !shouldSqueeze( current, parent, grandParent ) ) {
				appendIndentIfNecessary( grandChild, child, current, iterator.hasNext() );
			}
			grandParent = parent;
			parent = current;
			current = child;
			child = grandChild;
		}
	}

	private void appendIndentIfNecessary(StructureType grandChild, StructureType child, StructureType current,
			boolean hasParent) {
		switch ( current ) {
			case OBJECT:
				builder.append( style.indentInObject );
				break;
			case LIST:
				// Display a bullet point if:
				if (
						// We are adding an element directly to the list
						child == null
						// OR we are adding the first element to a squeezed element in the list
						|| shouldSqueeze( grandChild, child, current ) && !hasParent && first
				) {
					builder.append( style.indentInListBulletPoint );
				}
				else {
					builder.append( style.indentInListNoBulletPoint );
				}
				break;
			case UNNAMED_ENTRY:
			case NAMED_ENTRY:
				// No indent for these
				break;
		}
	}

	/**
	 * @param structureType The type of the potentially squeezed structure
	 * @param parentStructureType The type of the closest containing structure
	 * @param grandParentStructureType The type of the second closest containing structure
	 * @return {@code true} if the child structure should be squeezed,
	 * i.e. displayed on the same line as its parent if it's the first element,
	 * and have its indenting ignored.
	 */
	private boolean shouldSqueeze(StructureType structureType, StructureType parentStructureType,
			StructureType grandParentStructureType) {
		return style.squeezeObjectsInList
				&& StructureType.LIST.equals( grandParentStructureType )
				&& StructureType.UNNAMED_ENTRY.equals( parentStructureType )
				&& StructureType.OBJECT.equals( structureType );
	}

	private enum StructureType {
		OBJECT,
		LIST,
		NAMED_ENTRY,
		UNNAMED_ENTRY
	}

}
