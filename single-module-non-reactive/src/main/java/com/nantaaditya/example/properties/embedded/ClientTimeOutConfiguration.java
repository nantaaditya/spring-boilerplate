package com.nantaaditya.example.properties.embedded;

public record ClientTimeOutConfiguration(
    int connectRequestTimeOut,
    int connectTimeOut,
    int readTimeOut
) {

}
