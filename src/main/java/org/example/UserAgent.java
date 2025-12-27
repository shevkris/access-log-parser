package org.example;

public class UserAgent {
    private final String originalUserAgent;
    private final OperatingSystem os;
    private final Browser browser;

    public enum OperatingSystem {
        WINDOWS, MACOS, LINUX, ANDROID, IOS, FIREFOX_OS, UNKNOWN
    }

    public enum Browser {
        FIREFOX, CHROME, OPERA, EDGE, SAFARI, INTERNET_EXPLORER, OTHER,
        GOOGLEBOT, YANDEXBOT  // боты
    }

    public UserAgent(String userAgentString) {
        this.originalUserAgent = userAgentString == null ? "" : userAgentString;
        this.os = detectOperatingSystem(this.originalUserAgent);
        this.browser = detectBrowser(this.originalUserAgent);
    }

    private OperatingSystem detectOperatingSystem(String userAgent) {
        if (userAgent == null || userAgent.isEmpty() || userAgent.equals("-")){
            return OperatingSystem.UNKNOWN;
        }

        String uaLower = userAgent.toLowerCase();

        if (uaLower.contains("windows phone") || uaLower.contains("windows mobile")) {
            return OperatingSystem.WINDOWS;
        } else if (uaLower.contains("windows")) {
            return OperatingSystem.WINDOWS;
        } else if (uaLower.contains("mac os") || uaLower.contains("macintosh")) {
            if (uaLower.contains("iphone") || uaLower.contains("ipad")) {
                return OperatingSystem.IOS;
            }
            return OperatingSystem.MACOS;
        } else if (uaLower.contains("linux")) {
            if (uaLower.contains("android")) {
                return OperatingSystem.ANDROID;
            }
            return OperatingSystem.LINUX;
        } else if (uaLower.contains("android")) {
            return OperatingSystem.ANDROID;
        } else if (uaLower.contains("iphone") || uaLower.contains("ipad")) {
            return OperatingSystem.IOS;
        } else if (uaLower.contains("firefox os")) {
            return OperatingSystem.FIREFOX_OS;
        } else if (uaLower.contains("x11") || uaLower.contains("unix")) {
            return OperatingSystem.LINUX;
        } else {
            return OperatingSystem.UNKNOWN;
        }
    }

    private Browser detectBrowser(String userAgent) {
        if (userAgent == null || userAgent.isEmpty() || userAgent.equals("-")){
            return Browser.OTHER;
        }

        String uaLower = userAgent.toLowerCase();

        if (uaLower.contains("googlebot")) {
            return Browser.GOOGLEBOT;
        } else if (uaLower.contains("yandexbot")) {
            return Browser.YANDEXBOT;
        }

        if (uaLower.contains("msie") || uaLower.contains("trident")) {
            return Browser.INTERNET_EXPLORER;
        }

        if (uaLower.contains("firefox/") && !uaLower.contains("seamonkey")) {
            if (uaLower.contains("gecko/") && uaLower.contains("firefox/")) {
                return Browser.FIREFOX;
            }
        }
        if (uaLower.contains("edg/")) {
            return Browser.EDGE;
        }

        if (uaLower.contains(" opr/") ||
                (uaLower.contains("presto/") && uaLower.startsWith("opera/"))) {
            return Browser.OPERA;
        }

        if (uaLower.contains("chrome/")) {
            if (!uaLower.contains("edg/") && !uaLower.contains(" opr/")) {
                return Browser.CHROME;
            }
        }

        if (uaLower.contains("safari/") && !uaLower.contains("chrome/")) {
            if (uaLower.contains("applewebkit/") && !uaLower.contains("chrome/")) {
                return Browser.SAFARI;
            }
        }

        if (uaLower.contains("edge/") && !uaLower.contains("edg/")) {
            return Browser.EDGE;
        }

        return Browser.OTHER;
    }

    public OperatingSystem getOperatingSystem() {
        return os;
    }

    public Browser getBrowser() {
        return browser;
    }

    @Override
    public String toString() {
        return String.format("UserAgent{os=%s, browser=%s%s%s}",
                os, browser);
    }
}
