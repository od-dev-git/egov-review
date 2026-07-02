/**
 * DSAuthenticateWS.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.egov.dsc.emas.ws.ds;

public interface DSAuthenticateWS extends java.rmi.Remote {
    public java.lang.String authenticatePDF(java.lang.String arg0, java.lang.String arg1, java.lang.String arg2, java.lang.String arg3) throws java.rmi.RemoteException;
    public java.lang.String register(java.lang.String arg0, java.lang.String arg1, java.lang.String arg2, java.lang.String arg3, boolean arg4) throws java.rmi.RemoteException;
    public java.lang.String userExists(java.lang.String arg0) throws java.rmi.RemoteException;
    public java.lang.String authenticate(java.lang.String arg0, java.lang.String arg1, java.lang.String arg2, java.lang.String arg3) throws java.rmi.RemoteException;
    public java.lang.String generateRandomNumber(java.lang.String arg0) throws java.rmi.RemoteException;
    public java.lang.String reregister(java.lang.String arg0, java.lang.String arg1, java.lang.String arg2, java.lang.String arg3, boolean arg4) throws java.rmi.RemoteException;
    public java.lang.String enableUser(java.lang.String arg0) throws java.rmi.RemoteException;
    public java.lang.String deregister(java.lang.String arg0, java.lang.String arg1) throws java.rmi.RemoteException;
}
