This is a simple Java JSON-RPC implementation designed to be simple to implement and able to expose public methods in existing POJOs via a robust RPC framework.

For legacy documentation (as this may not yet be complete), see: http://werxltd.com/wp/portfolio/json-rpc/simple-java-json-rpc/

# Project name #
Why jpoxy?

Eric S Raymond wrote in his book, [The Art of Unix Programming](http://www.catb.org/~esr/writings/taoup/) that the secret to the success of the Unix system/culture has been how modular each component is.

In the section titled ["Case Study: C Considered as Thin Glue"](http://www.catb.org/~esr/writings/taoup/html/ch04s03.html#id2899777) Eric writes:
> This history is worth recalling and understanding because C shows us how powerful a clean, minimalist design can be. If Thompson and Ritchie had been less wise, they would have designed a language that did much more, relied on stronger assumptions, never ported satisfactorily off its original hardware platform, and withered away as the world changed out from under it. Instead, C has flourished — and the example Thompson and Ritchie set has influenced the style of Unix development ever since. As the writer, adventurer, artist, and aeronautical engineer Antoine de Saint-Exupéry once put it, writing about the design of airplanes: «La perfection est atteinte non quand il ne reste rien à ajouter, mais quand il ne reste rien à enlever». (_Perfection is attained not when there is nothing more to add, but when there is nothing more to remove_.)

jpoxy is designed to be very thin. We don't expect you to learn a new way of coding before you begin exposing your Java code to the web. Our aim is to be an un-invasive as possible. Our framework allows you to begin building Java-powered web applications in minutes without having to learn or use any unique annotations or funky design patterns.

# Goals #
Jpoxy is designed for rapid application development where Java is the backend in any given web stack. Because of this the goals of this project are:
  * asy to implement. Setup for this package should be kept at a minimum. This includes both development as well as production setup.
  * asy to code in. Application developers using this class should not need to know much about JSON-RPC beyond exposing methods in their code that can be called remotely from other applications via the web.
  * on-invasive. Developers using this implementation should be able to reuse [plain old Java object (POJO)](http://en.wikipedia.org/wiki/Plain_Old_Java_Object) classes as much as possible, making the transport layer of JSON-RPC as transparent as possible.

# Quickstart using Maven #
To quickly begin using this project, you can include it as a dependency in your Maven [pom.xml](http://maven.apache.org/pom.html)

First add the repository:
```
<repositories>
    <repository>
        <id>jpoxy-webdav-repo</id>
        <name>jpoxy maven repository</name>
        <url>http://jpoxy.googlecode.com/svn/maven/repo/</url>
        <layout>default</layout>
    </repository>
</repositories>
```

Then add the dependency:
```
<dependency>
    <groupId>org</groupId>
    <artifactId>jpoxy</artifactId>
    <version>1.0.20</version>
</dependency>
```

From here you should be able to use jpoxy in your web.xml configuration. Check the example for a good template of how to use jpoxy.

# Configuration #
There are a number of configuration options for jpoxy and they are all set via the standard web.xml file.

Here's a sample web.xml setup illistrating all the options
```
<web-app version="2.5" xmlns="http://java.sun.com/xml/ns/javaee"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://java.sun.com/xml/ns/javaee ">

<!-- Servlets -->
	<servlet>
		<servlet-name>example</servlet-name>
		<servlet-class>org.jpoxy.RPC</servlet-class>
                <load-on-startup>1</load-on-startup>
		<init-param>
			<param-name>rpcclasses</param-name>
			<param-value>org.jpoxy.Example</param-value>
			<!-- You can also use an * to process all classes in a package or all classes starting with a certain string. -->
		</init-param>
		<init-param>
			<param-name>expose_methods</param-name>
			<param-value>true</param-value>
		</init-param>
		<init-param>
			<param-name>persist_class</param-name>
			<param-value>true</param-value>
		</init-param>
		<init-param>
			<param-name>detailed_errors</param-name>
			<param-value>true</param-value>
		</init-param>
		<init-param>
			<param-name>use_full_classname</param-name>
			<param-value>false</param-value>
		</init-param>
	</servlet>

	<servlet-mapping>
		<servlet-name>example</servlet-name>
		<url-pattern>/</url-pattern>
	</servlet-mapping>

</web-app>
```
# Questions? #
If you have any questions/comments, feel free to let us know via our [Google group](http://groups.google.com/group/jpoxy)
