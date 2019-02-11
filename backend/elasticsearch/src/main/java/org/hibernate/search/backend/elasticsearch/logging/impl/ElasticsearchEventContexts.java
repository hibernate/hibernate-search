/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.logging.impl;

import org.hibernate.search.util.EventContext;
import org.hibernate.search.util.EventContextElement;

import org.jboss.logging.Messages;

public final class ElasticsearchEventContexts {

	private static final ElasticsearchEventContextMessages MESSAGES = Messages.getBundle( ElasticsearchEventContextMessages.class );

	private static final EventContext SCHEMA_VALIDATION = EventContext.create(
			new EventContextElement() {
				@Override
				public String toString() {
					return "EventContextElement[" + render() + "]";
				}

				@Override
				public String render() {
					return MESSAGES.schemaValidation();
				}
			}
	);

	private ElasticsearchEventContexts() {
	}

	public static EventContext getSchemaValidation() {
		return SCHEMA_VALIDATION;
	}

}
