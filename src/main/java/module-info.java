module com.github.marcinmazurek1.sas {
	requires javafx.controls;
	requires jacorb.omgapi;
	requires java.sql;
	requires log4j;
	requires sas.svc.connection;
	requires sas.oma.omi;
	requires sas.oma.joma.rmt;
	requires sas.svc.connection.platform;
	requires java.rmi;
	opens com.github.marcinmazurek1.sas to javafx.graphics;
}