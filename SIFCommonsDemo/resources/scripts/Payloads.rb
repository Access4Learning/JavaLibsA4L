require 'java'

include_class 'org.sifassociation.testing.IScriptHooks'

import org.sifassociation.messaging.SIF2Payloads
import org.sifassociation.messaging.SIFVersion
import org.sifassociation.messaging.SIF2MessageXML
import org.sifassociation.messaging.SIFRefId
import java.net.URL

# Create example payloads.
class Payloads
    def run
        
        # Information we use to create both the register payload and message.
        name = "John's SIF Agent"
        infrastructureVersion = SIFVersion.new("2.3")
        
        # So we can register our agent.
        registerPayload = SIF2Payloads.createRegister(
            name,
            infrastructureVersion,
            infrastructureVersion,
            infrastructureVersion,
            16777216,
            "Pull",
            URL.new("http://localhost:8080/SIFAgent/Agents/23"),
            "SIF Association",
            "0.1",
            "SIF Association",
            name,
            "0.1",
            nil)
        
        puts registerPayload
        puts "\n"
        
        # So we can send our registration information.
        registerMessage = SIF2MessageXML.new()
        registerMessage.setType("Register")
        registerMessage.setMessageId(SIFRefId.new("192.168.1.104", 127))
        registerMessage.setInfrastructureVersion(infrastructureVersion)
        registerMessage.setSourceId(name)
        registerMessage.setTransport("HTTP")  # So we test conversion.
        registerMessage.setPayload(registerPayload)
        
        puts registerMessage.toString()
        puts "\n"

        registerMessage.setTransport("SOAP")
        
        puts registerMessage.toString()
        puts "\n"

#=begin        
        # So our agent can pull messages.
        getMessagePayload = SIF2Payloads.createGetMessage()
        
        # So we can send our request for the next message.
        getMessageMessage = SIF2MessageXML.new()
        getMessageMessage.setType("SystemControl")
        getMessageMessage.setMessageId(SIFRefId.new("192.168.1.104", 127))
        getMessageMessage.setInfrastructureVersion(infrastructureVersion)
        getMessageMessage.setSourceId("Vince's Super Agent")
        getMessageMessage.setTransport("HTTP")
        getMessageMessage.setPayload(getMessagePayload)
        
        puts getMessageMessage.toString()
        puts "\n"

        getMessageMessage.setTransport("SOAP")
        
        puts getMessageMessage.toString()
        puts "\n"

        
        # So our agent can release a pulled message from the queue.
        statusPayload = SIF2Payloads.createStatus(1, "Discard the referenced message.")
        
        # So we can let the middleware know the message has been received.
        ackMessage = SIF2MessageXML.new()
        ackMessage.ack(getMessageMessage)
        ackMessage.setMessageId(SIFRefId.new("192.168.1.104", 127))
        ackMessage.setInfrastructureVersion(infrastructureVersion)
        ackMessage.setSourceId(name)
        ackMessage.setTransport("HTTP")
        ackMessage.setPayload(statusPayload)
        
        puts ackMessage.toString()
        puts "\n"
        
        ackMessage.setTransport("SOAP")
        
        puts ackMessage.toString()
        puts "\n"
        
        errorPayload = SIF2Payloads.createError("DataModelError", 1, 4, "XML Validation",
            "Invalid value for element/attribute.")

        ackMessage.setTransport("HTTP")
        ackMessage.setPayload(errorPayload)
        
        puts ackMessage.toString()
        puts "\n"
        
        ackMessage.setTransport("SOAP")
        
        puts ackMessage.toString()
        puts "\n"
        
        # So our agent can cancel requests.
        refOne = SIFRefId.new("192.168.1.104", 127)
        refOne.setGeneric(false)
        refTwo = SIFRefId.new("192.168.1.104", 127)
        refTwo.setGeneric(false)
        cancelRequestsPayload = SIF2Payloads.createCancelRequests("Standard", 
            [refOne, refTwo])
        
        # So we can send our message.
        cancelRequestsMessage = SIF2MessageXML.new()
        cancelRequestsMessage.setType("SystemControl")
        cancelRequestsMessage.setMessageId(SIFRefId.new("192.168.1.104", 127))
        cancelRequestsMessage.setInfrastructureVersion(infrastructureVersion)
        cancelRequestsMessage.setSourceId(name)
        cancelRequestsMessage.setTransport("HTTP")
        cancelRequestsMessage.setPayload(cancelRequestsPayload)
        
        puts cancelRequestsMessage.toString()
        puts "\n"
        
        cancelRequestsMessage.setTransport("SOAP")
        
        puts cancelRequestsMessage.toString()
        puts "\n"

