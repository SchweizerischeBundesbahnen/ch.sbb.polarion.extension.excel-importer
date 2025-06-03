package ch.sbb.polarion.extension.excel_importer.utils;

import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.core.util.StringUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Getter
@EqualsAndHashCode
public class LinkInfo {

    private String roleId;
    private String projectId;
    private String workItemId;
    private boolean external;

    private LinkInfo() {
    }

    public LinkInfo(String roleId, String projectId, String workItemId, boolean external) {
        this.roleId = roleId;
        this.projectId = projectId;
        this.workItemId = workItemId;
        this.external = external;
    }

    /**
     * Parses link like {@code 'relates_to:elibrary/EL-123'} or {@code 'duplicates:https://url/to/workitem'}.
     * Both roleId and projectId are optional - in this case default data will be fetched using pivot workitem.
     */
    public static List<LinkInfo> fromString(String value, @NotNull IWorkItem pivotWorkItem) {
        return Stream.of(StringUtils.getEmptyIfNull(value).split("\\s*,\\s*")).filter(v -> !StringUtils.isEmptyTrimmed(v)).map(link -> {
            LinkInfo linkInfo = new LinkInfo();
            int roleDelimiter = link.indexOf(":");
            if (roleDelimiter != -1 && roleDelimiter != link.indexOf("://")) {
                linkInfo.roleId = link.substring(0, roleDelimiter);
                link = link.substring(roleDelimiter + 1);
            } else {
                linkInfo.roleId = pivotWorkItem.getProject().getWorkItemLinkRoleEnum().getDefaultOption(pivotWorkItem.getType()).getId();
            }

            String lowerCasedLink = link.toLowerCase();
            if (lowerCasedLink.startsWith("http://") || lowerCasedLink.startsWith("https://")) {
                linkInfo.external = true;
            } else {
                if (link.contains("/")) {
                    linkInfo.projectId = link.substring(0, link.indexOf("/"));
                    link = link.substring(link.indexOf("/") + 1);
                } else {
                    linkInfo.projectId = pivotWorkItem.getProjectId();
                }
            }
            linkInfo.workItemId = link;
            return linkInfo;
        }).toList();
    }

    public boolean containedIn(IWorkItem workItem) {
        return external && workItem.getExternallyLinkedWorkItemsStructs().stream().anyMatch(s ->
                Objects.equals(s.getLinkRole().getId(), roleId) &&
                        Objects.equals(s.getLinkedWorkItemURI().getURI().toString(), workItemId)) ||
                workItem.getLinkedWorkItemsStructsDirect().stream().anyMatch(s ->
                        Objects.equals(s.getLinkRole().getId(), roleId) &&
                                Objects.equals(s.getLinkedItem().getProjectId(), projectId) &&
                                Objects.equals(s.getLinkedItem().getId(), workItemId)
                );
    }
}
