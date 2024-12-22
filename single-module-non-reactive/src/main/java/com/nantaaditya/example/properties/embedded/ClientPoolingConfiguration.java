package com.nantaaditya.example.properties.embedded;

public record ClientPoolingConfiguration(
    int maxTotal,
    int maxPerRoute
) {

}
