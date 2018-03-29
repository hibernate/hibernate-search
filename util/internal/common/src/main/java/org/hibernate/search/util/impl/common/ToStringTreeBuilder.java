/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.common;

import org.hibernate.search.util.AssertionFailure;

public class ToStringTreeBuilder {

	private final StringBuilder builder = new StringBuilder();
	private final ToStringStyle style;

	private boolean first = true;
	private int depth = 0;

	public ToStringTreeBuilder() {
		this( ToStringStyle.INLINE );
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
			attribute( name, (ToStringTreeAppendable) value );
		}
		else {
			beforeElement();
			name( name );
			builder.append( value );
		}
		return this;
	}

	public ToStringTreeBuilder attribute(String name, ToStringTreeAppendable value) {
		if ( value != null ) {
			startObject( name );
			value.appendTo( this );
			endObject();
		}
		else {
			attribute( name, (Object) null );
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
		beforeElement();
		name( name );
		builder.append( "{" );
		++depth;
		first = true;
		appendNewline();
		return this;
	}

	public ToStringTreeBuilder endObject() {
		if ( depth == 0 ) {
			throw new AssertionFailure( "Cannot pop, already at root" );
		}
		--depth;
		if ( !first ) {
			appendNewline();
		}
		first = false;
		appendIndent();
		builder.append( "}" );
		return this;
	}

	public ToStringTreeBuilder startList() {
		return startList( null );
	}

	public ToStringTreeBuilder startList(String name) {
		beforeElement();
		name( name );
		builder.append( "[" );
		++depth;
		first = true;
		appendNewline();
		return this;
	}

	public ToStringTreeBuilder endList() {
		if ( depth == 0 ) {
			throw new AssertionFailure( "Cannot pop, already at root" );
		}
		--depth;
		if ( !first ) {
			appendNewline();
		}
		first = false;
		appendIndent();
		builder.append( "]" );
		return this;
	}

	private void beforeElement() {
		if ( first ) {
			first = false;
			appendIndent();
		}
		else {
			builder.append( style.separator );
			appendNewline();
			appendIndent();
		}
	}

	private void name(String name) {
		if ( name != null && !name.isEmpty() ) {
			builder.append( name );
			builder.append( "=" );
		}
	}

	private void appendNewline() {
		builder.append( style.newline );
	}

	private void appendIndent() {
		if ( !style.indent.isEmpty() ) {
			for ( int i = 0; i < depth; ++i ) {
				builder.append( style.indent );
			}
		}
	}

}
