package one.rootz.deliverooapi;

import java.time.LocalDateTime;
import java.time.LocalTime;

public class DeliverooDemo {

	public static void main(String[] args) {
		System.out.println("Hello out there!");
		Client client = new Client();
		Session.begin(client);
		for (Restaurant restaurant : Restaurant.getRestaurants(client, new Coordinate("52.5166791", "13.4584727"))) {
			System.out.println(restaurant.toString());
		}
	}

}
