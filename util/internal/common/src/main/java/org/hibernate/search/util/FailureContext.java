/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import org.hibernate.search.util.impl.common.logging.CommonFailureContextMessages;

import org.jboss.logging.Messages;

/**
 * A sequence of {@link FailureContextElement}.
 */
public final class FailureContext {

	private static final CommonFailureContextMessages MESSAGES = Messages.getBundle( CommonFailureContextMessages.class );

	public static FailureContext create(FailureContextElement firstElement, FailureContextElement ... otherElements) {
		FailureContext result = new FailureContext( null, firstElement );
		for ( FailureContextElement otherElement : otherElements ) {
			result = new FailureContext( result, otherElement );
		}
		return result;
	}

	public static FailureContext concat(FailureContext first, FailureContext ... others) {
		FailureContext result = first;
		for ( FailureContext other : others ) {
			result = first.append( other );
		}
		return result;
	}

	private final FailureContext parent;
	private final FailureContextElement element;

	private FailureContext(FailureContext parent, FailureContextElement element) {
		this.parent = parent;
		this.element = element;
	}

	@Override
	public String toString() {
		return getClass() + "[" + render() + "]";
	}

	public List<FailureContextElement> getElements() {
		List<FailureContextElement> result = new ArrayList<>();
		addTo( result );
		return result;
	}

	/**
	 * @return A human-readable representation of this context.
	 */
	public String render() {
		StringJoiner contextJoiner = new StringJoiner( MESSAGES.contextSeparator() );
		for ( FailureContextElement element : getElements() ) {
			contextJoiner.add( element.render() );
		}
		return MESSAGES.contextPrefix() + contextJoiner.toString();
	}

	public FailureContext append(FailureContext other) {
		return other.appendTo( this );
	}

	private FailureContext appendTo(FailureContext other) {
		FailureContext result = other;
		if ( parent != null ) {
			result = parent.appendTo( result );
		}
		result = new FailureContext( result, element );
		return result;
	}

	private void addTo(List<FailureContextElement> list) {
		if ( parent != null ) {
			parent.addTo( list );
		}
		list.add( element );
	}

}
