package eu.neclab.ngsildbroker.subscriptionmanager.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import com.google.common.collect.ArrayListMultimap;

import eu.neclab.ngsildbroker.commons.datatypes.Notification;
import eu.neclab.ngsildbroker.commons.exceptions.ResponseException;
import eu.neclab.ngsildbroker.commons.interfaces.NotificationHandler;
import eu.neclab.ngsildbroker.commons.ldcontext.ContextResolverBasic;
import eu.neclab.ngsildbroker.commons.serialization.DataSerializer;
import eu.neclab.ngsildbroker.commons.tools.EntityTools;
import eu.neclab.ngsildbroker.commons.tools.HttpUtils;

public class NotificationHandlerREST implements NotificationHandler {

	private final static Logger logger = LogManager.getLogger(NotificationHandlerREST.class);
	private SubscriptionService subscriptionManagerService;
	HttpUtils httpUtils;

	public NotificationHandlerREST(SubscriptionService subscriptionManagerService,
			ContextResolverBasic contextResolver) {
		this.subscriptionManagerService = subscriptionManagerService;
		httpUtils = HttpUtils.getInstance(contextResolver);
	}

	HashMap<String, Long> subId2LastReport = new HashMap<String, Long>();
	ArrayListMultimap<String, Notification> subId2Notifications = ArrayListMultimap.create();
	Timer executor = new Timer(true);

	@Override
	public void notify(Notification notification, URI callback, String acceptHeader, String subId, List<Object> context,
			int throttling) {

		ArrayList<String> subIds = new ArrayList<String>();
		subIds.add(subId);

		try {
			if (throttling > 0) {
				synchronized (subId2Notifications) {
					subId2Notifications.put(subId, notification);
					Long lastReport = subId2LastReport.get(subId);
					Long now = System.currentTimeMillis() / 1000;
					if (lastReport == null) {
						lastReport = 0l;
					}
					Long delay = 0l;
					Long delta = now - lastReport;
					if (delta < throttling) {
						delay = delta;
					}
					executor.schedule(new TimerTask() {

						@Override
						public void run() {
							synchronized (subId2Notifications) {
								Notification sendOutNotification = EntityTools
										.squashNotifications(subId2Notifications.removeAll(subId));
								String jsonStr = DataSerializer.toJson(sendOutNotification.getData());
								Long now = System.currentTimeMillis();
								subId2LastReport.put(subId, now / 1000);
								subscriptionManagerService.reportNotification(subId, now);
								try {
									logger.trace("Sending notification");
									logger.debug("Json to be sent: " + jsonStr);
									ResponseEntity<Object> reply = generateNotificationResponse(acceptHeader, jsonStr, context);
									logger.debug("body to be sent: " + reply.getBody().toString());
									httpUtils.doPost(callback, reply.getBody().toString(),
											reply.getHeaders().toSingleValueMap());
									subscriptionManagerService.reportSuccessfulNotification(subId, now);
								} catch (IOException | ResponseException e) {
									logger.error("Exception ::", e);
									subscriptionManagerService.reportFailedNotification(subId, now);
									e.printStackTrace();
								}
							}

						}
					}, delay);

				}

			} else {
				// TODO handle errors for notifications
				String jsonStr = DataSerializer.toJson(notification.getData());
				logger.trace("Sending notification");
				ResponseEntity<Object> reply;
				try {
					reply = generateNotificationResponse(acceptHeader, jsonStr, context);
					logger.debug(reply.getBody().toString());
					httpUtils.doPost(callback, reply.getBody().toString(), reply.getHeaders().toSingleValueMap());
				} catch (ResponseException e) {
					logger.error("Exception ::", e);
					subscriptionManagerService.reportFailedNotification(subId, System.currentTimeMillis());
					e.printStackTrace();
				}

			}

		} catch (IOException e) {
			logger.error("Exception ::", e);
			e.printStackTrace();
		}

	}

