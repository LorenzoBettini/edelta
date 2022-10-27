This is not meant to be a real p2 repository to publish:
it is a way of caching all the p2 ius of the target platform in GitHub Actions.
It's not even part of the main reactor.

For example, in GitHub Actions or locally:

```
./mvnw
        -f edelta.parent/edelta.targetplatform.repository/pom.xml
        clean package
```

After running this, all the features and bundles of the target platform will be cached.