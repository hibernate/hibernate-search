/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.manualsource.impl;

import java.sql.Time;

import org.hibernate.search.manualsource.source.EntitySourceContext;
import org.hibernate.search.manualsource.source.ObjectInitializer;
import org.hibernate.search.query.engine.spi.TimeoutManager;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class ObjectInitializerContext implements ObjectInitializer.Context {
	private final EntitySourceContext entitySourceContext;
	private final TimeoutManager timeoutManager;

	public ObjectInitializerContext(EntitySourceContext entitySourceContext, TimeoutManager timeoutManager) {
		this.entitySourceContext = entitySourceContext;
		this.timeoutManager = timeoutManager;
	}

	@Override
	public EntitySourceContext getEntitySourceContext() {
		return entitySourceContext;
	}

	@Override
	public TimeoutManager getTimeoutManager() {
		return timeoutManager;
	}
}
