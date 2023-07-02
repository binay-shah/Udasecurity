module com.udacity.catpoint.ImageService {
    exports com.udacity.catpoint.ImageService;
    requires org.slf4j;
    requires java.desktop;
    requires software.amazon.awssdk.services.rekognition;
    requires software.amazon.awssdk.regions;
    requires software.amazon.awssdk.auth;
    requires software.amazon.awssdk.core;
}