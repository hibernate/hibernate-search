/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.common.reporting;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

import org.hibernate.search.util.common.reporting.impl.CommonEventContextMessages;

import org.jboss.logging.Messages;

/**
 * A sequence of {@link EventContextElement}.
 */
public final class EventContext {

	private static final CommonEventContextMessages MESSAGES = Messages.getBundle( CommonEventContextMessages.class );

	public static EventContext create(EventContextElement firstElement, EventContextElement... otherElements) {
		EventContext result = new EventContext( null, firstElement );
		for ( EventContextElement otherElement : otherElements ) {
			result = new EventContext( result, otherElement );
		}
		return result;
	}

	public static EventContext concat(EventContext first, EventContext... others) {
		EventContext result = first;
		for ( EventContext other : others ) {
			result = first.append( other );
		}
		return result;
	}

	private final EventContext parent;
	private final EventContextElement element;

	private EventContext(EventContext parent, EventContextElement element) {
		this.parent = parent;
		this.element = element;
	}

	@Override
	public String toString() {
		return getClass() + "[" + render() + "]";
	}

	@Override
	public boolean equals(Object obj) {
		if ( obj == null || !getClass().equals( obj.getClass() ) ) {
			return false;
		}

		EventContext other = (EventContext) obj;

		return Objects.equals( parent, other.parent )
				&& element.equals( other.element );
	}

	@Override
	public int hashCode() {
		return Objects.hash( parent, element );
	}

	public List<EventContextElement> getElements() {
		List<EventContextElement> result = new ArrayList<>();
		addTo( result );
		return result;
	}

	/**
	 * @return A human-readable representation of this context.
	 */
	public String render() {
		StringJoiner contextJoiner = new StringJoiner( MESSAGES.contextSeparator() );
		for ( EventContextElement element : getElements() ) {
			contextJoiner.add( element.render() );
		}
		return contextJoiner.toString();
	}

	/**
	 * @return A human-readable representation of this context, with a "Context: " prefix.
	 */
	public String renderWithPrefix() {
		return MESSAGES.contextPrefix() + render();
	}

	public EventContext append(EventContext other) {
		return other.appendTo( this );
	}

	private EventContext appendTo(EventContext other) {
		EventContext result = other;
		if ( parent != null ) {
			result = parent.appendTo( result );
		}
		result = new EventContext( result, element );
		return result;
	}

	private void addTo(List<EventContextElement> list) {
		if ( parent != null ) {
			parent.addTo( list );
		}
		list.add( element );
	}

}
