package ch.sbb.polarion.extension.excel_importer.utils;

import com.polarion.alm.tracker.model.IExternallyLinkedWorkItemStruct;
import com.polarion.alm.tracker.model.ILinkedWorkItemStruct;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.core.util.StringUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

@Getter
@EqualsAndHashCode
public class LinkInfo {

    /**
     * Tests whether an external {@link LinkInfo} matches a given {@link IExternallyLinkedWorkItemStruct}
     * by comparing link role ID and work item URI. Returns {@code false} for non-external links.
     */
    public static final BiPredicate<LinkInfo, IExternallyLinkedWorkItemStruct> MATCHES_EXTERNAL_STRUCT = (li, s) ->
            li.isExternal() &&
                    Objects.equals(li.getRoleId(), s.getLinkRole().getId()) &&
                    Objects.equals(li.getWorkItemId(), s.getLinkedWorkItemURI().getURI().toString());

    /**
     * Tests whether a non-external {@link LinkInfo} matches a given {@link ILinkedWorkItemStruct}
     * by comparing link role ID, project ID and work item ID. Returns {@code false} for external links.
     */
    public static final BiPredicate<LinkInfo, ILinkedWorkItemStruct> MATCHES_DIRECT_STRUCT = (li, s) ->
            !li.isExternal() &&
                    Objects.equals(li.getRoleId(), s.getLinkRole().getId()) &&
                    Objects.equals(li.getProjectId(), s.getLinkedItem().getProjectId()) &&
                    Objects.equals(li.getWorkItemId(), s.getLinkedItem().getId());

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
     * Parses comma-separated list of links like {@code 'relates_to:elibrary/EL-123'} or {@code 'duplicates:https://url/to/workitem'}.
     * Both roleId and projectId in a link are optional - in this case default data will be fetched using pivot workitem.
     */
    public static List<LinkInfo> fromString(String value, @NotNull IWorkItem pivotWorkItem) {
        return Stream.of(StringUtils.getEmptyIfNull(value).split(",")).map(String::trim).filter(v -> !StringUtils.isEmpty(v)).map(link -> {
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
        return workItem.getExternallyLinkedWorkItemsStructs().stream().anyMatch(s -> MATCHES_EXTERNAL_STRUCT.test(this, s)) ||
                workItem.getLinkedWorkItemsStructsDirect().stream().anyMatch(s -> MATCHES_DIRECT_STRUCT.test(this, s));
    }
}