	private ResponseEntity<Object> generateNotificationResponse(String acceptHeader, String body, List<Object> context) throws ResponseException {
		HttpServletRequest request = new HttpServletRequest() {

			@Override
			public Object getAttribute(String name) {
				
				return null;
			}

			@Override
			public Enumeration<String> getAttributeNames() {
				
				return null;
			}

			@Override
			public String getCharacterEncoding() {
				
				return null;
			}

			@Override
			public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
				
				
			}

			@Override
			public int getContentLength() {
				
				return 0;
			}

			@Override
			public long getContentLengthLong() {
				
				return 0;
			}

			@Override
			public String getContentType() {
				
				return null;
			}

			@Override
			public ServletInputStream getInputStream() throws IOException {
				
				return null;
			}

			@Override
			public String getParameter(String name) {
				
				return null;
			}

			@Override
			public Enumeration<String> getParameterNames() {
				
				return null;
			}

			@Override
			public String[] getParameterValues(String name) {
				
				return null;
			}

			@Override
			public Map<String, String[]> getParameterMap() {
				
				return null;
			}

			@Override
			public String getProtocol() {
				
				return null;
			}

			@Override
			public String getScheme() {
				
				return null;
			}

			@Override
			public String getServerName() {
				
				return null;
			}

			@Override
			public int getServerPort() {
				
				return 0;
			}

			@Override
			public BufferedReader getReader() throws IOException {
				
				return null;
			}

			@Override
			public String getRemoteAddr() {
				
				return null;
			}

			@Override
			public String getRemoteHost() {
				
				return null;
			}

			@Override
			public void setAttribute(String name, Object o) {

				
			}

			@Override
			public void removeAttribute(String name) {

				
			}

			@Override
			public Locale getLocale() {

				return null;
			}

			@Override
			public Enumeration<Locale> getLocales() {

				return null;
			}

			@Override
			public boolean isSecure() {

				return false;
			}

			@Override
			public RequestDispatcher getRequestDispatcher(String path) {

				return null;
			}

			@Override
			public String getRealPath(String path) {
				
				return null;
			}

			@Override
			public int getRemotePort() {

				return 0;
			}

			@Override
			public String getLocalName() {

				return null;
			}

			@Override
			public String getLocalAddr() {

				return null;
			}

			@Override
			public int getLocalPort() {

				return 0;
			}

			@Override
			public ServletContext getServletContext() {

				return null;
			}

			@Override
			public AsyncContext startAsync() throws IllegalStateException {

				return null;
			}

			@Override
			public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse)
					throws IllegalStateException {

				return null;
			}

			@Override
			public boolean isAsyncStarted() {

				return false;
			}

			@Override
			public boolean isAsyncSupported() {

				return false;
			}

			@Override
			public AsyncContext getAsyncContext() {

				return null;
			}

			@Override
			public DispatcherType getDispatcherType() {

				return null;
			}

			@Override
			public String getAuthType() {

				return null;
			}

			@Override
			public Cookie[] getCookies() {

				return null;
			}

			@Override
			public long getDateHeader(String name) {

				return 0;
			}

			@Override
			public String getHeader(String name) {
				if(HttpHeaders.ACCEPT.equals(name)) {
					return acceptHeader;
				}
				return null;
			}
			
			ArrayList<String> acceptHeaderList = new ArrayList<String>();
			{acceptHeaderList.add(acceptHeader);}
			
			@Override
			public Enumeration<String> getHeaders(String name) {
				if(HttpHeaders.ACCEPT.equals(name)) {
					return Collections.enumeration(acceptHeaderList);
				}
				return null;
			}

			@Override
			public Enumeration<String> getHeaderNames() {
			
				return null;
			}

			@Override
			public int getIntHeader(String name) {
			
				return 0;
			}

			@Override
			public String getMethod() {

				return null;
			}

			@Override
			public String getPathInfo() {
				
				return null;
			}

			@Override
			public String getPathTranslated() {
				
				return null;
			}

			@Override
			public String getContextPath() {
				
				return null;
			}

			@Override
			public String getQueryString() {
				
				return null;
			}

			@Override
			public String getRemoteUser() {
				
				return null;
			}

			@Override
			public boolean isUserInRole(String role) {
				
				return false;
			}

			@Override
			public Principal getUserPrincipal() {
				
				return null;
			}

			@Override
			public String getRequestedSessionId() {
				
				return null;
			}

			@Override
			public String getRequestURI() {
				
				return null;
			}

			@Override
			public StringBuffer getRequestURL() {
				
				return null;
			}

			@Override
			public String getServletPath() {
				
				return null;
			}

			@Override
			public HttpSession getSession(boolean create) {
				
				return null;
			}

			@Override
			public HttpSession getSession() {
				
				return null;
			}

			@Override
			public String changeSessionId() {
				
				return null;
			}

			@Override
			public boolean isRequestedSessionIdValid() {
				
				return false;
			}

			@Override
			public boolean isRequestedSessionIdFromCookie() {
				
				return false;
			}

			@Override
			public boolean isRequestedSessionIdFromURL() {
				
				return false;
			}

			@Override
			public boolean isRequestedSessionIdFromUrl() {
				
				return false;
			}

			@Override
			public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
				
				return false;
			}

			@Override
			public void login(String username, String password) throws ServletException {
				
				
			}

			@Override
			public void logout() throws ServletException {
				
				
			}

			@Override
			public Collection<Part> getParts() throws IOException, ServletException {
				
				return null;
			}

			@Override
			public Part getPart(String name) throws IOException, ServletException {
				
				return null;
			}

			@Override
			public <T extends HttpUpgradeHandler> T upgrade(Class<T> httpUpgradeHandlerClass)
					throws IOException, ServletException {
				
				return null;
			}
			
		};
		return httpUtils.generateReply(request, body, null, context);
	}

}
