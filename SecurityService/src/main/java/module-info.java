module SecurityService {
    //exports com.udacity.catpoint.security.service;
    exports com.udacity.catpoint.security.data;
    exports com.udacity.catpoint.security.application;
    requires java.desktop;
    requires com.udacity.catpoint.ImageService;
    requires miglayout;
    requires com.google.common;
    requires com.google.gson;
    requires java.prefs;

    opens com.udacity.catpoint.security.data to com.google.gson;
}