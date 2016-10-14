package com.bonitasoft.delivery.tests;

import java.util.HashMap;
import java.util.Map;

import org.bonitasoft.engine.archive.ArchiveService;
import org.bonitasoft.engine.builder.BuilderFactory;
import org.bonitasoft.engine.core.connector.ConnectorInstanceService;
import org.bonitasoft.engine.core.process.instance.api.exceptions.SActivityModificationException;
import org.bonitasoft.engine.core.process.instance.api.exceptions.SActivityReadException;
import org.bonitasoft.engine.core.process.instance.api.exceptions.SFlowNodeNotFoundException;
import org.bonitasoft.engine.core.process.instance.api.exceptions.SFlowNodeReadException;
import org.bonitasoft.engine.core.process.instance.impl.ActivityInstanceServiceImpl;
import org.bonitasoft.engine.core.process.instance.model.SFlowNodeInstance;
import org.bonitasoft.engine.core.process.instance.model.SHumanTaskInstance;
import org.bonitasoft.engine.data.instance.api.DataInstanceService;
import org.bonitasoft.engine.events.EventActionType;
import org.bonitasoft.engine.events.EventService;
import org.bonitasoft.engine.events.model.SFireEventException;
import org.bonitasoft.engine.events.model.SUpdateEvent;
import org.bonitasoft.engine.events.model.builders.SEventBuilderFactory;
import org.bonitasoft.engine.log.technical.TechnicalLoggerService;
import org.bonitasoft.engine.recorder.Recorder;
import org.bonitasoft.engine.services.PersistenceService;
import org.bonitasoft.engine.services.SPersistenceException;

/**
 * @author Laurent Leseigneur
 */
public class ActivityInstanceServiceExtImpl extends ActivityInstanceServiceImpl {

    private static final String QUERY_UPDATE_ASSIGNEE_ID = "updateAssigneeId";
    private static final String HUMAN_TASK_INSTANCE_ASSIGNEE = "updateAssigneeId";

    public ActivityInstanceServiceExtImpl(Recorder recorder, PersistenceService persistenceService, ArchiveService archiveService,
                                          DataInstanceService dataInstanceService, ConnectorInstanceService connectorInstanceService, EventService eventService,
                                          TechnicalLoggerService logger) {
        super(recorder, persistenceService, archiveService, dataInstanceService, connectorInstanceService, eventService, logger);
    }

    /**
     * <p>Use a more restrictive mechanism than default one in {@link org.bonitasoft.engine.core.process.instance.impl.ActivityInstanceServiceImpl} to assign human task:
     * <ul>
     * <li>exception when task is already assign to a different user</li>
     * <li>only update claimed date when assign to same user</li>
     * <li>remove claimed date when assign to user with id 0 (un-assign)</li>
     * </ul>
     * </p>
     * <p>
     * <p>under hight load, getting pending tasks could return tasks that are being assign in a previous transaction in a separate thread, and thus assignee is override </p>
     *
     * @param userTaskId
     * @param userId
     * @throws SFlowNodeNotFoundException
     * @throws SFlowNodeReadException
     * @throws SActivityModificationException
     */
    public void assignHumanTask(final long userTaskId, final long userId) throws SFlowNodeNotFoundException, SFlowNodeReadException,
            SActivityModificationException {
        final SFlowNodeInstance flowNodeInstance = getFlowNodeInstance(userTaskId);
        if (flowNodeInstance instanceof SHumanTaskInstance) {
            long claimedDate = 0;
            if (userId > 0) {
                claimedDate = System.currentTimeMillis();
            }
            Map<String, Object> params = new HashMap<>();
            params.put("flowNodeInstanceId", flowNodeInstance.getId());
            params.put("assigneeId", userId);
            params.put("claimedDate", claimedDate);

            try {
                int updatedRows = getPersistenceService().update(QUERY_UPDATE_ASSIGNEE_ID, params);
                if (updatedRows != 1) {
                    throw new SActivityModificationException("the activity with id " + userTaskId + " is already assigned. Call assignHumanTask(" + userTaskId
                            + ",0) to remove assignee before ", null);
                }
                if (getEventService().hasHandlers(HUMAN_TASK_INSTANCE_ASSIGNEE, EventActionType.UPDATED)) {
                    SUpdateEvent updateEvent = (SUpdateEvent) BuilderFactory.get(SEventBuilderFactory.class).createUpdateEvent(HUMAN_TASK_INSTANCE_ASSIGNEE)
                            .setObject(flowNodeInstance).done();
                    getEventService().fireEvent(updateEvent);
                }
            } catch (SPersistenceException | SFireEventException e) {
                throw new SActivityModificationException(e);
            }
        } else {
            throw new SActivityReadException("the activity with id " + userTaskId + " is not a user task");
        }
    }
}
