/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.cdi.context.jpa.impl;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanAttributes;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionTargetFactory;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import org.hibernate.search.exception.SearchException;
import org.hibernate.search.jsr352.context.jpa.EntityManagerFactoryRegistry;

/**
 * An {@link EntityManagerFactoryRegistry} that retrieves the entity manager factory
 * from the CDI context by its bean name.
 * <p>
 * When calling {@link #getDefault()}, the single registered EntityManagerFactory bean
 * will be returned, or if there is none the entity manager factory will be retrieved
 * using a {@literal @PersistenceUnit annotation}.
 * <p>
 * When calling {@link #get(String)} or {@link #get(String, String)}, the reference
 * will be interpreted as a {@link Named} qualifier.
 * <p>
 * <strong>Caution:</strong> {@link EntityManagerFactory} are not considered as beans per
 * default, and thus can't be retrieved without a specific user configuration. In order
 * for retrieval by name to work, users should have producer methods expose the entity manager
 * factories in their context, for instance like this:
 *
 * <pre>
&#064;ApplicationScoped
public class EntityManagerFactoriesProducer {

	&#064;PersistenceUnit(unitName = "db1")
	private EntityManagerFactory db1Factory;

	&#064;PersistenceUnit(unitName = "db2")
	private EntityManagerFactory db2Factory;

	&#064;Produces
	&#064;Singleton
	&#064;Named("db1") // The name to use when referencing the bean
	public EntityManagerFactory createEntityManagerFactoryForDb1() {
		return db1Factory;
	}

	&#064;Produces
	&#064;Singleton
	&#064;Named("db2") // The name to use when referencing the bean
	public EntityManagerFactory createEntityManagerFactoryForDb2() {
		return db2Factory;
	}
}
 * </pre>
 * <p>
 * Note that retrieving an EntityManagerFactory by its persistence unit name is not
 * supported, because CDI does not offer any API allowing to achieve it dynamically.
 * Indeed:
 * <ul>
 * <li>{@literal @PersistenceUnit} is not a qualifier annotation, so the usual CDI
 * approaches for retrieving a bean dynamically ({@literal Instance.select},
 * {@literal BeanManager.getBeans}, ...) won't work.
 * <li>there is no way to inject all the persistence units (so we could filter them and
 * select one by its name) because {@literal @PersistenceUnit} does not work on a
 * {@literal Instance<EntityManagerFactory>} (at least not with Weld).
 * </ul>
 *
 * @author Yoann Rodiere
 */
@Singleton
public class CDIEntityManagerFactoryRegistry implements EntityManagerFactoryRegistry {

	private static final String CDI_SCOPE_NAME = "cdi";

	@Inject
	private Instance<EntityManagerFactory> entityManagerFactoryInstance;

	@Inject
	private BeanManager beanManager;

	@Override
	public EntityManagerFactory getDefault() {
		if ( entityManagerFactoryInstance.isUnsatisfied() ) {
			try {
				return getVetoedBeanReference( beanManager, PersistenceUnitAccessor.class ).entityManagerFactory;
			}
			catch (RuntimeException e) {
				throw new SearchException( "Exception while retrieving the EntityManagerFactory using @PersistenceUnit."
						+ " This generally happens either because the persistence wasn't configured properly"
						+ " or because there are multiple persistence units." );
			}
		}
		else if ( entityManagerFactoryInstance.isAmbiguous() ) {
			throw new SearchException( "Multiple entity manager factories have been registered in the CDI context."
					+ " Please provide the bean name for the selected entity manager factory to the batch indexing job through"
					+ " the 'entityManagerFactoryReference' parameter." );
		}
		else {
			return entityManagerFactoryInstance.get();
		}
	}

	@Override
	public EntityManagerFactory get(String reference) {
		return get( CDI_SCOPE_NAME, reference );
	}

	@Override
	public EntityManagerFactory get(String scopeName, String reference) {
		EntityManagerFactory factory;

		switch ( scopeName ) {
			case CDI_SCOPE_NAME:
				Instance<EntityManagerFactory> instance = entityManagerFactoryInstance.select( new NamedQualifier( reference ) );
				if ( instance.isUnsatisfied() ) {
					throw new SearchException( "No entity manager factory available in the CDI context with this bean name: '" + reference +"'."
							+ " Make sure your entity manager factory is a named bean." );
				}
				factory = instance.get();
				break;
			default:
				throw new SearchException( "Unknown entity manager factory scope: '" + scopeName + "'."
						+ " Please use a supported scope." );
		}

		return factory;
	}

	/**
	 * Creates an instance of a @Vetoed bean type using the given bean manager.
	 * <p>
	 * This seems overly complicated, but all the usual solutions
	 * fail when you want to create access an {@link EntityManagerFactory}
	 * from the CDI context lazily...
	 * <p>
	 * <ol>
	 * <li>Adding a {@literal @PersistenceUnit} on an {@literal Instance<EntityManagerFactory>}
	 * field or on a {@literal Provider<EntityManagerFactory> field will make Weld throw
	 * an exception (it only allows a field of type {@literal EntityManagerFactory}).
	 * <li>Weld seems to check {@literal @PersistenceUnit} annotations when creating injection
	 * points, not when injecting. This means that a {@literal @PersistenceUnit} without a unitName
	 * will make the application startup fail when there are multiple persistence units, even if
	 * the bean on which this annotation is applied is never instantiated.
	 * </ol>
	 * <p>
	 * Thus:
	 *
	 * <ol>
	 * <li>We access the {@literal @PeristenceUnit} field from a different bean, instantiated only
	 * when (if) we need it
	 * <li>The {@literal @PersistentUnit}-annotated bean is {@literal @Vetoed} so that it's not
	 * processed by the CDI engine by default, but only when we request processing explicitly.
	 * And that's what this method does: it makes the CDI engine process the type and instantiate it.
	 * </ol>
	 */
	private static <T> T getVetoedBeanReference(BeanManager beanManager, Class<T> vetoedType) {
		AnnotatedType<T> annotatedType = beanManager.createAnnotatedType( vetoedType );
		BeanAttributes<T> beanAttributes = beanManager.createBeanAttributes( annotatedType );
		InjectionTargetFactory<T> injectionTargetFactory = beanManager.getInjectionTargetFactory( annotatedType );
		Bean<T> bean = beanManager.createBean( beanAttributes, vetoedType, injectionTargetFactory );
		CreationalContext<T> creationalContext = beanManager.createCreationalContext( bean );
		return vetoedType.cast( beanManager.getReference( bean, vetoedType, creationalContext ) );
	}

	/**
	 * @see CDIEntityManagerFactoryRegistry#getVetoedBeanReference(BeanManager, Class)
	 */
	@Vetoed
	private static class PersistenceUnitAccessor {
		@PersistenceUnit
		private EntityManagerFactory entityManagerFactory;
	}

	private static class NamedQualifier extends AnnotationLiteral<Named> implements Named {
		private final String name;

		public NamedQualifier(String name) {
			super();
			this.name = name;
		}

		@Override
		public String value() {
			return name;
		}
	}

}
