/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.mapper.orm;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EmbeddableType;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.Metamodel;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.Query;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.mapping.impl.HibernateOrmMapping;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.test.function.ThrowingBiFunction;
import org.hibernate.search.util.impl.test.function.ThrowingConsumer;
import org.hibernate.search.util.impl.test.function.ThrowingFunction;

import org.hibernate.testing.junit4.CustomParameterized;
import org.junit.rules.MethodRule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import org.jboss.logging.Logger;

/**
 * A test rule to create a {@link SessionFactory} and reuse it across test methods,
 * automatically reinitializing data between test methods.
 * <p>
 * Useful for tests with many test methods, where recreating the {@link SessionFactory}
 * before each method would take too much time, in particular when testing against some
 * databases that are slow to create schemas (Oracle, DB2, ...).
 * <p>
 * Usage with BackendMock:
 * <pre>{@code
 * 	@ClassRule
 * 	public static BackendMock backendMock = new BackendMock();
 *
 * 	@ClassRule
 * 	public static ReusableOrmSetupHolder setupHolder = ReusableOrmSetupHolder.withBackendMock( backendMock );
 *
 * 	@Rule
 * 	public MethodRule setupHolderMethodRule = setupHolder.methodRule();
 *
 * 	@ReusableOrmSetupHolder.Setup
 * 	public void setup(OrmSetupHelper.SetupContext setupContext, ReusableOrmSetupHolder.DataClearConfig dataClearConfig) {
 * 	    // configure setupContext here, but do NOT call setupContext.setup(); the rule will do that.
 * 		...
 * 	}
 * }</pre>
 * <p>
 * Usage with real backends:
 * <pre>{@code
 *  @ClassRule
 *  public static ReusableOrmSetupHolder setupHolder =
 *  		ReusableOrmSetupHolder.withSingleBackend( BackendConfigurations.simple() );
 *
 * 	@Rule
 * 	public MethodRule setupHolderMethodRule = setupHolder.methodRule();
 *
 * 	@ReusableOrmSetupHolder.Setup
 * 	public void setup(OrmSetupHelper.SetupContext setupContext, ReusableOrmSetupHolder.DataClearConfig dataClearConfig) {
 * 	    // configure setupContext here, but do NOT call setupContext.setup(); the rule will do that.
 * 		...
 * 	}
 * }</pre>
 */
public class ReusableOrmSetupHolder implements TestRule, PersistenceRunner<Session, Transaction> {
	private static final Logger log = Logger.getLogger( ReusableOrmSetupHolder.class.getName() );

