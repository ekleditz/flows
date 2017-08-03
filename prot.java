importPackage(java.util);
importPackage(java.lang);
importPackage(java.io);
importPackage(com.cloupia.lib.util);
importPackage(org.apache.commons.httpclient);
importPackage(org.apache.commons.httpclient.cookie);
importPackage(org.apache.commons.httpclient.methods);
importPackage(org.apache.commons.httpclient.auth);
importPackage(org.apache.commons.httpclient.protocol);
importClass(org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory);
importPackage(com.cloupia.lib.cIaaS.vcd.api);

/****************************************************************************************/
/**                                                                                    **/
/**                           !!! IMPORTANT NOTE !!!                                   **/
/**                                                                                    **/
/**                                                                                    **/
/**         THIS SCRIPT REQUIRES A MINIMUM UCS DIRECTOR VERSION OF 5.4.0.0             **/
/**                                                                                    **/
/****************************************************************************************/

//----------------------------------------------------------------------------------------
//
//        Author: Russ Whitear (rwhitear@cisco.com)
//
// Workflow Task Name: HTTP_Function_v3_5.4.0.0
//
//            Version: 3.0
//
//            Updates: SSL Socket Factory modified for JDK8.
//
//      Modifications: Script now retrieves the network comment field from within the
//                     Infoblox token message response. 
//
//             Inputs: none.
//
//            Outputs: none.
//
//----------------------------------------------------------------------------------------


//----------------------------------------------------------------------------------------
//                                 ### FUNCTIONS ###
//----------------------------------------------------------------------------------------

//----------------------------------------------------------------------------------------
//
//        Author: Russ Whitear (rwhitear@cisco.com)
//
// Function Name: httpRequest()
//
//       Version: 3.0
//
// Modifications: Added HTTP header Connection:close to execute method to overcome the
//                CLOSE_WAIT issue caused with releaseConnection().
//
//                Modified SSL socket factory code to work with UCS Director 5.4.0.0.
//
//   Description: HTTP Request function - httpRequest.
//
//                I have made the httpClient functionality more object like in order to 
//                make cloupia scripts more readable when making many/multiple HTTP/HTTPS 
//                requests within a single script. 
//
//      Usage: 1. var request = new httpRequest();                   // Create new object.
//                
//             2. request.setup("192.168.10.10","https","admin","cisco123");      // SSL.
//          or:   request.setup("192.168.10.10","http","admin","cisco123");       // HTTP.
//          or:   request.setup("192.168.10.10","https");           // SSL, no basicAuth.
//          or:   request.setup("192.168.10.10","http");            // HTTP, no basicAuth.
//
//             3. request.getRequest("/");                    // HTTP GET (URI). 
//          or:   request.postRequest("/","some body text");  // HTTP POST (URI,BodyText). 
//          or:   request.deleteRequest("/");                 // HTTP DELETE (URI). 
//
//  (optional) 4. request.contentType("json");            // Add Content-Type HTTP header.
//          or:   request.contentType("xml"); 
//
//  (optional) 5. request.addHeader("X-Cloupia-Request-Key","1234567890");  // Any Header.
//
//             6. var statusCode = request.execute();                     // Send request.
//
//             7. var response = request.getResponse("asString");   // Response as string.
//          or:   var response = request.getResponse("asStream");   // Response as stream.
//
//             8. request.disconnect();                             // Release connection.
//
//
//          Note: Be sure to add these lines to the top of your script:
//
//          importPackage(java.util);
//          importPackage(com.cloupia.lib.util);
//          importPackage(org.apache.commons.httpclient);
//          importPackage(org.apache.commons.httpclient.cookie);
//          importPackage(org.apache.commons.httpclient.methods);
//          importPackage(org.apache.commons.httpclient.auth);
//          importPackage(org.apache.commons.httpclient.protocol);
//          importClass(org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory);
//          importPackage(com.cloupia.lib.cIaaS.vcd.api);
//
//----------------------------------------------------------------------------------------

var httpRequest = function () {};

httpRequest.prototype.setup = function(serverIp, transport, username, password) {
    this.serverIp = serverIp;
    this.transport = transport;
    this.username = username;
    this.password = password;
    
    
    this.httpClient = new HttpClient();
    
    // Decide whether to create an HTTP or HTTPS connection based up 'transport'.
    if( this.transport == "https" ) {   
		this.httpClient = CustomEasySSLSocketFactory.getIgnoreSSLClient(this.serverIp, 443);
		this.httpClient.getParams().setCookiePolicy("default");
    } else {
        // Create new HTTP connection.
        this.httpClient.getHostConfiguration().setHost(this.serverIp, 80, "http");       
    }
    
    this.httpClient.getParams().setCookiePolicy("default");
    
    // If username and password supplied, then use basicAuth.
    if( this.username && this.password ) {
        this.httpClient.getParams().setAuthenticationPreemptive(true);
        this.defaultcreds = new UsernamePasswordCredentials(this.username, this.password);
        this.httpClient.getState().setCredentials(new AuthScope(this.serverIp, -1, null), this.defaultcreds);
    }
};

