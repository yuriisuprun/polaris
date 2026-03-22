package com.polaris.config;

public class BackoffConfig {

    public String type = "fixed"; // fixed | exponential
    public long delay = 100;
}