	/**
	 * When applied to a public instance method in a test,
	 * designates a setup method which must be called by {@link ReusableOrmSetupHolder}
	 * when creating the session factory.
	 * <p>
	 * The method can get passed arguments of the following types:
	 * <ul>
	 *     <li>{@link org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper.SetupContext}</li>
	 *     <li>{@link DataClearConfig}</li>
	 * </ul>
	 *
	 * @see ReusableOrmSetupHolder
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public @interface Setup {

	}

	/**
	 * When applied to a public instance method in a test,
	 * designates a method which must be called by {@link ReusableOrmSetupHolder}
	 * to inspect the parameters of the current test that could impact session factory setup.
	 * <p>
	 * Useful (and mandatory) when using {@link ReusableOrmSetupHolder}
	 * in conjunction with the {@link org.junit.runners.Parameterized} runner,
	 * so that the session factory can be recreated for each set of parameters,
	 * but reused across test methods using the same parameters.
	 * <p>
	 * The method must return a {@code Collection<?>} and must not accept any parameter.
	 *
	 * @see ReusableOrmSetupHolder
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public @interface SetupParams {

	}

	public interface DataClearConfig {

		DataClearConfig tenants(String ... tenantIds);

		DataClearConfig preClear(Consumer<Session> preClear);

		<T> DataClearConfig preClear(Class<T> entityType, Consumer<T> preClear);

		DataClearConfig clearOrder(Class<?>... entityClasses);

	}

	public static ReusableOrmSetupHolder withBackendMock(BackendMock backendMock) {
		return new ReusableOrmSetupHolder( OrmSetupHelper.withBackendMock( backendMock ),
				Collections.singletonList( backendMock ), IndexDataClearStrategy.NONE );
	}

	public static ReusableOrmSetupHolder withBackendMocks(BackendMock defaultBackendMock,
			Map<String, BackendMock> namedBackendMocks) {
		List<BackendMock> allBackendMocks = new ArrayList<>();
		if ( defaultBackendMock != null ) {
			allBackendMocks.add( defaultBackendMock );
		}
		allBackendMocks.addAll( namedBackendMocks.values() );
		return new ReusableOrmSetupHolder( OrmSetupHelper.withBackendMocks( defaultBackendMock, namedBackendMocks ),
				allBackendMocks, IndexDataClearStrategy.NONE );
	}

	public static ReusableOrmSetupHolder withSingleBackend(BackendConfiguration backendConfiguration) {
		return new ReusableOrmSetupHolder( OrmSetupHelper.withSingleBackend( backendConfiguration ),
				Collections.emptyList(), IndexDataClearStrategy.DROP_AND_CREATE_SCHEMA );
	}

	private final OrmSetupHelper setupHelper;
	private final List<BackendMock> allBackendMocks;
	private final IndexDataClearStrategy indexDataClearStrategy;

	private boolean inClassStatement;
	private boolean inMethodStatement;
	private DataClearConfigImpl config;
	private SessionFactoryImplementor sessionFactory;
	private Collection<?> testParamsForSessionFactory;

	private ReusableOrmSetupHolder(OrmSetupHelper setupHelper, List<BackendMock> allBackendMocks,
			IndexDataClearStrategy indexDataClearStrategy) {
		this.setupHelper = setupHelper;
		this.allBackendMocks = allBackendMocks;
		this.indexDataClearStrategy = indexDataClearStrategy;
	}

	// We need the class rule and test rule to be two separate objects, unfortunately.
	// See
	public MethodRule methodRule() {
		return new MethodRule() {
			@Override
			public Statement apply(Statement base, FrameworkMethod method, Object target) {
				return methodStatement( base, target );
			}
		};
	}

	public ReusableOrmSetupHolder coordinationStrategy(CoordinationStrategyExpectations coordinationStrategyExpectations) {
		setupHelper.coordinationStrategy( coordinationStrategyExpectations );
		return this;
	}

	public boolean areEntitiesProcessedInSession() {
		return setupHelper.areEntitiesProcessedInSession();
	}

	public EntityManagerFactory entityManagerFactory() {
		return sessionFactory();
	}

	public SessionFactoryImplementor sessionFactory() {
		if ( !inMethodStatement ) {
			throw new Error( "The session factory cannot be used outside of methods annotated with @Test, @Before, @After."
					+ " In particular, you cannot use it in a method annotated with " + Setup.class.getName() + ";"
					+ " use a @Before method instead." );
		}
		if ( sessionFactory == null ) {
			throw new Error( "The session factory in " + getClass().getSimpleName() + " was not created."
					+ " Did you use the rule as explained in the javadoc, with both a @ClassRule and a @Rule,"
					+ " on two separate fields?" );
		}
		return sessionFactory;
	}

	private PersistenceRunner<Session, Transaction> with() {
		//CHECKSTYLE:OFF: RegexpSinglelineJava - cannot use static import as that would clash with method of this class
		return OrmUtils.with( sessionFactory() );
		////CHECKSTYLE:ON
	}

	public PersistenceRunner<Session, Transaction> with(String tenantId) {
		//CHECKSTYLE:OFF: RegexpSinglelineJava - cannot use static import as that would clash with method of this class
		return OrmUtils.with( sessionFactory(), tenantId );
		//CHECKSTYLE:ON
	}

	@Override
	public <R, E extends Throwable> R applyNoTransaction(ThrowingFunction<? super Session, R, E> action) throws E {
		return with().applyNoTransaction( action );
	}

	@Override
	public <R, E extends Throwable> R applyInTransaction(ThrowingBiFunction<? super Session, ? super Transaction, R, E> action) throws E {
		return with().applyInTransaction( action );
	}

	public <E extends Throwable> void runInTransaction(ThrowingConsumer<? super Session, E> action) throws E {
		with().runInTransaction( action );
	}

	public <E extends Throwable> void runNoTransaction(ThrowingConsumer<? super Session, E> action) throws E {
		with().runNoTransaction( action );
	}

	@Override
	public Statement apply(Statement base, Description description) {
		return classStatement( base, description );
	}

	private Statement classStatement(Statement base, Description description) {
		Statement wrapped = new Statement() {
			@Override
			public void evaluate() throws Throwable {
				try ( Closer<Exception> closer = new Closer<>() ) {
					try {
						inClassStatement = true;
						base.evaluate();
					}
					finally {
						inClassStatement = false;
						// Do this in the closer in order to preserve the original exception.
						closer.push( ReusableOrmSetupHolder::tearDownSessionFactory, ReusableOrmSetupHolder.this );
					}
				}
			}
		};
		return setupHelper.apply( wrapped, description );
	}

	private Statement methodStatement(Statement base, Object testInstance) {
		return new Statement() {
			@Override
			public void evaluate() throws Throwable {
				try {
					setupSessionFactory( testInstance );
					inMethodStatement = true;
					base.evaluate();
					// Since BackendMock is used as a ClassRule,
					// we must explicitly force the verify/reset after each test method.
					for ( BackendMock backendMock : allBackendMocks ) {
						backendMock.verifyExpectationsMet();
					}
				}
				finally {
					inMethodStatement = false;
					for ( BackendMock backendMock : allBackendMocks ) {
						backendMock.resetExpectations();
					}
				}
			}
		};
	}

	private void setupSessionFactory(Object testInstance) {
		if ( !inClassStatement ) {
			throw new Error( "This usage of " + getClass().getSimpleName() + " is invalid and may result"
					+ " in the session factory not being closed."
					+ " Did you use the rule as explained in the javadoc, with both a @ClassRule and a @Rule,"
					+ " on two separate fields?" );
		}

		Collection<?> testParams = testParams( testInstance );

		if ( sessionFactory != null ) {
			if ( testParams.equals( testParamsForSessionFactory ) ) {
				log.infof( "Test parameters did not change (%s vs %s). Clearing data and reusing the same session factory.",
						testParamsForSessionFactory, testParams );
				try {
					clearAllData( sessionFactory );
				}
				catch (RuntimeException e) {
					// Close the session factory (and consequently drop the schema) so that later tests
					// are not affected by the failure.
					new SuppressingCloser( e )
							.push( this::tearDownSessionFactory );
					throw new Error( "Failed to clear data before test execution: " + e.getMessage(), e );
				}
				return;
			}
			else {
				log.infof( "Test parameters changed (%s vs %s). Closing the current session factory and creating another one.",
						testParamsForSessionFactory, testParams );
				tearDownSessionFactory();
			}
		}

		OrmSetupHelper.SetupContext setupContext = setupHelper.start();
		config = new DataClearConfigImpl();
		TestCustomSetup customSetup = new TestCustomSetup( testInstance );
		customSetup.callSetupMethods( setupContext, config );
		sessionFactory = setupContext.setup().unwrap( SessionFactoryImplementor.class );
		testParamsForSessionFactory = testParams;

		// If any backend expectations where set during setup, verify them immediately.
		for ( BackendMock backendMock : allBackendMocks ) {
			backendMock.verifyExpectationsMet();
		}
	}

	private void tearDownSessionFactory() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( SessionFactory::close, sessionFactory );
			sessionFactory = null;
			config = null;
		}
	}

	private Collection<?> testParams(Object testInstance) {
		@SuppressWarnings("rawtypes")
		List<TestPluggableMethod<Collection>> setupParamsMethods = TestPluggableMethod.createAll( SetupParams.class,
				testInstance.getClass(), Collection.class, Collections.emptyList() );
		if ( setupParamsMethods.size() > 1 ) {
			throw new Error( "Test class " + testInstance.getClass()
					+ " must not declare more than one method annotated with " + SetupParams.class.getName() );
		}
		if ( setupParamsMethods.isEmpty() ) {
			Class<?> runnerClass = runnerClass( testInstance.getClass() );
			if ( Parameterized.class.equals( runnerClass ) || CustomParameterized.class.equals( runnerClass ) ) {
				throw new Error( "Test class " + testInstance.getClass()
						+ " must declare one method annotated with " + SetupParams.class.getName()
						+ " because it uses runner " + runnerClass.getSimpleName() );
			}
			else {
				return Collections.emptyList();
			}
		}
		return setupParamsMethods.iterator().next().call( testInstance, Collections.emptyMap() );
	}

	private Class<?> runnerClass(Class<?> testClass) {
		RunWith annotation = testClass.getAnnotation( RunWith.class );
		return annotation == null ? null : annotation.value();
	}

	private void clearAllData(SessionFactoryImplementor sessionFactory) {
		HibernateOrmMapping mapping;
		try {
			mapping = ( (HibernateOrmMapping) Search.mapping( sessionFactory ) );
		}
		catch (SearchException e) {
			if ( e.getMessage().contains( "not initialized" ) ) {
				// Hibernate Search is simply disabled.
				mapping = null;
			}
			else {
				throw e;
			}
		}

		sessionFactory.getCache().evictAllRegions();

		clearDatabase( sessionFactory, mapping );

		// Must re-clear the caches as they may have been re-populated
		// while executing queries in clearDatabase().
		sessionFactory.getCache().evictAllRegions();

		if ( mapping != null ) {
			switch ( indexDataClearStrategy ) {
				case NONE:
					break;
				case DROP_AND_CREATE_SCHEMA:
					Search.mapping( sessionFactory ).scope( Object.class ).schemaManager().dropAndCreate();
					break;
			}
		}
	}

	private void clearDatabase(SessionFactoryImplementor sessionFactory, HibernateOrmMapping mapping) {
		if ( config.tenantsIds.isEmpty() ) {
			clearDatabase( sessionFactory, mapping, null );
		}
		else {
			for ( String tenantsId : config.tenantsIds ) {
				clearDatabase( sessionFactory, mapping, tenantsId );
			}
		}
	}


	private void clearDatabase(SessionFactoryImplementor sessionFactory, HibernateOrmMapping mapping, String tenantId) {
		for ( ThrowingConsumer<Session, RuntimeException> preClear : config.preClear ) {
			if ( mapping != null ) {
				mapping.listenerEnabled( false );
			}
			//CHECKSTYLE:OFF: RegexpSinglelineJava - cannot use static import as that would clash with method of this class
			OrmUtils.with( sessionFactory, tenantId ).runInTransaction( preClear );
			//CHECKSTYLE:ON
			if ( mapping != null ) {
				mapping.listenerEnabled( true );
			}
		}

		Set<String> clearedEntityNames = new HashSet<>();
		for ( Class<?> entityClass : config.entityClearOrder ) {
			EntityType<?> entityType;
			try {
				entityType = sessionFactory.getJpaMetamodel().entity( entityClass );
			}
			catch (IllegalArgumentException e) {
				// When using annotatedTypes to infer the clear order,
				// some annotated types may not be entities;
				// this can be ignored.
				continue;
			}
			if ( clearedEntityNames.add( entityType.getName() ) ) {
				clearEntityInstances( sessionFactory, mapping, tenantId, entityType );
			}
		}

		// Just in case some entity types were not mentioned in entityClearOrder,
		// we try to delete all remaining entity types.
		// Note we're stabilizing the order, because ORM uses a HashSet internally
		// and the order may change from one execution to the next.
		List<EntityType<?>> sortedEntityTypes = sessionFactory.getJpaMetamodel().getEntities().stream()
				.sorted( Comparator.comparing( EntityType::getName ) )
				.collect( Collectors.toList() );
		for ( EntityType<?> entityType : sortedEntityTypes ) {
			if ( clearedEntityNames.add( entityType.getName() ) ) {
				clearEntityInstances( sessionFactory, mapping, tenantId, entityType );
			}
		}
	}

	private static void clearEntityInstances(SessionFactoryImplementor sessionFactory, HibernateOrmMapping mapping,
			String tenantId, EntityType<?> entityType) {
		if ( Modifier.isAbstract( entityType.getJavaType().getModifiers() ) ) {
			// There are no instances of this specific class,
			// only instances of subclasses, and those are handled separately.
			return;
		}
		if (
				// Workaround until https://hibernate.atlassian.net/browse/HHH-5529 gets implemented
				hasPotentiallyJoinTable( sessionFactory, entityType )
				// Workaround until https://hibernate.atlassian.net/browse/HHH-14814 gets fixed
				|| hasEntitySubclass( sessionFactory, entityType )
		) {
			if ( mapping != null ) {
				mapping.listenerEnabled( false );
			}
			try {
				//CHECKSTYLE:OFF: RegexpSinglelineJava - cannot use static import as that would clash with method of this class
				OrmUtils.with( sessionFactory, tenantId ).runInTransaction( s -> {
					//CHECKSTYLE:ON
					Query<?> query = selectAllOfSpecificType( entityType, s );
					try {
						query.list().forEach( s::remove );
					}
					catch (RuntimeException e) {
						throw new RuntimeException( "Failed to delete all entity instances returned by "
								+ query.getQueryString() + " on type " + entityType + ": " + e.getMessage(), e );
					}
				} );
			}
			finally {
				if ( mapping != null ) {
					mapping.listenerEnabled( true );
				}
			}
		}
		else {
			//CHECKSTYLE:OFF: RegexpSinglelineJava - cannot use static import as that would clash with method of this class
			OrmUtils.with( sessionFactory, tenantId ).runInTransaction( s -> {
				//CHECKSTYLE:ON
				Query<?> query = deleteAllOfSpecificType( entityType, s );
				try {
					query.executeUpdate();
				}
				catch (RuntimeException e) {
					throw new RuntimeException( "Failed to execute " + query.getQueryString() + " on type " + entityType
							+ ": " + e.getMessage(), e );
				}
			} );
		}
	}

	private static Query<?> selectAllOfSpecificType(EntityType<?> entityType, Session session) {
		return createSelectOrDeleteAllOfSpecificTypeQuery( entityType, session, QueryType.SELECT );
	}

	private static Query<?> deleteAllOfSpecificType(EntityType<?> entityType, Session session) {
		return createSelectOrDeleteAllOfSpecificTypeQuery( entityType, session, QueryType.DELETE );
	}

	enum QueryType {
		SELECT,
		DELETE
	}

	private static Query<?> createSelectOrDeleteAllOfSpecificTypeQuery(EntityType<?> entityType, Session session,
			QueryType queryType) {
		StringBuilder builder = new StringBuilder( QueryType.SELECT.equals( queryType ) ? "select e " : "delete " );

		builder.append( "from " ).append( entityType.getName() ).append( " e" );
		Class<?> typeArg = null;
		if ( hasEntitySubclass( session.getSessionFactory(), entityType ) ) {
			// We must target the type explicitly, without polymorphism,
			// because subtypes might have associations pointing to the supertype,
			// in which case deleting subtypes and supertypes in the same query
			// may fail or not, depending on processing order (supertype before subtype or subtype before supertype).
			builder.append( " where type( e ) in (:type)" );
			typeArg = entityType.getJavaType();
		}
		@SuppressWarnings("deprecation")
		Query<?> query = QueryType.SELECT.equals( queryType )
				? session.createQuery( builder.toString(), entityType.getJavaType() )
				: session.createQuery( builder.toString() );
		if ( typeArg != null ) {
			query.setParameter( "type", typeArg );
		}
		return query;
	}

	private static boolean hasEntitySubclass(SessionFactory sessionFactory, EntityType<?> parentEntity) {
		Metamodel metamodel = sessionFactory.unwrap( SessionFactoryImplementor.class ).getJpaMetamodel();
		for ( EntityType<?> entity : metamodel.getEntities() ) {
			if ( parentEntity.equals( entity.getSupertype() ) ) {
				return true;
			}
		}
		return false;
	}

	private static boolean hasPotentiallyJoinTable(SessionFactoryImplementor sessionFactory,
			ManagedType<?> managedType) {
		for ( Attribute<?, ?> attribute : managedType.getAttributes() ) {
			switch ( attribute.getPersistentAttributeType() ) {
				case MANY_TO_ONE:
				case ONE_TO_ONE:
				case BASIC:
					break;
				case MANY_TO_MANY:
				case ONE_TO_MANY:
				case ELEMENT_COLLECTION:
					return true;
				case EMBEDDED:
					EmbeddableType<?> embeddable = sessionFactory.getJpaMetamodel().embeddable( attribute.getJavaType() );
					if ( hasPotentiallyJoinTable( sessionFactory, embeddable ) ) {
						return true;
					}
					break;
			}
		}
		return false;
	}

	private static class TestCustomSetup {

		private static final TestPluggableMethod.ArgumentKey<OrmSetupHelper.SetupContext> SETUP_CONTEXT_KEY =
				new TestPluggableMethod.ArgumentKey<>( OrmSetupHelper.SetupContext.class, "setupContext" );
		private static final TestPluggableMethod.ArgumentKey<DataClearConfig> CONFIG_CONTEXT_KEY =
				new TestPluggableMethod.ArgumentKey<>( DataClearConfig.class, "configContext" );
		private static final List<TestPluggableMethod.ArgumentKey<?>> KEYS =
				Arrays.asList( SETUP_CONTEXT_KEY, CONFIG_CONTEXT_KEY );

		private final List<TestPluggableMethod<Void>> setupMethods;

		private final Object testInstance;

		TestCustomSetup(Object testInstance) {
			this.testInstance = testInstance;
			this.setupMethods = TestPluggableMethod.createAll( Setup.class, testInstance.getClass(), void.class, KEYS );
		}

		void callSetupMethods(OrmSetupHelper.SetupContext setupContext, DataClearConfig dataClearConfig) {
			Map<TestPluggableMethod.ArgumentKey<?>, Object> context = new HashMap<>();
			context.put( SETUP_CONTEXT_KEY, setupContext );
			context.put( CONFIG_CONTEXT_KEY, dataClearConfig );

			for ( TestPluggableMethod<Void> pluggableMethod : setupMethods ) {
				pluggableMethod.call( testInstance, context );
			}
		}
	}

	private static class DataClearConfigImpl implements DataClearConfig {
		private final List<String> tenantsIds = new ArrayList<>();

		private final List<Class<?>> entityClearOrder = new ArrayList<>();

		private final List<ThrowingConsumer<Session, RuntimeException>> preClear = new ArrayList<>();

		@Override
		public DataClearConfig tenants(String ... tenantIds) {
			Collections.addAll( this.tenantsIds, tenantIds );
			return this;
		}

		@Override
		public DataClearConfig preClear(Consumer<Session> preClear) {
			this.preClear.add( c -> preClear.accept( c ) );
			return this;
		}

		@Override
		public <T> DataClearConfig preClear(Class<T> entityType, Consumer<T> preClear) {
			return preClear( session -> {
				// We'll go through subtypes as well here,
				// on contrary to selectAllOfSpecificType(),
				// because we are performing updates only, not deletes.

				CriteriaBuilder builder = session.getCriteriaBuilder();
				CriteriaQuery<T> query = builder.createQuery( entityType );
				Root<T> root = query.from( entityType );
				query.select( root );
				for ( T entity : session.createQuery( query ).list() ) {
					preClear.accept( entity );
				}
			} );
		}

		@Override
		public DataClearConfigImpl clearOrder(Class<?>... entityClasses) {
			entityClearOrder.clear();
			Collections.addAll( entityClearOrder, entityClasses );
			return this;
		}
	}

	private enum IndexDataClearStrategy {
		NONE,
		DROP_AND_CREATE_SCHEMA
	}

}
