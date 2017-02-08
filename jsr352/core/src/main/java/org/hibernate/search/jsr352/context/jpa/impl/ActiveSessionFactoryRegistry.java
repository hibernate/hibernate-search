/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.context.jpa.impl;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.search.exception.SearchException;

/**
 * A registry containing all the currently active (non-closed) session factories
 * that use the same classloader.
 * <p>
 * This implementation has the advantage of not relying on external frameworks
 * (like a dependency injection framework), but has two downsides:
 * <ul>
 * <li>Session factories cannot be instantiated on demand: they must
 * be created <strong>before</strong> being retrieved from the registry.
 * <li>Only session factories created from the same classloader are
 * visible in the registry.
 * </ul>
 *
 * @author Yoann Rodiere
 */
public class ActiveSessionFactoryRegistry implements MutableSessionFactoryRegistry {

	private static final MutableSessionFactoryRegistry INSTANCE = new ActiveSessionFactoryRegistry();

	private static final String PERSISTENCE_UNIT_NAME_SCOPE_NAME = "persistence-unit-name";
	private static final String SESSION_FACTORY_NAME_SCOPE_NAME = "session-factory-name";

	public static MutableSessionFactoryRegistry getInstance() {
		return INSTANCE;
	}

	private final Collection<SessionFactoryImplementor> sessionFactories = new HashSet<>();

	private final ConcurrentMap<String, SessionFactoryImplementor> sessionFactoriesByPUName = new ConcurrentHashMap<>();

	private final ConcurrentMap<String, SessionFactoryImplementor> sessionFactoriesByName = new ConcurrentHashMap<>();

	private ActiveSessionFactoryRegistry() {
		// Use getInstance()
	}

	@Override
	public synchronized void register(SessionFactoryImplementor sessionFactory) {
		sessionFactories.add( sessionFactory );
		Object persistenceUnitName = sessionFactory.getProperties().get( AvailableSettings.PERSISTENCE_UNIT_NAME );
		if ( persistenceUnitName instanceof String ) {
			sessionFactoriesByPUName.put( (String) persistenceUnitName, sessionFactory );
		}
		String name = sessionFactory.getName();
		if ( name != null ) {
			sessionFactoriesByName.put( name, sessionFactory );
		}
	}

	@Override
	public synchronized void unregister(SessionFactoryImplementor sessionFactory) {
		sessionFactories.remove( sessionFactory );
		/*
		 * Remove by value. This is inefficient, but we don't expect to have billions of session factories anyway,
		 * and it allows to easily handle the case where multiple session factories have been registered with the same name.
		 */
		sessionFactoriesByPUName.values().remove( sessionFactory );
		sessionFactoriesByName.values().remove( sessionFactory );
	}

	@Override
	public synchronized EntityManagerFactory getDefault() {
		if ( sessionFactories.isEmpty() ) {
			throw new SearchException( "No entity manager factory has been created yet."
					+ " Make sure that the entity manager factory has already been created and wasn't closed before"
					+ " you launched the job." );
		}
		else if ( sessionFactories.size() > 1 ) {
			throw new SearchException( "Multiple entity manager factories are currently active."
					+ " Please provide the name of the selected persistence unit to the batch indexing job through"
					+ " the 'entityManagerFactoryReference' parameter (you may also use the 'entityManagerFactoryScope'"
					+ " parameter for more referencing options)." );
		}
		else {
			return sessionFactories.iterator().next();
		}
	}

	@Override
	public EntityManagerFactory get(String reference) {
		return get( PERSISTENCE_UNIT_NAME_SCOPE_NAME, reference );
	}

	@Override
	public EntityManagerFactory get(String scopeName, String reference) {
		SessionFactory factory;

		switch ( scopeName ) {
			case PERSISTENCE_UNIT_NAME_SCOPE_NAME:
				factory = sessionFactoriesByPUName.get( reference );
				if ( factory == null ) {
					throw new SearchException( "No entity manager factory has been created with this persistence unit name yet: '" + reference +"'."
							+ " Make sure you use the JPA API to create your entity manager factory (use a 'persistence.xml' file)"
							+ " and that the entity manager factory has already been created and wasn't closed before"
							+ " you launch the job." );
				}
				break;
			case SESSION_FACTORY_NAME_SCOPE_NAME:
				factory = sessionFactoriesByName.get( reference );
				if ( factory == null ) {
					throw new SearchException( "No entity manager factory has been created with this name yet: '" + reference +"'."
							+ " Make sure your entity manager factory is named"
							+ " (for instance by setting the '" + org.hibernate.cfg.AvailableSettings.SESSION_FACTORY_NAME + "' option)"
							+ " and that the entity manager factory has already been created and wasn't closed before"
							+ " you launch the job." );
				}
				break;
			default:
				throw new SearchException( "Unknown entity manager factory scope: '" + scopeName + "'."
						+ " Please use a supported scope." );
		}

		return factory;
	}

}
