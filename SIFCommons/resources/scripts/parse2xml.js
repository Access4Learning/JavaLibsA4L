// Added by John W. Lovell/2016 for Java invocation from JavaX.

// Parse and pass to Goessner's function.
function parse2xml(json, tab) {
  return json2xml(eval('json='+json), tab);
}
