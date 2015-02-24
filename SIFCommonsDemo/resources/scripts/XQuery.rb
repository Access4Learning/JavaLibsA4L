require 'java'

include_class 'org.sifassociation.testing.IScriptHooks'

import org.springframework.context.ApplicationContext
import org.springframework.context.support.FileSystemXmlApplicationContext

# Loads an XQuery, runs it, and prints the results.
class XQuery
    def run
        # Load the XQuery for an object.
        file = File.open("resources/queries/XQuery/AllStudentPersonals.xq", "r")
        query = file.read
        puts query + "\n\n"
        
        # Load the eXist collection via Spring!
        ctx = FileSystemXmlApplicationContext.new("SIFCommonsDemo.xml")
        engine = ctx.getBean("US26Collection")
        
        # Run the query against the collection.
        engine.run(query)
        
        # Print all of the results.
        begin
            result = engine.getNext()
            puts result
        end while "" != result
    end
end

XQuery.new