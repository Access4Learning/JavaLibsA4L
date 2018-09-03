package org.sifassociation.messaging;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import nu.xom.Attribute;
import nu.xom.Element;

/**
 * Collection of static methods to help create common SIF 2.x payloads.
 * 
 * @author jlovell
 * @since 3.0
 * @version 3.0
 */
public class SIF2Payloads {
    
    public static String createRegister(
            String name,
            SIFVersion infrastructureVersion,
            SIFVersion dataModelVersion,
            SIFVersion receiveVersion,
            long maxBufferSize,
            String mode,
            URL pushURL,
            String nodeVendor,
            String nodeVersion,
            String applicationVendor,
            String applicationProduct,
            String applicationVersion,
            URL icon) {
        // So we always have a top grouping tag to our payload.
        Element body = new Element("Body", 
                "http://schemas.xmlsoap.org/soap/envelope/");
        
        // So we have the proper namespace.
        String ns = "http://www.sifassociation.org/message/soap/2.x";
        
        // So we have the proper place to start this SOAP payload.
        Element root = new Element("Register", ns);
        body.appendChild(root);
        
        Element current = new Element("Name", ns);
        current.appendChild(name);
        root.appendChild(current);
        
        current = new Element("Version", ns);
        current.appendChild(receiveVersion.toString());
        root.appendChild(current);
        
        current = new Element("MaxBufferSize", ns);
        current.appendChild(String.valueOf(maxBufferSize));
        root.appendChild(current);
        
        current = new Element("Mode", ns);
        current.appendChild(mode);
        root.appendChild(current);
        
        String url = "";
        boolean secure = false;
                
        if("Push".equals(mode) && null != pushURL) {
            // So we know to use secure or insecure values in the protocol.
            url = pushURL.toString().toLowerCase();
            secure = url.toString().startsWith("https");

            
            Element protocol = new Element("Protocol", ns);
            if(secure) {
                protocol.addAttribute(new Attribute("Type", "HTTPS-SOAP1.1"));
                protocol.addAttribute(new Attribute("Secure", "Yes"));
            }
            else {
                protocol.addAttribute(new Attribute("Type", "HTTP-SOAP1.1"));
                protocol.addAttribute(new Attribute("Secure", "No"));
            }
            root.appendChild(protocol);
            
            current = new Element("URL", ns);
            current.appendChild(pushURL.toString());
            protocol.appendChild(current);
            
            Element property = new Element("Property", ns);
            current = new Element("Name", ns);
            current.appendChild("InfrastructureVersion");
            property.appendChild(current);
            current = new Element("Value", ns);
            current.appendChild(infrastructureVersion.toString());
            property.appendChild(current);
            protocol.appendChild(property);
            
            property = new Element("Property", ns);
            current = new Element("Name", ns);
            current.appendChild("DataModelVersion");
            property.appendChild(current);
            current = new Element("Value", ns);
            current.appendChild(dataModelVersion.toString());
            property.appendChild(current);
            protocol.appendChild(property);
            
            property = new Element("Property", ns);
            current = new Element("Name", ns);
            current.appendChild("DataModelURL");
            property.appendChild(current);
            current = new Element("Value", ns);
            current.appendChild(pushURL.toString());
            property.appendChild(current);
            protocol.appendChild(property);
            
            property = new Element("Property", ns);
            current = new Element("Name", ns);
            current.appendChild("ZoneServiceURL");
            property.appendChild(current);
            current = new Element("Value", ns);
            current.appendChild(pushURL.toString());
            property.appendChild(current);
            protocol.appendChild(property);
            
            if(0 < nodeVendor.length()) {
                current = new Element("NodeVendor", ns);
                current.appendChild(nodeVendor);
                root.appendChild(current);
            }

            if(0 < nodeVersion.length()) {
                current = new Element("NodeVersion", ns);
                current.appendChild(nodeVersion);
                root.appendChild(current);
            }
           
            if(0 < applicationVendor.length() ||
                    0 < applicationProduct.length() ||
                    0 < applicationVersion.length()) {
                Element application = new Element("Application", ns);
                root.appendChild(application);
                
                current = new Element("Vendor", ns);
                current.appendChild(applicationVendor);
                application.appendChild(current);

                current = new Element("Product", ns);
                current.appendChild(applicationProduct);
                application.appendChild(current);

                current = new Element("Version", ns);
                current.appendChild(applicationVersion);
                application.appendChild(current);                
            }
            
            if(null != icon) {
                current = new Element("Icon", ns);
                current.appendChild(icon.toString());
                root.appendChild(current);                
            }
        }
        
        return body.toXML();
    }
    
