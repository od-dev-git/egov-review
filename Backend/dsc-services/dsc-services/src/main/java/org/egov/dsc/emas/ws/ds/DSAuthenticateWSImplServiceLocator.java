/**
 * DSAuthenticateWSImplServiceLocator.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.egov.dsc.emas.ws.ds;

import javax.xml.rpc.ServiceException;

import org.egov.dsc.config.ApplicationProperties;
import org.springframework.beans.factory.annotation.Autowired;

public class DSAuthenticateWSImplServiceLocator extends org.apache.axis.client.Service implements DSAuthenticateWSImplService {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public ApplicationProperties applicationProperties = new ApplicationProperties();
	
    public DSAuthenticateWSImplServiceLocator() {
    }


    public DSAuthenticateWSImplServiceLocator(org.apache.axis.EngineConfiguration config) {
        super(config);
    }

    public DSAuthenticateWSImplServiceLocator(java.lang.String wsdlLoc, javax.xml.namespace.QName sName) throws javax.xml.rpc.ServiceException {
        super(wsdlLoc, sName);
    }

    // Use to get a proxy class for DSAuthenticateWSImplPort
    private java.lang.String DSAuthenticateWSImplPort_address = applicationProperties.getEmasWsUrl();

    public java.lang.String getDSAuthenticateWSImplPortAddress() {
	    System.out.println("soap url ::::"+DSAuthenticateWSImplPort_address);
        return DSAuthenticateWSImplPort_address;
    }

    // The WSDD service name defaults to the port name.
    private java.lang.String DSAuthenticateWSImplPortWSDDServiceName = "DSAuthenticateWSImplPort";

    public java.lang.String getDSAuthenticateWSImplPortWSDDServiceName() {
        return DSAuthenticateWSImplPortWSDDServiceName;
    }

    public void setDSAuthenticateWSImplPortWSDDServiceName(java.lang.String name) {
        DSAuthenticateWSImplPortWSDDServiceName = name;
    }

    public DSAuthenticateWS getDSAuthenticateWSImplPort(String _endpoint) throws javax.xml.rpc.ServiceException {
       java.net.URL endpoint;
       DSAuthenticateWSImplPort_address=_endpoint;
        try {
            endpoint = new java.net.URL(DSAuthenticateWSImplPort_address);
        }
        catch (java.net.MalformedURLException e) {
            throw new javax.xml.rpc.ServiceException(e);
        }
        return getDSAuthenticateWSImplPort(endpoint);
    }

    public DSAuthenticateWS getDSAuthenticateWSImplPort(java.net.URL portAddress) throws javax.xml.rpc.ServiceException {
        try {
            DSAuthenticateWSImplServiceSoapBindingStub _stub = new DSAuthenticateWSImplServiceSoapBindingStub(portAddress, this);
            _stub.setPortName(getDSAuthenticateWSImplPortWSDDServiceName());
            return _stub;
        }
        catch (org.apache.axis.AxisFault e) {
            return null;
        }
    }

    public void setDSAuthenticateWSImplPortEndpointAddress(java.lang.String address) {
        DSAuthenticateWSImplPort_address = address;
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        try {
            if (DSAuthenticateWS.class.isAssignableFrom(serviceEndpointInterface)) {
                DSAuthenticateWSImplServiceSoapBindingStub _stub = new DSAuthenticateWSImplServiceSoapBindingStub(new java.net.URL(DSAuthenticateWSImplPort_address), this);
                _stub.setPortName(getDSAuthenticateWSImplPortWSDDServiceName());
                return _stub;
            }
        }
        catch (java.lang.Throwable t) {
            throw new javax.xml.rpc.ServiceException(t);
        }
        throw new javax.xml.rpc.ServiceException("There is no stub implementation for the interface:  " + (serviceEndpointInterface == null ? "null" : serviceEndpointInterface.getName()));
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(javax.xml.namespace.QName portName, Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        if (portName == null) {
            return getPort(serviceEndpointInterface);
        }
        java.lang.String inputPortName = portName.getLocalPart();
        if ("DSAuthenticateWSImplPort".equals(inputPortName)) {
            return getDSAuthenticateWSImplPort();
        }
        else  {
            java.rmi.Remote _stub = getPort(serviceEndpointInterface);
            ((org.apache.axis.client.Stub) _stub).setPortName(portName);
            return _stub;
        }
    }

    public javax.xml.namespace.QName getServiceName() {
        return new javax.xml.namespace.QName("http://ds.ws.emas/", "DSAuthenticateWSImplService");
    }

    private java.util.HashSet ports = null;

    public java.util.Iterator getPorts() {
        if (ports == null) {
            ports = new java.util.HashSet();
            ports.add(new javax.xml.namespace.QName("http://ds.ws.emas/", "DSAuthenticateWSImplPort"));
        }
        return ports.iterator();
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(java.lang.String portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        
if ("DSAuthenticateWSImplPort".equals(portName)) {
            setDSAuthenticateWSImplPortEndpointAddress(address);
        }
        else 
{ // Unknown Port Name
            throw new javax.xml.rpc.ServiceException(" Cannot set Endpoint Address for Unknown Port" + portName);
        }
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(javax.xml.namespace.QName portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        setEndpointAddress(portName.getLocalPart(), address);
    }


	@Override
	public DSAuthenticateWS getDSAuthenticateWSImplPort() throws ServiceException {
		// TODO Auto-generated method stub
		return null;
	}

}
