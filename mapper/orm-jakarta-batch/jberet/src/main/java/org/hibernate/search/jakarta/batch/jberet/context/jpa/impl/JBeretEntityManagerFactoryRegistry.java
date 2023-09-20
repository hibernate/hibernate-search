/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jakarta.batch.jberet.context.jpa.impl;

import java.lang.invoke.MethodHandles;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Vetoed;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanAttributes;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionTargetFactory;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;

import org.hibernate.search.jakarta.batch.core.context.jpa.spi.EntityManagerFactoryRegistry;
import org.hibernate.search.jakarta.batch.jberet.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * An {@link EntityManagerFactoryRegistry} that retrieves the entity manager factory
 * from the CDI context by its bean name.
 * <p>
 * When calling {@link #useDefault()}, the single registered EntityManagerFactory bean
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
@ApplicationScoped
public class JBeretEntityManagerFactoryRegistry implements EntityManagerFactoryRegistry {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final String CDI_NAMESPACE_NAME = "cdi";

	@Inject
	private Instance<EntityManagerFactory> entityManagerFactoryInstance;

	@Inject
	private BeanManager beanManager;

	@Override
	public EntityManagerFactory useDefault() {
		if ( entityManagerFactoryInstance.isUnsatisfied() ) {
			try {
				return getVetoedBeanReference( beanManager, PersistenceUnitAccessor.class ).entityManagerFactory;
			}
			catch (RuntimeException e) {
				throw log.cannotRetrieveEntityManagerFactoryInJakartaBatch();
			}
		}
		else if ( entityManagerFactoryInstance.isAmbiguous() ) {
			throw log.ambiguousEntityManagerFactoryInJakartaBatch();
		}
		else {
			return entityManagerFactoryInstance.get();
		}
	}

	@Override
	public EntityManagerFactory get(String reference) {
		return get( CDI_NAMESPACE_NAME, reference );
	}

	@Override
	public EntityManagerFactory get(String namespace, String reference) {
		EntityManagerFactory factory;

		switch ( namespace ) {
			case CDI_NAMESPACE_NAME:
				Instance<EntityManagerFactory> instance =
						entityManagerFactoryInstance.select( new NamedQualifier( reference ) );
				if ( instance.isUnsatisfied() ) {
					throw log.noAvailableEntityManagerFactoryInCDI( reference );
				}
				factory = instance.get();
				break;
			default:
				throw log.unknownEntityManagerFactoryNamespace( namespace );
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
	 * @see JBeretEntityManagerFactoryRegistry#getVetoedBeanReference(BeanManager, Class)
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