    public static String createGetMessage() {
        // So we always have a top grouping tag to our payload.
        Element body = new Element("Body", 
                "http://schemas.xmlsoap.org/soap/envelope/");        
        
        // So we have the proper namespace.
        String ns = "http://www.sifassociation.org/message/soap/2.x";
        
        // So we have the proper place to start this SOAP payload.
        Element root = new Element("GetMessage", ns);
        body.appendChild(root);
        
        return body.toXML();
    }
    
    public static String createStatus(int code, String description) {
        // So we always have a top grouping tag to our payload.
        Element body = new Element("Body", 
                "http://schemas.xmlsoap.org/soap/envelope/");
        
        // So we have the proper namespace.
        String ns = "http://www.sifassociation.org/message/soap/2.x";
        
        // So we have the proper place to start this SOAP payload.
        Element root = new Element("Status", ns);
        body.appendChild(root);
        
        Element current = new Element("Code", ns);
        current.appendChild(Integer.toString(code));
        root.appendChild(current);

        current = new Element("Desc", ns);
        current.appendChild(description);
        root.appendChild(current);
        
        return body.toXML();
    }
    
    /**
     * Creates a SIF_Error XML snippet from the given parameters.
     * 
     * @param tag  The root of the error (DataModelError, ZoneServiceError, DeQueueError, and TransportError).
     * @param category
     * @param code
     * @param description
     * @param extended
     * @return 
     */
    public static String createError(
            String tag, 
            int category, 
            int code, 
            String description, 
            String extended) {
        // So we always have a top grouping tag to our payload.
        Element body = new Element("Body", 
                "http://schemas.xmlsoap.org/soap/envelope/");
        
        // So we have the proper namespace.
        String ns = "http://www.sifassociation.org/message/soap/2.x";
        
        // So we have the proper place to start this SOAP payload.
        Element root = new Element(tag, ns);
        body.appendChild(root);
        
        Element current = new Element("Category", ns);
        current.appendChild(Integer.toString(category));
        root.appendChild(current);
        
        current = new Element("Code", ns);
        current.appendChild(Integer.toString(code));
        root.appendChild(current);

        current = new Element("Desc", ns);
        current.appendChild(description);
        root.appendChild(current);

        current = new Element("ExtendedDesc", ns);
        current.appendChild(extended);
        root.appendChild(current);
        
        return body.toXML();
    }
    
    // To Do: For the ZIS these IDs need to be converted (probably not here)!!!
    public static String createCancelRequests(
            String notificationType,
            List<SIFRefId> msgIds) {
        // So we do not create invalid payloads.
        List<String> types = Collections.unmodifiableList(Arrays.asList(
                "None", "Standard"));
        if(! types.contains(notificationType)) {
            throw new IllegalArgumentException(notificationType + "is not a "
                    + "valid NotificationType.");
        }
        
        // So we always have a top grouping tag to our payload.
        Element body = new Element("Body", 
                "http://schemas.xmlsoap.org/soap/envelope/");
        
        // So we have the proper namespace.
        String ns = "http://www.sifassociation.org/message/soap/2.x";
        
        // So we have the proper place to start this SOAP payload.
        Element root = new Element("CancelRequests", ns);
        body.appendChild(root);
        
        // So we know what kind of response we expect/support.
        Element current = new Element("NotificationType", ns);
        current.appendChild(notificationType);
        root.appendChild(current);
        
        Element ids = new Element("RequestMsgIds", ns);
        root.appendChild(ids);
        
        // So we cancel all the desired requests.
        for(SIFRefId msgId : msgIds) {
            current = new Element("RequestMsgId", ns);
            current.appendChild(msgId.toString());
            ids.appendChild(current);
        }
        
        return body.toXML();
        
    }
    
