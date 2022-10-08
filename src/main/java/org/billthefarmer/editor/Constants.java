package org.billthefarmer.editor;

import java.util.regex.Pattern;

public class Constants {
    public final static String PATH = "path";
    public final static String EDIT = "edit";
    public final static String MATCH = "match";
    public final static String CHANGED = "changed";
    public final static String CONTENT = "content";
    public final static String MODIFIED = "modified";
    public final static String PREF_FILE = "pref_file";
    public final static String PREF_HIGHLIGHT = "pref_highlight";
    public final static String PREF_PATHS = "pref_paths";
    public final static String PREF_SAVE = "pref_save";
    public final static String PREF_VIEW = "pref_view";
    public final static String PREF_SIZE = "pref_size";
    public final static String PREF_SUGGEST = "pref_suggest";
    public final static String PREF_THEME = "pref_theme";
    public final static String PREF_TYPE = "pref_type";
    public final static String PREF_WRAP = "pref_wrap";
    public final static String DOCUMENTS = "Documents";
    public final static String FOLDER = "Folder:  ";
    public final static String UTF_8 = "UTF-8";
    public final static String NEW_FILE = "Untitled.txt";
    public final static String EDIT_FILE = "Editor.txt";
    public final static String HTML_FILE = "Editor.html";
    public final static String TEXT_HTML = "text/html";
    public final static String TEXT_PLAIN = "text/plain";
    public final static String TEXT_WILD = "text/*";
    public final static Pattern PATTERN_CHARS =
        Pattern.compile("[()\\[\\]{}<>\"'`]");
    public final static String BRACKET_CHARS = "([{<";
    public final static String HTML_HEAD =
        "<!DOCTYPE html>\n<html>\n<head>\n<meta charset=\"utf-8\">\n" +
        "<meta name=\"viewport\" content=\"width=device-width, " +
        "initial-scale=1.0\">\n</head>\n<body>\n";
    public final static String HTML_TAIL = "\n</body>\n</html>\n";
    public final static String FILE_PROVIDER =
        "org.billthefarmer.editor.fileprovider";
    public final static String OPEN_NEW =
        "org.billthefarmer.editor.OPEN_NEW";
    public final static String CC_EXT =
        "\\.(c(c|pp|xx|\\+\\+)?|go|h|java|js|kt|m|py|swift)";
    public final static String HTML_EXT =
        "\\.html?";
    public final static String CSS_EXT =
        "\\.css?";
    public final static String ORG_EXT =
        "\\.org";
    public final static String MD_EXT =
        "\\.md";
    public final static String SH_EXT =
        "\\.sh";
    // Syntax patterns
    public final static Pattern KEYWORDS = Pattern.compile
        ("\\b(abstract|and|arguments|as(m|sert|sociativity)?|auto|break|" +
         "case|catch|chan|char|class|con(st|tinue|venience)|continue|" +
         "de(bugger|f|fault|fer|in|init|l|lete)|didset|do(ne)?|dynamic" +
         "(type)?|el(if|se)|enum|esac|eval|ex(cept|ec|plicit|port|" +
         "tends|tension|tern)|fal(lthrough|se)|fi(nal|nally)?|for|" +
         "friend|from|fun(c(tion)?)?|get|global|go(to)?|if|" +
         "im(plements|port)|in(fix|it|line|out|stanceof|terface|" +
         "ternal)?|is|lambda|lazy|left|let|local|map|mut(able|ating)|" +
         "namespace|native|new|nil|none|nonmutating|not|null|" +
         "operator|optional|or|override|package|pass|postfix|" +
         "pre(cedence|fix)|print|private|prot(ected|ocol)|public|" +
         "raise|range|register|required|return|right|select|self|" +
         "set|signed|sizeof|static|strictfp|struct|subscript|super|" +
         "switch|synchronized|template|th(en|is|rows?)|transient|" +
         "true|try|type(alias|def|id|name|of)?|un(ion|owned|signed)|" +
         "using|va([lr])|virtual|void|volatile|weak|wh(en|ere|ile)|willset|" +
         "with|yield)\\b", Pattern.MULTILINE);
    public final static Pattern TYPES = Pattern.compile
        ("\\b(j?bool(ean)?|([uj])?(byte|char|double|float|int(eger)?|" +
         "long|short))\\b", Pattern.MULTILINE);
    public final static Pattern ANNOTATION =
        Pattern.compile("@\\b[A-Za-z]+\\b", Pattern.MULTILINE);
    public final static Pattern CC_COMMENT = Pattern.compile
        ("//.*$|(\"(?:\\\\[^\"]|\\\\\"|.)*?\")|(?s)/\\*.*?\\*/",
         Pattern.MULTILINE);
    public final static Pattern CLASS = Pattern.compile
        ("\\b[A-Z][A-Za-z0-9_]+\\b", Pattern.MULTILINE);
    public final static Pattern CONSTANT = Pattern.compile
        ("\\b(([A-Z][A-Z0-9_]+)|(k[A-Z][A-Za-z0-9]+))\\b",
         Pattern.MULTILINE);
    public final static Pattern OPERATOR = Pattern.compile
        ("[+-=:;<>|!%^&*/?]+", Pattern.MULTILINE);
    public final static Pattern NUMBER = Pattern.compile
        ("\\b\\d+(\\.\\d*)?(e([+\\-])?\\d+)?\\b",
         Pattern.MULTILINE);
    public final static Pattern QUOTED = Pattern.compile
        // "'([^\\\\']+|\\\\([btnfr\"'\\\\]|" +
        // "[0-3]?[0-7]{1,2}|u[0-9a-fA-F]{4}))*'|" +
        ("\"([^\\\\\"]+|\\\\([btnfr\"'\\\\]|" +
         "[0-3]?[0-7]{1,2}|u[0-9a-fA-F]{4}))*\"",
         Pattern.MULTILINE);
    public final static Pattern HTML_TAGS = Pattern.compile
        ("\\b(html|base|head|link|meta|style|title|body|address|article|" +
         "aside|footer|header|h\\d|hgroup|nav|section|blockquote|dd" +
         "|div|dl|dt|figcaption|figure|hr|li|main|ol|p|pre|ul|a|abbr|" +
         "b|bdi|bdo|br|cite|code|data|dfn|em|i|kbd|mark|q|rb|rp|rt|rtc|" +
         "ruby|s|samp|small|span|strong|sub|sup|time|tt|u|var|wbr|area|" +
         "audio|img|map|track|video|embed|iframe|object|" +
         "param|picture|source|canvas|noscript|script|del|ins|caption|" +
         "col|colgroup|table|tbody|td|tfoot|th|thead|tr|button|datalist|" +
         "fieldset|form|input|label|legend|meter|optgroup|option|output|" +
         "progress|select|textarea|details|dialog|menu|menuitem|summary" +
         "|shadow|slot|template|acronym|applet|basefont|" +
         "bgsound|big|blink|center|command|content|dir|element|font|" +
         "frame|frameset|image|isindex|keygen|listing|marquee|" +
         "multicol|nextid|nobr|noembed|noframes|plaintext|spacer|" +
         "strike|xmp|doctype)\\b",
         Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
    public final static Pattern HTML_ATTRS = Pattern.compile
        ("\\b(accept|accesskey|action|align|allow|alt|async|" +
         "auto(capitalize|complete|focus|play)|background|" +
         "bgcolor|border|buffered|challenge|charset|checked|cite|" +
         "class|code(base)?|color|cols|colspan|content(" +
         "editable)?|contextmenu|controls|coords|crossorigin|" +
         "csp|data|datetime|decoding|def(ault|er)|dir|dirname|" +
         "disabled|download|draggable|dropzone|enctype|enterkeyhint|" +
         "equiv|for|form(action|novalidate)?|headers|height|" +
         "hidden|high|href(lang)?|http|icon|id|importance|" +
         "inputmode|integrity|intrinsicsize|ismap|itemprop|keytype|" +
         "kind|label|lang|language|list|loading|loop|low|manifest|" +
         "max|maxlength|media|method|min|minlength|multiple|muted|" +
         "name|novalidate|open|optimum|pattern|ping|placeholder|" +
         "poster|preload|property|radiogroup|readonly|referrerpolicy|" +
         "rel|required|reversed|rows|rowspan|sandbox|scope|scoped|" +
         "selected|shape|size|sizes|slot|span|spellcheck|src|srcdoc|" +
         "srclang|srcset|start|step|style|summary|tabindex|target|" +
         "title|translate|type|usemap|value|width|wrap)\\b",
         Pattern.MULTILINE);
    public final static Pattern HTML_COMMENT =
        Pattern.compile("<!--.*?-->", Pattern.MULTILINE);
    public final static Pattern CSS_STYLES = Pattern.compile
        ("\\b(action|active|additive|adjust|after|align|all|alternates|" +
         "animation|annotation|area|areas|as|asian|attachment|attr|" +
         "auto|backdrop|backface|background|basis|before|behavior|" +
         "bezier|bidi|blend|block|blur|border|both|bottom|box|break|" +
         "brightness|calc|caps|caption|caret|cells|center|ch|change|" +
         "character|charset|checked|child|circle|clamp|clear|clip|" +
         "cm|collapse|color|column|columns|combine|composite|conic|" +
         "content|contrast|count|counter|counters|cross|cubic|cue|" +
         "cursor|decoration|default|deg|delay|dir|direction|" +
         "disabled|display|dpcm|dpi|dppx|drop|duration|east|element|" +
         "ellipse|em|emphasis|empty|enabled|end|env|events|ex|face|" +
         "fade|fallback|family|feature|fill|filter|first|fit|flex|" +
         "float|flow|focus|font|format|forms|fr|frames|fullscreen|" +
         "function|gap|grad|gradient|grayscale|grid|grow|hanging|" +
         "height|historical|hover|hsl|hsla|hue|hyphens|hz|image|import|" +
         "in|increment|indent|indeterminate|index|inherit|initial|" +
         "inline|inset|inside|invalid|invert|isolation|items|" +
         "iteration|justify|khz|kerning|keyframes|lang|language|" +
         "last|layout|leader|left|letter|ligatures|line|linear|link|" +
         "list|local|margin|mask|matrix|matrix3d|max|media|min|" +
         "minmax|mix|mm|mode|ms|name|namespace|negative|none|not|nth|" +
         "numeric|object|of|offset|only|opacity|optical|optional|" +
         "order|orientation|origin|ornaments|orphans|out|outline|" +
         "outset|outside|overflow|override|pad|padding|page|path|pc|" +
         "perspective|place|placeholder|play|pointer|polygon|" +
         "position|prefix|property|pt|punctuation|px|q|quotes|rad|" +
         "radial|radius|range|read|rect|relative|rem|rendering|repeat|" +
         "repeating|required|reset|resize|revert|rgb|rgba|right|" +
         "root|rotate|rotate3d|rotatex|rotatey|rotatez|row|rows|" +
         "rule|s|saturate|scale|scale3d|scalex|scaley|scalez|scope|" +
         "scroll|scrollbar|selection|self|sepia|set|settings|shadow|" +
         "shape|shrink|side|size|sizing|skew|skewx|skewy|slice|" +
         "slotted|snap|source|space|spacing|span|speak|src|start|" +
         "state|static|steps|stop|stretch|style|styleset|stylistic|suffix|" +
         "supports|swash|symbols|synthesis|system|tab|table|target|" +
         "template|text|threshold|timing|top|touch|transform|" +
         "transition|translate|translate3d|translatex|translatey|" +
         "translatez|turn|type|underline|unicode|unset|upright|url|" +
         "user|valid|values|var|variant|variation|vertical|vh|" +
         "viewport|visibility|visited|vmax|vmin|vw|weight|white|" +
         "widows|width|will|word|wrap|write|writing|x|y|z|zoom)\\b",
         Pattern.MULTILINE);
    public final static Pattern CSS_HEX = Pattern.compile
        ("#\\b[A-Fa-f0-9]+\\b", Pattern.MULTILINE);
    public final static Pattern ORG_HEADER = Pattern.compile
        ("(^\\*+ +.+$)|(^#\\+.+$)", Pattern.MULTILINE);
    public final static Pattern ORG_LINK = Pattern.compile
        ("\\[\\[.*?]]", Pattern.MULTILINE);
    public final static Pattern ORG_EMPH = Pattern.compile
        ("(([*~/+=]+)\\b(\\w| )+?\\b\\2)|(\\b(_{1,2})(\\w| )+?\\5\\b)",
         Pattern.MULTILINE);
    public final static Pattern ORG_COMMENT = Pattern.compile
        ("(^# .*$)|(@@.*?@@)", Pattern.MULTILINE);
    public final static Pattern MD_HEADER = Pattern.compile
        ("(^.+\\s+-+$)|(^.+\\s+=+$)|(^#+ +.+$)", Pattern.MULTILINE);
    public final static Pattern MD_LINK = Pattern.compile
        ("(!?\\[.+] *\\(.+\\))|(!?\\[.+] *\\[.+])|" +
         "( *\\[.+]: +.+$)", Pattern.MULTILINE);
    public final static Pattern MD_EMPH = Pattern.compile
        ("(([*~]{1,2})\\b(\\w| )+?\\b\\2)|(\\b(_{1,2})(\\w| )+?\\5\\b)",
         Pattern.MULTILINE);
    public final static Pattern MD_CODE = Pattern.compile
        ("(^ {4,}.+$)|(`.+?`)", Pattern.MULTILINE);
    public final static Pattern SH_VAR = Pattern.compile
        ("(\\$\\b\\w+\\b)|(\\$\\{.+?\\})|(\\$\\(.+?\\))", Pattern.MULTILINE);
    public final static Pattern SH_COMMENT = Pattern.compile
        ("#.*$", Pattern.MULTILINE);
    public final static Pattern MODE_PATTERN = Pattern.compile
        ("^\\S+\\s+ed:(.+)$", Pattern.MULTILINE);
    public final static Pattern OPTION_PATTERN = Pattern.compile
        ("(\\s+(no)?(vw|ww|sg|cs|hs|th|ts|tf)(:\\w)?)", Pattern.MULTILINE);
    public final static Pattern WORD_PATTERN = Pattern.compile
        ("\\w+", Pattern.MULTILINE);
    final static double KEYBOARD_RATIO = 0.25;
    final static int LAST_SIZE = 256;
    final static int MENU_SIZE = 192;
    final static int FIRST_SIZE = 256;
    final static int TOO_LARGE = 524288;
    final static int FOLDER_OFFSET = 0x7d000000;
    final static int POSITION_DELAY = 128;
    final static int UPDATE_DELAY = 128;
    final static int FIND_DELAY = 128;
    final static int MAX_PATHS = 10;
    private final static int GET_TEXT = 0;
    final static int REQUEST_READ = 1;
    final static int REQUEST_SAVE = 2;
    final static int REQUEST_OPEN = 3;
    final static int OPEN_DOCUMENT   = 1;
    final static int CREATE_DOCUMENT = 2;
    final static int LIGHT = 1;
    final static int DARK  = 2;
    final static int BLACK = 3;
    final static int RETRO = 4;
    final static int TINY   = 8;
    final static int SMALL  = 12;
    final static int MEDIUM = 18;
    final static int LARGE  = 24;
    final static int HUGE  =  32;
    final static int NORMAL = 1;
    final static int MONO   = 2;
    final static int NO_SYNTAX   = 0;
    final static int CC_SYNTAX   = 1;
    final static int HTML_SYNTAX = 2;
    final static int CSS_SYNTAX  = 3;
    final static int ORG_SYNTAX  = 4;
    final static int MD_SYNTAX   = 5;
    final static int SH_SYNTAX   = 6;
    final static int DEF_SYNTAX  = 7;
}