=begin
        
        # So our agent can cancel zone service calls.
        refOne = SIFRefId.new("192.168.1.104", 127)
        refOne.setGeneric(false)
        refTwo = SIFRefId.new("192.168.1.104", 127)
        refTwo.setGeneric(false)
        cancelInputsPayload = SIF2Payloads.createCancelServiceInputs("Standard", 
            [refOne, refTwo])
        
        # So we can send our message.
        cancelInputsMessage = SIF2MessageXML.new()
        cancelInputsMessage.setType("SystemControl")
        cancelInputsMessage.setMessageId(SIFRefId.new("192.168.1.104", 127))
        cancelInputsMessage.setInfrastructureVersion(infrastructureVersion)
        cancelInputsMessage.setSourceId(name)
        cancelInputsMessage.setTransport("HTTP")
        cancelInputsMessage.setPayload(cancelInputsPayload)
        
        puts cancelInputsMessage.toString()
        puts "\n"

        # So our agent can retrieve its privacy settings synchronously.
        getACLPayload = SIF2Payloads.createGetAgentACL()
        
        # So we can send our request for the privacy settings.
        getACLMessage = SIF2MessageXML.new()
        getACLMessage.setType("SystemControl")
        getACLMessage.setMessageId(SIFRefId.new("192.168.1.104", 127))
        getACLMessage.setInfrastructureVersion(infrastructureVersion)
        getACLMessage.setSourceId(name)
        getACLMessage.setTransport("HTTP")
        getACLMessage.setPayload(getACLPayload)
        
        puts getACLMessage.toString()
        puts "\n"

        # So our agent can retrieve the status of the zone synchronously.
        getStatusPayload = SIF2Payloads.createGetZoneStatus()
        
        # So we can send our request for the privacy settings.
        getStatusMessage = SIF2MessageXML.new()
        getStatusMessage.setType("SystemControl")
        getStatusMessage.setMessageId(SIFRefId.new("192.168.1.104", 127))
        getStatusMessage.setInfrastructureVersion(infrastructureVersion)
        getStatusMessage.setSourceId(name)
        getStatusMessage.setTransport("HTTP")
        getStatusMessage.setPayload(getStatusPayload)
        
        puts getStatusMessage.toString()
        puts "\n"

        # So our agent can retrieve the status of the zone synchronously.
        getStatusPayload = SIF2Payloads.createGetZoneStatus()
        
        # So we can send our request for the status.
        getStatusMessage = SIF2MessageXML.new()
        getStatusMessage.setType("SystemControl")
        getStatusMessage.setMessageId(SIFRefId.new("192.168.1.104", 127))
        getStatusMessage.setInfrastructureVersion(infrastructureVersion)
        getStatusMessage.setSourceId(name)
        getStatusMessage.setTransport("HTTP")
        getStatusMessage.setPayload(getStatusPayload)
        
        puts getStatusMessage.toString()
        puts "\n"
        
        # So our agent can check on the ZIS.
        pingPayload = SIF2Payloads.createPing()
        
        # So we can send our request for the status.
        pingMessage = SIF2MessageXML.new()
        pingMessage.setType("SystemControl")
        pingMessage.setMessageId(SIFRefId.new("192.168.1.104", 127))
        pingMessage.setInfrastructureVersion(infrastructureVersion)
        pingMessage.setSourceId(name)
        pingMessage.setTransport("HTTP")
        pingMessage.setPayload(pingPayload)
        
        puts pingMessage.toString()
        puts "\n"
        
        # So our agent can provide objects (without provision).
        providePayload = SIF2Payloads.createProvide(
            ["StudentPersonal", "StudentSchoolEnrollment"],
            [true, false],
            [["SIF_Default", "WorkStudy"],["SIF_Default"]])
        
        # So we can send our abilities to provide.
        provideMessage = SIF2MessageXML.new()
        provideMessage.setType("SystemControl")
        provideMessage.setMessageId(SIFRefId.new("192.168.1.104", 127))
        provideMessage.setInfrastructureVersion(infrastructureVersion)
        provideMessage.setSourceId(name)
        provideMessage.setTransport("HTTP")
        provideMessage.setPayload(providePayload)
        
        puts provideMessage.toString()
        puts "\n"
        
        # So our agent can provide objects (without provision).
        provisionPayload = SIF2Payloads.createProvision(
            # Provide
            ["StudentPersonal", "StudentSchoolEnrollment"],
            [true, false],
            [["SIF_Default", "WorkStudy"],["SIF_Default"]],
            # Subscribe
            ["StudentPersonal", "StudentSchoolEnrollment"],
            [["SIF_Default", "WorkStudy"],["SIF_Default"]],
            # Publish Add
            ["StudentPersonal", "StudentSchoolEnrollment"],
            [["SIF_Default", "WorkStudy"],["SIF_Default"]],
            # Publish Change
            ["StudentPersonal", "StudentSchoolEnrollment"],
            [["SIF_Default", "WorkStudy"],["SIF_Default"]],
            # Publish Delete
            ["StudentPersonal", "StudentSchoolEnrollment"],
            [["SIF_Default", "WorkStudy"],["SIF_Default"]],
            # Request
            ["StudentPersonal", "StudentSchoolEnrollment"],
            [true, false],
            [["SIF_Default", "WorkStudy"],["SIF_Default"]],
            # Respond
            ["StudentPersonal", "StudentSchoolEnrollment"],
            [true, false],
            [["SIF_Default", "WorkStudy"],["SIF_Default"]],
            # Provide Service
            ["serviceAssessmentProcessing"],
            [["SIF_Default"]],
            # Respond Service
            ["serviceAssessmentProcessing"],
            [["SIF_Default"]],
            # Request Service
            ["serviceAssessmentAdministration"],
            [["SIF_Default"]],
            [["ItemsScored"]],
            # Subscribe Service
            ["serviceAssessmentAdministration"],
            [["SIF_Default"]],
            [["ItemsScored"]])
        
        # So we can send our abilities to provide.
        provisionMessage = SIF2MessageXML.new()
        provisionMessage.setType("SystemControl")
        provisionMessage.setMessageId(SIFRefId.new("192.168.1.104", 127))
        provisionMessage.setInfrastructureVersion(infrastructureVersion)
        provisionMessage.setSourceId(name)
        provisionMessage.setTransport("HTTP")
        provisionMessage.setPayload(provisionPayload)
        
        puts provisionMessage.toString()
        puts "\n"

        # So we can tell the ZIS we are going temporarily offline.
        sleepPayload = SIF2Payloads.createSleep()
        
        # So we can send our our offline status.
        sleepMessage = SIF2MessageXML.new()
        sleepMessage.setType("SystemControl")
        sleepMessage.setMessageId(SIFRefId.new("192.168.1.104", 127))
        sleepMessage.setInfrastructureVersion(infrastructureVersion)
        sleepMessage.setSourceId(name)
        sleepMessage.setTransport("HTTP")
        sleepMessage.setPayload(sleepPayload)
        
        puts sleepMessage.toString()
        puts "\n"

        # So our agent can subscribe to objects (without provision).
        subscribePayload = SIF2Payloads.createSubscribe(
            ["StudentPersonal", "StudentSchoolEnrollment"],
            [["SIF_Default", "WorkStudy"],["SIF_Default"]])
        
        # So we can send our desired subscriptions.
        subscribeMessage = SIF2MessageXML.new()
        subscribeMessage.setType("SystemControl")
        subscribeMessage.setMessageId(SIFRefId.new("192.168.1.104", 127))
        subscribeMessage.setInfrastructureVersion(infrastructureVersion)
        subscribeMessage.setSourceId(name)
        subscribeMessage.setTransport("HTTP")
        subscribeMessage.setPayload(subscribePayload)
        
        puts subscribeMessage.toString()
        puts "\n"

        # So our agent can stop providing objects.
        unprovidePayload = SIF2Payloads.createUnprovide(
            ["StudentPersonal", "StudentSchoolEnrollment"],
            [["SIF_Default", "WorkStudy"],["SIF_Default"]])
        
        # So we can send our new settings.
        unprovideMessage = SIF2MessageXML.new()
        unprovideMessage.setType("SystemControl")
        unprovideMessage.setMessageId(SIFRefId.new("192.168.1.104", 127))
        unprovideMessage.setInfrastructureVersion(infrastructureVersion)
        unprovideMessage.setSourceId(name)
        unprovideMessage.setTransport("HTTP")
        unprovideMessage.setPayload(unprovidePayload)
        
        puts unprovideMessage.toString()
        puts "\n"

        # So we can tell the ZIS we are going away.
        unregisterPayload = SIF2Payloads.createUnregister()
        
        # So we can send our our absent status.
        unregisterMessage = SIF2MessageXML.new()
        unregisterMessage.setType("SystemControl")
        unregisterMessage.setMessageId(SIFRefId.new("192.168.1.104", 127))
        unregisterMessage.setInfrastructureVersion(infrastructureVersion)
        unregisterMessage.setSourceId(name)
        unregisterMessage.setTransport("HTTP")
        unregisterMessage.setPayload(unregisterPayload)
        
        puts unregisterMessage.toString()
        puts "\n"

        # So our agent can stop receiving events for objects.
        unsubscribePayload = SIF2Payloads.createUnsubscribe(
            ["StudentPersonal", "StudentSchoolEnrollment"],
            [["SIF_Default", "WorkStudy"],["SIF_Default"]])
        
        # So we can send our new settings.
        unsubscribeMessage = SIF2MessageXML.new()
        unsubscribeMessage.setType("SystemControl")
        unsubscribeMessage.setMessageId(SIFRefId.new("192.168.1.104", 127))
        unsubscribeMessage.setInfrastructureVersion(infrastructureVersion)
        unsubscribeMessage.setSourceId(name)
        unsubscribeMessage.setTransport("HTTP")
        unsubscribeMessage.setPayload(unsubscribePayload)
        
        puts unsubscribeMessage.toString()
        puts "\n"

        # So we can tell the we are back online.
        wakeupPayload = SIF2Payloads.createWakeup()
        
        # So we can send our our online status.
        wakeupMessage = SIF2MessageXML.new()
        wakeupMessage.setType("SystemControl")
        wakeupMessage.setMessageId(SIFRefId.new("192.168.1.104", 127))
        wakeupMessage.setInfrastructureVersion(infrastructureVersion)
        wakeupMessage.setSourceId(name)
        wakeupMessage.setTransport("HTTP")
        wakeupMessage.setPayload(wakeupPayload)
        
        puts wakeupMessage.toString()
        puts "\n"
=end
    end
end

Payloads.new