    // To Do: For the ZIS these IDs need to be converted (probably not here)!!!
    public static String createCancelServiceInputs(
            String notificationType,
            List<SIFRefId> msgIds) {
        // So we do not create invalid payloads.
        List<String> types = Collections.unmodifiableList(Arrays.asList(
                "None", "Standard"));
        if(! types.contains(notificationType)) {
            throw new IllegalArgumentException(notificationType + "is not a "
                    + "valid NotificationType.");
        }
        
        // So we always have a top grouping tag to our payload.
        Element body = new Element("Body", 
                "http://schemas.xmlsoap.org/soap/envelope/");
        
        // So we have the proper namespace.
        String ns = "http://www.sifassociation.org/message/soap/2.x";
        
        // So we have the proper place to start this SOAP payload.
        Element root = new Element("CancelServiceInputs", ns);
        body.appendChild(root);
        
        // So we know what kind of response we expect/support.
        Element current = new Element("NotificationType", ns);
        current.appendChild(notificationType);
        root.appendChild(current);
        
        Element ids = new Element("ServiceMsgIds", ns);
        root.appendChild(ids);
        
        // So we cancel all the desired requests.
        for(SIFRefId msgId : msgIds) {
            current = new Element("ServiceMsgId", ns);
            current.appendChild(msgId.toString());
            ids.appendChild(current);
        }
        
        return body.toXML();
        
    }
    
    public static String createGetAgentACL() {
        // So we always have a top grouping tag to our payload.
        Element body = new Element("Body", 
                "http://schemas.xmlsoap.org/soap/envelope/");        
        
        // So we have the proper namespace.
        String ns = "http://www.sifassociation.org/message/soap/2.x";
        
        // So we have the proper place to start this SOAP payload.
        Element root = new Element("GetAgentACL", ns);
        body.appendChild(root);
        
        return body.toXML();
    }
    
    public static String createGetZoneStatus() {
        // So we always have a top grouping tag to our payload.
        Element body = new Element("Body", 
                "http://schemas.xmlsoap.org/soap/envelope/");        
        
        // So we have the proper namespace.
        String ns = "http://www.sifassociation.org/message/soap/2.x";
        
        // So we have the proper place to start this SOAP payload.
        Element root = new Element("GetZoneStatus", ns);
        body.appendChild(root);
        
        return body.toXML();
    }
    
    
    public static String createPing() {
        // So we always have a top grouping tag to our payload.
        Element body = new Element("Body", 
                "http://schemas.xmlsoap.org/soap/envelope/");        
        
        // So we have the proper namespace.
        String ns = "http://www.sifassociation.org/message/soap/2.x";
        
        // So we have the proper place to start this SOAP payload.
        Element root = new Element("Ping", ns);
        body.appendChild(root);
        
        return body.toXML();
    }

    // Provide = Services Requests
    public static String createProvide(
            List<String> objects,
            List<Boolean> extensions,
            List<List<String>> contexts) {
        // So we always have a top grouping tag to our payload.
        Element body = new Element("Body", 
                "http://schemas.xmlsoap.org/soap/envelope/");
        
        body.appendChild(createACLObjectExtended(
                "Provide",
                objects,
                extensions,
                contexts));
                
        return body.toXML();
    }

    public static String createProvision(
            List<String> provideObjects,
            List<Boolean> provideExtensions,
            List<List<String>> provideContexts,
            List<String> subscribeObjects,
            List<List<String>> subscribeContexts,
            List<String> publishAddObjects,
            List<List<String>> publishAddContexts,
            List<String> publishChangeObjects,
            List<List<String>> publishChangeContexts,
            List<String> publishDeleteObjects,
            List<List<String>> publishDeleteContexts,
            List<String> requestObjects,
            List<Boolean> requestExtensions,
            List<List<String>> requestContexts,            
            List<String> respondObjects,
            List<Boolean> respondExtensions,
            List<List<String>> respondContexts,
            List<String> provideServices,
            List<List<String>> provideServicesContexts,
            List<String> respondServices,
            List<List<String>> respondServicesContexts,
            List<String> requestServices,
            List<List<String>> requestServicesContexts,
            List<List<String>> requestServicesOperations,
            List<String> subscribeServices,
            List<List<String>> subscribeServicesContexts,
            List<List<String>> subscribeServicesOperations) {
        return createACL(
                "Provision",
                provideObjects,
                provideExtensions,
                provideContexts,
                subscribeObjects,
                subscribeContexts,
                publishAddObjects,
                publishAddContexts,
                publishChangeObjects,
                publishChangeContexts,
                publishDeleteObjects,
                publishDeleteContexts,
                requestObjects,
                requestExtensions,
                requestContexts,            
                respondObjects,
                respondExtensions,
                respondContexts,
                provideServices,
                provideServicesContexts,
                respondServices,
                respondServicesContexts,
                requestServices,
                requestServicesContexts,
                requestServicesOperations,
                subscribeServices,
                subscribeServicesContexts,
                subscribeServicesOperations,
                "Objects");
    }
    
