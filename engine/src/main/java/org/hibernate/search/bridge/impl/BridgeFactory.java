/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XMember;
import org.hibernate.search.annotations.ClassBridge;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Parameter;
import org.hibernate.search.annotations.Spatial;
import org.hibernate.search.bridge.AppliedOnTypeAwareBridge;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.ParameterizedBridge;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.bridge.TwoWayStringBridge;
import org.hibernate.search.bridge.builtin.impl.BuiltinArrayBridge;
import org.hibernate.search.bridge.builtin.impl.BuiltinIterableBridge;
import org.hibernate.search.bridge.builtin.impl.BuiltinMapBridge;
import org.hibernate.search.bridge.builtin.impl.String2FieldBridgeAdaptor;
import org.hibernate.search.bridge.builtin.impl.TwoWayString2FieldBridgeAdaptor;
import org.hibernate.search.bridge.spi.BridgeProvider;
import org.hibernate.search.engine.service.classloading.spi.ClassLoaderService;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.util.impl.ReflectionHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * This factory is responsible for creating and initializing build-in and custom {@code FieldBridge}s.
 *
 * @author Emmanuel Bernard
 * @author John Griffin
 */
public final class BridgeFactory {

	private static final Log LOG = LoggerFactory.make();

	private final Set<BridgeProvider> annotationBasedProviders = new HashSet<>( 5 );
	private final Set<BridgeProvider> regularProviders = new HashSet<>();

	public BridgeFactory(ServiceManager serviceManager) {
		annotationBasedProviders.add( new CalendarBridgeProvider() );
		annotationBasedProviders.add( new DateBridgeProvider() );
		annotationBasedProviders.add( new NumericBridgeProvider() );
		annotationBasedProviders.add( new SpatialBridgeProvider() );
		annotationBasedProviders.add( new TikaBridgeProvider() );

		if ( JavaTimeBridgeProvider.isActive() ) {
			annotationBasedProviders.add( new JavaTimeBridgeProvider() );
		}

		ClassLoaderService classLoaderService = serviceManager.requestService( ClassLoaderService.class );
		try {
			for ( BridgeProvider provider : classLoaderService.loadJavaServices( BridgeProvider.class ) ) {
				regularProviders.add( provider );
			}
		}
		finally {
			serviceManager.releaseService( ClassLoaderService.class );
		}
		regularProviders.add( new EnumBridgeProvider() );
		regularProviders.add( new BasicJDKTypesBridgeProvider( serviceManager ) );
	}

	/**
	 * This extracts and instantiates the implementation class from a {@code ClassBridge} annotation.
	 *
	 * @param cb the class bridge annotation
	 * @param clazz the {@code Class} on which the annotation is defined on
	 * @return Returns the specified {@code FieldBridge} instance
	 */
	public FieldBridge extractType(ClassBridge cb, Class<?> clazz) {
		FieldBridge bridge = null;
		Class<?> bridgeType = null;

		if ( cb != null ) {
			bridgeType = cb.impl();
			if ( bridgeType != null ) {
				try {
					Object instance = bridgeType.newInstance();
					if ( FieldBridge.class.isAssignableFrom( bridgeType ) ) {
						bridge = (FieldBridge) instance;
					}
					else if ( org.hibernate.search.bridge.TwoWayStringBridge.class.isAssignableFrom( bridgeType ) ) {
						bridge = new TwoWayString2FieldBridgeAdaptor(
								(org.hibernate.search.bridge.TwoWayStringBridge) instance
						);
					}
					else if ( org.hibernate.search.bridge.StringBridge.class.isAssignableFrom( bridgeType ) ) {
						bridge = new String2FieldBridgeAdaptor( (org.hibernate.search.bridge.StringBridge) instance );
					}
					else {
						throw LOG.noFieldBridgeInterfaceImplementedByClassBridge( bridgeType.getName() );
					}
				}
				catch (Exception e) {
					throw LOG.cannotInstantiateClassBridgeOfType( bridgeType.getName(), clazz.getName(), e );
				}
			}
		}
		if ( bridge == null ) {
			throw LOG.unableToDetermineClassBridge( ClassBridge.class.getName() );
		}

		populateReturnType( clazz, bridgeType, bridge );

		return bridge;
	}

