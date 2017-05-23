# How to reverse-engineer the Deliveroo API
## Motivation
Almost any application relies on some kind of "data", that can be retrieved using APIs.
However, APIs are often undocumented, even though there is a good chance you can get access to them.

This blog is about how I reimplemented the `Deliveroo API` using the Android app of the food delivery service called `Deliveroo`.

## Sniffing the traffic
Basically we're doing a MITM attack on the app's TLS encrypted traffic using `Packet Capture`. If you know what that means, you can skip to the next topic.

To capture the deliveroo app's communications you need to somehow put a spy in the "middle" between your app and the internet.
Such a "spy" is `tcpdump`, capturing all your traffic on a certain interface.
However this is quite complicated to install (either on your router or phone) as it requires root privileges and some knowledge about Linux networking.

For Android there is an easier way: On Android it is possible to route all your traffic through a VPN without having root access.
Furthermore it is possible to run your own VPN service on your device, thus giving you access to all your phones communications.

Of course app developers try to avoid such MITM _(man in the middle)_ attacks using transport layer security (TLS), relying on trusted certificate authorities (CA) to prove you are communicating with the "real" server and not an attacker.
As we are trying to breach our own phone, we can easily install our own CA.

`Packet Capture` is an Android App doing all this stuff for you. The Deliveroo app is talking encrypted with the VPN service assuming it is the real deliveroo server (as the VPN utilizes our own CA). On the other side our VPN services is talking with the deliveroo server acting like it is the app running on your phone.

Obviously this is a security problem, considering a malware on your phone could act this way. That's why there is `certificate pinning`, which would prevent these attacks.

Fortunately, Deliveroo does not use any `Certificate Pinning`. (@Deliveroo if you are reading this: YOU SHOULD REALLY IMPLEMENT THAT! Everybody can retrieve your Google Maps API key and some other keys you are probably paying for...)

Even if there was certificate pinning used, we wouldn't be out of luck. There are articles covering that, but there are also techniques against that (signature checks): Basically the app developer does not trust any CA your phone trusts, it just trusts those, the app developer decides to trust, thus an app with certificate pinning won't trust our selfmade CA. You can read more about certificate and public key pinning [here](https://www.owasp.org/index.php/Certificate_and_Public_Key_Pinning).

## Reading the log
After installing and running the MITM VPN, I stated the Deliveroo app and searched for some restaurants close to me.
There were a lot of requests coming from Deliveroo and going to different servers.

The requests going to facebook etc. are interesting to look at. They are telling facebook that you searched for some restaurants at your geolocation...

However, this is not about how Deliveroo is breaching your privacy, this about reverse engineering their API. So let's take a look at the relevant communication.

This is the request coming from our phone. You can also try out! Just click [here](deliveroo.co.uk/orderapp/v1/restaurants?lat=52.5166791&lng=13.4584727).
```
GET /orderapp/v1/restaurants?lat=52.5166791&lng=13.4584727&track=1 HTTP/1.1
Host: deliveroo.co.uk
Connection: Keep-Alive
Accept-Encoding: gzip
User-Agent: Deliveroo/2.14.0 (OnePlus ONE A2003;Android 6.0.1;de-DE;releaseEnv release)
X-Roo-Country: de
```
(I have censored my cookies and some other stuff)
There was a Session cookie sent, so I expected, I first had to get a session cookie as well, when re-implementing the API client. When I copy pasted the link inside `Postman`, a wonderful tool for testing APIs and it turned out, that no session was necessary for querying the restaurants nearby. So let's take a look at the response.

First, the headers
```ini
HTTP/1.1 200 OK
Server: Cowboy
Status: 200 OK
Content-Type: application/json
X-Roo: 1
Cache-Control: no-cache
X-Watch-While-You-Eat: Dial M for Murgh
X-Content-Type-Options: nosniff
Via: 1.1 vegur
Via: 1.1 varnish
Content-Length: 97704
Accept-Ranges: bytes
Date: Sat, 06 May 2017 22:04:09 GMT
Via: 1.1 varnish
Age: 0
Connection: keep-alive
X-Served-By: cache-lcy1141-LCY, cache-fra1236-FRA
X-Cache: MISS, MISS
X-Cache-Hits: 0, 0
X-Timer: S1494108248.298047,VS0,VE1343
```

Ok, nothing interesting here, besides the `X-Watch-While-You-Eat`-header ;) Good sense of humor!

