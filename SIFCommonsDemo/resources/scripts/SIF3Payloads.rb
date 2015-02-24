require 'java'

include_class 'org.sifassociation.testing.IScriptHooks'

import org.sifassociation.messaging.SIF3Payloads
import org.sifassociation.messaging.SIFVersion
#import org.sifassociation.messaging.SIF2MessageXML
import org.sifassociation.messaging.SIFRefId
import java.net.URL

# Create example payloads.
class Payloads
    def run

        environmentPayload = SIF3Payloads.createEnvironment(
          "testSolution",
          "Basic",
          "",
          "guest",
          "SIF_Student_Flicker",
          SIFVersion.new("3.0"),
          "SIF-US",
          SIFVersion.new("3.0"),
          "REST",
          "SIF Association",
          "Student Flicker",
          "3.0")

        puts environmentPayload
        puts "\n"

    end
end

Payloads.new