	/**
	 * Injects any parameters configured via the given {@code ClassBridge} annotation into the given object, in case
	 * this is a {@link ParameterizedBridge}.
	 *
	 * @param classBridgeConfiguration the parameter source
	 * @param classBridge the object to inject the parameters into
	 */
	public void injectParameters(ClassBridge classBridgeConfiguration, Object classBridge) {
		if ( classBridgeConfiguration.params().length > 0 && ParameterizedBridge.class.isAssignableFrom( classBridge.getClass() ) ) {
			Map<String, String> params = new HashMap<>( classBridgeConfiguration.params().length );
			for ( Parameter param : classBridgeConfiguration.params() ) {
				params.put( param.name(), param.value() );
			}
			( (ParameterizedBridge) classBridge ).setParameterValues( params );
		}
	}

	/**
	 * This instantiates the SpatialFieldBridge from a {@code Spatial} annotation.
	 *
	 * @param spatial the {@code Spatial} annotation
	 * @param clazz the {@code XClass} on which the annotation is defined on
	 * @return Returns the {@code SpatialFieldBridge} instance
	 * @param latitudeField a {@link java.lang.String} object.
	 * @param longitudeField a {@link java.lang.String} object.
	 */
	public FieldBridge buildSpatialBridge(Spatial spatial, XClass clazz, String latitudeField, String longitudeField) {
		FieldBridge bridge;
		try {
			bridge = SpatialBridgeProvider.buildSpatialBridge( spatial, latitudeField, longitudeField );
		}
		catch (Exception e) {
			throw LOG.unableToInstantiateSpatial( clazz.getName(), e );
		}
		if ( bridge == null ) {
			throw LOG.unableToInstantiateSpatial( clazz.getName(), null );
		}

		return bridge;
	}

	public FieldBridge buildFieldBridge(XMember member,
			boolean isId,
			boolean isExplicitlyMarkedAsNumeric,
			ReflectionManager reflectionManager,
			ServiceManager serviceManager
	) {
		return buildFieldBridge( null, member, isId, isExplicitlyMarkedAsNumeric, reflectionManager, serviceManager );
	}

	public FieldBridge buildFieldBridge(Field field,
			XMember member,
			boolean isId,
			boolean isExplicitlyMarkedAsNumeric,
			ReflectionManager reflectionManager,
			ServiceManager serviceManager
	) {
		FieldBridge bridge = findExplicitFieldBridge( field, member, reflectionManager );
		if ( bridge != null ) {
			return bridge;
		}

		ExtendedBridgeProvider.ExtendedBridgeProviderContext context = new XMemberBridgeProviderContext(
				member, isId, isExplicitlyMarkedAsNumeric, reflectionManager, serviceManager
		);
		ContainerType containerType = getContainerType( member, reflectionManager );

		// We do annotation based providers as Tika at least needs priority over
		// default providers because it might override the type for String
		// TODO: introduce the notion of bridge contributor annotations to cope with this in the future
		for ( BridgeProvider provider : annotationBasedProviders ) {
			bridge = getFieldBridgeFromBridgeProvider(
					provider,
					context,
					containerType
			);
			if ( bridge != null ) {
				return bridge;
			}
		}

		// walk through all regular bridges and if multiple match
		// raise an exception containing the conflicting bridges
		StringBuilder multipleMatchError = null;
		BridgeProvider initialMatchingBridgeProvider = null;
		for ( BridgeProvider provider : regularProviders ) {
			FieldBridge createdBridge = getFieldBridgeFromBridgeProvider(
					provider,
					context,
					containerType
			);
			if ( createdBridge != null ) {
				// oops we found a duplicate
				if ( bridge != null ) {
					// first duplicate, add the initial bridge
					if ( multipleMatchError == null ) {
						multipleMatchError = new StringBuilder( "\n" )
								.append( "FieldBridge: " )
								.append( bridge )
								.append( " - BridgeProvider: " )
								.append( initialMatchingBridgeProvider.getClass() );
					}
					multipleMatchError
							.append( "\n" )
							.append( "FieldBridge: ")
							.append( createdBridge )
							.append( " - BridgeProvider: " )
							.append( provider.getClass() );
				}
				else {
					bridge = createdBridge;
					initialMatchingBridgeProvider = provider;
				}
			}
		}
		if ( multipleMatchError != null ) {
			throw LOG.multipleMatchingFieldBridges( member, member.getType(), multipleMatchError.toString() );
		}
		if ( bridge != null ) {
			return bridge;
		}

		throw LOG.unableToGuessFieldBridge( member.getType().getName(), member.getName() );
	}

