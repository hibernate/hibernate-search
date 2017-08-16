## Explanation of testing profiles
### Obtaining WildFly distribution
- The default is that the TS obtains WildFly from Maven and prepares it automatically.
- To test against WildFly distributions which you prepared yourself, just pass the `-DjbossHome.node1` and `-DjbossHome.node2` properties containing paths to the distributions

### Testing included modules vs. preparing modules 
- The default mode is that the TS unzips Hibernate Search and required libraries as WildFly modules into the distributions
- If you want to test against the modules included in the official WildFly distributions, pass the property `-DuseBuiltinModules`. In this case, you should also specify the expected Hibernate Search version (the one included in the WildFly distribution), using the `-DbuiltinVersion` property. If not specified, the test suite will expect that the included version is the same as the one you are running the test suite from. 