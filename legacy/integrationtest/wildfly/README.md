## Explanation of testing profiles
### Obtaining WildFly distribution
- The default is that the TS obtains WildFly from Maven and prepares it automatically.
- To test against WildFly distributions which you prepared yourself, pass `-DbuiltinVersion=<bundled_hsearch_version>`,
  as well as `-DjbossHome.node1` and `-DjbossHome.node2` containing the paths to the distributions.
  - Some tests for integrations not included in official WildFly distribution are skipped