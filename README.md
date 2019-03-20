GUI for test connection with SAS server, execute 4GL commands and export results into a file

Requirements
=

You need necessary libraries ([SAS Drivers for JDBC and SAS/CONNECT](https://support.sas.com/downloads/browse.htm?fil=1&cat=50))
* sas.core.jar
* sas.entities.jar
* sas.oma.joma.jar
* sas.oma.joma.rmt.jar
* sas.oma.omi.jar
* sas.rutil.jar
* sas.security.sspi.jar
* sas.svc.connection.jar
* sas.svc.connection.platform.jar
* sastpj.rutil.jar

Console
=

Install into your Maven Repository
-
```
mvn install:install-file -Dfile=sas.core.jar -DgroupId=com.sas -DartifactId=sas-core -Dversion=9.4 -Dpackaging=jar
mvn install:install-file -Dfile=sas.entities.jar -DgroupId=com.sas -DartifactId=sas-entities -Dversion=9.4 -Dpackaging=jar
mvn install:install-file -Dfile=sas.oma.joma.jar -DgroupId=com.sas -DartifactId=sas-oma-joma -Dversion=9.4 -Dpackaging=jar
mvn install:install-file -Dfile=sas.oma.joma.rmt.jar -DgroupId=com.sas -DartifactId=sas-oma-joma-rmt -Dversion=9.4 -Dpackaging=jar
mvn install:install-file -Dfile=sas.oma.omi.jar -DgroupId=com.sas -DartifactId=sas-oma-omi -Dversion=9.4 -Dpackaging=jar
mvn install:install-file -Dfile=sas.security.sspi.jar -DgroupId=com.sas -DartifactId=sas-security-sspi -Dversion=9.4 -Dpackaging=jar
mvn install:install-file -Dfile=sas.svc.connection.jar -DgroupId=com.sas -DartifactId=sas-svc-connection -Dversion=9.4 -Dpackaging=jar
mvn install:install-file -Dfile=sas.svc.connection.platform.jar -DgroupId=com.sas -DartifactId=sas-svc-connection-platform -Dversion=9.4 -Dpackaging=jar
mvn install:install-file -Dfile=sas.rutil.jar -DgroupId=com.sas -DartifactId=sas-rutil -Dversion=9.4 -Dpackaging=jar
mvn install:install-file -Dfile=sastpj.rutil.jar -DgroupId=com.sas -DartifactId=sastpj-rutil -Dversion=9.4 -Dpackaging=jar
```

Create package
-
```
mvn clean package
```

Run program into `target` folder
-
```
java --module-path mod -cp lib/* -m com.github.marcinmazurek1.sas/com.github.marcinmazurek1.sas.App
```
