/**
 * $Id: Service.java 1890 2010-01-29 22:07:53Z jbuchbinder $
 */

package com.github.freemed.gwt.client;

import com.google.gwt.user.client.rpc.RemoteService;

public interface Service extends RemoteService {

	public String[][] getSalesData();

	public String[][] getSalesExtensions();

	public Long getSerialVersion();

	public String[] getGraphColors();

}
