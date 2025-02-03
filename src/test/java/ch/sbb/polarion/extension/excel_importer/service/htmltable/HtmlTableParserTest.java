package ch.sbb.polarion.extension.excel_importer.service.htmltable;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HtmlTableParserTest {

    private final HtmlTableParser parser = new HtmlTableParser();

    @Test
    void testExtractSimpleText() {
        Element element = Jsoup.parseBodyFragment("Simple text").body();
        assertEquals("Simple text", parser.extractTextWithLineBreaks(element).getText());
    }

    @Test
    void testExtractSimpleTagText() {
        Element element = Jsoup.parseBodyFragment("<span>Simple text</span>").body().child(0);
        assertEquals("Simple text", parser.extractTextWithLineBreaks(element).getText());
    }

    @Test
    void testExtractTextWithBr() {
        Element element = Jsoup.parseBodyFragment("<span>Line 1<br>Line 2<br>Line 3</span>").body().child(0);
        assertEquals("Line 1\nLine 2\nLine 3", parser.extractTextWithLineBreaks(element).getText());
    }

    @Test
    void testExtractTextWithBrVariants() {
        Element element = Jsoup.parseBodyFragment("<span>First line<br/>Second line<br />Third line</span>").body().child(0);
        assertEquals("First line\nSecond line\nThird line", parser.extractTextWithLineBreaks(element).getText());
    }

    @Test
    void testExtractNestedElements() {
        Element element = Jsoup.parseBodyFragment("<span>Outer <b>Bold</b> Text<br>Next Line</span>").body().child(0);
        assertEquals("Outer Bold Text\nNext Line", parser.extractTextWithLineBreaks(element).getText());
    }

    @Test
    void testExtractDeeplyNestedElements() {
        Element element = Jsoup.parseBodyFragment("<div><div>Parent <span>Child <b>Bold</b></span></div><br>New Line</div>").body().child(0);
        assertEquals("Parent Child Bold\nNew Line", parser.extractTextWithLineBreaks(element).getText());
    }

    @Test
    void testExtractEmptyElement() {
        Element element = Jsoup.parseBodyFragment("<div></div>").body().child(0);
        assertEquals("", parser.extractTextWithLineBreaks(element).getText());
    }

    @Test
    void testExtractOnlyBrElements() {
        Element element = Jsoup.parseBodyFragment("<span><br><br><br></span>").body().child(0);
        assertEquals("", parser.extractTextWithLineBreaks(element).getText());
    }

    @Test
    void testExtractBrElementsInTheMiddle() {
        Element element = Jsoup.parseBodyFragment("<span>Some<br><br><br>text</span>").body().child(0);
        assertEquals("Some\n\n\ntext", parser.extractTextWithLineBreaks(element).getText());
    }

    @Test
    void testExtractRenderedWorkItem() {
        Element element = Jsoup.parseBodyFragment("<span class='polarion-no-style-cleanup' style='white-space:nowrap;' title='EL-12345 - Mega WorkItem'><span style='color:#000000;'>EL-12345</span></span>").body().child(0);
        assertEquals("EL-12345", parser.extractTextWithLineBreaks(element).getText());
    }

    @Test
    void testExtractHyperlinks() {
        Element element = Jsoup.parseBodyFragment("<a href='https://polarion.url/polarion/#/project/elibrary/workitem?id=EL-12345'>EL-12345 - Mega WorkItem</a>").body().child(0);
        assertEquals("EL-12345 - Mega WorkItem", parser.extractTextWithLineBreaks(element).getText());
        assertEquals("https://polarion.url/polarion/#/project/elibrary/workitem?id=EL-12345", parser.extractTextWithLineBreaks(element).getLink());
    }

    @Test
    void testExtractEmbeddedHyperlinks() {
        Element element = Jsoup.parseBodyFragment("<span>Some text<br><a href='https://polarion.url/polarion/#/project/elibrary/workitem?id=EL-12345'>EL-12345 - Mega WorkItem</a></span>").body().child(0);
        assertEquals("Some text\nEL-12345 - Mega WorkItem", parser.extractTextWithLineBreaks(element).getText());
        assertEquals("https://polarion.url/polarion/#/project/elibrary/workitem?id=EL-12345", parser.extractTextWithLineBreaks(element).getLink());
    }

}
