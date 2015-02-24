/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sifassociation.messaging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



/**
 * Class for translating Classic "Types" and WSA Actions.
 * 
 * SIF_Ack (if synchronous, no conversion required)
 *  SIF_Status (HTTP OK)   : http://www.sifassociation.org/contract/ZoneService-S11/2.x/Status
 *             (HTTP Post) : http://www.sifassociation.org/contract/QueueManagement-S11/2.x/GetMessageReturnStatus
 *  SIF_Error (HTTP Code) : http://www.w3.org/2005/08/addressing/soap/fault
 *            (HTTP Post) : http://www.sifassociation.org/contract/QueueManagement-S11/2.x/GetMessageReturnError
 *  
 * SIF_Event : http://www.sifassociation.org/contract/DataModel-S11/2.x/Event
 *  Add
 *  Change
 *  Delete
 * 
 * SIF_Provide : http://www.sifassociation.org/contract/Administrate_Provision-S11/2.x/Provide
 * 
 * SIF_Provision : http://www.sifassociation.org/contract/Administrate_Provision-S11/2.x/Provision
 * 
 * SIF_Register : http://www.sifassociation.org/contract/Administrate_Provision-S11/2.x/Register
 * 
 * SIF_Request
 *  SIF_Query : http://www.sifassociation.org/contract/DataModel-S11/2.x/Query
 *  SIF_ExtendedQuery : http://www.sifassociation.org/contract/DataModel-S11/2.x/ExtendedQuery
 * 
 * SIF_Response
 *  SIF_Error : http://www.sifassociation.org/contract/DataModel-S11/2.x/DataModelError
 *  SIF_ObjectData : http://www.sifassociation.org/contract/DataModel-S11/2.x/QueryResults
 *  SIF_ExtendedQueryResults : http://www.sifassociation.org/contract/DataModel-S11/2.x/ExtendedQueryResults
 * 
 * SIF_ServiceInput : http://www.sifassociation.org/contract/ZoneService-S11/2.x/ServiceInput
 * 
 * SIF_ServiceNotify : http://www.sifassociation.org/contract/ZoneService-S11/2.x/ServiceNotify
 * 
 * SIF_ServiceOutput
 *  SIF_Body : http://www.sifassociation.org/contract/ZoneService-S11/2.x/ServiceOutput
 *  SIF_Error : http://www.sifassociation.org/contract/ZoneService-S11/2.x/ZoneServiceError
 * 
 * SIF_Subscribe : http://www.sifassociation.org/contract/Administrate_Provision-S11/2.x/Subscribe
 * 
 * SIF_SystemControl
 *  SIF_CancelRequests : http://www.sifassociation.org/contract/DataModel-S11/2.x/CancelRequests
 *  SIF_CancelServiceInputs : http://www.sifassociation.org/contract/ZoneService-S11/2.x/CancelServiceInputs
 *  SIF_Ping : http://www.sifassociation.org/contract/FlowControl-S11/2.x/Ping
 *  SIF_Sleep : http://www.sifassociation.org/contract/FlowControl-S11/2.x/Sleep
 *  SIF_Wakeup : http://www.sifassociation.org/contract/FlowControl-S11/2.x/Wakeup
 *  SIF_GetMessage : http://www.sifassociation.org/contract/QueueManagement-S11/2.x/GetMessage
 *  SIF_GetZoneStatus : http://www.sifassociation.org/contract/Administrate_Provision-S11/2.x/GetZoneStatus
 *  SIF_GetAgentACL : http://www.sifassociation.org/contract/Administrate_Provision-S11/2.x/GetAgentACL
 * 
 * SIF_Unprovide : http://www.sifassociation.org/contract/Administrate_Provision-S11/2.x/Unprovide
 * 
 * SIF_Unregister : http://www.sifassociation.org/contract/Administrate_Provision-S11/2.x/Unregister
 * 
 * SIF_Unsubscribe : http://www.sifassociation.org/contract/Administrate_Provision-S11/2.x/Unsubscribe
 * 
 * @author jlovell
 * @version 3.0
 * @since 3.0
 */
public class SIFActionUtil {
    
