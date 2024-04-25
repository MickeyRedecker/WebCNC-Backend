# WebCNC Backend
## Web-based Centralized Network Controller (CNC) for Time-Sensitive Networking (TNS)

This software is released under the MIT License. 

It has been developed as part of a bachelor thesis at the University of Stuttgart.

The WebCNC can be used to manually configure IEEE 802.1Qbv compliant switches that support SNMPv3 through a web application. 

This project is the backend, the webcnc_frontend project is also required to use the WebCNC.

### Prerequisites:

#### Building the backend

- Java SE JDK (compatible with Version 17 and newer)
- Apache Maven (tested with version 3.9.2)

#### Hosting the backend

- Java SE JRE (compatible with Version 17 and newer)

### Building the project

#### Step 1: TLS Certificates

Using a TLS certificate is required for the HTTPS API offered by the backend.

About self-signed certificates: When using self-signed certificates, it is recommended to add them to the browser's trust store. 
Otherwise, under Firefox, the WebCNC won´t work at all. Under Chrome, it will work when backend and frontend use the same self-signed certificate.

- generate a TLS certificate and key
- convert it to a PKCS12 key store with a keyAlias and a password:
```
openssl pkcs12 -export -out keyStore.p12 -inkey mykey.pem -in mycertificate.pem
```
- move it to a folder on the server hosting the backend

#### Step 2: Configuring the backend

- navigate to /src/main/resources
- open the application.properties file

- set *server.port* to the port at which the backend listens for HTTPS requests
- set *the server.ssl.key-store* to the path of the PKCS12 key store on the server
- set *server.ssl.key-store-password* to the key store's password
- set *server.ssl.keyAlias* to the key's alias

#### Step 3: Building the backend

- navigate to the project's root directory (the directory containing this file)
- build the project using maven:
```
mvn package
```

- in the target folder, you will find the webcnc-1.0.jar file. This needs to be moved to the server

### Running the backend

The backend accepts launch parameters to configure some settings of the WebCNC.

It is recommended to run the backend in a screen or similar so that it keeps running when you close the terminal.

#### not recommended

Running the backend without any launch parameters is simple. Just run 
```
java -jar webcnc-1.0.jar
```
in the respective directory. It will use default values for all available parameters.
THIS IS NOT RECOMMENDED! IT WILL USE THE DEFAULT PASSWORD!

#### launch parameters

the following launch parameters are supported:

- password (recommended): modifies the WebCNC's password (default: admin)
- switchConnectionRetries (only use when encountering problems): defines how often the webcnc attempts to connect to a switch before declaring it unreachable (default: 3)
- switchConnectionTimeout (only use when encountering problems): the time in ms that the webcnc waits for a switches response until a retry is attempted (default: 3000)

Using the launch parameters can look like this:
```
java -jar webcnc-1.0.jar --password=myPassword --switchConnectionRetries=5 --switchConnectionTimeout=2000
```

### stopping the backend

If you want to stop the backend, just kill the process (with STRG `+` C on Linux).
If the *server.shutdown* is set to *true* in the application.properties, it will perform a graceful shutdown that finishes all active HTTP requests, but maximally for the duration of the *spring.lifecycle.timeout-per-shutdown-phase* value.