    // Provide = Default Provider
    // Subscribe = Processes Events
    public static String createACL(
            String name,
            List<String> provideObjects,
            List<Boolean> provideExtensions,
            List<List<String>> provideContexts,
            List<String> subscribeObjects,
            List<List<String>> subscribeContexts,
            List<String> publishAddObjects,
            List<List<String>> publishAddContexts,
            List<String> publishChangeObjects,
            List<List<String>> publishChangeContexts,
            List<String> publishDeleteObjects,
            List<List<String>> publishDeleteContexts,
            List<String> requestObjects,
            List<Boolean> requestExtensions,
            List<List<String>> requestContexts,            
            List<String> respondObjects,
            List<Boolean> respondExtensions,
            List<List<String>> respondContexts,
            List<String> provideServices,
            List<List<String>> provideServicesContexts,
            List<String> respondServices,
            List<List<String>> respondServicesContexts,
            List<String> requestServices,
            List<List<String>> requestServicesContexts,
            List<List<String>> requestServicesOperations,
            List<String> subscribeServices,
            List<List<String>> subscribeServicesContexts,
            List<List<String>> subscribeServicesOperations,
            String suffix) {
        // So we always have a top grouping tag to our payload.
        Element body = new Element("Body", 
                "http://schemas.xmlsoap.org/soap/envelope/");
        
        // So we have the proper place to start this SOAP payload.
        Element root = new Element(name, 
                "http://www.sifassociation.org/message/soap/2.x");
        body.appendChild(root);
        
        root.appendChild(createACLObjectExtended(
                "Provide" + suffix,
                provideObjects,
                provideExtensions,
                provideContexts));
        
        root.appendChild(createACLObject(
                "Subscribe" + suffix,
                subscribeObjects,
                subscribeContexts));
        
        root.appendChild(createACLObject(
                "PublishAdd" + suffix,
                publishAddObjects,
                publishAddContexts));
        
        root.appendChild(createACLObject(
                "PublishChange" + suffix,
                publishChangeObjects,
                publishChangeContexts));

        root.appendChild(createACLObject(
                "PublishDelete" + suffix,
                publishDeleteObjects,
                publishDeleteContexts));

        root.appendChild(createACLObjectExtended(
                "Request" + suffix,
                requestObjects,
                requestExtensions,
                requestContexts));
        
        root.appendChild(createACLObjectExtended(
                "Respond" + suffix,
                respondObjects,
                respondExtensions,
                respondContexts));
        
        if(null != provideServices) {
            root.appendChild(createACLService(
                    "ProvideService",
                    provideServices,
                    provideServicesContexts));
        }
        
        if(null != respondServices) {
            root.appendChild(createACLService(
                    "RespondService",
                    respondServices,
                    respondServicesContexts));
        }

        if(null != requestServices) {
            root.appendChild(createACLServiceOperational(
                    "RequestService",
                    requestServices,
                    requestServicesContexts,
                    requestServicesOperations));
        }
        
        if(null != subscribeServices) {
            root.appendChild(createACLServiceOperational(
                    "SubscribeService",
                    subscribeServices,
                    subscribeServicesContexts,
                    subscribeServicesOperations));
        }        
        
        return body.toXML();
    }
    
    public static String createSleep() {
        // So we always have a top grouping tag to our payload.
        Element body = new Element("Body", 
                "http://schemas.xmlsoap.org/soap/envelope/");        
        
        // So we have the proper namespace.
        String ns = "http://www.sifassociation.org/message/soap/2.x";
        
        // So we have the proper place to start this SOAP payload.
        Element root = new Element("Sleep", ns);
        body.appendChild(root);
        
        return body.toXML();
    }
    
