/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sifassociation.messaging;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.params.HttpParams;

/**
 * So we can ensure TLS 1.1 is supported.
 * 
 * @author jlovell
 */
public class SIF3SocketFactory extends SSLSocketFactory {

    public SIF3SocketFactory(TrustStrategy trustStrategy, X509HostnameVerifier hostnameVerifier) throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, UnrecoverableKeyException {
        super(trustStrategy, hostnameVerifier);
    }

    @Override
    public Socket createSocket(HttpParams params) throws IOException {
        return super.createSocket(params);
    }

    @Override
    public Socket connectSocket(Socket socket, InetSocketAddress remoteAddress, InetSocketAddress localAddress, HttpParams params) throws IOException, UnknownHostException, ConnectTimeoutException {
        return super.connectSocket(socket, remoteAddress, localAddress, params);
    }

    @Override
    public Socket createLayeredSocket(Socket socket, String host, int port, HttpParams params) throws IOException, UnknownHostException {
        return super.createLayeredSocket(socket, host, port, params);
    }
    
    @Override
    protected void prepareSocket(SSLSocket socket) throws IOException {
        super.prepareSocket(socket);

        socket.setEnabledProtocols( new String[] {"SSLv3", "TLSv1", "TLSv1.1"} );
    }
    
    @Override
    public boolean isSecure(Socket sock) throws IllegalArgumentException {
        return super.isSecure(sock);
    }

    @Override
    public X509HostnameVerifier getHostnameVerifier() {
        return super.getHostnameVerifier();
    }

}
