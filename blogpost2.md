Writing a `ConsoleService` for susi.ai
======================================

`ConsoleServices` offer the ability to write skills for Susi AI that go beyond skills that can be created using `susi dreams`.
For example it is possible to cascade API queries: First ask Nominatim about the coordinates of "Berlin" and then ask another API using the exact coordinates for some other information.

## Mapping a susi skill to a `ConsoleService`
Susi skills are defined in `json` files:
```json
{"rules":[
	{
		"example":"How to get from München HBF to Potsdam Griebnitzsee",
		"phrases":[ {"type":"pattern", "expression":"How to get from * to *"}
		],
		"process":[ {"type":"console", "expression":"SELECT description FROM bahn WHERE from='$1$' to='$2$';"}],
		"actions":[ {"type":"answer", "select":"random", "phrases":[
			"$description$",
		]}],
	},
]}
```

This is a basic example containing one rule, I wrote to add support for public transit in Susi AI.

As you can see, there's an ```"example"``` Phrase, providing an example request for your rule.

The `"phrases"` are in my case something similar to `regular expressions` that try to match the users' input. For keeping things easy, I've just added one pattern. The pattern matching works **case insensitive**. The asterisk is a _wildcard_ (similar to a `regex capture group`) matching any user input. The string matched to this _wildcard_ can be used in the further processing of this rule.

These _wildcards_ (their assigned value) was used in the `"process"` part, where we define via `"type":"console"` to use a `ConsoleService`. In the `"expression"` that is looking like **SQL**, it is possible to access the matched input using `$1$`, `$2$`, and so on. The first matched _wildcard_ will be `$1$` (**non zero-index based**).

So if a user enters _How to get from München HBF to Potsdam Griebnitzsee_ `$1$` will be equal to _München HBF_ and `$2$` to _Potsdam Griebnitzsee_.

Within the `"actions"` section several kinds of outputting the result can be defined. See also [#198](https://github.com/fossasia/susi_server/issues/198).

In this example, I am just building a simple sentence with the answer provided from the `ConsoleService` by selecting the `description` from the response.

## Our own query language
The **SQL** like `"Expression"` part was inspired from the [`Yahoo! Query Language`](https://developer.yahoo.com/yql/).

The expression will be matched by a **regular expression** within the `ai.susi.server.api.susi.ConsoleService`:

```java
dbAccess.put(Pattern.compile("SELECT +?(.*?) +?FROM +?bahn +?WHERE +?from ??= ??'(.*?)' +?to ??= ??'(.*?)' ??;"), (flow, matcher) -> {
	String query = matcher.group(1);
	String from = matcher.group(2);
	String to = matcher.group(3);
	SusiThought json = new SusiThought();
	try {
		json = (new BahnService()).getConnections(from, to);
		SusiTransfer transfer = new SusiTransfer(query);
        json.setData(transfer.conclude(json.getData()));
        return json;
	} catch (Exception e) {
		e.printStackTrace();
	}
	return json;
});
```

This guide assumes that you are familiar with `Regular expressions`. If not try tinkering around with [regex101.com](https://regex101.com) or googling will provide you some good tutorials.

The **YQL** query from above will match the **Regex** in the `ConsoleService` and the lambda expression will be executed.

## Implementing an API
Within the lambda expression, we're receiving the response using the self-written `BahnService`, which relies on `transportr-enabler` to query some public transportation API for the german railroads.

**No, it is NOT possible to query the Deutsche Bahn API using a susi dream.** First, you have to query the stations based on the station name provided by the user. In the next step the connection is looked up. `transportr-enabler` parses the Deutsche Bahn website. There is no Open Source fancy `json` speaking API from the Deutsche Bahn. And this is true for most of the other public transit providers.

```java
public SusiThought getConnections(String from, String to, int hoursDep, int minutesDep) throws IOException, NoStationFoundException {
		// Yes, Date is deprecated, but the referenced library needs it...
		Date date = new Date();
		date.setHours(hoursDep);
		date.setMinutes(minutesDep);
		SusiThought response = new SusiThought();
		JSONArray data = new JSONArray();

		List<Location> fromLocations = provider.suggestLocations(from).getLocations();
		List<Location> toLocations = provider.suggestLocations(to).getLocations();
		if (fromLocations.size() == 0) {
			throw new NoStationFoundException(from);
		}
		if (toLocations.size() == 0) {
			throw new NoStationFoundException(to);
		}

		QueryTripsResult tripsResult = provider.queryTrips(fromLocations.get(0), null, toLocations.get(0), date, true, null, null, null, null, null);
		response.setHits(tripsResult.trips.size());

		for (Trip trip : tripsResult.trips) {
			data.put(JSONConverter.tripToJSON(trip));
		}
		response.setData(data);

		return response;
	}
```

So, once we got our connection, it's converted to `json`:
Within the `tripToJSON(Trip trip)` we include a description:
```java
tripObj.put("description", JSONConverter.getDescription(trip.legs));
```

That is generated this way:

```java
public static String getDescription(List<Leg> legs) {
		String response = "";
		for (int i = 0; i < legs.size(); i++) {
			if (!(legs.get(i) instanceof Trip.Public)) continue;
			if (i == 0) {
				response += "Take the ";
			} else {
				response += " From there, take the ";
			}
			Trip.Public pub = (Trip.Public)legs.get(i);
			response += pub.line.label;
			response += " (departing at " + pub.getDepartureTime().getHours() + ":" + pub.getDepartureTime().getMinutes();
			response += ", +" + (pub.getDepartureDelay() / 60000.0) + " mins delay) to ";
			response += pub.arrival.name;
			response += " (arriving at " + pub.getArrivalTime().getHours() + ":" + pub.getArrivalTime().getMinutes();
			response += ", +" + (pub.getArrivalDelay() / 60000.0) + " mins delay).";
		}
		return response;
	}
```

Basically the `SusiThought` returned by the lambda expression inside the `ConsoleService` is a container for storing our `JSONObject` containing the answer to the users' question (the `"description"` field).

A `SusiTransfer` is used to filter and limit our answer (the `JSONObject`) to the requested field `"description"`.

## Conclusion
When asked _"How to get from Berlin HBF to Lemgo"_, Susi AI will answer _"Take the ICE546 (departing at 14:34, +0.0 mins delay) to Bielefeld Hbf (arriving at 17:22, +2.0 mins delay). From there, take the ERB90249 (departing at 18:15, +0.0 mins delay) to Lemgo (arriving at 18:54, +0.0 mins delay)."_
As you can see, with a `ConsoleService` there is much more you can do to answer a user's query. You can do complex calculations, consult several (non json speaking) APIs and so on.

Right now, Susi only supports public transport in germany, but `transportr-enabler` offers API implementations for a lot of towns. Thus we need to dispatch the user's request (depending on his geolocation) to different network operators around the world.

**AND PLEASE REVIEW MY PULL REQUEST.** It's already been pending for a week.
