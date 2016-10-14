# new implementation of activityInstanceService

## overview 

Under high load, getting pending tasks could return tasks that are being assign in a parallel transaction in a 
separate thread, and thus assignee is override in an already assign task

To avoid this concurrency issue, this new service implementation use a more restrictive rule than default 
`ActivityInstanceService` implementation to assign human task:
     
     * exception is throw when a task is already assign to a different user
     * only update claimed date when assign to same user
     * remove claimed date when assign to user with id 0 (un-assign)

In order to avoid exception when use case is to re assign task, an additional call to `assignHumanTask` with `userId` 
set to `0` is required     

## how to deploy

### build project

`mvn clean install`

copy jar into bundle classpath folder

### configure bundle

* stop bundle

* replace default service implementation

edit file `engine-server/work/platform/bonita-platform-community.xml` in `BONITA_HOME` folder and modify bean with id `activityInstanceService`:

change `class` attribute to `org.bonitasoft.engine.service.extension.ActivityInstanceServiceExtImpl`

bean should result in : 
```xml
 <bean id="activityInstanceService" class="org.bonitasoft.engine.service.extension.ActivityInstanceServiceExtImpl">
        <constructor-arg name="recorder" ref="tenantRecorderSync" />
        <constructor-arg name="persistenceService" ref="persistenceService" />
        <constructor-arg name="archiveService" ref="archiveService" />
        <constructor-arg name="dataInstanceService" ref="dataInstanceService" />
        <constructor-arg name="connectorInstanceService" ref="connectorInstanceService" />
        <constructor-arg name="eventService" ref="tenantEventService" />
        <constructor-arg name="logger" ref="tenantTechnicalLoggerService" />
    </bean>
```
 
* register new query
 
edit file `engine-server/work/tenant/TENANT_ID/bonita-tenant-community.xml` in `BONITA_HOME` and edit bean with id `communityHbmResourcesProvider` 

add   

```xml
<value>org/bonitasoft/engine/service/extension/activityInstanceServiceExt.queries.hbm.xml</value>
```
 
 at the end of `<property><set>` xml node
 
* restart bundle