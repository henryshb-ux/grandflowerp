package com.artivisi.accountingfinance.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.theme")
public class ThemeConfig {

    private static final String THEMES_PATH_PREFIX = "/themes/";

    private String name;
    private String footerText;
    private String dir;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFooterText() {
        return footerText;
    }

    public void setFooterText(String footerText) {
        this.footerText = footerText;
    }

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }

    public String getLogoPath() {
        return THEMES_PATH_PREFIX + name + "/logo.svg";
    }

    public String getLogoDarkPath() {
        return THEMES_PATH_PREFIX + name + "/logo-dark.svg";
    }

    public String getThemeCssPath() {
        return THEMES_PATH_PREFIX + name + "/theme.css";
    }
}
