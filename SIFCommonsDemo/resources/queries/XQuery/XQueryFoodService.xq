declare default element namespace "http://namespaces.sifinfo.org/compliance/csq";
for $csq in /CombinedCSQ
where $csq/ObjectMatrix/Object[
                matches(@name, 'Foodservice.+') and 
                @processesAdds = "true" and 
                @publishesAdds = "false"]
return $csq/CertificationAuthorityData/ProductInformation/Product
