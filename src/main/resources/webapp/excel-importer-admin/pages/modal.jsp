<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
    <%! String bundleTimestamp = ch.sbb.polarion.extension.generic.util.VersionUtils.getVersion().getBundleBuildTimestampDigitsOnly(); %>
<head>
    <link rel="stylesheet" href="../css/micromodal.css?bundle=<%= bundleTimestamp %>">
</head>
<body>
<div class="modal micromodal-slide" id="modal-popup" aria-hidden="true">
    <div class="modal__overlay" tabindex="-1"> <%--data-micromodal-close - add this attribute to allow cloing popup by clicking outside popup--%>
        <div class="modal__container" role="dialog" aria-modal="true" aria-labelledby="modal-popup-title">
            <header class="modal__header">
                <h2 class="modal__title" id="modal-popup-title">
                    ${param.titleText}
                </h2>
            </header>
            <main class="modal__content" id="modal-popup-content">
                This text must be dynamically replaced by the proper popup content.
            </main>
            <footer class="modal__footer">
                <button class="toolbar-button" data-micromodal-close onclick="${param.okClickFunction}">${param.okText}</button>
                <button class="toolbar-button" data-micromodal-close aria-label="Close this dialog window">${param.cancelText}</button>
            </footer>
        </div>
    </div>
</div>
<script type="text/javascript" src="../js/micromodal.min.js?bundle=<%= bundleTimestamp %>"></script>
<script>
    MicroModal.init();
</script>
</body>
</html>