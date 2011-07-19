/**
 * $Id: ServiceAsync.java 1890 2010-01-29 22:07:53Z jbuchbinder $
 */

package com.github.freemed.gwt.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface ServiceAsync {

	public void getSalesData(AsyncCallback<String[][]> callback);

	public void getSalesExtensions(AsyncCallback<String[][]> callback);

	public void getSerialVersion(AsyncCallback<Long> callback);

	public void getGraphColors(AsyncCallback<String[]> callback);

}
