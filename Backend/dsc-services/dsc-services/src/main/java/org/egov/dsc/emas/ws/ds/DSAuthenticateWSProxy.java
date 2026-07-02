package org.egov.dsc.emas.ws.ds;

import java.rmi.RemoteException;

public class DSAuthenticateWSProxy implements DSAuthenticateWS {
  private String _endpoint = null;
  private DSAuthenticateWS dSAuthenticateWS = null;
  
  public DSAuthenticateWSProxy() {
    _initDSAuthenticateWSProxy();
  }
  
  public DSAuthenticateWSProxy(String endpoint) {
    _endpoint = endpoint;
    _initDSAuthenticateWSProxy();
  }
  
  private void _initDSAuthenticateWSProxy() {
    try {
      dSAuthenticateWS = (new DSAuthenticateWSImplServiceLocator()).getDSAuthenticateWSImplPort(_endpoint);
      if (dSAuthenticateWS != null) {
        if (_endpoint != null)
          ((javax.xml.rpc.Stub)dSAuthenticateWS)._setProperty("javax.xml.rpc.service.endpoint.address", _endpoint);
        else
          _endpoint = (String)((javax.xml.rpc.Stub)dSAuthenticateWS)._getProperty("javax.xml.rpc.service.endpoint.address");
      }
      
    }
    catch (javax.xml.rpc.ServiceException serviceException) {}
  }
  
  public String getEndpoint() {
    return _endpoint;
  }
  
  public void setEndpoint(String endpoint) {
    _endpoint = endpoint;
    if (dSAuthenticateWS != null)
      ((javax.xml.rpc.Stub)dSAuthenticateWS)._setProperty("javax.xml.rpc.service.endpoint.address", _endpoint);
    
  }
  
  public DSAuthenticateWS getDSAuthenticateWS() {
    if (dSAuthenticateWS == null)
      _initDSAuthenticateWSProxy();
    return dSAuthenticateWS;
  }
  
  public java.lang.String authenticatePDF(java.lang.String arg0, java.lang.String arg1, java.lang.String arg2, java.lang.String arg3) throws java.rmi.RemoteException{
    if (dSAuthenticateWS == null)
      _initDSAuthenticateWSProxy();
    return dSAuthenticateWS.authenticatePDF(arg0, arg1, arg2, arg3);
  }
  
  public java.lang.String register(java.lang.String arg0, java.lang.String arg1, java.lang.String arg2, java.lang.String arg3, boolean arg4) throws java.rmi.RemoteException{
    if (dSAuthenticateWS == null)
      _initDSAuthenticateWSProxy();
    return dSAuthenticateWS.register(arg0, arg1, arg2, arg3, arg4);
  }
  
  public java.lang.String userExists(java.lang.String arg0) throws java.rmi.RemoteException{
    if (dSAuthenticateWS == null)
      _initDSAuthenticateWSProxy();
    return dSAuthenticateWS.userExists(arg0);
  }
  
  public java.lang.String authenticate(java.lang.String arg0, java.lang.String arg1, java.lang.String arg2, java.lang.String arg3) throws java.rmi.RemoteException{
    if (dSAuthenticateWS == null)
      _initDSAuthenticateWSProxy();
    return dSAuthenticateWS.authenticate(arg0, arg1, arg2, arg3);
  }
  
  public java.lang.String generateRandomNumber(java.lang.String arg0) throws java.rmi.RemoteException{
    if (dSAuthenticateWS == null)
      _initDSAuthenticateWSProxy();
    return dSAuthenticateWS.generateRandomNumber(arg0);
  }
  
  public java.lang.String reregister(java.lang.String arg0, java.lang.String arg1, java.lang.String arg2, java.lang.String arg3, boolean arg4) throws java.rmi.RemoteException{
    if (dSAuthenticateWS == null)
      _initDSAuthenticateWSProxy();
    return dSAuthenticateWS.reregister(arg0, arg1, arg2, arg3, arg4);
  }
  
  public java.lang.String enableUser(java.lang.String arg0) throws java.rmi.RemoteException{
    if (dSAuthenticateWS == null)
      _initDSAuthenticateWSProxy();
    return dSAuthenticateWS.enableUser(arg0);
  }

  public java.lang.String deregister(java.lang.String arg0, java.lang.String arg1) throws java.rmi.RemoteException {
	if (dSAuthenticateWS == null)
	  _initDSAuthenticateWSProxy();
	return dSAuthenticateWS.deregister(arg0, arg1);
  }
  
}