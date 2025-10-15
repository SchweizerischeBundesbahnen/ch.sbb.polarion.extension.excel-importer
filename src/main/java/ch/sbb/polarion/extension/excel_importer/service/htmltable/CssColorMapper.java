package ch.sbb.polarion.extension.excel_importer.service.htmltable;

import lombok.experimental.UtilityClass;

import java.util.Map;

/**
 * Utility class for mapping CSS color names to hex RGB values.
 * Supports all 147 CSS Color Module Level 3 named colors.
 *
 * @see <a href="https://www.w3.org/TR/css-color-3/#svg-color">CSS Color Module Level 3 - SVG color keywords</a>
 * @see <a href="https://www.w3.org/TR/css-color-3/#html4">CSS Color Module Level 3 - HTML4 color keywords</a>
 */
@UtilityClass
public class CssColorMapper {

    /**
     * Marker value for transparent color in the color map.
     * This is not a valid hex color and is used to identify transparent colors.
     */
    private static final String TRANSPARENT_MARKER = "transparent";

    /**
     * Map of CSS color names to their hex RGB values (uppercase).
     * All color names are in lowercase for case-insensitive lookup.
     * Based on CSS Color Module Level 3 specification.
     */
    @SuppressWarnings("java:S1192") // "transparent" is used as both map key (CSS color name) and value (marker)
    private static final Map<String, String> COLOR_MAP = Map.ofEntries(
            // Basic colors (HTML 4.01)
            Map.entry("black", "#000000"),
            Map.entry("silver", "#C0C0C0"),
            Map.entry("gray", "#808080"),
            Map.entry("grey", "#808080"),  // alias
            Map.entry("white", "#FFFFFF"),
            Map.entry("maroon", "#800000"),
            Map.entry("red", "#FF0000"),
            Map.entry("purple", "#800080"),
            Map.entry("fuchsia", "#FF00FF"),
            Map.entry("magenta", "#FF00FF"),  // alias
            Map.entry("green", "#008000"),
            Map.entry("lime", "#00FF00"),
            Map.entry("olive", "#808000"),
            Map.entry("yellow", "#FFFF00"),
            Map.entry("navy", "#000080"),
            Map.entry("blue", "#0000FF"),
            Map.entry("teal", "#008080"),
            Map.entry("aqua", "#00FFFF"),
            Map.entry("cyan", "#00FFFF"),  // alias

            // Extended colors (SVG/X11)
            Map.entry("aliceblue", "#F0F8FF"),
            Map.entry("antiquewhite", "#FAEBD7"),
            Map.entry("aquamarine", "#7FFFD4"),
            Map.entry("azure", "#F0FFFF"),
            Map.entry("beige", "#F5F5DC"),
            Map.entry("bisque", "#FFE4C4"),
            Map.entry("blanchedalmond", "#FFEBCD"),
            Map.entry("blueviolet", "#8A2BE2"),
            Map.entry("brown", "#A52A2A"),
            Map.entry("burlywood", "#DEB887"),
            Map.entry("cadetblue", "#5F9EA0"),
            Map.entry("chartreuse", "#7FFF00"),
            Map.entry("chocolate", "#D2691E"),
            Map.entry("coral", "#FF7F50"),
            Map.entry("cornflowerblue", "#6495ED"),
            Map.entry("cornsilk", "#FFF8DC"),
            Map.entry("crimson", "#DC143C"),
            Map.entry("darkblue", "#00008B"),
            Map.entry("darkcyan", "#008B8B"),
            Map.entry("darkgoldenrod", "#B8860B"),
            Map.entry("darkgray", "#A9A9A9"),
            Map.entry("darkgrey", "#A9A9A9"),  // alias
            Map.entry("darkgreen", "#006400"),
            Map.entry("darkkhaki", "#BDB76B"),
            Map.entry("darkmagenta", "#8B008B"),
            Map.entry("darkolivegreen", "#556B2F"),
            Map.entry("darkorange", "#FF8C00"),
            Map.entry("darkorchid", "#9932CC"),
            Map.entry("darkred", "#8B0000"),
            Map.entry("darksalmon", "#E9967A"),
            Map.entry("darkseagreen", "#8FBC8F"),
            Map.entry("darkslateblue", "#483D8B"),
            Map.entry("darkslategray", "#2F4F4F"),
            Map.entry("darkslategrey", "#2F4F4F"),  // alias
            Map.entry("darkturquoise", "#00CED1"),
            Map.entry("darkviolet", "#9400D3"),
            Map.entry("deeppink", "#FF1493"),
            Map.entry("deepskyblue", "#00BFFF"),
            Map.entry("dimgray", "#696969"),
            Map.entry("dimgrey", "#696969"),  // alias
            Map.entry("dodgerblue", "#1E90FF"),
            Map.entry("firebrick", "#B22222"),
            Map.entry("floralwhite", "#FFFAF0"),
            Map.entry("forestgreen", "#228B22"),
            Map.entry("gainsboro", "#DCDCDC"),
            Map.entry("ghostwhite", "#F8F8FF"),
            Map.entry("gold", "#FFD700"),
            Map.entry("goldenrod", "#DAA520"),
            Map.entry("greenyellow", "#ADFF2F"),
            Map.entry("honeydew", "#F0FFF0"),
            Map.entry("hotpink", "#FF69B4"),
            Map.entry("indianred", "#CD5C5C"),
            Map.entry("indigo", "#4B0082"),
            Map.entry("ivory", "#FFFFF0"),
            Map.entry("khaki", "#F0E68C"),
            Map.entry("lavender", "#E6E6FA"),
            Map.entry("lavenderblush", "#FFF0F5"),
            Map.entry("lawngreen", "#7CFC00"),
            Map.entry("lemonchiffon", "#FFFACD"),
            Map.entry("lightblue", "#ADD8E6"),
            Map.entry("lightcoral", "#F08080"),
            Map.entry("lightcyan", "#E0FFFF"),
            Map.entry("lightgoldenrodyellow", "#FAFAD2"),
            Map.entry("lightgray", "#D3D3D3"),
            Map.entry("lightgrey", "#D3D3D3"),  // alias
            Map.entry("lightgreen", "#90EE90"),
            Map.entry("lightpink", "#FFB6C1"),
            Map.entry("lightsalmon", "#FFA07A"),
            Map.entry("lightseagreen", "#20B2AA"),
            Map.entry("lightskyblue", "#87CEFA"),
            Map.entry("lightslategray", "#778899"),
            Map.entry("lightslategrey", "#778899"),  // alias
            Map.entry("lightsteelblue", "#B0C4DE"),
            Map.entry("lightyellow", "#FFFFE0"),
            Map.entry("limegreen", "#32CD32"),
            Map.entry("linen", "#FAF0E6"),
            Map.entry("mediumaquamarine", "#66CDAA"),
            Map.entry("mediumblue", "#0000CD"),
            Map.entry("mediumorchid", "#BA55D3"),
            Map.entry("mediumpurple", "#9370DB"),
            Map.entry("mediumseagreen", "#3CB371"),
            Map.entry("mediumslateblue", "#7B68EE"),
            Map.entry("mediumspringgreen", "#00FA9A"),
            Map.entry("mediumturquoise", "#48D1CC"),
            Map.entry("mediumvioletred", "#C71585"),
            Map.entry("midnightblue", "#191970"),
            Map.entry("mintcream", "#F5FFFA"),
            Map.entry("mistyrose", "#FFE4E1"),
            Map.entry("moccasin", "#FFE4B5"),
            Map.entry("navajowhite", "#FFDEAD"),
            Map.entry("oldlace", "#FDF5E6"),
            Map.entry("olivedrab", "#6B8E23"),
            Map.entry("orange", "#FFA500"),
            Map.entry("orangered", "#FF4500"),
            Map.entry("orchid", "#DA70D6"),
            Map.entry("palegoldenrod", "#EEE8AA"),
            Map.entry("palegreen", "#98FB98"),
            Map.entry("paleturquoise", "#AFEEEE"),
            Map.entry("palevioletred", "#DB7093"),
            Map.entry("papayawhip", "#FFEFD5"),
            Map.entry("peachpuff", "#FFDAB9"),
            Map.entry("peru", "#CD853F"),
            Map.entry("pink", "#FFC0CB"),
            Map.entry("plum", "#DDA0DD"),
            Map.entry("powderblue", "#B0E0E6"),
            Map.entry("rosybrown", "#BC8F8F"),
            Map.entry("royalblue", "#4169E1"),
            Map.entry("saddlebrown", "#8B4513"),
            Map.entry("salmon", "#FA8072"),
            Map.entry("sandybrown", "#F4A460"),
            Map.entry("seagreen", "#2E8B57"),
            Map.entry("seashell", "#FFF5EE"),
            Map.entry("sienna", "#A0522D"),
            Map.entry("skyblue", "#87CEEB"),
            Map.entry("slateblue", "#6A5ACD"),
            Map.entry("slategray", "#708090"),
            Map.entry("slategrey", "#708090"),  // alias
            Map.entry("snow", "#FFFAFA"),
            Map.entry("springgreen", "#00FF7F"),
            Map.entry("steelblue", "#4682B4"),
            Map.entry("tan", "#D2B48C"),
            Map.entry("thistle", "#D8BFD8"),
            Map.entry("tomato", "#FF6347"),
            Map.entry("turquoise", "#40E0D0"),
            Map.entry("violet", "#EE82EE"),
            Map.entry("wheat", "#F5DEB3"),
            Map.entry("whitesmoke", "#F5F5F5"),
            Map.entry("yellowgreen", "#9ACD32"),

            // Transparent (CSS3)
            Map.entry("transparent", TRANSPARENT_MARKER)
    );