httpRequest.prototype.contentType = function(contentType) {
    this.contentType = contentType;
    
    this.contentTypes = [
        ["xml","application/xml"],
        ["json","application/json"]
    ];
    
    for( this.i=0; this.i<this.contentTypes.length; this.i++) 
        if(this.contentTypes[this.i][0] == this.contentType)
            this.httpMethod.addRequestHeader("Content-Type", this.contentTypes[this.i][1]);
};

httpRequest.prototype.addHeader = function(headerName,headerValue) {
    this.headerName = headerName;
    this.headerValue = headerValue;
    
    this.httpMethod.addRequestHeader(this.headerName, this.headerValue);
};


httpRequest.prototype.acceptType = function(acceptType) {
    this.acceptType = acceptType;
    
    this.acceptTypes = [
        ["xml","application/xml"],
        ["json","application/json"]
    ];
    
    for( this.i=0; this.i<this.acceptTypes.length; this.i++) 
        if(this.acceptTypes[this.i][0] == this.acceptType)
            this.httpMethod.addRequestHeader("Accept", this.acceptTypes[this.i][1]);
};


httpRequest.prototype.execute = function() {    
    // Connection:close is hard coded here in order to ensure that the TCP connection
    // gets torn down immediately after the request. Comment this line out if you wish to
    // experiment with HTTP persistence.
    this.httpMethod.addRequestHeader("Connection", "close");
    
    this.httpClient.executeMethod(this.httpMethod);
    
    // Retrieve status code.
    this.statusCode = this.httpMethod.getStatusCode();
   
    return this.statusCode;
};

httpRequest.prototype.getResponseHeader = function(headerName) {
    //Using this to pull the Header value for Location, edit for your values
    //this.headerName = this.httpMethod.getResponseHeader("Location");
    this.headerName = this.httpMethod.getResponseHeader("Set-Cookie");
    return this.headerName;
};

//httpRequest.prototype.getCookieHeader = function(headerName) {
    //Using this to pull the Header value for Location, edit for your values
//    this.headerName = this.httpMethod.getResponseHeader("Set-Cookie");
//    return this.cookieName;
//};

httpRequest.prototype.getRequest = function(uri) {
    this.uri = uri;

    // Get request.
    this.httpMethod = new GetMethod(this.uri);
};

httpRequest.prototype.postRequest = function(uri,bodytext) {
    this.uri = uri;
    this.bodytext = bodytext;
    
    // POST Request.
    this.httpMethod = new PostMethod(this.uri);
    this.httpMethod.setRequestEntity(new StringRequestEntity(this.bodytext));
};

httpRequest.prototype.getResponse = function(asType) {
    this.asType = asType;

    if( this.asType == "asStream" )
        return this.httpMethod.getResponseBodyAsStream();
    else
        return this.httpMethod.getResponseBodyAsString();
};

httpRequest.prototype.deleteRequest = function(uri) {
    this.uri = uri;

    // Get request.
    this.httpMethod = new DeleteMethod(this.uri);
};


httpRequest.prototype.disconnect = function() {
    // Release connection.
    this.httpMethod.releaseConnection();
};

//
//  httpRequest function end.
//



//
// main();
//

var Proteus = input.Proteus_Name;
var un = input.APIUser;
var pw = input.APIPassword;
var ip =input.IPAddress;

//logger.addInfo("prot " +Proteus); 
//logger.addInfo("un: " +un); 
//logger.addInfo("pw: " +pw); 

// **************************LOGIN******************************
var request = new httpRequest();

//request.setup("10.52.249.120","http","admin","cisco123");

// Below is for test
// request.setup("10.83.163.4","http");
// var jsonBody ='<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:api="http://api.proteus.bluecatnetworks.com">[\r][\n]" "   <soapenv:Header/>[\r][\n]" "   <soapenv:Body>[\r][\n]" "      <api:login>[\r][\n]" "         <username>ucsdapi2bc</username>[\r][\n]" "         <password>t3mpl@b</password>[\r][\n]" "      </api:login>[\r][\n]" "   </soapenv:Body>[\r][\n]" "</soapenv:Envelope>'; 

//below is for prod

request.setup(""+Proteus, "https");
var jsonBody ='<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:api="http://api.proteus.bluecatnetworks.com">   <soapenv:Header/>   <soapenv:Body>      <api:login>         <username>'+un+'</username>         <password>'+pw+'</password>      </api:login>   </soapenv:Body></soapenv:Envelope>';

//logger.addInfo("SOAP: " +jsonBody); 

