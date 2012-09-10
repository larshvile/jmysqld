jmysqld - A java wrapper for the mysqld process
===============================================

Allows a Java application to create, start and stop MySQL databases
on-demand.

This little utility was originally inspired by the late MXJ project,
http://dev.mysql.com/doc/connector-mxj/en/connector-mxj.html =).

Testing
-------
The tests for this project requires that the MySQL binaries have been downloaded
to a folder called 'mysql-bin'. This can be done using the 'download-mysql-bin'
script.

* download-mysql-bin 5.5.27 ftp://mirror2.dataphone.se/pub/mysql/Downloads/MySQL-5.5/mysql-5.5.27-linux2.6-x86_64.tar.gz

This command downloads & unpacks the binaries under mysql-bin/5.5.27. The script is set up to not
download the binaries if they already exist, and can thus be used easily in a continous integration
environment.

The tests can then be run against the downloaded binaries by supplying a system property
called mysqlVersion, i.e. "-DmysqlVersion=5.5.27".

Some of the tests require that a JDBC-connection can be opened against the started server instances. The
default port for this connection is 3306, but it can be changed by supplying a "mysqlPort" system property,
i.e. "-DmysqlPort=3307".