    // Subscribe = Receive Events
    public static String createSubscribe(
            List<String> objects,
            List<List<String>> contexts) {
        // So we always have a top grouping tag to our payload.
        Element body = new Element("Body", 
                "http://schemas.xmlsoap.org/soap/envelope/");
        
        body.appendChild(createACLObject(
                "Subscribe",
                objects,
                contexts));
                
        return body.toXML();
    }
    
    public static String createUnprovide(
            List<String> objects,
            List<List<String>> contexts) {
        // So we always have a top grouping tag to our payload.
        Element body = new Element("Body", 
                "http://schemas.xmlsoap.org/soap/envelope/");
        
        body.appendChild(createACLObject(
                "Unprovide",
                objects,
                contexts));
                
        return body.toXML();
    }

    public static String createUnregister() {
        // So we always have a top grouping tag to our payload.
        Element body = new Element("Body", 
                "http://schemas.xmlsoap.org/soap/envelope/");        
        
        // So we have the proper namespace.
        String ns = "http://www.sifassociation.org/message/soap/2.x";
        
        // So we have the proper place to start this SOAP payload.
        Element root = new Element("Unregister", ns);
        body.appendChild(root);
        
        return body.toXML();
    }

    public static String createUnsubscribe(
            List<String> objects,
            List<List<String>> contexts) {
        // So we always have a top grouping tag to our payload.
        Element body = new Element("Body", 
                "http://schemas.xmlsoap.org/soap/envelope/");
        
        body.appendChild(createACLObject(
                "Unsubscribe",
                objects,
                contexts));
                
        return body.toXML();
    }
    
    public static String createWakeup() {
        // So we always have a top grouping tag to our payload.
        Element body = new Element("Body", 
                "http://schemas.xmlsoap.org/soap/envelope/");        
        
        // So we have the proper namespace.
        String ns = "http://www.sifassociation.org/message/soap/2.x";
        
        // So we have the proper place to start this SOAP payload.
        Element root = new Element("Wakeup", ns);
        body.appendChild(root);
        
        return body.toXML();
    }
    
    
    /* HELPERS */
    
    /**
     * So we include basic object (un)provisioning consistently.
     * 
     * Note:  Parameter lists must match in length, but null is supported!
     * 
     * @param name  The type of provision being done (ie Subscribe).
     * @param objects  The object names we are provisioning (null excludes).
     * @param contexts  The contexts names for each object (null excludes).
     * @return  The root of this provisioning chunk.
     * @since 3.0
     */
    private static Element createACLObject(
            String name,
            List<String> objects,
            List<List<String>> contexts) {
        
        // So we have the proper namespace.
        String ns = "http://www.sifassociation.org/message/soap/2.x";
        
        // So we have the proper place to start this chunk.
        Element root = new Element(name, ns);

        if(null != objects) {
            // So we do not create invalid payloads.
            if(null == contexts || objects.size() != contexts.size()) {
                throw new IllegalArgumentException("Arguments must be the same size.");
            }

            // So we support all the objects desired in a single message.
            Element current = null;
            for(int i = 0; i < objects.size(); i++) {
                // So we can exclude optional components.
                if(null != objects.get(i)) {
                    current = new Element("Object", ns);
                    current.addAttribute(new Attribute("ObjectName", objects.get(i)));
                    root.appendChild(current);

                    // So we can exclude optional components.
                    if(null != contexts.get(i)) {
                        Element scopes = new Element("Contexts", ns);
                        current.appendChild(scopes);

                        for(String context : contexts.get(i)) {
                            current = new Element("Context", ns);
                            current.appendChild(context);
                            scopes.appendChild(current);
                        }
                    }
                }
            }
        }
        
        return root;
    }
    
    public static Element createObjectList(
            List<String> objects,
            List<List<String>> contexts) {   
        
        // So we have consistent ACLs.
        Element root = createACLObject("ObjectList", objects, contexts);
                
        return root;
    }
    
