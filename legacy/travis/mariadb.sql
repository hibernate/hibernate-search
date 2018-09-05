CREATE DATABASE testingdb;
GRANT ALL ON testingdb.* TO hibernate_user@localhost IDENTIFIED BY 'hibernate_password';
FLUSH PRIVILEGES;
SET GLOBAL binlog_format = 'ROW';
