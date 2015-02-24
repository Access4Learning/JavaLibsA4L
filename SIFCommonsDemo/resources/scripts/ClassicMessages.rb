require 'java'

include_class 'org.sifassociation.testing.IScriptHooks'

import org.springframework.context.ApplicationContext
import org.springframework.context.support.FileSystemXmlApplicationContext

import org.sifassociation.messaging.SIF2MessageXML

# Loads all classic SIF messages at the source, coverts, and reloads.
# Note:  Acks are loaded and serialized but not converted and reloaded.
class ClassicMessages
    def run
        # So we work with all the messages at our disposal.
        source = "resources/messages/HTTP/"
        filter = /^.*\.(x|X)(m|M)(l|L)$/
        Dir.foreach(source) {
        |f|
        if filter =~ f then
            # Parse and convert a message from the filesystem.
            file = File.open(source + f, "r")
            contents = file.read
            
            msg = SIF2MessageXML.new()
            msg.parse(contents)
            puts msg.toString()
            
            # Since acknowledgements are transport specific.
            if "SIF_Ack" != msg.getType() then
                msg.setTransport("SOAP")
                temp = msg.toString() + "\n"
                puts temp
                
                msg = SIF2MessageXML.new()
                msg.parse(temp)
                msg.setTransport("HTTP")
                puts msg.toString() + "\n"
            end
        end
        }
    end
end

ClassicMessages.new