    private static Element createACLObjectExtended(
            String name,
            List<String> objects,
            List<Boolean> extensions,
            List<List<String>> contexts) {   
        
        // So we have the proper namespace.
        String ns = "http://www.sifassociation.org/message/soap/2.x";
        
        // So we have the proper place to start this chunk.
        Element root = new Element(name, ns);
        
        if(null != objects) {
            // So we do not create invalid payloads.
            if(null == extensions || null == contexts ||
                    objects.size() != extensions.size() || 
                    objects.size() != contexts.size()) {
                throw new IllegalArgumentException("Arguments must be the same size.");
            } 
            // So we support all the objects desired in a single message.
            Element current = null;
            for(int i = 0; i < objects.size(); i++) {
                // So we can exclude optional components.
                if(null != objects.get(i)) {
                    current = new Element("Object", ns);
                    current.addAttribute(new Attribute("ObjectName", objects.get(i)));
                    root.appendChild(current);

                    // So we can exclude optional components.
                    if(null != extensions.get(i)) {                
                        Element extended = new Element("ExtendedQuerySupport", ns);
                        if(extensions.get(i)) {
                            extended.appendChild("true");
                        }
                        else {
                            extended.appendChild("false");
                        }
                        current.appendChild(extended);
                    }

                    // So we can exclude optional components.
                    if(null != contexts.get(i)) {
                        Element scopes = new Element("Contexts", ns);
                        current.appendChild(scopes);

                        for(String context : contexts.get(i)) {
                            current = new Element("Context", ns);
                            current.appendChild(context);
                            scopes.appendChild(current);
                        }
                    }
                }
            }
        }
        
        return root;
    }
    
    public static Element createObjectListExtended(
            List<String> objects,
            List<Boolean> extensions,
            List<List<String>> contexts) {   
        
        // So we have consistent ACLs.
        Element root = createACLObjectExtended("ObjectList", objects, extensions, contexts);
                
        return root;
    }
    
    private static Element createACLService(
            String name,
            List<String> services,
            List<List<String>> contexts) {
        // So we do not create invalid payloads.
        if(services.size() != contexts.size()) {
            throw new IllegalArgumentException("Arguments must be the same size.");
        }
        
        // So we have the proper namespace.
        String ns = "http://www.sifassociation.org/message/soap/2.x";
        
        // So we have the proper place to start this chunk.
        Element root = new Element(name, ns);
        
        // So we support all the objects desired in a single message.
        Element current = null;
        for(int i = 0; i < services.size(); i++) {
            // So we can exclude optional components.
            if(null != services.get(i)) {
                current = new Element("Service", ns);
                current.addAttribute(new Attribute("ServiceName", services.get(i)));
                root.appendChild(current);

                // So we can exclude optional components.
                if(null != contexts.get(i)) {                
                    Element scopes = new Element("Contexts", ns);
                    current.appendChild(scopes);

                    for(String context : contexts.get(i)) {
                        current = new Element("Context", ns);
                        current.appendChild(context);
                        scopes.appendChild(current);
                    }
                }
            }
        }
        
        return root;
    }

    private static Element createACLServiceOperational(
            String name,
            List<String> services,
            List<List<String>> contexts,
            List<List<String>> operations) {
        // So we do not create invalid payloads.
        if(services.size() != contexts.size() ||
                services.size() != operations.size()) {
            throw new IllegalArgumentException("Arguments must be the same size.");
        }
        
        // So we have the proper namespace.
        String ns = "http://www.sifassociation.org/message/soap/2.x";
        
        // So we have the proper place to start this chunk.
        Element root = new Element(name, ns);
        
        // So we support all the objects desired in a single message.
        Element current = null;
        for(int i = 0; i < services.size(); i++) {
            // So we can exclude optional components.
            if(null != services.get(i)) {
                current = new Element("Service", ns);
                current.addAttribute(new Attribute("ServiceName", services.get(i)));
                root.appendChild(current);

                Element child = null;
                // So we can exclude optional components.
                if(null != contexts.get(i)) {                
                    Element scopes = new Element("Contexts", ns);
                    current.appendChild(scopes);

                    for(String context : contexts.get(i)) {
                        child = new Element("Context", ns);
                        child.appendChild(context);
                        scopes.appendChild(child);
                    }
                }

                // So we can exclude optional components.
                if(null != operations.get(i)) {                
                    Element methods = new Element("Operations", ns);
                    current.appendChild(methods);

                    for(String method : operations.get(i)) {
                        child = new Element("Operation", ns);
                        child.appendChild(method);
                        methods.appendChild(child);
                    }
                }
            }
        }
        
        return root;
    }

    
}
