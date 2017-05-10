package one.rootz.deliverooapi;

import org.apache.http.client.CookieStore;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

public class Client {
	private CloseableHttpClient client = null;
	private CookieStore cookieStore = new BasicCookieStore();
	
	public CloseableHttpClient getHttpClient() {
		if (this.client == null) {
			this.client = HttpClientBuilder.create().setDefaultCookieStore(cookieStore).build();
		}
		return this.client;
	}
	
	public void dumpCookies() {
		System.out.println("Cookies");
		for (Cookie cookie : cookieStore.getCookies()) {
			System.out.println(cookie.getName() + "=" + cookie.getValue());
		}
	}
}
