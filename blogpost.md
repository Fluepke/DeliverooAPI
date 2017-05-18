# How to reverse-engineer the Deliveroo API
## Motivation
Almost any application relies on some kind of "data", that can be retrieved using APIs.
However, APIs are often undocumented, even though there is good chance you can get access to them.

This blog is about how I reimplemented the `Deliveroo API` using the Android app of deliveroo.co.uk

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

Even if there was certificate pinning used, we wouldn't be out of luck. There are articles covering that, but there are also techniques against that (signature checks):


## Reading the log
After installing and running the MITM VPN, I stated the Deliveroo app and searched for some restaurants close to me.
There were a lot of requests coming from Deliveroo and going to different servers.

The requests going to facebook etc. are interesting to look at. They are telling facebook that you searched for some restaurants at your geolocation...

However, this is not about how Deliveroo is breaching your privacy, this about reverse engineering their API. So let's take a look at the relevant communication.

This is the request coming from our phone
```
GET /orderapp/v1/restaurants?lat=52.5166791&lng=13.4584727&track=1 HTTP/1.1
Host: deliveroo.co.uk
Connection: Keep-Alive
Accept-Encoding: gzip
User-Agent: Deliveroo/2.14.0 (OnePlus ONE A2003;Android 6.0.1;de-DE;releaseEnv release)
X-Roo-Country: de
```
(I have censored my cookies and some other stuff)

And this is the answer (parts of it)
```json
