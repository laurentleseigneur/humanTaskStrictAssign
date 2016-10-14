#

## how to deploy

### build project

`mvn clean install`

copy jar into bundle classpath folder

### configure bundle

in `bonita-platform-community.xml` file under bonita home, edit bean with id `communityHbmResourcesProvider`:

add   `<value>com/bonitasoft/delivery/tests/activityInstanceServiceExt.queries.hbm.xml</value>` at the end of `<property><set>` xml node
 
restart bundle