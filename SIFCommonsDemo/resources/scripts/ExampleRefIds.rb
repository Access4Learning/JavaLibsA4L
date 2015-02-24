require 'java'

include_class 'org.sifassociation.testing.IScriptHooks'

import org.sifassociation.messaging.SIFRefId

# Create example IDs.
class ExampleIds
    def run
        for i in 1..10 do
            puts SIFRefId.new("192.168.1.135", i).toString()
        end
    end
end

ExampleIds.new