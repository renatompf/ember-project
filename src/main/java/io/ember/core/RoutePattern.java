package io.ember.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RoutePattern {
    private final String rawPath;
    private final Pattern pattern;
    private final List<String> parameterNames;

    /**
     * Constructs a `RoutePattern` object by converting a raw path string into a regex pattern
     * and extracting parameter names from the path.
     *
     * @param rawPath The raw path string, which may include parameters (e.g., `:param` for single-segment parameters
     *                or `*param` for multi-segment parameters).
     *<p>
     * The constructor processes the raw path as follows:
     * <ul>- Segments starting with `:` are treated as single-segment parameters and matched with `([^/]+)`. </ul>
     * <ul>- Segments starting with `*` are treated as multi-segment parameters and matched with `(.*)`. </ul>
     * <ul> - Other segments are treated as literal strings and escaped using `Pattern.quote`. </ul>
     *<p>
     * The resulting regex pattern is compiled and stored in the `pattern` field.
     * Parameter names are extracted and stored in the `parameterNames` list.
     */
    public RoutePattern(String rawPath) {
       this.parameterNames = new ArrayList<>();
       this.rawPath = rawPath;
       String regex = Arrays.stream(rawPath.split("/"))
               .map(
                       segment -> {
                            if (segment.startsWith(":")) {
                                boolean optional = segment.endsWith("?");
                                String name = segment.substring(1).replace("?", "");
                                parameterNames.add(name);
                                return optional ? "([^/]+)?" : "([^/]+)";
                            } else if (segment.startsWith("*")) {
                                parameterNames.add("*");
                                return ".*";
                            } else {
                                 return Pattern.quote(segment);
                            }
                       }
               )
               .reduce((a, b) -> a + "/" + b)
               .orElse("");

        this.pattern = Pattern.compile("^" + regex + "$");
    }

    /**
     * Returns the original raw path string used to create this route pattern.
     *
     * @return The raw path string that was provided when constructing this RoutePattern.
     */
    public String getRawPath() {
        return rawPath;
    }

    /**
     * Returns the regex pattern used for matching paths.
     *
     * @return The regex pattern as a `Pattern` object.
     */
    public boolean matches(String path) {
        return pattern.matcher(path).matches();
    }

    /**
     * Extracts parameters from a given path based on the regex pattern and parameter names defined in this `RoutePattern`.
     *
     * @param path The path string to extract parameters from.
     * @return A map containing parameter names as keys and their corresponding values extracted from the path.
     *         If no parameters are found, an empty map is returned.
     */
    public HashMap<String, String> extractParameters(String path) {
        HashMap<String, String> parameters = new HashMap<>();

        Matcher matcher = pattern.matcher(path);
        if (!matcher.matches()) {
            return parameters;
        }

        for (int i = 0; i < parameterNames.size(); i++) {
            if (parameterNames.contains("*")) {
                int wildcardIndex = parameterNames.indexOf("*");
                String[] pathParts = path.split("/");
                // Skip the base path parts and join the rest
                String wildcardValue = String.join("/",
                        Arrays.copyOfRange(pathParts, wildcardIndex + 2, pathParts.length));
                parameters.put("*", wildcardValue);
            } else {
                String paramName = parameterNames.get(i);
                String paramValue = matcher.group(i + 1);
                parameters.put(paramName, paramValue);
            }
        }


        return parameters;
    }

}