    /**
     * Checks if a string is a valid 6-digit hex color format (#RRGGBB).
     *
     * @param color the color string to check
     * @return true if the string matches #RRGGBB format, false otherwise
     */
    public boolean isHex(String color) {
        return color != null && color.matches("^#[0-9A-Fa-f]{6}$");
    }

    /**
     * Converts a CSS color value (name or hex) to uppercase hex format.
     * <p>
     * Supported formats:
     * <ul>
     *   <li>CSS named colors (e.g., "red", "blue", "darkslategray") - case insensitive</li>
     *   <li>Hex colors in 6-digit format (e.g., "#FF0000", "#00ff00") - returned in uppercase</li>
     * </ul>
     * <p>
     * Note: Short hex format (#RGB) is not supported and will return null.
     *
     * @param color the color value (named color or hex)
     * @return the color in uppercase hex format (#RRGGBB), or null if the color is not recognized
     */
    public String toHex(String color) {
        if (color == null || color.isBlank()) {
            return null;
        }

        String trimmed = color.trim();

        // If it starts with #, validate it's in 6-digit format
        if (trimmed.startsWith("#")) {
            // Only accept 6-digit hex format (#RRGGBB)
            if (isHex(trimmed)) {
                return trimmed.toUpperCase();
            }
            return null;
        }

        // Otherwise, try to look up as a named color (case-insensitive)
        String hex = COLOR_MAP.get(trimmed.toLowerCase());

        // Return the hex value if found (but not transparent as it's not a valid hex)
        return TRANSPARENT_MARKER.equals(hex) ? null : hex;
    }

    /**
     * Checks if a given color value is a valid CSS color (either a named color or hex format).
     *
     * @param color the color value to check
     * @return true if the color is valid, false otherwise
     */
    public boolean isValidColor(String color) {
        if (color == null || color.isBlank()) {
            return false;
        }

        String trimmed = color.trim();

        // Check if it's a hex color
        if (isHex(trimmed)) {
            return true;
        }

        // Check if it's a named color
        return COLOR_MAP.containsKey(trimmed.toLowerCase());
    }
}
