package one.rootz.deliverooapi;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.util.EntityUtils;
import org.json.*;

public class Restaurant {
	private static final String API_PATH = "/restaurants";
	
	// Numeric id
	private int id;
	
	// The restaurant name
	private String name;
	
	// Seems to be identical to name
	private String nameWithBranch;
	
	// Lowercase unique name (without special chars)
	private String uname;
	
	// Seems to range from 1 to 3
	private int priceCategory;
	
	private String currencySymbol;
	
	private String primaryImageUrl;
	
	private String imageUrl;
	
	private Coordinate coordinate;
	
	private Boolean newlyAdded;
	
	private String category;
	
	// Maybe how busy the restaurant currently is?
	private double currentPreparationTime;
	
	private int delayTime;
	
	// Maybe average delivery time?
	private float baselineDeliverTime;
	
	// Maybe time until delivery?
	private int totalTime;
	
	// Distance in meters
	private int distance;
	
	private int travelTime;
	
	// no idea what this means
	private int kitchenOpenAdvance;
	
	// TODO opening hours
	
	public static List<Restaurant> getRestaurants(Client client, Coordinate coordinate) {
		List<Restaurant> restaurants = new ArrayList<>();
		try {
			URIBuilder uri = Config.getURIBuilder(Restaurant.API_PATH);
			uri.setParameter("lat", coordinate.getLatitude());
			uri.setParameter("lng", coordinate.getLongitude());
			
			HttpGet httpGet = new HttpGet(uri.build());
			CloseableHttpResponse httpResponse = client.getHttpClient().execute(httpGet);
			assertThat(httpResponse.getStatusLine().getStatusCode(), equalTo(200));
			
			String contentMimeType = ContentType.getOrDefault(httpResponse.getEntity()).getMimeType();
			assertThat(contentMimeType, equalTo(ContentType.APPLICATION_JSON.getMimeType()));
			
			// The output can be quite large, so storing it in a String is not ideal.
			// Maybe it's possible to parse a Stream?
			String rawJson = EntityUtils.toString(httpResponse.getEntity());
			assertThat(rawJson, notNullValue());
			
			JSONObject jsonRoot = new JSONObject(rawJson);
			assertThat(jsonRoot.has("restaurants"), equalTo(true));
			JSONArray restaurantsJsonArray = jsonRoot.getJSONArray("restaurants");
			
			Iterator<Object> restaurantsIterator = restaurantsJsonArray.iterator();
			
			while (restaurantsIterator.hasNext()) {
				Object obj = restaurantsIterator.next();
				if (obj instanceof JSONObject) {
					restaurants.add(new Restaurant((JSONObject)obj));
				}
			}
			
			return restaurants;
		} catch (Exception ex) {
			ex.printStackTrace();
			System.out.println("Error while fetching restaurants: " + ex.toString());
			return null;
		}
	}
	
	public Restaurant(JSONObject jsonObject) {
		this.id = jsonObject.getInt("id");
		this.name = jsonObject.getString("name");
		this.nameWithBranch = jsonObject.getString("name_with_branch");
		this.uname = jsonObject.getString("uname");
		this.priceCategory = jsonObject.getInt("price_category");
		this.currencySymbol = jsonObject.getString("currency_symbol");
		this.primaryImageUrl = jsonObject.getString("primary_image_url");
		this.imageUrl = jsonObject.getString("image_url");
		JSONArray coordinateJsonArray = jsonObject.getJSONArray("coordinates");
		this.coordinate = new Coordinate(coordinateJsonArray.getDouble(0), coordinateJsonArray.getDouble(1));
		this.newlyAdded = jsonObject.getBoolean("newly_added");
		this.category = jsonObject.getString("category");
		this.currentPreparationTime = jsonObject.getDouble("curr_prep_time");
		this.baselineDeliverTime = jsonObject.getInt("baseline_deliver_time");
		this.totalTime = jsonObject.getInt("total_time");
		this.distance = jsonObject.getInt("distance_m");
		this.travelTime = jsonObject.getInt("travel_time");
		this.kitchenOpenAdvance = jsonObject.getInt("kitchen_open_advance");
		
	}
	
	/**
	 * @Override
	 */
	public String toString() {
		return this.name + "(" + this.category + ")";
	}
}
