# Native Push Server

Server and library which to send push notification to users
of your app. The server can be used as a microservice
together with
[native_push_client](https://pub.dev/packages/native_push_client).
You can also use the kotlin library together with the
[native_push](https://pub.dev/packages/native_push) in
your own server. The server must be combined with a database and
supports PostgreSQL, MySQL and MariaBD.

## Configuring

### Overview

You can specify the following environment
variables to customize the server:

- ID_TYPE: The type of the user id. Can be one of Long,
  UUID and String. The default value is String.
- POSTGRES_HOST: The Hostname of the PostgreSQL server
- POSTGRES_DB: The database name of the PostgreSQL database
- POSTGRES_USER: The username for the PostgreSQL database
- POSTGRES_PASSWORD: The password for the PostgreSQL database
- MYSQL_HOST: The Hostname of the MySQL server
- MYSQL_DB: The database name of the MySQL database
- MYSQL_USER: The username for the MySQL database
- MYSQL_PASSWORD: The password for the MySQL database
- MARIA_HOST: The Hostname of the MariaDB server
- MARIA_DB: The database name of the MariaDB database
- MARIA_USER: The password for the MariaDB database
- MARIA_PASSWORD: The Hostname of the MariaDB server
- PUSH_SYSTEMS: The push systems which should be supported
  by the server. Can be a combination of FCM, APNS and
  WEB_PUSH as a comma-seperated list. The default value
  activates all push systems.
- FIREBASE_SERVICE_ACCOUNT_FILE: The Firebase service
  account file
- APNS_P8_FILE: The APNS p8 key file
- APNS_KEY_ID: The key id of the APNS p8 key
- APNS_TEAM_ID: The APNS key id
- APNS_P12_FILE: The APNS p12 file
- APNS_P12_PASSWORD: The password for the APNS p12 file
- APNS_TOPIC: The APNS topic (should be the bundle id
  of the app)
- WEB_PUSH_SUBJECT: The subject for web push
- VAPID_KEYS_FILE: The vapid keys file
- VAPID_PUBLIC_KEY: The vapid public key
- VAPID_PRIVATE_KEY: The vapid private key
- AUTHORIZATION_VALIDATION_URL: The authorization url to check
  if the user should be allowed to insert or update a token.
- DEVELOPMENT: If the app is in production or in development
  (currently only used for APNS). The default value is false.

### Database

On set of the database parameters must be specified
(host, db, user and password).

### Firebase

The FIREBASE_SERVICE_ACCOUNT_FILE must be specified when
using FCM. See
[here](https://firebase.google.com/docs/admin/setup#initialize_the_sdk_in_non-google_environments)
to learn how the get the file.

### APNS

When using APNS you must specify the APNS_TOPIC
and one of the following combinations: APNS_P8_FILE and
APNS_KEY_ID and APNS_TEAM_ID or APNS_P12_FILE and
APNS_P12_PASSWORD. See
[here](https://developer.apple.com/documentation/usernotifications/establishing-a-token-based-connection-to-apns#Obtain-an-encryption-key-and-key-ID-from-Apple)
for information about the p8 file and
[here](https://developer.apple.com/documentation/usernotifications/establishing-a-certificate-based-connection-to-apns#Obtain-a-provider-certificate-from-Apple)
for information about the p12 file.

### Web Push

When using Web Push you must specify the WEB_PUSH_SUBJECT
and either VAPID_KEYS_FILE or VAPID_PUBLIC_KEY and
VAPID_PRIVATE_KEY. See
[here](https://datatracker.ietf.org/doc/html/draft-thomson-webpush-vapid#section-2.1)
for information about the subject and
[here](https://github.com/svenopdehipt/native_push_vapid)
for a tool to generate the vapid keys.

### User Authorization

You can specify AUTHORIZATION_VALIDATION_URL with an url
to check if the user should be allowed to insert or update
its notification token. The server will send a POST-request
to the specified url. It will forward the `Authorization`-Header
and put the user id in a header named `User-Id`. The server
expects a response with the json format `{success: Bool}` and
view the user as valid if the response code is in the range
of 200 and 299 and the `success` value is `true`. This value
can also be omitted, but this isn't recommended because the
requests won't be validated at all and every request with
be viewed as valid.

## Routing

The server supports the following routes:
- POST /{userId}/token
- PUT /{userId}/token/{id}
- DELETE /{userId}/token/{id}
- POST /{userId}/send-notification

The first route is used to insert a new notification token
while the second and the third route a used to update and
delete existing tokens. The first two routes expect a json
body with the format `{token: String, system: String}` where
system is one the following: APNS, FCM or WEBPUSH. The third
routes doesn't expect a body at all. The first routes
response with a json body with the format
`{id: String}` where id is the uuid of the newly created
token. The second and third route respond with a json body
with the following format: `{success: Bool}`. The first three
routes a check if the user should have access to the database
if the AUTHORIZATION_VALIDATION_URL environment variable is
specified. The last route is used to send new notifications
to the users. It expects a body with the following json format:
```
{
  title: String,
  titleLocalizationKey: String,
  titleLocalizationArgs: String[],
  body: String,
  bodyLocalizationKey: String,
  bodyLocalizationArgs: String[],
  imageUrl: String,
  channelId: String,
  sound: String,
  icon: String,
  collapseKey: String,
  priority: String,
  data: Object with String values
}
```
All keys are optional and don't have to be specified. The
priority argument can be one of MIN, LOW, DEFAULT, HIGH, MAX.
The remaining keys should all explain themselves. The
collapseKey will be used as collapseId for APNS and the
icon will be ignored. The keys channelId, sound, icon,
collapseKey and priority will be ignored for web push.
The
[APNS](https://developer.apple.com/documentation/usernotifications/generating-a-remote-notification)
and
[FCM](https://firebase.google.com/docs/reference/admin/java/reference/com/google/firebase/messaging/package-summary)
documentation can be read for further information.