Then let's look at the body, which consists of `json` (Hoooray, no parsing necessary!)
In the beginning, there's only some uninteresting stuff concerning delivery times, let's skip to the restaraunt list:
```json
{
      "id": 4370,
      "name": "Santa Maria Eastside",
      "name_with_branch": "Santa Maria Eastside",
      "uname": "santa-maria-eastside",
      "price_category": 2,
      "currency_symbol": "€",
      "primary_image_url": "https://cdn1.deliveroo.co.uk/media/menus/4370/320x180.jpg?v=1473408811",
      "image_url": "https://cdn1.deliveroo.co.uk/media/menus/4370/{w}x{h}.jpg?v=1473408811{&filters}{&quality}",
      "neighborhood": {
        "id": 519,
        "name": "Friedrichshain",
        "uname": "friedrichshain"
      },
      "coordinates": [
        13.4582335,
        52.5104878
      ],
      "newly_added": false,
      "category": "Mexikanisch",
      "curr_prep_time": 15,
      "delay_time": 0,
      "baseline_deliver_time": 17,
      "total_time": 15,
      "distance_m": 688,
      "travel_time": 10,
      "kitchen_open_advance": 0,
      "hours": {
        "today": [
          [
            "ASAP"
          ],
          [
            "18:00",
            "22:30"
          ]
        ],
        "tomorrow": [
          [
            "12:15",
            "22:30"
          ]
        ]
      },
      "opening_hours": {
        "today": [
          [
            "12:00",
            "22:30"
          ]
        ],
        "tomorrow": [
          [
            "12:00",
            "22:30"
          ]
        ]
      },
      "target_delivery_time": {
        "minutes": "10 - 20"
      },
      "menu": {
        "menu_tags": [
          {
            "id": 34,
            "type": "Locale",
            "name": "Mexikanisch"
          },
          {
            "id": 293,
            "type": "Collection",
            "name": "Nur Bei Uns"
          },
          {
            "id": 289,
            "type": "Collection",
            "name": "Lokale Empfehlungen"
          },
          {
            "id": 19,
            "type": "Food",
            "name": "Burritos"
          },
          {
            "id": 225,
            "type": "Food",
            "name": "Street Food"
          },
          {
            "id": 244,
            "type": "Food",
            "name": "Tacos"
          }
        ]
      },
      "open": true,
      "delivery_hours": {
        "today": [
          [
            "ASAP"
          ],
          [
            "18:00",
            "22:30"
          ]
        ],
        "tomorrow": [
          [
            "12:15",
            "22:30"
          ]
        ]
      }
    }
```
Pretty nice, isn't it?

When I clicked the restaurant in the app, I noticed a request to `orderapp/v1/restaurants/4370` where 4370 is obviously the restaurant id.

This API endpoint can be used to retrieve the menu for a restaurant:

```json
"menu_items": [
      {
        "id": 280692,
        "name": "Guacamole mit Maismehl-Tortillachips",
        "description": "Hausgemachte Avocadocreme mit Maismehl-Tortillachips/ homemade guacamole served with corn tortilla chips ",
        "omit_from_receipts": false,
        "price": "8.0",
        "alt_mod_price": null,
        "sort_order": 1,
        "available": true,
        "popular": true,
        "category_id": -1,
        "modifier_group_ids": []
      },
      {
        "id": 427472,
        "name": "Cochinita Especial",
        "description": "Mexikanisches pulled pork mit eingelegten roten Zwiebeln, Chipotle Sahne Soße, Guacamole und fruchtiger Habanero Salsa/ Mexican pulled pork with pickled red onions, chipotle cream, guacamole and tropical habanero salsa ",
        "omit_from_receipts": false,
        "price": "10.5",
        "alt_mod_price": null,
        "sort_order": 4,
        "available": true,
        "popular": true,
        "category_id": -1,
        "modifier_group_ids": [
          69342
        ]
      }]
```

Once you figured out, how to deal with the API, the implementation is pretty straight forward. For parsing the json, I use `org.json`. For creating the http requests, I'm using `org.apache.http`.


## Other interesting findings
The deliveroo app is sending various data about your phone (including your hardware ID and carrier to):
  * branch.io (also a notification when you are closing the app)
  * appsflyer.com
  * crashlytis.com
  * facebook.com (tells them you were looking for a restaurant at your exact coordinates)

**UPDATE** Somehow packet capturing the app no longer works, maybe there is a problem with my network or deliveroo has implemented certificate pinning, which seems most likely.