	private ContainerType getContainerType(XMember member, ReflectionManager reflectionManager) {
		if ( ! member.isAnnotationPresent( IndexedEmbedded.class ) ) {
			return ContainerType.SINGLE;
		}
		if ( member.isArray() ) {
			return ContainerType.ARRAY;
		}
		Class<?> typeClass = reflectionManager.toClass( member.getType() );
		if ( Iterable.class.isAssignableFrom( typeClass ) ) {
			return ContainerType.ITERABLE;
		}
		if ( member.isCollection() && Map.class.equals( member.getCollectionClass() ) ) {
			return ContainerType.MAP;
		}
		// marked @IndexedEmbedded but not a container
		// => probably a @Field @IndexedEmbedded Foo foo;
		return ContainerType.SINGLE;
	}

	private FieldBridge getFieldBridgeFromBridgeProvider(
			BridgeProvider bridgeProvider,
			ExtendedBridgeProvider.ExtendedBridgeProviderContext context,
			ContainerType containerType
	) {
		FieldBridge bridge = bridgeProvider.provideFieldBridge( context );
		if ( bridge == null ) {
			return null;
		}
		populateReturnType( context.getReturnType(), bridge.getClass(), bridge );
		switch ( containerType ) {
			case SINGLE:
				return bridge;
			case ITERABLE:
				// Should we cache these per bridge instance?
				// would make sense at least for the known built-in bridges
				// but is that worth it?
				return new BuiltinIterableBridge( bridge );
			case ARRAY:
				return new BuiltinArrayBridge( bridge );
			case MAP:
				return new BuiltinMapBridge( bridge );
			default:
				throw new AssertionFailure( "Unknown ContainerType " + containerType );
		}
	}

	/**
	 * @return the field bridge explicitly specified via {@code @Field.bridge} or {@code @FieldBridge}. {@code null}
	 * is returned if none is present.
	 */
	private FieldBridge findExplicitFieldBridge(Field field, XMember member, ReflectionManager reflectionManager) {
		//TODO Should explicit FieldBridge also support the notion of container like numeric fields and provider based fields?
		//     the main problem is that support for a bridge accepting a Map would break
		FieldBridge bridge = null;

		org.hibernate.search.annotations.FieldBridge bridgeAnnotation;
		//@Field bridge has priority over @FieldBridge
		if ( field != null && void.class != field.bridge().impl() ) {
			bridgeAnnotation = field.bridge();
		}
		else {
			bridgeAnnotation = member.getAnnotation( org.hibernate.search.annotations.FieldBridge.class );
		}
		if ( bridgeAnnotation != null ) {
			bridge = createFieldBridgeFromAnnotation(
					bridgeAnnotation, member.getName(), reflectionManager.toClass( member.getType() )
			);
		}
		return bridge;
	}

