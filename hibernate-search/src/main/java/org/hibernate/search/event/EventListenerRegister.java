/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.event;

import java.util.Properties;

import org.slf4j.Logger;

import org.hibernate.event.EventListeners;
import org.hibernate.event.FlushEventListener;
import org.hibernate.event.PostCollectionRecreateEventListener;
import org.hibernate.event.PostCollectionRemoveEventListener;
import org.hibernate.event.PostCollectionUpdateEventListener;
import org.hibernate.event.PostDeleteEventListener;
import org.hibernate.event.PostInsertEventListener;
import org.hibernate.event.PostUpdateEventListener;
import org.hibernate.search.Environment;
import org.hibernate.search.util.LoggerFactory;

/**
 * Helper methods initializing Hibernate Search event listeners.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 * @author Sanne Grinovero
 */
public class EventListenerRegister {

	private static final Logger log = LoggerFactory.make();

	/**
	 * Add the FullTextIndexEventListener to all listeners, if enabled in configuration
	 * and if not already registered.
	 *
	 * @param listeners
	 * @param properties the Search configuration
	 */
	public static void enableHibernateSearch(EventListeners listeners, Properties properties) {
		// check whether search is explicitly disabled - if so there is nothing to do
		String enableSearchListeners = properties.getProperty( Environment.AUTOREGISTER_LISTENERS );
		if ( "false".equalsIgnoreCase( enableSearchListeners ) ) {
			log.info(
					"Property hibernate.search.autoregister_listeners is set to false." +
							" No attempt will be made to register Hibernate Search event listeners."
			);
			return;
		}
		final FullTextIndexEventListener searchListener =
				new FullTextIndexEventListener( FullTextIndexEventListener.Installation.SINGLE_INSTANCE );

		// PostInsertEventListener
		listeners.setPostInsertEventListeners(
				addIfNeeded(
						listeners.getPostInsertEventListeners(),
						searchListener,
						new PostInsertEventListener[] { searchListener }
				)
		);
		// PostUpdateEventListener
		listeners.setPostUpdateEventListeners(
				addIfNeeded(
						listeners.getPostUpdateEventListeners(),
						searchListener,
						new PostUpdateEventListener[] { searchListener }
				)
		);
		// PostDeleteEventListener
		listeners.setPostDeleteEventListeners(
				addIfNeeded(
						listeners.getPostDeleteEventListeners(),
						searchListener,
						new PostDeleteEventListener[] { searchListener }
				)
		);

		// PostCollectionRecreateEventListener
		listeners.setPostCollectionRecreateEventListeners(
				addIfNeeded(
						listeners.getPostCollectionRecreateEventListeners(),
						searchListener,
						new PostCollectionRecreateEventListener[] { searchListener }
				)
		);
		// PostCollectionRemoveEventListener
		listeners.setPostCollectionRemoveEventListeners(
				addIfNeeded(
						listeners.getPostCollectionRemoveEventListeners(),
						searchListener,
						new PostCollectionRemoveEventListener[] { searchListener }
				)
		);
		// PostCollectionUpdateEventListener
		listeners.setPostCollectionUpdateEventListeners(
				addIfNeeded(
						listeners.getPostCollectionUpdateEventListeners(),
						searchListener,
						new PostCollectionUpdateEventListener[] { searchListener }
				)
		);
		// Adding also as FlushEventListener to manage events out-of-transaction
		listeners.setFlushEventListeners(
				addIfNeeded(
						listeners.getFlushEventListeners(),
						searchListener,
						new FlushEventListener[] { searchListener }
				)
		);
	}

	/**
	 * Verifies if a Search listener is already present; if not it will return
	 * a grown address adding the listener to it.
	 *
	 * @param <T> the type of listeners
	 * @param listeners
	 * @param searchEventListener
	 * @param toUseOnNull this is returned if listeners==null
	 *
	 * @return
	 */
	private static <T> T[] addIfNeeded(T[] listeners, T searchEventListener, T[] toUseOnNull) {
		if ( listeners == null ) {
			return toUseOnNull;
		}
		else if ( !isPresentInListeners( listeners ) ) {
			return appendToArray( listeners, searchEventListener );
		}
		else {
			return listeners;
		}
	}

	/**
	 * Will add one element to the end of an array.
	 *
	 * @param <T> The array type
	 * @param listeners The original array
	 * @param newElement The element to be added
	 *
	 * @return A new array containing all listeners and newElement.
	 */
	@SuppressWarnings("unchecked")
	private static <T> T[] appendToArray(T[] listeners, T newElement) {
		int length = listeners.length;
		T[] ret = ( T[] ) java.lang.reflect.Array.newInstance(
				listeners.getClass().getComponentType(), length + 1
		);
		System.arraycopy( listeners, 0, ret, 0, length );
		ret[length] = newElement;
		return ret;
	}

	/**
	 * Verifies if a FullTextIndexEventListener is contained in the array.
	 *
	 * @param listeners
	 *
	 * @return true if it is contained in.
	 */
	@SuppressWarnings("deprecation")
	private static boolean isPresentInListeners(Object[] listeners) {
		for ( Object eventListener : listeners ) {
			if ( FullTextIndexEventListener.class == eventListener.getClass() ) {
				return true;
			}
		}
		return false;
	}
}
