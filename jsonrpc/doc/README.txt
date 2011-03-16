This is a Java implementation of the JSON-RPC () framework, designed to faciliate
the rapid creation and deployment of server-side logic. This project aims at 

Used by:
<repositories>
  <repository>
    <id>werxltd</id>
    <url>http://maven.werxltd.com/</url>
    <snapshots>
      <enabled>true</enabled>
    </snapshots>
    <releases>
      <enabled>true</enabled>
    </releases>
  </repository>
</repositories>

<dependencies>
	<dependency>
		<groupId>com.werxltd</groupId>
		<artifactId>jsonrpc</artifactId>
		<version>1.0-SNAPSHOT</version>
	</dependency>
</dependencies>

Run test server on port 9000 via:
mvn -Djetty.port=9000 jetty:run
