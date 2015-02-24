require 'java'

include_class 'org.sifassociation.testing.IScriptHooks'

import org.sifassociation.messaging.SIF2Payloads
import org.sifassociation.messaging.SIFVersion
import org.sifassociation.messaging.SIF2MessageXML
import org.sifassociation.messaging.SIFRefId
import org.sifassociation.util.SIFXOMUtil
import Java::nu.xom.Builder
import Java::nu.xom.Node
import Java::nu.xom.Document
import Java::nu.xom.Element
import Java::nu.xom.Attribute
import java.net.URL
 import java.util.ArrayList

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
 
        # So we can test editing a XOM tree.
        parser = Builder.new()
        doc = parser.build(registerPayload, nil)
        root = doc.getRootElement()
 
        limits = ArrayList.new()
 
        SIFXOMUtil.editValue(
            root, 
            "Name", 
            "http://www.sifassociation.org/message/soap/2.x", 
            "This SIF Agent belongs to Vince",
            limits)

        registerPayload = root.toXML();
            
        puts registerPayload
        puts "\n"        

        SIFXOMUtil.editValue(
            root, 
            "Name", 
            "http://www.sifassociation.org/message/soap/2.x", 
            "Stollen by Ron",
            limits)

        registerPayload = root.toXML();
            
        puts registerPayload
        puts "\n"
        
        # So we have an attribute to modify.
        change = Attribute.new("change", "me")
        root.addAttribute(change)

        registerPayload = root.toXML();
            
        puts registerPayload
        puts "\n"
        
        SIFXOMUtil.editAttribute(
            root, 
            "Body", 
            "http://schemas.xmlsoap.org/soap/envelope/",
            "change", 
            "you",
            limits)
        
        registerPayload = root.toXML();
            
        puts registerPayload
        puts "\n"

    end
end

Payloads.new