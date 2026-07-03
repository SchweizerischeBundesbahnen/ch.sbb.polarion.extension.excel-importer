<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
    <%! String bundleTimestamp = ch.sbb.polarion.extension.generic.util.VersionUtils.getVersion().getBundleBuildTimestampDigitsOnly(); %>
<head>
    <link rel="stylesheet" href="../ui/generic/css/micromodal.css?bundle=<%= bundleTimestamp %>">
    <style>
        /* Full-width dark header like the pdf/docx/strictdoc dialogs: drop the container's
           default 30px padding (which leaves a white frame around the header) and move the
           padding onto the content and footer instead. */
        #modal-popup .modal__container {
            padding: 0;
            min-width: 520px;
            max-width: 90vw;
            overflow: hidden;
        }

        #modal-popup .modal__close {
            margin-left: auto;
            font-size: 20px;
            line-height: 1;
            cursor: pointer;
        }

        #modal-popup .modal__content {
            margin: 0;
            padding: 20px 24px;
            max-height: 78vh;
            overflow: auto;
        }

        #modal-popup .modal__footer {
            justify-content: flex-end;
            gap: 8px;
            padding: 12px 20px 16px;
        }

        /* Teal outline (secondary) / filled (primary) buttons, matching the exporter dialogs. */
        #modal-popup .modal__btn {
            background-color: #fff;
            color: #007993;
            border: 2px solid #007993;
            border-radius: 4px;
            font-weight: 700;
            font-size: 14px;
            padding: 6px 18px;
        }

        #modal-popup .modal__btn:hover,
        #modal-popup .modal__btn:focus {
            background-color: #f0f8f9;
            transform: none;
        }

        #modal-popup .modal__btn-primary {
            background-color: #007993;
            color: #fff;
        }

        #modal-popup .modal__btn-primary:hover,
        #modal-popup .modal__btn-primary:focus {
            background-color: #0a6a80;
        }
    </style>
</head>
<body>
<div class="modal micromodal-slide" id="modal-popup" aria-hidden="true">
    <div class="modal__overlay" tabindex="-1"> <%--data-micromodal-close - add this attribute to allow cloing popup by clicking outside popup--%>
        <div class="modal__container" role="dialog" aria-modal="true" aria-labelledby="modal-popup-title">
            <header class="modal__header">
                <h2 class="modal__title" id="modal-popup-title">
                    ${param.titleText}
                </h2>
                <button type="button" class="modal__close" aria-label="Close this dialog window" data-micromodal-close></button>
            </header>
            <main class="modal__content" id="modal-popup-content">
                This text must be dynamically replaced by the proper popup content.
            </main>
            <footer class="modal__footer">
                <button class="modal__btn" data-micromodal-close aria-label="Close this dialog window">${param.cancelText}</button>
                <button id="toolbar-button-ok" class="modal__btn modal__btn-primary" data-micromodal-close>${param.okText}</button>
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