request.postRequest('/Services/API', jsonBody);
request.addHeader("Accept-Encoding", "gzip, deflate");
request.addHeader("Content-Type", "text/xml;charset=UTF-8");
request.addHeader("SOAPAction", "");

var statusCode = request.execute();

logger.addInfo("Request 1 status code: " +statusCode);
var response = request.getResponse("asString");
//logger.addInfo("Response2: " +response);   

if (statusCode != 200)
    {   
        logger.addError("Request failed. HTTP response code: "+statusCode);
     
        var response = request.getResponse("asString");
        logger.addInfo("Response2: " +response); 
        request.disconnect();  
    
        ctxt.setFailed("Request failed.");
    } else { 
           
       var cookieGet = request.getResponseHeader();
       var cookieStr = cookieGet.toString();
       var cookieSplit = cookieStr.split(";");
       var cookieClean1 = cookieSplit[0];
       var cookieClean1 = cookieClean1.toString();
       var cookieUse = cookieClean1.match(/\s(.*)/);
       var cookieUse = cookieUse.toString();
       var cookieUse1 = cookieUse.split(",");
       var cookieUseable = cookieUse1[0];
       //var cookieUseable = cookieUseable.toString();
       logger.addInfo("Cookie: " +cookieUseable);   
       
       request.disconnect(); 
     }

request.disconnect();

//*******************END LOGIN***************** -> Pass cookie into forward

//****************Delete the A record*************
var request = new httpRequest();

//request.setup("10.52.249.120","http","admin","cisco123");
// below is for test
//request.setup("10.83.163.4","http");
//below is for prod
request.setup(""+Proteus, "https");

var jsonBody ='<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:api="http://api.proteus.bluecatnetworks.com"> <soapenv:Header/> <soapenv:Body> <api:deleteDeviceInstance> <configName>PGE Corporate</configName> <identifier>'+ip+'</identifier> <options></options> </api:deleteDeviceInstance> </soapenv:Body> </soapenv:Envelope>'; 

//logger.addInfo("SOAP: " +jsonBody); 

request.postRequest('/Services/API', jsonBody);
request.addHeader("Accept-Encoding", "gzip, deflate");
request.addHeader("Content-Type", "text/xml;charset=UTF-8");
request.addHeader("SOAPAction", "");
request.addHeader("Cookie", cookieUseable);
request.addHeader("Cookie2", "$Version=1");

var statusCode = request.execute();

logger.addInfo("Request 1 status code: " +statusCode);
var response = request.getResponse("asString");
//logger.addInfo("Response2: " +response);   

if (statusCode != 200)
{   
    logger.addError("Request failed. HTTP response code: "+statusCode);
 
    var response = request.getResponse("asString");
    logger.addInfo("Response2: " +response); 
    request.disconnect();  

    ctxt.setFailed("Request failed.");
} else { 
	  
	 var response = request.getResponse("asString");
   //logger.addInfo("LogoutResponse: " +response);  
   //  var tempIp = response.match(/ip=(.*)netmask/);
   //  tempIp = tempIp.toString();
   //  var Ip = tempIp.split(",");
    // var Ip = Ip[1];
    // var Ip = Ip.slice(0,-1);   
    // logger.addInfo("IP: " +Ip); 
    // output.IPAddress = Ip;
     request.disconnect(); 
 }

request.disconnect();

//*************END A Record Deletion***********

//*******************LOGOUT********************
var request = new httpRequest();

//request.setup("10.52.249.120","http","admin","cisco123");
//below is for test
//request.setup("10.83.163.4","http");
//below is for prod
request.setup(""+Proteus, "https");

var jsonBody ='<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:api="http://api.proteus.bluecatnetworks.com"> <soapenv:Header/> <soapenv:Body> <api:logout/> </soapenv:Body> </soapenv:Envelope>'; 

request.postRequest('/Services/API', jsonBody);
request.addHeader("Accept-Encoding", "gzip, deflate");
request.addHeader("Content-Type", "text/xml;charset=UTF-8");
request.addHeader("SOAPAction", "");
request.addHeader("Cookie", cookieUseable);
request.addHeader("Cookie2", "$Version=1");

var statusCode = request.execute();

logger.addInfo("Request 1 status code: " +statusCode);
var response = request.getResponse("asString");
//logger.addInfo("Response2: " +response);   

if (statusCode != 200)
  {   
      logger.addError("Request failed. HTTP response code: "+statusCode);
   
      var response = request.getResponse("asString");
      //logger.addInfo("Response2: " +response); 
      request.disconnect();  
  
      ctxt.setFailed("Request failed.");
  } else { 
	  
	 var response = request.getResponse("asString");
     //logger.addInfo("LogoutResponse: " +response);  
     request.disconnect(); 
   }

request.disconnect();

//*****************END LOGOUT******************