    // So we can lookup SOAP Actions based on Clasic Types.
    static public String TypeToAction(String type, String extension) {
        // So we always have something to return.
        String action = "";

        // So we can leverage Java's built in fast lookup capabilities.
        String base = "http://www.sifassociation.org/contract/";
        Map<String, String> SACT = new HashMap<String, String>();
        SACT.put("SIF_Event", base + "DataModel-S11/2.x/Event");
        SACT.put("SIF_Provide", base + "Administrate_Provision-S11/2.x/Provide");
        SACT.put("SIF_Provision", base + 
                "Administrate_Provision-S11/2.x/Provision");
        SACT.put("SIF_Register", base + 
                "Administrate_Provision-S11/2.x/Register");
        SACT.put("SIF_Request&SIF_Query", base + "DataModel-S11/2.x/Query");
        SACT.put("SIF_Request&SIF_ExtendedQuery", base + 
                "DataModel-S11/2.x/ExtendedQuery");
        SACT.put("SIF_Response&SIF_Error", base + 
                "DataModel-S11/2.x/DataModelError");
        SACT.put("SIF_Response&SIF_ObjectData", base + 
                "DataModel-S11/2.x/QueryResults");
        SACT.put("SIF_Response&SIF_ExtendedQueryResults", base + 
                "DataModel-S11/2.x/ExtendedQueryResults");
        SACT.put("SIF_ServiceInput", base + "ZoneService-S11/2.x/ServiceInput");
        SACT.put("SIF_ServiceNotify", base + "ZoneService-S11/2.x/ServiceNotify");
        SACT.put("SIF_ServiceOutput&SIF_Body", base + 
                "ZoneService-S11/2.x/ServiceOutput");
        SACT.put("SIF_ServiceOutput&SIF_Error", base + 
                "ZoneService-S11/2.x/ZoneServiceError");
        SACT.put("SIF_Subscribe", base + 
                "Administrate_Provision-S11/2.x/Subscribe");
        SACT.put("SIF_SystemControl&SIF_CancelRequests", base + 
                "DataModel-S11/2.x/CancelRequests");
        SACT.put("SIF_SystemControl&SIF_CancelServiceInputs", base + 
                "ZoneService-S11/2.x/CancelServiceInputs");
        SACT.put("SIF_SystemControl&SIF_Ping", base + "FlowControl-S11/2.x/Ping");
        SACT.put("SIF_SystemControl&SIF_Sleep", base + 
                "FlowControl-S11/2.x/Sleep");
        SACT.put("SIF_SystemControl&SIF_Wakeup", base + 
                "FlowControl-S11/2.x/Wakeup");
        SACT.put("SIF_SystemControl&SIF_GetMessage", base + 
                "QueueManagement-S11/2.x/GetMessage");
        SACT.put("SIF_SystemControl&SIF_GetZoneStatus", base + 
                "Administrate_Provision-S11/2.x/GetZoneStatus");
        SACT.put("SIF_SystemControl&SIF_GetAgentACL", base + 
                "Administrate_Provision-S11/2.x/GetAgentACL");
        SACT.put("SIF_Unprovide", base + 
                "Administrate_Provision-S11/2.x/Unprovide");
        SACT.put("SIF_Unregister", base + 
                "Administrate_Provision-S11/2.x/Unregister");
        SACT.put("SIF_Unsubscribe", base + 
                "Administrate_Provision-S11/2.x/Unsubscribe");
        
        // So we can perform the lookup.
        String key = "";
        if(extension.isEmpty()) {
            key = type;
        }
        else {
            key = type + "&" + extension;
        }
        
        if(SACT.containsKey(key)) {
            action = (String)SACT.get(key);
        }
        
        return action;
    }
    
    // So we can lookup classic types based on SOAP actions.
    static public String ActionToType(String action) {
        // So we always have something to return.
        String type = "";
        
        // So we can leverage Java's built in fast lookup capabilities.
        String base = "http://www.sifassociation.org/contract/";
        Map<String, String> SACT = new HashMap<String, String>();
        SACT.put(base + "DataModel-S11/2.x/Event", "SIF_Event");
        SACT.put(base + "Administrate_Provision-S11/2.x/Provide", "SIF_Provide");
        SACT.put(base + "Administrate_Provision-S11/2.x/Provision", 
                "SIF_Provision");
        SACT.put(base + "Administrate_Provision-S11/2.x/Register", 
                "SIF_Register");
        SACT.put(base + "DataModel-S11/2.x/Query", "SIF_Request");
        SACT.put(base + "DataModel-S11/2.x/ExtendedQuery", "SIF_Request");
        SACT.put(base + "DataModel-S11/2.x/DataModelError", "SIF_Response");
        SACT.put(base + "DataModel-S11/2.x/QueryResults", "SIF_Response");
        SACT.put(base + "DataModel-S11/2.x/ExtendedQueryResults", "SIF_Response");
        SACT.put(base + "ZoneService-S11/2.x/ServiceInput", "SIF_ServiceInput");
        SACT.put(base + "ZoneService-S11/2.x/ServiceNotify", "SIF_ServiceNotify");
        SACT.put(base + "ZoneService-S11/2.x/ServiceOutput", "SIF_ServiceOutput");
        SACT.put(base + "ZoneService-S11/2.x/ZoneServiceError", 
                "SIF_ServiceOutput");
        SACT.put(base + "Administrate_Provision-S11/2.x/Subscribe", 
                "SIF_Subscribe");
        SACT.put(base + "DataModel-S11/2.x/CancelRequests", "SIF_SystemControl");
        SACT.put(base + "ZoneService-S11/2.x/CancelServiceInputs", 
                "SIF_SystemControl");
        SACT.put(base + "FlowControl-S11/2.x/Ping", "SIF_SystemControl");
        SACT.put(base + "FlowControl-S11/2.x/Sleep", "SIF_SystemControl");
        SACT.put(base + "FlowControl-S11/2.x/Wakeup", "SIF_SystemControl");
        SACT.put(base + "QueueManagement-S11/2.x/GetMessage", 
                "SIF_SystemControl");
        SACT.put(base + "Administrate_Provision-S11/2.x/GetZoneStatus", 
                "SIF_SystemControl");
        SACT.put(base + "Administrate_Provision-S11/2.x/GetAgentACL", 
                "SIF_SystemControl");
        SACT.put(base + "Administrate_Provision-S11/2.x/Unprovide", 
                "SIF_Unprovide");
        SACT.put(base + "Administrate_Provision-S11/2.x/Unregister", 
                "SIF_Unregister");
        SACT.put(base + "Administrate_Provision-S11/2.x/Unsubscribe", 
                "SIF_Unsubscribe");
        
        // So we can perform the lookup.
        if(SACT.containsKey(action)) {
            type = (String)SACT.get(action);
        }
        
        return type;
    }
    
    // To Do:  ActionToExtension
}