	private FieldBridge createFieldBridgeFromAnnotation(
			org.hibernate.search.annotations.FieldBridge bridgeAnn,
			String appliedOnName,
			Class<?> appliedOnType) {
		if ( bridgeAnn == null ) {
			throw new AssertionFailure( "@FieldBridge instance cannot be null" );
		}

		FieldBridge bridge;
		Class<?> fieldBridgeClass = bridgeAnn.impl();
		if ( fieldBridgeClass == void.class ) {
			throw LOG.noImplementationClassInFieldBridge( appliedOnName );
		}
		try {
			Object instance = ReflectionHelper.createInstance( fieldBridgeClass, true );

			if ( FieldBridge.class.isAssignableFrom( fieldBridgeClass ) ) {
				bridge = (FieldBridge) instance;
			}
			else if ( TwoWayStringBridge.class.isAssignableFrom( fieldBridgeClass ) ) {
				bridge = new TwoWayString2FieldBridgeAdaptor(
						(TwoWayStringBridge) instance
				);
			}
			else if ( org.hibernate.search.bridge.StringBridge.class.isAssignableFrom( fieldBridgeClass ) ) {
				bridge = new String2FieldBridgeAdaptor( (org.hibernate.search.bridge.StringBridge) instance );
			}
			else {
				throw LOG.noFieldBridgeInterfaceImplementedByFieldBridge( fieldBridgeClass.getName(), appliedOnName );
			}
			if ( bridgeAnn.params().length > 0 && ParameterizedBridge.class.isAssignableFrom( fieldBridgeClass ) ) {
				Map<String, String> params = new HashMap<>( bridgeAnn.params().length );
				for ( Parameter param : bridgeAnn.params() ) {
					params.put( param.name(), param.value() );
				}
				( (ParameterizedBridge) instance ).setParameterValues( params );
			}
			populateReturnType( appliedOnType, fieldBridgeClass, instance );
		}
		catch (Exception e) {
			throw LOG.unableToInstantiateFieldBridge( appliedOnName, appliedOnType.getName(), e );
		}
		return bridge;
	}

	private void populateReturnType(Class<?> appliedOnType, Class<?> bridgeType, Object bridgeInstance) {
		if ( AppliedOnTypeAwareBridge.class.isAssignableFrom( bridgeType ) ) {
			( (AppliedOnTypeAwareBridge) bridgeInstance ).setAppliedOnType( appliedOnType );
		}
	}

	/**
	 * Takes in a fieldBridge and will return you a TwoWayFieldBridge instance.
	 *
	 * @param fieldBridge the field bridge annotation
	 * @param appliedOnType the type the bridge is applied on
	 * @param reflectionManager The reflection manager instance
	 * @return a TwoWayFieldBridge instance if the Field Bridge is an instance of a TwoWayFieldBridge.
	 * @throws org.hibernate.search.exception.SearchException if the FieldBridge passed in is not an instance of a TwoWayFieldBridge.
	 */
	public TwoWayFieldBridge extractTwoWayType(org.hibernate.search.annotations.FieldBridge fieldBridge,
													XClass appliedOnType,
													ReflectionManager reflectionManager) {
		FieldBridge fb = extractType( fieldBridge, appliedOnType, reflectionManager );
		if ( fb instanceof TwoWayFieldBridge ) {
			return (TwoWayFieldBridge) fb;
		}
		else {
			throw LOG.fieldBridgeNotAnInstanceof( TwoWayFieldBridge.class.getSimpleName() );
		}
	}

	/**
	 * This extracts and instantiates the implementation class from a ClassBridge
	 * annotation.
	 *
	 * @param fieldBridgeAnnotation the FieldBridge annotation
	 * @param appliedOnType the type the bridge is applied on
	 * @param reflectionManager The reflection manager instance
	 * @return FieldBridge
	 */
	private FieldBridge extractType(org.hibernate.search.annotations.FieldBridge fieldBridgeAnnotation,
										XClass appliedOnType,
										ReflectionManager reflectionManager) {
		FieldBridge bridge = null;

		if ( fieldBridgeAnnotation != null ) {
			bridge = createFieldBridgeFromAnnotation(
					fieldBridgeAnnotation,
					appliedOnType.getName(),
					reflectionManager.toClass( appliedOnType )
			);
		}

		if ( bridge == null ) {
			throw LOG.unableToDetermineClassBridge( appliedOnType.getName() );
		}

		return bridge;
	}

	private static enum ContainerType {
		SINGLE,
		ARRAY,
		ITERABLE,
		MAP,
	}
}
