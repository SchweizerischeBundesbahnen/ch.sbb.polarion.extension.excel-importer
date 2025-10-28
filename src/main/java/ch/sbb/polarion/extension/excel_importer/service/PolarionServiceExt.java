package ch.sbb.polarion.extension.excel_importer.service;

import ch.sbb.polarion.extension.generic.fields.model.FieldMetadata;
import ch.sbb.polarion.extension.generic.fields.model.Option;
import com.polarion.alm.projects.IProjectService;
import com.polarion.alm.tracker.ITestManagementService;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.alm.tracker.model.IAttachmentBase;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.alm.tracker.model.ITypeOpt;
import com.polarion.alm.tracker.model.IWithAttachments;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.platform.IPlatformService;
import com.polarion.platform.core.PlatformContext;
import com.polarion.platform.persistence.IEnumeration;
import com.polarion.platform.persistence.model.IPObjectList;
import com.polarion.platform.security.ISecurityService;
import com.polarion.platform.service.repository.IRepositoryService;
import com.polarion.subterra.base.data.identification.IContextId;
import com.polarion.subterra.base.data.model.internal.PrimitiveType;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class PolarionServiceExt extends ch.sbb.polarion.extension.generic.service.PolarionService {

    private static final Set<Option> BOOLEAN_OPTIONS_MAPPING = Set.of(new Option("True", "true"), new Option("False", "false"));

    public PolarionServiceExt() {
    }

    public PolarionServiceExt(@NotNull ITrackerService trackerService,
                              @NotNull IProjectService projectService,
                              @NotNull ISecurityService securityService,
                              @NotNull IPlatformService platformService,
                              @NotNull IRepositoryService repositoryService) {
        super(trackerService, projectService, securityService, platformService, repositoryService);
    }

    public Set<FieldMetadata> getWorkItemsFields(@NotNull String projectId, @NotNull String workItemType) {
        final ITrackerProject trackerProject = findProject(projectId);
        final ITypeOpt typeOpt = findWorkItemTypeInProject(trackerProject, workItemType);

        IContextId contextId = trackerProject.getContextId();
        Set<FieldMetadata> fields = new TreeSet<>();
        fields.addAll(getGeneralFields(IWorkItem.PROTO, contextId)); // get common fields for WorkItem
        fields.addAll(getCustomFields(IWorkItem.PROTO, contextId, null)); // get custom fields for WorkItem with any type in the project (-- All Types --)
        fields.addAll(getCustomFields(IWorkItem.PROTO, contextId, typeOpt.getId())); // get custom fields for WorkItem with specific type in the project
        fillBooleanOptionMappings(fields); // set mappings for booleans
        return fields;
    }

    public List<ITypeOpt> getWorkItemTypes(@NotNull String projectId) {
        final ITrackerProject trackerProject = findProject(projectId);

        IEnumeration<ITypeOpt> workItemTypeEnum = trackerProject.getWorkItemTypeEnum();
        return workItemTypeEnum.getAllOptions();
    }

    public ITypeOpt findWorkItemTypeInProject(@NotNull ITrackerProject trackerProject, @NotNull String workItemTypeId) {
        ITypeOpt typeOpt = trackerProject.getWorkItemTypeEnum().wrapOption(workItemTypeId);
        if (typeOpt.isPhantom()) {
            throw new IllegalArgumentException(String.format("Cannot find WorkItem type '%s' in scope of the project '%s'", workItemTypeId, trackerProject.getId()));
        }
        return typeOpt;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<IWorkItem> findWorkItemsById(String projectId, String identifierCustomFieldId, Collection<String> ids) {
        String query = "project.id:" + projectId + " AND (" + ids.stream()
                .map(i -> identifierCustomFieldId + ":" + i)
                .collect(Collectors.joining(" OR ")) + ")";

        IPObjectList workItemsList = trackerService.queryWorkItems(query, "id");
        return workItemsList.stream()
                .filter(IWorkItem.class::isInstance)
                .toList();
    }

    public IWorkItem createWorkItem(ITrackerProject project, ITypeOpt workItemCreationType) {
        IWorkItem workItem = trackerService.createWorkItem(project);
        workItem.setType(workItemCreationType);
        return workItem;
    }

    public ITrackerProject findProject(@NotNull String projectId) {
        ITrackerProject trackerProject = trackerService.getTrackerProject(projectId);
        if (trackerProject.isUnresolvable()) {
            throw new IllegalArgumentException(String.format("Project '%s' does not exist", projectId));
        }
        return trackerProject;
    }

    public IWithAttachments<? extends IAttachmentBase> getObjectAttachments(String projectId, String objectType, String objectId) {
        switch (objectType.toUpperCase()) {
            case "MODULE" -> {
                String delimiter = "/";
                String spaceId = objectId.split(delimiter)[0];
                String documentName = objectId.split(delimiter)[1];
                return getModule(projectId, spaceId, documentName);
            }
            case "RICHPAGE" -> {
                return getTrackerService().getRichPageManager().getRichPage().path(objectId);
            }
            case "TESTRUN" -> {
                ITestManagementService testManagementService = PlatformContext.getPlatform().lookupService(ITestManagementService.class);
                return testManagementService.getTestRun(projectId, objectId);
            }
            case "WORKITEM" -> {
                return getWorkItem(projectId, objectId);
            }
            default -> throw new IllegalArgumentException(String.format("Object type %s not found", objectType));
        }
    }

    private void fillBooleanOptionMappings(Set<FieldMetadata> fields) {
        fields.forEach(f -> {
            if (f.getType() instanceof PrimitiveType primitiveType && Boolean.class.getTypeName().equals(primitiveType.getTypeName())) {
                f.setOptions(BOOLEAN_OPTIONS_MAPPING);
            }
        });
    }
}
