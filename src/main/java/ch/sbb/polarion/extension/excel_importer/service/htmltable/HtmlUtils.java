package ch.sbb.polarion.extension.excel_importer.service.htmltable;

import lombok.experimental.UtilityClass;

@UtilityClass
public class HtmlUtils {

    public static final String CSS = System.lineSeparator() +
            """
            <style>
              .header-td {
                font-weight: bold;
                background-color: #F0F0F0;
                text-align: left;
                vertical-align: top;
                height: 12px;
                border: 1px solid #CCCCCC;
                padding: 5px;
              }
              .row-td {
                text-align: left;
                vertical-align: top;
                height: 12px;
                border: 1px solid #CCCCCC;
                padding: 5px;
              }
            </style>
            """;

}
